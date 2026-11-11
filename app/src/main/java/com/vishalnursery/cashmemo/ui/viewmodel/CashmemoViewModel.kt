package com.vishalnursery.cashmemo.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishalnursery.cashmemo.data.ocr.GeminiVisionService
import com.vishalnursery.cashmemo.data.ocr.OcrScannerService
import com.vishalnursery.cashmemo.data.repository.CashmemoRepository
import com.vishalnursery.cashmemo.domain.model.AiExtractedItem
import com.vishalnursery.cashmemo.domain.model.AiScanPreview
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DashboardStats
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoFilter
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.model.OcrScriptMode
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.domain.model.round2
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.domain.util.ReceiptParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CashmemoViewModel(
    private val repository: CashmemoRepository,
    private val ocrService: OcrScannerService,
    private val geminiService: GeminiVisionService
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
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
                combine(searchQueryFlow, historyFilterFlow) { q, f -> q to f }
            ) { memos, customers, rates, settings, queryAndFilter ->
                val (query, filter) = queryAndFilter
                val filtered = filterMemos(memos, query, filter)
                val stats = DashboardStats(
                    totalMemos = memos.size,
                    totalSales = round2(memos.sumOf { it.totalPayable }),
                    totalCollected = round2(memos.sumOf { it.paidAmount }),
                    totalPendingDues = round2(memos.sumOf { it.balanceDue })
                )
                QuadrupleResult(memos, filtered, customers, rates, settings, query, filter, stats)
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
                        historyFilter = data.filter,
                        dashboardStats = data.stats,
                        editor = updatedEditor
                    )
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(activeDestination = destination) }
    }

    // --- Scanner, Gemini AI Vision & Quick-Edit Actions ---

    fun setOcrScriptMode(mode: OcrScriptMode) {
        _uiState.update { it.copy(scanner = it.scanner.copy(ocrScriptMode = mode)) }
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

    fun runOfflineMlKitFallback(uri: Uri) {
        val scriptMode = _uiState.value.scanner.ocrScriptMode
        _uiState.update {
            it.copy(
                scanner = it.scanner.copy(
                    isProcessingOcr = true,
                    processingLabel = "Running Offline ML Kit OCR...",
                    showApiKeyPrompt = false,
                    pendingUriForKeySetup = "",
                    lastImageUri = uri.toString(),
                    errorMessage = null
                )
            )
        }

        viewModelScope.launch {
            ocrService.recognizeTextFromUri(uri, scriptMode)
                .onSuccess { text ->
                    if (text.isBlank()) {
                        _uiState.update {
                            it.copy(
                                scanner = it.scanner.copy(
                                    isProcessingOcr = false,
                                    extractedRawText = "",
                                    errorMessage = "No readable text detected offline. Use Gemini AI for handwritten notes."
                                )
                            )
                        }
                    } else {
                        val settings = _uiState.value.settings
                        val extracted = ReceiptParser.parse(text)
                        val draft = ReceiptParser.toDraftMemo(
                            extracted = extracted,
                            defaultMemoNumber = settings.nextMemoNumber.toString(),
                            sourceImageUri = uri.toString()
                        )
                        val preview = AiScanPreview(
                            memoNumber = draft.memoNumber,
                            date = draft.date,
                            customerName = draft.customerName,
                            customerMobile = draft.customerMobile,
                            customerAddress = draft.customerAddress,
                            items = draft.items.map {
                                AiExtractedItem(
                                    originalText = it.description,
                                    translatedName = it.description,
                                    quantity = formatEditableNumber(it.quantity),
                                    rate = formatEditableNumber(it.rate)
                                )
                            },
                            rawTranscription = text,
                            warnings = "Offline ML Kit used. Verify handwriting carefully.",
                            modelUsed = "ML Kit Offline",
                            sourceImageUri = uri.toString()
                        )
                        _uiState.update {
                            it.copy(
                                scanner = it.scanner.copy(
                                    isProcessingOcr = false,
                                    extractedRawText = text,
                                    pendingAiReview = preview,
                                    errorMessage = null
                                )
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            scanner = it.scanner.copy(
                                isProcessingOcr = false,
                                errorMessage = err.localizedMessage ?: "Offline OCR failed"
                            )
                        )
                    }
                }
        }
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
                _uiState.update {
                    it.copy(
                        scanner = it.scanner.copy(
                            isProcessingOcr = false,
                            errorMessage = err.localizedMessage ?: "Gemini scan failed"
                        )
                    )
                }
            }
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
                    sourceImageUri = edited.sourceImageUri
                )
            )
        }
        viewModelScope.launch {
            _events.emit(UiEvent.ShowMessage("Pasted ${memoItems.size} item(s) into Cash Memo. Edit & Save!"))
        }
    }

    fun openReviewFromLastScan() {
        val pending = _uiState.value.scanner.pendingAiReview
        if (pending != null) {
            applyAiReviewToEditor(pending)
        } else {
            navigateTo(AppDestination.EDITOR)
        }
    }

    fun startNewBlankMemo() {
        val settings = _uiState.value.settings
        _uiState.update {
            it.copy(
                activeDestination = AppDestination.EDITOR,
                editor = EditorUiState(
                    memoNumber = settings.nextMemoNumber.toString(),
                    taxPercentageText = if (settings.enableTax && settings.defaultTaxPercentage > 0) {
                        formatEditableNumber(settings.defaultTaxPercentage)
                    } else ""
                )
            )
        }
    }

    // --- Editor Actions (Unidirectional Data Flow) ---

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
                previousDuesText = if (customer.outstandingBalance > 0.009) {
                    formatEditableNumber(customer.outstandingBalance)
                } else ""
            )
        }
    }

    fun toggleWholesaleMode(enabled: Boolean) = updateEditor { it.copy(isWholesaleMode = enabled) }

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
            state.copy(items = mutable.mapIndexed { idx, item -> item.copy(serialNo = idx + 1) })
        }
    }

    fun removeItemRow(index: Int) {
        updateEditor { state ->
            if (state.items.size <= 1) {
                state.copy(items = listOf(MemoItem(serialNo = 1)))
            } else {
                val filtered = state.items.filterIndexed { i, _ -> i != index }
                    .mapIndexed { idx, item -> item.copy(serialNo = idx + 1) }
                state.copy(items = filtered)
            }
        }
    }

    fun updateItemDescription(index: Int, desc: String) {
        updateItemAt(index) { it.copy(description = desc) }
    }

    fun updateItemQuantity(index: Int, qtyText: String) {
        val qty = qtyText.toDoubleOrNull() ?: 0.0
        updateItemAt(index) { it.copy(quantity = qty) }
    }

    fun updateItemRate(index: Int, rateText: String) {
        val rate = rateText.toDoubleOrNull() ?: 0.0
        updateItemAt(index) { it.copy(rate = rate) }
    }

    fun applyPlantRateSuggestion(index: Int, rateItem: PlantRate) {
        val price = if (_uiState.value.editor.isWholesaleMode) rateItem.wholesalePrice else rateItem.retailPrice
        updateItemAt(index) { it.copy(description = rateItem.name, rate = price) }
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

    fun saveCurrentMemo(printAfterSave: Boolean = false) {
        val editor = _uiState.value.editor
        val validItems = editor.items.filter { it.description.isNotBlank() || it.amount > 0.0 }
        if (validItems.isEmpty()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Add at least one item before saving."))
            }
            return
        }

        val calc = editor.calculation
        val memoToSave = CashMemo(
            id = editor.editingMemoId ?: CashMemo.newMemoId(),
            memoNumber = editor.memoNumber.ifBlank { _uiState.value.settings.nextMemoNumber.toString() },
            date = editor.date.ifBlank { CashMemo.todayFormatted() },
            customerName = editor.customerName.trim(),
            customerMobile = editor.customerMobile.trim(),
            customerAddress = editor.customerAddress.trim(),
            items = validItems.mapIndexed { idx, item -> item.copy(serialNo = idx + 1) },
            discount = editor.discountText.toDoubleOrNull() ?: 0.0,
            discountType = editor.discountType,
            taxPercentage = calc.taxRate,
            taxAmount = calc.taxAmount,
            previousDues = calc.previousDues,
            subtotal = calc.subtotal,
            grandTotal = calc.grandTotal,
            paidAmount = calc.effectivePaid,
            balanceDue = calc.balanceDue,
            paymentMethod = editor.paymentMethod,
            paymentStatusNote = if (calc.balanceDue > 0.009) {
                editor.paymentStatusNote.ifBlank { "Carry Forward" }
            } else "Fully Paid",
            notes = editor.notes.trim(),
            rawOcrText = editor.rawOcrText,
            sourceImageUri = editor.sourceImageUri,
            createdAt = editor.createdAt
        )

        viewModelScope.launch {
            repository.saveMemo(memoToSave)
                .onSuccess { saved ->
                    updateEditor { it.copy(editingMemoId = saved.id) }
                    val msg = if (saved.isSynced) {
                        "Memo #${saved.memoNumber} saved & synced!"
                    } else {
                        "Memo #${saved.memoNumber} saved offline!"
                    }
                    _events.emit(UiEvent.ShowMessage(msg))
                    if (printAfterSave) {
                        _events.emit(UiEvent.TriggerPrint(saved, _uiState.value.settings))
                    }
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage("Save failed: ${err.localizedMessage}"))
                }
        }
    }

    // --- History & Export Actions ---

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun updateHistoryFilter(filter: MemoFilter) {
        historyFilterFlow.value = filter
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
        val duplicated = memo.copy(
            id = CashMemo.newMemoId(),
            memoNumber = nextNum,
            date = CashMemo.todayFormatted(),
            createdAt = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                activeDestination = AppDestination.EDITOR,
                editor = duplicated.toEditorState(isNewDraft = true)
            )
        }
        viewModelScope.launch {
            _events.emit(UiEvent.ShowMessage("Duplicated as Memo #$nextNum"))
        }
    }

    fun deleteMemo(id: String) {
        viewModelScope.launch {
            repository.deleteMemo(id)
            _events.emit(UiEvent.ShowMessage("Cash memo deleted"))
        }
    }

    fun printMemo(memo: CashMemo) {
        viewModelScope.launch {
            _events.emit(UiEvent.TriggerPrint(memo, _uiState.value.settings))
        }
    }

    fun shareMemoSummary(memo: CashMemo) {
        val settings = _uiState.value.settings
        val summary = buildString {
            appendLine("${settings.shopNameEnglish} (${settings.shopNameHindi})")
            appendLine("Cash Memo #${memo.memoNumber} | Date: ${memo.date}")
            if (memo.customerName.isNotBlank()) appendLine("Customer: ${memo.customerName}")
            if (memo.customerMobile.isNotBlank()) appendLine("Mobile: ${memo.customerMobile}")
            appendLine("--------------------------------")
            memo.items.forEachIndexed { idx, item ->
                appendLine("${idx + 1}. ${item.description} (${item.quantity} x ${item.rate}) = ${IndianCurrencyFormatter.formatInr(item.amount)}")
            }
            appendLine("--------------------------------")
            appendLine("Grand Total: ${IndianCurrencyFormatter.formatInr(memo.grandTotal)}")
            if (memo.previousDues > 0) appendLine("Previous Dues: ${IndianCurrencyFormatter.formatInr(memo.previousDues)}")
            appendLine("Paid (${memo.paymentMethod}): ${IndianCurrencyFormatter.formatInr(memo.paidAmount)}")
            if (memo.hasPendingDue) {
                appendLine("Carry Forward: ${IndianCurrencyFormatter.formatInr(memo.balanceDue)}")
            }
            appendLine(settings.termsNote)
        }
        viewModelScope.launch {
            _events.emit(UiEvent.ShareFile("Memo_${memo.memoNumber}.txt", "text/plain", summary))
        }
    }

    fun exportCsvReport() {
        viewModelScope.launch {
            val csv = repository.exportMemosCsv()
            _events.emit(UiEvent.ShareFile("VishalNursery_Memos.csv", "text/csv", csv))
        }
    }

    fun exportJsonBackup() {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            _events.emit(UiEvent.ShareFile("VishalNursery_Backup_v6.json", "application/json", json))
        }
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            repository.importBackupJson(json)
                .onSuccess { msg -> _events.emit(UiEvent.ShowMessage(msg)) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Import failed: ${err.localizedMessage}")) }
        }
    }

    // --- Customers & Plant Rates ---

    fun saveCustomer(customer: CustomerProfile) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            _events.emit(UiEvent.ShowMessage("Saved customer ${customer.name}"))
        }
    }

    fun settleCustomerDue(customerId: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, note)
            _events.emit(UiEvent.ShowMessage("Recorded payment of ${IndianCurrencyFormatter.formatInr(amount)}"))
        }
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
        viewModelScope.launch { repository.deletePlantRate(id) }
    }

    // --- Settings & Backend Sync ---

    fun saveSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
                .onSuccess { _events.emit(UiEvent.ShowMessage("Settings saved")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Error: ${err.localizedMessage}")) }
        }
    }

    fun syncNow(urlOverride: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            repository.syncWithBackend(urlOverride)
                .onSuccess { msg -> _events.emit(UiEvent.ShowMessage(msg)) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Sync failed: ${err.localizedMessage}")) }
            _uiState.update { it.copy(isSyncing = false) }
        }
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

    private fun filterMemos(memos: List<CashMemo>, query: String, filter: MemoFilter): List<CashMemo> {
        val q = query.trim().lowercase()
        return memos.filter { memo ->
            val matchesFilter = when (filter) {
                MemoFilter.ALL -> true
                MemoFilter.PENDING_DUE -> memo.hasPendingDue
                MemoFilter.PAID -> !memo.hasPendingDue
            }
            if (!matchesFilter) return@filter false
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
            paidAmountText = if (hasCustom && paidAmount > 0.0) formatEditableNumber(paidAmount) else "",
            hasCustomPayment = hasCustom,
            paymentMethod = paymentMethod,
            paymentStatusNote = paymentStatusNote,
            notes = notes,
            rawOcrText = rawOcrText,
            sourceImageUri = sourceImageUri,
            createdAt = createdAt
        )
    }

    private fun formatEditableNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    private data class QuadrupleResult(
        val memos: List<CashMemo>,
        val filtered: List<CashMemo>,
        val customers: List<CustomerProfile>,
        val rates: List<PlantRate>,
        val settings: BusinessSettings,
        val query: String,
        val filter: MemoFilter,
        val stats: DashboardStats
    )

    class Factory(
        private val repository: CashmemoRepository,
        private val ocrService: OcrScannerService,
        private val geminiService: GeminiVisionService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CashmemoViewModel(repository, ocrService, geminiService) as T
        }
    }
}
