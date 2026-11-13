package com.vishalnursery.cashmemo.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishalnursery.cashmemo.data.ocr.GeminiVisionService
import com.vishalnursery.cashmemo.data.repository.CashmemoRepository
import com.vishalnursery.cashmemo.data.repository.PlantRateImportPolicy
import com.vishalnursery.cashmemo.data.repository.SyncMode
import com.vishalnursery.cashmemo.data.repository.SyncScopes
import com.vishalnursery.cashmemo.domain.model.AiExtractedItem
import com.vishalnursery.cashmemo.domain.model.AiScanPreview
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DashboardStats
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoFilter
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.ScanEngine
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.domain.model.round2
import com.vishalnursery.cashmemo.domain.util.AiTextParser
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CashmemoViewModel(
    private val repository: CashmemoRepository,
    private val geminiService: GeminiVisionService
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedHistoryDateFlow = MutableStateFlow("")
    private val historyFilterFlow = MutableStateFlow(MemoFilter.ALL)

    private val _uiState = MutableStateFlow(CashmemoUiState())
    val uiState: StateFlow<CashmemoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var initializedEditorNumber = false

    init {
        observeStateStreams()
    }

    private fun observeStateStreams() {
        viewModelScope.launch {
            combine(
                repository.memosFlow,
                repository.customersFlow,
                repository.plantRatesFlow,
                repository.settingsFlow,
                repository.settingsFlow.combine(searchQueryFlow) { s, q -> s to q }
            ) { memos, customers, rates, settings, _ ->
                val query = searchQueryFlow.value
                val selectedDate = selectedHistoryDateFlow.value
                val filter = historyFilterFlow.value
                val filtered = filterMemos(memos, query, selectedDate, filter)
                val stats = DashboardStats(
                    totalMemos = memos.size,
                    totalSales = round2(memos.sumOf { it.totalPayable }),
                    totalCollected = round2(memos.sumOf { it.paidAmount }),
                    totalPendingDues = round2(memos.sumOf { it.balanceDue })
                )
                QuadrupleResult(memos, filtered, customers, rates, settings, query, selectedDate, filter, stats)
            }.collect { data ->
                _uiState.update { current ->
                    val updatedEditor = if (!initializedEditorNumber && current.editor.editingMemoId == null) {
                        initializedEditorNumber = true
                        current.editor.copy(
                            memoNumber = data.settings.nextMemoNumber.toString(),
                            taxPercentageText = if (data.settings.enableTax && data.settings.defaultTaxPercentage > 0) {
                                formatEditableNumber(data.settings.defaultTaxPercentage)
                            } else ""
                        )
                    } else {
                        current.editor
                    }
                    current.copy(
                        allMemos = data.memos,
                        filteredMemos = data.filtered,
                        customers = data.customers,
                        plantRates = data.rates,
                        settings = data.settings,
                        searchQuery = data.query,
                        selectedHistoryDate = data.selectedDate,
                        historyFilter = data.filter,
                        dashboardStats = data.stats,
                        editor = updatedEditor
                    )
                }
            }
        }

        // Also observe last sync status
        viewModelScope.launch {
            repository.settingsFlow.collect {
                // Keep settings in sync
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(activeDestination = destination) }
    }

    // --- Scanner, Gemini AI Vision & Quick-Edit Actions ---

    fun setScanEngine(engine: ScanEngine) {
        val current = _uiState.value.settings
        if (current.scanEngine == engine) return
        viewModelScope.launch {
            repository.saveSettings(current.copy(scanEngine = engine))
        }
    }

    fun setTranslationMode(mode: TranslationMode) {
        val current = _uiState.value.settings
        if (current.translationMode == mode) return
        viewModelScope.launch {
            repository.saveSettings(current.copy(translationMode = mode))
        }
    }

    fun openApiKeyPrompt() {
        _uiState.update {
            it.copy(scanner = it.scanner.copy(showApiKeyPrompt = true))
        }
    }

    fun dismissApiKeyPrompt() {
        _uiState.update {
            it.copy(scanner = it.scanner.copy(showApiKeyPrompt = false, pendingUriForKeySetup = ""))
        }
    }

    fun saveGeminiConfigAndResumeScan(apiKey: String, model: String) {
        val cleanKey = apiKey.trim()
        val cleanModel = model.trim().ifBlank { "gemini-3.5-flash" }
        val pendingUri = _uiState.value.scanner.pendingUriForKeySetup
        val updatedSettings = _uiState.value.settings.copy(
            geminiApiKey = cleanKey,
            geminiModel = cleanModel
        )
        viewModelScope.launch {
            repository.saveSettings(updatedSettings)
            _uiState.update {
                it.copy(
                    scanner = it.scanner.copy(
                        showApiKeyPrompt = false,
                        pendingUriForKeySetup = ""
                    )
                )
            }
            if (pendingUri.isNotBlank() && cleanKey.isNotEmpty()) {
                runGeminiScan(Uri.parse(pendingUri), updatedSettings)
            } else {
                _events.emit(UiEvent.ShowMessage("Gemini settings saved"))
            }
        }
    }

    fun detectAvailableGeminiModels(apiKeyOverride: String? = null) {
        val keyToUse = (apiKeyOverride ?: _uiState.value.settings.geminiApiKey).trim()
        if (keyToUse.isEmpty()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Enter your Gemini API key first."))
            }
            return
        }
        _uiState.update { it.copy(scanner = it.scanner.copy(isDetectingModels = true)) }
        viewModelScope.launch {
            geminiService.listAvailableFlashModels(keyToUse)
                .onSuccess { models ->
                    _uiState.update {
                        it.copy(
                            scanner = it.scanner.copy(
                                isDetectingModels = false,
                                availableFlashModels = models
                            )
                        )
                    }
                    val summary = if (models.isEmpty()) "No Flash models returned" else "Found: ${models.take(3).joinToString(", ")}"
                    _events.emit(UiEvent.ShowMessage(summary))
                }
                .onFailure { err ->
                    _uiState.update { it.copy(scanner = it.scanner.copy(isDetectingModels = false)) }
                    _events.emit(UiEvent.ShowMessage(err.localizedMessage ?: "Model check failed"))
                }
        }
    }

    fun processImageForOcr(uri: Uri) {
        val settings = _uiState.value.settings
        if (settings.scanEngine == ScanEngine.OTHER_AI) {
            openAiPaste(uri.toString())
            return
        }
        if (settings.geminiApiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    scanner = it.scanner.copy(
                        lastImageUri = uri.toString(),
                        pendingUriForKeySetup = uri.toString(),
                        showApiKeyPrompt = true,
                        errorMessage = null
                    )
                )
            }
            return
        }
        runGeminiScan(uri, settings)
    }

    private fun runGeminiScan(uri: Uri, settings: BusinessSettings) {
        _uiState.update {
            it.copy(
                scanner = it.scanner.copy(
                    isProcessingOcr = true,
                    processingLabel = "Scanning & Translating (${settings.geminiModel})...",
                    lastImageUri = uri.toString(),
                    errorMessage = null
                )
            )
        }

        viewModelScope.launch {
            geminiService.analyzeReceipt(
                uri = uri,
                apiKey = settings.geminiApiKey,
                preferredModel = settings.geminiModel,
                translationMode = settings.translationMode
            ).onSuccess { preview ->
                // Treat "Gemini succeeded but found no usable items" as failure
                val hasUsableItems = preview.items.any { it.translatedName.isNotBlank() || it.originalText.isNotBlank() }
                if (!hasUsableItems) {
                    _uiState.update {
                        it.copy(
                            scanner = it.scanner.copy(
                                isProcessingOcr = false,
                                errorMessage = "Gemini couldn't find any items in this image."
                            )
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        scanner = it.scanner.copy(
                            isProcessingOcr = false,
                            extractedRawText = preview.rawTranscription,
                            pendingAiReview = preview,
                            errorMessage = null
                        )
                    )
                }
                _events.emit(UiEvent.ShowMessage("AI extracted ${preview.items.size} item(s). Review & confirm!"))
            }.onFailure { err ->
                val errorMsg = mapGeminiError(err)
                _uiState.update {
                    it.copy(
                        scanner = it.scanner.copy(
                            isProcessingOcr = false,
                            errorMessage = errorMsg
                        )
                    )
                }
            }
        }
    }

    private fun mapGeminiError(err: Throwable): String {
        return when {
            err is java.net.UnknownHostException || err is java.io.IOException ->
                "No internet - couldn't reach Gemini."
            err.message?.contains("429") == true || err.message?.contains("limit", ignoreCase = true) == true || err.message?.contains("quota", ignoreCase = true) == true ->
                "Gemini free-tier limit reached. Try later or use another AI."
            err.message?.contains("400") == true || err.message?.contains("403") == true || err.message?.contains("API key", ignoreCase = true) == true ->
                "Gemini rejected the API key."
            err.message?.contains("404") == true || err.message?.contains("not found", ignoreCase = true) == true ->
                "Gemini model not found."
            else -> err.localizedMessage ?: "Gemini scan failed"
        }
    }

    fun openAiPaste(imageUri: String? = null) {
        _uiState.update {
            val uriToUse = imageUri ?: it.scanner.lastImageUri.takeIf { u -> u.isNotBlank() }
            it.copy(
                scanner = it.scanner.copy(
                    isAiPasteOpen = true,
                    aiPasteImageUri = uriToUse,
                    errorMessage = null,
                    showApiKeyPrompt = false
                )
            )
        }
    }

    fun closeAiPaste() {
        _uiState.update { it.copy(scanner = it.scanner.copy(isAiPasteOpen = false)) }
    }

    fun updateAiPasteText(text: String) {
        _uiState.update { it.copy(scanner = it.scanner.copy(aiPasteText = text)) }
    }

    fun submitAiPaste() {
        val state = _uiState.value.scanner
        val preview = AiTextParser.parseMemoText(state.aiPasteText, state.aiPasteImageUri)
        applyAiReviewToEditor(preview)
        _uiState.update {
            it.copy(
                scanner = it.scanner.copy(
                    isAiPasteOpen = false,
                    aiPasteText = ""
                )
            )
        }
    }

    fun dismissAiReviewDialog() {
        _uiState.update { it.copy(scanner = it.scanner.copy(pendingAiReview = null)) }
    }

    fun applyAiReviewToEditor(edited: AiScanPreview) {
        val settings = _uiState.value.settings
        val memoItems = edited.items.mapIndexedNotNull { idx, item ->
            val desc = item.translatedName.trim()
            val qty = item.quantity.toDoubleOrNull() ?: 1.0
            val rate = item.rate.toDoubleOrNull() ?: 0.0
            if (desc.isEmpty() && rate == 0.0) null
            else MemoItem(
                serialNo = idx + 1,
                description = desc,
                quantity = qty,
                rate = rate
            )
        }.ifEmpty { listOf(MemoItem(serialNo = 1)) }
            .mapIndexed { idx, item -> item.copy(serialNo = idx + 1) }

        val hasPaid = edited.paidAmount.isNotBlank()
        _uiState.update {
            it.copy(
                activeDestination = AppDestination.EDITOR,
                scanner = it.scanner.copy(pendingAiReview = null),
                editor = EditorUiState(
                    memoNumber = edited.memoNumber.ifBlank { settings.nextMemoNumber.toString() },
                    date = edited.date.ifBlank { CashMemo.todayFormatted() },
                    customerName = edited.customerName.trim(),
                    customerMobile = edited.customerMobile.trim(),
                    customerAddress = edited.customerAddress.trim(),
                    items = memoItems,
                    discountText = edited.discount,
                    discountType = DiscountType.AMOUNT,
                    taxPercentageText = edited.taxPercentage.ifBlank {
                        if (settings.enableTax && settings.defaultTaxPercentage > 0) {
                            formatEditableNumber(settings.defaultTaxPercentage)
                        } else ""
                    },
                    previousDuesText = edited.previousDues,
                    paidAmountText = edited.paidAmount,
                    hasCustomPayment = hasPaid,
                    rawOcrText = edited.rawTranscription,
                    sourceImageUri = edited.sourceImageUri,
                    rateLinks = emptyMap()
                )
            )
        }
    }

    fun openReviewFromLastScan() {
        val scanner = _uiState.value.scanner
        if (scanner.lastImageUri.isBlank() && scanner.extractedRawText.isBlank()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("No scanned memo yet. Take a photo first!"))
            }
            return
        }
        navigateTo(AppDestination.EDITOR)
    }

    // --- Review Editor Actions ---

    fun startNewBlankMemo() {
        val settings = _uiState.value.settings
        _uiState.update {
            it.copy(
                activeDestination = AppDestination.EDITOR,
                editor = EditorUiState(
                    editingMemoId = null,
                    memoNumber = settings.nextMemoNumber.toString(),
                    date = CashMemo.todayFormatted(),
                    taxPercentageText = if (settings.enableTax && settings.defaultTaxPercentage > 0) {
                        formatEditableNumber(settings.defaultTaxPercentage)
                    } else "",
                    rateLinks = emptyMap()
                )
            )
        }
    }

    fun updateMemoNumber(value: String) = updateEditor { it.copy(memoNumber = value) }
    fun updateDate(value: String) = updateEditor { it.copy(date = value) }
    fun updateCustomerName(value: String) = updateEditor { it.copy(customerName = value) }
    fun updateCustomerMobile(value: String) = updateEditor { it.copy(customerMobile = value) }
    fun updateCustomerAddress(value: String) = updateEditor { it.copy(customerAddress = value) }

    fun applyCustomerSuggestion(customer: CustomerProfile) {
        updateEditor {
            it.copy(
                customerName = customer.name,
                customerMobile = customer.phoneNumber,
                customerAddress = customer.address,
                previousDuesText = if (customer.outstandingBalance > 0.0) formatEditableNumber(customer.outstandingBalance) else it.previousDuesText
            )
        }
    }

    fun toggleWholesaleMode(enabled: Boolean) {
        updateEditor { state ->
            val rates = _uiState.value.plantRates
            var switchedCount = 0
            var customCount = 0

            val updatedItems = state.items.map { item ->
                val link = state.rateLinks[item.id]
                if (link != null) {
                    if (link.customRate) {
                        customCount++
                        item
                    } else {
                        val plant = rates.firstOrNull { it.id == link.plantRateId }
                        if (plant != null) {
                            switchedCount++
                            val newPrice = if (enabled) {
                                if (plant.wholesalePrice > 0.0) plant.wholesalePrice else plant.retailPrice
                            } else plant.retailPrice
                            item.copy(rate = newPrice)
                        } else item
                    }
                } else {
                    // Check unlinked item matching description
                    val plant = rates.firstOrNull { it.name.trim().equals(item.description.trim(), ignoreCase = true) }
                    if (plant != null && (item.rate == plant.retailPrice || (plant.wholesalePrice > 0.0 && item.rate == plant.wholesalePrice))) {
                        switchedCount++
                        val newPrice = if (enabled) {
                            if (plant.wholesalePrice > 0.0) plant.wholesalePrice else plant.retailPrice
                        } else plant.retailPrice
                        item.copy(rate = newPrice)
                    } else {
                        item
                    }
                }
            }

            val updatedLinks = state.rateLinks.toMutableMap()
            for (item in updatedItems) {
                if (!updatedLinks.containsKey(item.id)) {
                    val plant = rates.firstOrNull { it.name.trim().equals(item.description.trim(), ignoreCase = true) }
                    if (plant != null) {
                        updatedLinks[item.id] = RateLink(plantRateId = plant.id, customRate = false)
                    }
                }
            }

            viewModelScope.launch {
                val modeName = if (enabled) "Wholesale" else "Retail"
                val msg = if (customCount > 0) {
                    "Switched $switchedCount items to $modeName - $customCount custom price kept"
                } else {
                    "Switched $switchedCount items to $modeName"
                }
                _events.emit(UiEvent.ShowMessage(msg))
            }

            state.copy(isWholesaleMode = enabled, items = updatedItems, rateLinks = updatedLinks)
        }
    }

    fun addItemRow() {
        updateEditor { state ->
            val updated = state.items + MemoItem(serialNo = state.items.size + 1)
            state.copy(items = updated)
        }
    }

    fun cloneItemRow(index: Int) {
        updateEditor { state ->
            val source = state.items.getOrNull(index) ?: return@updateEditor state
            val cloned = source.copy(id = MemoItem().id)
            val mutable = state.items.toMutableList()
            mutable.add(index + 1, cloned)
            val updatedLinks = state.rateLinks.toMutableMap()
            state.rateLinks[source.id]?.let { link ->
                updatedLinks[cloned.id] = link.copy()
            }
            state.copy(
                items = mutable.mapIndexed { idx, item -> item.copy(serialNo = idx + 1) },
                rateLinks = updatedLinks
            )
        }
    }

    fun removeItemRow(index: Int) {
        updateEditor { state ->
            val item = state.items.getOrNull(index)
            val updatedLinks = if (item != null) state.rateLinks - item.id else state.rateLinks
            if (state.items.size <= 1) {
                state.copy(items = listOf(MemoItem(serialNo = 1)), rateLinks = emptyMap())
            } else {
                val filtered = state.items.filterIndexed { i, _ -> i != index }
                    .mapIndexed { idx, itm -> itm.copy(serialNo = idx + 1) }
                state.copy(items = filtered, rateLinks = updatedLinks)
            }
        }
    }

    fun updateItemDescription(index: Int, desc: String) {
        updateEditor { state ->
            val item = state.items.getOrNull(index) ?: return@updateEditor state
            val link = state.rateLinks[item.id]
            val rates = _uiState.value.plantRates
            val plant = if (link != null) rates.firstOrNull { it.id == link.plantRateId } else null
            val updatedLinks = if (plant != null && !plant.name.trim().equals(desc.trim(), ignoreCase = true)) {
                state.rateLinks - item.id
            } else {
                state.rateLinks
            }
            val updatedItem = item.copy(description = desc)
            val updatedItems = state.items.mapIndexed { idx, itm -> if (idx == index) updatedItem else itm }
            state.copy(items = updatedItems, rateLinks = updatedLinks)
        }
    }

    fun updateItemQuantity(index: Int, qtyText: String) {
        val qty = qtyText.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
        updateItemAt(index) { it.copy(quantity = qty) }
    }

    fun updateItemRate(index: Int, rateText: String) {
        val rate = rateText.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
        updateEditor { state ->
            val item = state.items.getOrNull(index) ?: return@updateEditor state
            val link = state.rateLinks[item.id]
            val rates = _uiState.value.plantRates
            val plant = if (link != null) rates.firstOrNull { it.id == link.plantRateId }
                        else rates.firstOrNull { it.name.trim().equals(item.description.trim(), ignoreCase = true) }
            val tierPrice = if (plant != null) (if (state.isWholesaleMode) plant.wholesalePrice else plant.retailPrice) else null
            val isCustom = tierPrice != null && rate != tierPrice

            val updatedItem = item.copy(rate = rate)
            val updatedItems = state.items.mapIndexed { idx, itm -> if (idx == index) updatedItem else itm }
            val updatedLinks = state.rateLinks.toMutableMap()
            if (plant != null) {
                updatedLinks[item.id] = RateLink(plantRateId = plant.id, customRate = isCustom)
            }
            state.copy(items = updatedItems, rateLinks = updatedLinks)
        }
    }

    fun resetItemToSavedRate(index: Int) {
        updateEditor { state ->
            val item = state.items.getOrNull(index) ?: return@updateEditor state
            val link = state.rateLinks[item.id]
            val rates = _uiState.value.plantRates
            val plant = if (link != null) rates.firstOrNull { it.id == link.plantRateId }
                        else rates.firstOrNull { it.name.trim().equals(item.description.trim(), ignoreCase = true) }
            if (plant != null) {
                val tierPrice = if (state.isWholesaleMode) {
                    if (plant.wholesalePrice > 0.0) plant.wholesalePrice else plant.retailPrice
                } else plant.retailPrice
                val updatedItem = item.copy(rate = tierPrice)
                val updatedItems = state.items.mapIndexed { idx, itm -> if (idx == index) updatedItem else itm }
                val updatedLinks = state.rateLinks + (item.id to RateLink(plantRateId = plant.id, customRate = false))
                state.copy(items = updatedItems, rateLinks = updatedLinks)
            } else state
        }
    }

    fun applyPlantRateSuggestion(index: Int, rateItem: PlantRate) {
        val price = if (_uiState.value.editor.isWholesaleMode) {
            if (rateItem.wholesalePrice > 0.0) rateItem.wholesalePrice else rateItem.retailPrice
        } else rateItem.retailPrice
        updateEditor { state ->
            val item = state.items.getOrNull(index) ?: return@updateEditor state
            val updatedItem = item.copy(description = rateItem.name, rate = price)
            val updatedItems = state.items.mapIndexed { idx, itm -> if (idx == index) updatedItem else itm }
            val updatedLinks = state.rateLinks + (updatedItem.id to RateLink(plantRateId = rateItem.id, customRate = false))
            state.copy(items = updatedItems, rateLinks = updatedLinks)
        }
    }

    fun updateDiscount(value: String) = updateEditor { it.copy(discountText = value) }
    fun updateDiscountType(type: DiscountType) = updateEditor { it.copy(discountType = type) }
    fun updateTaxPercentage(value: String) = updateEditor { it.copy(taxPercentageText = value) }
    fun updatePreviousDues(value: String) = updateEditor { it.copy(previousDuesText = value) }

    fun updatePaidAmount(value: String) {
        updateEditor {
            it.copy(
                paidAmountText = value,
                hasCustomPayment = value.isNotBlank()
            )
        }
    }

    fun updatePaymentMethod(method: String) = updateEditor { it.copy(paymentMethod = method) }
    fun updatePaymentStatusNote(note: String) = updateEditor { it.copy(paymentStatusNote = note) }

    fun setBillExportLayout(layout: BillExportLayout) {
        val current = _uiState.value.settings
        if (current.billExportLayout == layout) return
        viewModelScope.launch {
            repository.saveSettings(current.copy(billExportLayout = layout))
        }
    }

    fun saveCurrentMemo(printAfterSave: Boolean = false) {
        val editor = _uiState.value.editor
        val calc = editor.calculation
        val settings = _uiState.value.settings

        val validItems = editor.items
            .filter { it.description.isNotBlank() || it.rate > 0.0 }
            .ifEmpty { listOf(MemoItem(serialNo = 1, description = "Plant Item", quantity = 1.0, rate = calc.subtotal)) }
            .mapIndexed { idx, item -> item.copy(serialNo = idx + 1) }

        val memo = CashMemo(
            id = editor.editingMemoId ?: CashMemo.newMemoId(),
            memoNumber = editor.memoNumber.ifBlank { settings.nextMemoNumber.toString() },
            date = editor.date.ifBlank { CashMemo.todayFormatted() },
            customerName = editor.customerName.trim(),
            customerMobile = editor.customerMobile.trim(),
            customerAddress = editor.customerAddress.trim(),
            items = validItems,
            discount = editor.discountText.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0,
            discountType = editor.discountType,
            taxPercentage = calc.taxRate,
            taxAmount = calc.taxAmount,
            previousDues = calc.previousDues,
            subtotal = calc.subtotal,
            grandTotal = calc.grandTotal,
            paidAmount = calc.effectivePaid,
            balanceDue = calc.balanceDue,
            paymentMethod = editor.paymentMethod,
            paymentStatusNote = if (calc.balanceDue > 0.009) editor.paymentStatusNote.ifBlank { "Carry Forward" } else "Fully Paid",
            notes = editor.notes,
            rawOcrText = editor.rawOcrText,
            sourceImageUri = editor.sourceImageUri,
            createdAt = editor.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveMemo(memo)
                .onSuccess { saved ->
                    _events.emit(UiEvent.ShowMessage("Memo #${saved.memoNumber} saved successfully"))
                    if (printAfterSave) {
                        _events.emit(UiEvent.TriggerPrint(saved, settings))
                    }
                    _uiState.update {
                        it.copy(
                            activeDestination = AppDestination.HISTORY,
                            editor = EditorUiState(
                                editingMemoId = null,
                                memoNumber = settings.nextMemoNumber.toString(),
                                date = CashMemo.todayFormatted(),
                                rateLinks = emptyMap()
                            )
                        )
                    }
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage("Failed to save memo: ${err.localizedMessage}"))
                }
        }
    }

    private fun buildTransientMemoForExport(): CashMemo {
        val editor = _uiState.value.editor
        val calc = editor.calculation
        val settings = _uiState.value.settings

        val validItems = editor.items
            .filter { it.description.isNotBlank() || it.rate > 0.0 }
            .ifEmpty { listOf(MemoItem(serialNo = 1, description = "Plant Item", quantity = 1.0, rate = calc.subtotal)) }
            .mapIndexed { idx, item -> item.copy(serialNo = idx + 1) }

        return CashMemo(
            id = editor.editingMemoId ?: CashMemo.newMemoId(),
            memoNumber = editor.memoNumber.ifBlank { settings.nextMemoNumber.toString() },
            date = editor.date.ifBlank { CashMemo.todayFormatted() },
            customerName = editor.customerName.trim(),
            customerMobile = editor.customerMobile.trim(),
            customerAddress = editor.customerAddress.trim(),
            items = validItems,
            discount = editor.discountText.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0,
            discountType = editor.discountType,
            taxPercentage = calc.taxRate,
            taxAmount = calc.taxAmount,
            previousDues = calc.previousDues,
            subtotal = calc.subtotal,
            grandTotal = calc.grandTotal,
            paidAmount = calc.effectivePaid,
            balanceDue = calc.balanceDue,
            paymentMethod = editor.paymentMethod,
            paymentStatusNote = if (calc.balanceDue > 0.009) editor.paymentStatusNote.ifBlank { "Carry Forward" } else "Fully Paid",
            notes = editor.notes,
            rawOcrText = editor.rawOcrText,
            sourceImageUri = editor.sourceImageUri,
            createdAt = editor.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun shareCurrentEditorAsJpeg() {
        val memo = buildTransientMemoForExport()
        viewModelScope.launch {
            _events.emit(UiEvent.ShareMemoImages(memo, _uiState.value.settings))
        }
    }

    fun shareCurrentEditorAsPdf() {
        val memo = buildTransientMemoForExport()
        viewModelScope.launch {
            _events.emit(UiEvent.ShareMemoPdf(memo, _uiState.value.settings))
        }
    }

    fun savePermanentUpiConfig(vpa: String, payeeName: String = "", note: String = "") {
        val current = _uiState.value.settings
        viewModelScope.launch {
            repository.saveSettings(
                current.copy(
                    upiVpa = vpa.trim(),
                    shopNameEnglish = if (payeeName.isNotBlank()) payeeName.trim() else current.shopNameEnglish,
                    upiDefaultNote = note.trim()
                )
            ).onSuccess {
                _events.emit(UiEvent.ShowMessage("Store UPI QR configuration saved permanently"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("Could not save QR: ${err.localizedMessage}"))
            }
        }
    }

    fun savePermanentUpiConfig(vpa: String, note: String) {
        savePermanentUpiConfig(vpa, "", note)
    }

    // --- History & Ledger Actions ---

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateSelectedHistoryDate(date: String) {
        selectedHistoryDateFlow.value = date
        _uiState.update { it.copy(selectedHistoryDate = date) }
    }

    fun updateHistoryFilter(filter: MemoFilter) {
        historyFilterFlow.value = filter
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun editMemo(memo: CashMemo) {
        _uiState.update {
            it.copy(
                activeDestination = AppDestination.EDITOR,
                editor = memo.toEditorState(isNewDraft = false)
            )
        }
    }

    fun duplicateMemo(memo: CashMemo) {
        val nextNum = _uiState.value.settings.nextMemoNumber.toString()
        _uiState.update {
            it.copy(
                activeDestination = AppDestination.EDITOR,
                editor = memo.toEditorState(isNewDraft = true).copy(memoNumber = nextNum)
            )
        }
    }

    fun deleteMemo(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMemo(id) }
                .onSuccess { _events.emit(UiEvent.ShowMessage("Memo deleted")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Delete failed: ${err.localizedMessage}")) }
        }
    }

    fun printMemo(memo: CashMemo) {
        viewModelScope.launch {
            _events.emit(UiEvent.TriggerPrint(memo, _uiState.value.settings))
        }
    }

    fun shareMemoAsJpeg(memo: CashMemo) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShareMemoImages(memo, _uiState.value.settings))
        }
    }

    fun shareMemoAsPdf(memo: CashMemo) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShareMemoPdf(memo, _uiState.value.settings))
        }
    }

    fun shareMemoSummary(memo: CashMemo) {
        val settings = _uiState.value.settings
        val summary = buildString {
            appendLine("${settings.shopNameHindi} (${settings.shopNameEnglish})")
            appendLine("Memo #${memo.memoNumber} | Date: ${memo.date}")
            if (memo.customerName.isNotBlank()) appendLine("Customer: ${memo.customerName}")
            appendLine("--------------------------------")
            for (item in memo.items) {
                appendLine("${item.description} x ${item.quantity} = ${IndianCurrencyFormatter.formatInr(item.amount, settings.currencySymbol)}")
            }
            appendLine("--------------------------------")
            appendLine("Total: ${IndianCurrencyFormatter.formatInr(memo.totalPayable, settings.currencySymbol)}")
            appendLine("Paid: ${IndianCurrencyFormatter.formatInr(memo.paidAmount, settings.currencySymbol)}")
            appendLine("Balance: ${IndianCurrencyFormatter.formatInr(memo.balanceDue, settings.currencySymbol)}")
        }
        viewModelScope.launch {
            _events.emit(
                UiEvent.ShareFile(
                    fileName = "Cashmemo_${memo.memoNumber}.txt",
                    mimeType = "text/plain",
                    content = summary
                )
            )
        }
    }

    fun exportCsvReport() {
        viewModelScope.launch {
            runCatching {
                val csv = repository.exportMemosCsv()
                val dateStr = SimpleDateFormat("ddMMyyyy_HHmm", Locale.US).format(Date())
                _events.emit(
                    UiEvent.ShareFile(
                        fileName = "Vishal_Nursery_Sales_$dateStr.csv",
                        mimeType = "text/csv",
                        content = csv
                    )
                )
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("CSV export failed: ${err.localizedMessage}"))
            }
        }
    }

    fun exportJsonBackup() {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportBackupJson()
                val dateStr = SimpleDateFormat("ddMMyyyy_HHmm", Locale.US).format(Date())
                _events.emit(
                    UiEvent.ShareFile(
                        fileName = "Vishal_Nursery_Backup_$dateStr.json",
                        mimeType = "application/json",
                        content = json
                    )
                )
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("Backup failed: ${err.localizedMessage}"))
            }
        }
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            repository.importBackupJson(json)
                .onSuccess { msg -> _events.emit(UiEvent.ShowMessage(msg)) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Import failed: ${err.localizedMessage}")) }
        }
    }

    fun createNewBillForCustomer(customer: CustomerProfile) {
        val settings = _uiState.value.settings
        _uiState.update {
            it.copy(
                activeDestination = AppDestination.EDITOR,
                editor = EditorUiState(
                    editingMemoId = null,
                    memoNumber = settings.nextMemoNumber.toString(),
                    date = CashMemo.todayFormatted(),
                    customerName = customer.name,
                    customerMobile = customer.phoneNumber,
                    customerAddress = customer.address,
                    previousDuesText = if (customer.outstandingBalance > 0.009) formatEditableNumber(customer.outstandingBalance) else "",
                    rateLinks = emptyMap()
                )
            )
        }
    }

    fun saveCustomer(customer: CustomerProfile) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            _events.emit(UiEvent.ShowMessage("Customer saved: ${customer.name}"))
        }
    }

    fun settleCustomerDue(customerId: String, amount: Double, promiseNote: String) {
        viewModelScope.launch {
            runCatching { repository.recordCustomerPayment(customerId, amount, promiseNote) }
                .onSuccess { _events.emit(UiEvent.ShowMessage("Payment recorded")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Payment failed: ${err.localizedMessage}")) }
        }
    }

    fun settleCustomerDue(customer: CustomerProfile, amount: Double, promiseNote: String) {
        settleCustomerDue(customer.id, amount, promiseNote)
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch { repository.deleteCustomer(id) }
    }

    fun savePlantRate(rate: PlantRate) {
        viewModelScope.launch {
            repository.savePlantRate(rate)
            _events.emit(UiEvent.ShowMessage("Saved plant rate: ${rate.name}"))
        }
    }

    fun deletePlantRate(id: String) {
        viewModelScope.launch {
            runCatching { repository.deletePlantRate(id) }
                .onSuccess { _events.emit(UiEvent.ShowMessage("Plant rate deleted")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Delete failed: ${err.localizedMessage}")) }
        }
    }

    fun exportPlantRatesJson() {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportPlantRatesJson()
                val dateStr = SimpleDateFormat("ddMMyyyy_HHmm", Locale.US).format(Date())
                _events.emit(
                    UiEvent.ShareFile(
                        fileName = "Vishal_Nursery_Rates_$dateStr.json",
                        mimeType = "application/json",
                        content = json
                    )
                )
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("Export failed: ${err.localizedMessage}"))
            }
        }
    }

    fun exportPlantRatesCsv() {
        viewModelScope.launch {
            runCatching {
                val csv = repository.exportPlantRatesCsv()
                val dateStr = SimpleDateFormat("ddMMyyyy_HHmm", Locale.US).format(Date())
                _events.emit(
                    UiEvent.ShareFile(
                        fileName = "Vishal_Nursery_Rates_$dateStr.csv",
                        mimeType = "text/csv",
                        content = csv
                    )
                )
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("Export failed: ${err.localizedMessage}"))
            }
        }
    }

    fun importPlantRates(
        rates: List<PlantRate>,
        policy: PlantRateImportPolicy,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.importPlantRates(rates, policy)
                .onSuccess { summary ->
                    val msg = "Imported: ${summary.addedCount} added, ${summary.updatedCount} updated, ${summary.unchangedCount} unchanged"
                    _events.emit(UiEvent.ShowMessage(msg))
                    onComplete(msg)
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage("Import failed: ${err.localizedMessage}"))
                }
        }
    }

    // --- Web Sync Methods (Task D) ---

    fun testConnection(serverUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncProgressText = "Testing connection...") }
            repository.syncManager.testConnection(serverUrl)
                .onSuccess { stats ->
                    _uiState.update { it.copy(isSyncing = false, syncProgressText = null) }
                    _events.emit(UiEvent.ShowMessage("Connected! Server: ${stats.billsCount} bills, ${stats.customersCount} customers, ${stats.ratesCount} plant rates"))
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSyncing = false, syncProgressText = null) }
                    _events.emit(UiEvent.ShowMessage("Connection failed: ${err.localizedMessage ?: "Server unreachable"}"))
                }
        }
    }

    fun requestSyncDryRun(mode: SyncMode, scopes: SyncScopes) {
        val serverUrl = _uiState.value.settings.serverBaseUrl
        if (serverUrl.isBlank()) {
            viewModelScope.launch { _events.emit(UiEvent.ShowMessage("Enter server URL first")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncProgressText = "Analyzing sync preview...") }
            repository.syncManager.computeDryRun(mode, scopes, serverUrl)
                .onSuccess { preview ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncProgressText = null,
                            dryRunPreview = preview.copy(onApply = {
                                executeActualSync(mode, scopes, serverUrl)
                            })
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSyncing = false, syncProgressText = null) }
                    _events.emit(UiEvent.ShowMessage("Preview failed: ${err.localizedMessage}"))
                }
        }
    }

    fun dismissDryRun() {
        _uiState.update { it.copy(dryRunPreview = null) }
    }

    fun confirmDryRun() {
        val preview = _uiState.value.dryRunPreview ?: return
        val applyAction = preview.onApply ?: return
        viewModelScope.launch {
            applyAction()
        }
    }

    private fun executeActualSync(mode: SyncMode, scopes: SyncScopes, serverUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncProgressText = "Starting sync...") }
            repository.syncManager.executeSync(
                mode = mode,
                scopes = scopes,
                serverUrl = serverUrl,
                onProgress = { step ->
                    _uiState.update { it.copy(syncProgressText = step) }
                }
            ).onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressText = null,
                        lastSyncStatus = summary.message,
                        dryRunPreview = SyncDryRunResult(
                            title = "Sync Complete",
                            description = summary.message,
                            billsToAdd = summary.pushedCount,
                            billsToUpdate = summary.pulledCount,
                            billsToSkipDuplicates = summary.skippedDuplicates,
                            billsToDelete = summary.deletedCount,
                            isAppliedResult = true
                        )
                    )
                }
                _events.emit(UiEvent.ShowMessage(summary.message))
            }.onFailure { err ->
                _uiState.update { it.copy(isSyncing = false, syncProgressText = null) }
                _events.emit(UiEvent.ShowMessage("Sync failed: ${err.localizedMessage}"))
            }
        }
    }

    fun restoreSnapshotFile(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncProgressText = "Restoring snapshot...") }
            repository.syncManager.restoreSnapshot(file)
                .onSuccess { msg ->
                    _uiState.update { it.copy(isSyncing = false, syncProgressText = null) }
                    _events.emit(UiEvent.ShowMessage(msg))
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSyncing = false, syncProgressText = null) }
                    _events.emit(UiEvent.ShowMessage("Restore failed: ${err.localizedMessage}"))
                }
        }
    }

    fun listSafetySnapshots(): List<File> = repository.syncManager.listSnapshots()

    fun saveSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
                .onSuccess { _events.emit(UiEvent.ShowMessage("Settings saved")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Error: ${err.localizedMessage}")) }
        }
    }

    fun syncNow(urlOverride: String? = null) {
        val url = urlOverride ?: _uiState.value.settings.serverBaseUrl
        if (url.isBlank()) {
            viewModelScope.launch { _events.emit(UiEvent.ShowMessage("Server URL is empty")) }
            return
        }
        requestSyncDryRun(
            mode = SyncMode.BOTH_WAYS,
            scopes = SyncScopes(syncBills = true, syncCustomers = true, syncRates = true, syncSettings = false)
        )
    }

    // --- Internal Helpers ---

    private fun updateEditor(transform: (EditorUiState) -> EditorUiState) {
        _uiState.update { it.copy(editor = transform(it.editor)) }
    }

    private fun updateItemAt(index: Int, transform: (MemoItem) -> MemoItem) {
        updateEditor { state ->
            val updated = state.items.mapIndexed { idx, item ->
                if (idx == index) transform(item) else item
            }
            state.copy(items = updated)
        }
    }

    private fun filterMemos(
        memos: List<CashMemo>,
        query: String,
        selectedDate: String,
        filter: MemoFilter
    ): List<CashMemo> {
        val q = query.trim().lowercase()
        val d = selectedDate.trim()
        return memos.filter { memo ->
            val matchesFilter = when (filter) {
                MemoFilter.ALL -> true
                MemoFilter.PENDING_DUE -> memo.hasPendingDue
                MemoFilter.PAID -> !memo.hasPendingDue
            }
            if (!matchesFilter) return@filter false
            if (d.isNotEmpty() && memo.date.trim() != d) return@filter false
            if (q.isEmpty()) return@filter true

            memo.memoNumber.lowercase().contains(q) ||
                memo.customerName.lowercase().contains(q) ||
                memo.customerMobile.lowercase().contains(q) ||
                memo.customerAddress.lowercase().contains(q) ||
                memo.date.lowercase().contains(q) ||
                memo.items.any { it.description.lowercase().contains(q) }
        }
    }

    private fun CashMemo.toEditorState(isNewDraft: Boolean): EditorUiState {
        val hasCustom = balanceDue > 0.009 || (paidAmount > 0.0 && kotlin.math.abs(paidAmount - totalPayable) > 0.01)
        return EditorUiState(
            editingMemoId = if (isNewDraft) null else id,
            memoNumber = memoNumber,
            date = date,
            customerName = customerName,
            customerMobile = customerMobile,
            customerAddress = customerAddress,
            items = if (items.isNotEmpty()) items else listOf(MemoItem(serialNo = 1)),
            discountText = if (discount > 0.0) formatEditableNumber(discount) else "",
            discountType = discountType,
            taxPercentageText = if (taxPercentage > 0.0) formatEditableNumber(taxPercentage) else "",
            previousDuesText = if (previousDues > 0.0) formatEditableNumber(previousDues) else "",
            paidAmountText = if (hasCustom) formatEditableNumber(paidAmount) else "",
            hasCustomPayment = hasCustom,
            paymentMethod = paymentMethod,
            paymentStatusNote = paymentStatusNote,
            notes = notes,
            rawOcrText = rawOcrText,
            sourceImageUri = sourceImageUri,
            createdAt = createdAt,
            rateLinks = emptyMap()
        )
    }

    private fun formatEditableNumber(value: Double): String {
        val clean = if (value.isFinite()) value else 0.0
        return if (clean % 1.0 == 0.0) clean.toLong().toString() else clean.toString()
    }

    private data class QuadrupleResult(
        val memos: List<CashMemo>,
        val filtered: List<CashMemo>,
        val customers: List<CustomerProfile>,
        val rates: List<PlantRate>,
        val settings: BusinessSettings,
        val query: String,
        val selectedDate: String,
        val filter: MemoFilter,
        val stats: DashboardStats
    )

    class Factory(
        private val repository: CashmemoRepository,
        private val geminiService: GeminiVisionService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CashmemoViewModel(repository, geminiService) as T
        }
    }
}
