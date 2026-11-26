package com.vishalnursery.cashmemo.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishalnursery.cashmemo.data.ocr.GeminiVisionService
import com.vishalnursery.cashmemo.data.repository.CashmemoRepository
import com.vishalnursery.cashmemo.data.repository.PlantRateImportPolicy
import com.vishalnursery.cashmemo.data.repository.SyncMode
import com.vishalnursery.cashmemo.data.repository.SyncScopes
import com.vishalnursery.cashmemo.data.update.UpdateCheckResult
import com.vishalnursery.cashmemo.data.update.UpdateChecker
import com.vishalnursery.cashmemo.domain.model.AiExtractedItem
import com.vishalnursery.cashmemo.domain.model.AiScanPreview
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerPayment
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DashboardStats
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoFilter
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.model.MemoType
import com.vishalnursery.cashmemo.domain.model.OrderStatus
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.RateNameStyle
import com.vishalnursery.cashmemo.domain.model.ScanEngine
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.domain.model.round2
import com.vishalnursery.cashmemo.domain.util.AiTextParser
import com.vishalnursery.cashmemo.domain.util.CustomerMatcher
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.domain.util.ParsedRateRow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

class CashmemoViewModel(
    private val repository: CashmemoRepository,
    private val geminiService: GeminiVisionService
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedHistoryDateFlow = MutableStateFlow("")
    private val historyFilterFlow = MutableStateFlow(MemoFilter.ALL)
    private val historyTabFlow = MutableStateFlow(0)

    private val historyControlsFlow = combine(
        searchQueryFlow,
        selectedHistoryDateFlow,
        historyFilterFlow,
        historyTabFlow
    ) { query, date, filter, tab ->
        HistoryControls(query, date, filter, tab)
    }

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
                repository.ordersFlow,
                repository.paymentsFlow,
                combine(repository.customersFlow, repository.plantRatesFlow) { c, r -> c to r },
                combine(repository.settingsFlow, historyControlsFlow) { s, h -> s to h }
            ) { memos, orders, payments, custRates, settingsControls ->
                val (customers, rates) = custRates
                val (settings, controls) = settingsControls
                val filtered = filterMemos(memos, controls.query, controls.selectedDate, controls.filter)
                val stats = DashboardStats(
                    totalMemos = memos.size,
                    totalSales = round2(memos.sumOf { it.grandTotal }),
                    totalCollected = round2(memos.sumOf { it.paidAmount } + payments.sumOf { it.amount }),
                    totalPendingDues = round2(memos.sumOf { it.effectiveDue })
                )
                QuadrupleResult(
                    memos = memos,
                    orders = orders,
                    payments = payments,
                    filtered = filtered,
                    customers = customers,
                    rates = rates,
                    settings = settings,
                    query = controls.query,
                    selectedDate = controls.selectedDate,
                    filter = controls.filter,
                    historyTab = controls.historyTab,
                    stats = stats
                )
            }.collect { data ->
                _uiState.update { current ->
                    val updatedEditor = if (!initializedEditorNumber && current.editor.editingMemoId == null) {
                        initializedEditorNumber = true
                        current.editor.copy(
                            memoNumber = if (current.editor.isOrder) data.settings.nextOrderNumber.toString() else data.settings.nextMemoNumber.toString(),
                            taxPercentageText = if (data.settings.enableTax && data.settings.defaultTaxPercentage > 0) {
                                formatEditableNumber(data.settings.defaultTaxPercentage)
                            } else ""
                        )
                    } else {
                        current.editor
                    }
                    current.copy(
                        allMemos = data.memos,
                        allOrders = data.orders,
                        payments = data.payments,
                        filteredMemos = data.filtered,
                        customers = data.customers,
                        plantRates = data.rates,
                        settings = data.settings,
                        searchQuery = data.query,
                        selectedHistoryDate = data.selectedDate,
                        historyFilter = data.filter,
                        historyTab = data.historyTab,
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

        // Silent update check once on cold start
        viewModelScope.launch {
            delay(2500)
            checkUpdateSilent()
        }
    }

    // --- Back Stack Navigation (Capped at 20, no duplicate destinations) ---
    private val backStack = ArrayDeque<AppDestination>()

    private fun go(
        destination: AppDestination,
        replaceCurrent: Boolean = false,
        updateState: ((CashmemoUiState) -> CashmemoUiState)? = null
    ) {
        val current = _uiState.value.activeDestination
        if (destination != current) {
            if (!replaceCurrent) {
                backStack.remove(current)
                backStack.addLast(current)
                while (backStack.size > 20) {
                    backStack.removeFirst()
                }
            }
            backStack.remove(destination)
        }

        _uiState.update { state ->
            val updated = updateState?.invoke(state) ?: state
            updated.copy(
                activeDestination = destination,
                canGoBack = backStack.isNotEmpty()
            )
        }
    }

    fun navigateTo(destination: AppDestination) {
        go(destination, replaceCurrent = false)
    }

    fun navigateBack(): Boolean {
        if (backStack.isEmpty()) return false
        val previous = backStack.removeLast()
        _uiState.update {
            it.copy(
                activeDestination = previous,
                canGoBack = backStack.isNotEmpty()
            )
        }
        return true
    }

    fun closeSettings() {
        if (!navigateBack()) {
            go(AppDestination.EDITOR, replaceCurrent = true)
        }
    }

    // --- In-App Updates (GitHub Releases) ---
    private var downloadJob: Job? = null

    private suspend fun checkUpdateSilent() {
        runCatching {
            when (val res = UpdateChecker.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    _uiState.update {
                        it.copy(
                            showStartupUpdateDialog = true,
                            startupUpdateInfo = UpdateState.Available(
                                version = res.version,
                                apkUrl = res.apkUrl,
                                sizeBytes = res.sizeBytes
                            )
                        )
                    }
                }
                else -> { /* Silent on UpToDate or error */ }
            }
        }
    }

    fun checkForUpdatesManual() {
        _uiState.update { it.copy(updateState = UpdateState.Checking) }
        viewModelScope.launch {
            when (val res = UpdateChecker.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    _uiState.update {
                        it.copy(
                            updateState = UpdateState.Available(
                                version = res.version,
                                apkUrl = res.apkUrl,
                                sizeBytes = res.sizeBytes
                            )
                        )
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.update { it.copy(updateState = UpdateState.UpToDate) }
                }
                is UpdateCheckResult.Error -> {
                    _uiState.update { it.copy(updateState = UpdateState.Error(res.message)) }
                }
            }
        }
    }

    fun startDownloadAndInstall(context: Context, available: UpdateState.Available) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updateState = UpdateState.Downloading(
                        progress = 0f,
                        bytesRead = 0L,
                        totalBytes = available.sizeBytes
                    )
                )
            }
            runCatching {
                UpdateChecker.downloadApk(
                    context = context,
                    apkUrl = available.apkUrl,
                    targetVersion = available.version
                ) { bytesRead, totalBytes ->
                    val progress = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                    _uiState.update {
                        it.copy(
                            updateState = UpdateState.Downloading(
                                progress = progress.coerceIn(0f, 1f),
                                bytesRead = bytesRead,
                                totalBytes = totalBytes
                            )
                        )
                    }
                }
            }.onSuccess { apkFile ->
                _uiState.update { it.copy(updateState = UpdateState.ReadyToInstall(apkFile)) }
                UpdateChecker.installApk(context, apkFile)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        updateState = UpdateState.Error(
                            "Download failed: ${err.localizedMessage ?: "Unknown error"}"
                        )
                    )
                }
            }
        }
    }

    fun installDownloadedApk(context: Context, file: File) {
        UpdateChecker.installApk(context, file)
    }

    fun cancelUpdateDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }

    fun dismissStartupUpdateDialog() {
        _uiState.update { it.copy(showStartupUpdateDialog = false) }
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

    fun setRateNameStyle(style: RateNameStyle) {
        val current = _uiState.value.settings
        if (current.rateNameStyle == style) return
        viewModelScope.launch {
            repository.saveSettings(current.copy(rateNameStyle = style))
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
        go(AppDestination.EDITOR) {
            it.copy(
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
        go(AppDestination.EDITOR) {
            it.copy(
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
    fun updateDueDate(value: String) = updateEditor { it.copy(dueDate = value) }
    fun updateOrderStatusInEditor(status: OrderStatus) = updateEditor { it.copy(orderStatus = status) }

    fun toggleMemoType(type: MemoType) {
        val settings = _uiState.value.settings
        updateEditor { state ->
            if (state.editingMemoId != null || state.memoType == type) return@updateEditor state
            val isOrder = type == MemoType.ORDER
            val defaultCashNo = settings.nextMemoNumber.toString()
            val defaultOrderNo = settings.nextOrderNumber.toString()
            val updatedNo = when {
                isOrder && (state.memoNumber.isBlank() || state.memoNumber == defaultCashNo) -> defaultOrderNo
                !isOrder && (state.memoNumber.isBlank() || state.memoNumber == defaultOrderNo) -> defaultCashNo
                else -> state.memoNumber
            }
            val updatedMethod = if (isOrder && state.paymentMethod.equals("Credit", ignoreCase = true)) {
                "Cash"
            } else {
                state.paymentMethod
            }
            val updatedNote = when {
                isOrder && state.paymentStatusNote == "Carry Forward" -> ""
                !isOrder && state.paymentStatusNote.isBlank() -> "Carry Forward"
                else -> state.paymentStatusNote
            }
            state.copy(
                memoType = type,
                memoNumber = updatedNo,
                previousDuesText = if (isOrder) "" else state.previousDuesText,
                dueDate = if (isOrder) state.dueDate else "",
                deliveringOrderId = if (isOrder) null else state.deliveringOrderId,
                paymentMethod = updatedMethod,
                paymentStatusNote = updatedNote
            )
        }
    }

    fun updateCustomerName(value: String) = updateEditor { it.copy(customerName = value) }
    fun updateCustomerMobile(value: String) = updateEditor { it.copy(customerMobile = value) }
    fun updateCustomerAddress(value: String) = updateEditor { it.copy(customerAddress = value) }

    fun applyCustomerSuggestion(customer: CustomerProfile) {
        updateEditor {
            it.copy(
                customerName = customer.name,
                customerMobile = customer.phoneNumber,
                customerAddress = customer.address,
                previousDuesText = if (!it.isOrder && customer.outstandingBalance > 0.0) formatEditableNumber(customer.outstandingBalance) else it.previousDuesText
            )
        }
        checkAndAutoApplyRememberedTier(customer.name, customer.phoneNumber)
    }

    fun lastTierFor(name: String, phone: String, excludeMemoId: String? = null): Boolean? {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isBlank() && cleanPhone.isBlank()) return null

        val memos = _uiState.value.allMemos
        val matchedMemo = memos
            .filter { m -> excludeMemoId == null || m.id != excludeMemoId }
            .filter { m ->
                (cleanPhone.isNotEmpty() && m.customerMobile.trim() == cleanPhone) ||
                    (cleanName.isNotEmpty() && m.customerName.trim().equals(cleanName, ignoreCase = true))
            }
            .maxByOrNull { it.createdAt }

        return matchedMemo?.isWholesale
    }

    private fun applyWholesaleModeInternal(
        state: EditorUiState,
        enabled: Boolean,
        tierTouched: Boolean,
        messageOverride: String? = null
    ): EditorUiState {
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
                    if (plant != null && plant.hasPriceForTier(enabled)) {
                        switchedCount++
                        item.copy(rate = plant.priceForTier(enabled))
                    } else item
                }
            } else {
                val plant = rates.firstOrNull { it.name.trim().equals(item.description.trim(), ignoreCase = true) }
                if (plant != null &&
                    plant.hasPriceForTier(enabled) &&
                    ((plant.retailPrice > 0.0 && item.rate == plant.retailPrice) ||
                        (plant.wholesalePrice > 0.0 && item.rate == plant.wholesalePrice))
                ) {
                    switchedCount++
                    item.copy(rate = plant.priceForTier(enabled))
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
            val msg = messageOverride ?: run {
                val modeName = if (enabled) "Wholesale" else "Retail"
                if (customCount > 0) {
                    "Switched $switchedCount items to $modeName - $customCount custom price kept"
                } else {
                    "Switched $switchedCount items to $modeName"
                }
            }
            _events.emit(UiEvent.ShowMessage(msg))
        }

        return state.copy(
            isWholesaleMode = enabled,
            tierTouchedByUser = tierTouched,
            items = updatedItems,
            rateLinks = updatedLinks
        )
    }

    fun toggleWholesaleMode(enabled: Boolean) {
        updateEditor { state ->
            applyWholesaleModeInternal(state, enabled, tierTouched = true, messageOverride = null)
        }
    }

    fun checkAndAutoApplyRememberedTier(name: String, phone: String) {
        val state = _uiState.value.editor
        if (state.editingMemoId != null || state.tierTouchedByUser) return
        val lastTier = lastTierFor(name, phone) ?: return
        if (state.isWholesaleMode != lastTier) {
            updateEditor { s ->
                val customerLabel = name.trim().ifBlank { "Customer" }
                val tierName = if (lastTier) "Wholesale" else "Retail"
                applyWholesaleModeInternal(
                    s,
                    enabled = lastTier,
                    tierTouched = false,
                    messageOverride = "$customerLabel: $tierName (from last bill)"
                )
            }
        }
    }

    fun addCurrentCustomerToLedger() {
        val editor = _uiState.value.editor
        val name = editor.customerName.trim()
        val mobile = editor.customerMobile.trim()
        val address = editor.customerAddress.trim()
        if (name.isBlank() && mobile.isBlank()) return

        val existing = _uiState.value.customers
        if (CustomerMatcher.findMatch(existing, name, mobile) != null) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Already in Ledger"))
            }
            return
        }

        viewModelScope.launch {
            val created = repository.addRegularCustomer(name, mobile, address)
            _events.emit(UiEvent.ShowMessage("Added ${created.name} to Ledger customers"))
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

    fun moveItemRow(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        updateEditor { state ->
            if (fromIndex !in state.items.indices || toIndex !in state.items.indices) {
                return@updateEditor state
            }
            val mutable = state.items.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            state.copy(items = mutable.mapIndexed { idx, item -> item.copy(serialNo = idx + 1) })
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
            val hasTierPrice = plant?.hasPriceForTier(state.isWholesaleMode) == true
            val isCustom = if (plant != null && hasTierPrice) {
                kotlin.math.abs(rate - plant.priceForTier(state.isWholesaleMode)) > 0.01
            } else {
                false
            }

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
            if (plant != null && plant.hasPriceForTier(state.isWholesaleMode)) {
                val tierPrice = plant.priceForTier(state.isWholesaleMode)
                val updatedItem = item.copy(rate = tierPrice)
                val updatedItems = state.items.mapIndexed { idx, itm -> if (idx == index) updatedItem else itm }
                val updatedLinks = state.rateLinks + (item.id to RateLink(plantRateId = plant.id, customRate = false))
                state.copy(items = updatedItems, rateLinks = updatedLinks)
            } else state
        }
    }

    fun applyPlantRateSuggestion(index: Int, rateItem: PlantRate) {
        val price = rateItem.priceForTier(_uiState.value.editor.isWholesaleMode)
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

        val defaultNo = if (editor.isOrder) settings.nextOrderNumber.toString() else settings.nextMemoNumber.toString()
        val statusNote = if (editor.isOrder) {
            editor.paymentStatusNote.trim()
        } else if (calc.balanceDue > 0.009) {
            editor.paymentStatusNote.ifBlank { "Carry Forward" }
        } else {
            "Fully Paid"
        }

        val memo = CashMemo(
            id = editor.editingMemoId ?: CashMemo.newMemoId(),
            memoNumber = editor.memoNumber.ifBlank { defaultNo },
            date = editor.date.ifBlank { CashMemo.todayFormatted() },
            dueDate = if (editor.isOrder) editor.dueDate.trim() else "",
            customerName = editor.customerName.trim(),
            customerMobile = editor.customerMobile.trim(),
            customerAddress = editor.customerAddress.trim(),
            items = validItems,
            discount = editor.discountText.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0,
            discountType = editor.discountType,
            taxPercentage = calc.taxRate,
            taxAmount = calc.taxAmount,
            previousDues = if (editor.isOrder) 0.0 else calc.previousDues,
            subtotal = calc.subtotal,
            grandTotal = calc.grandTotal,
            paidAmount = calc.effectivePaid,
            balanceDue = calc.balanceDue,
            paymentMethod = editor.paymentMethod,
            paymentStatusNote = statusNote,
            notes = editor.notes,
            rawOcrText = editor.rawOcrText,
            sourceImageUri = editor.sourceImageUri,
            isWholesale = editor.isWholesaleMode,
            memoType = editor.memoType,
            orderStatus = if (editor.isOrder) editor.orderStatus else OrderStatus.PENDING,
            linkedMemoId = if (editor.isOrder) editor.linkedMemoId else "",
            createdAt = editor.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        val linkedOrderId = if (editor.isOrder) null else editor.deliveringOrderId

        viewModelScope.launch {
            repository.saveMemo(memo, linkedOrderId = linkedOrderId)
                .onSuccess { saved ->
                    val label = if (saved.isOrder) "Order" else "Memo"
                    _events.emit(UiEvent.ShowMessage("$label #${saved.memoNumber} saved successfully"))
                    if (printAfterSave) {
                        _events.emit(UiEvent.TriggerPrint(saved, settings))
                    }
                    val targetTab = if (saved.isOrder) 1 else 0
                    historyTabFlow.value = targetTab
                    val nextCashNo = if (!saved.isOrder) {
                        val used = saved.memoNumber.filter { it.isDigit() }.toIntOrNull()
                        if (used != null && used >= settings.nextMemoNumber) used + 1 else settings.nextMemoNumber
                    } else {
                        settings.nextMemoNumber
                    }
                    go(AppDestination.HISTORY, replaceCurrent = true) {
                        it.copy(
                            historyTab = targetTab,
                            editor = EditorUiState(
                                editingMemoId = null,
                                memoType = MemoType.CASH,
                                memoNumber = nextCashNo.toString(),
                                date = CashMemo.todayFormatted(),
                                taxPercentageText = if (settings.enableTax && settings.defaultTaxPercentage > 0) {
                                    formatEditableNumber(settings.defaultTaxPercentage)
                                } else "",
                                rateLinks = emptyMap()
                            )
                        )
                    }
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage("Failed to save: ${err.localizedMessage}"))
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

        val defaultNo = if (editor.isOrder) settings.nextOrderNumber.toString() else settings.nextMemoNumber.toString()
        val statusNote = if (editor.isOrder) {
            editor.paymentStatusNote.trim()
        } else if (calc.balanceDue > 0.009) {
            editor.paymentStatusNote.ifBlank { "Carry Forward" }
        } else {
            "Fully Paid"
        }

        return CashMemo(
            id = editor.editingMemoId ?: CashMemo.newMemoId(),
            memoNumber = editor.memoNumber.ifBlank { defaultNo },
            date = editor.date.ifBlank { CashMemo.todayFormatted() },
            dueDate = if (editor.isOrder) editor.dueDate.trim() else "",
            customerName = editor.customerName.trim(),
            customerMobile = editor.customerMobile.trim(),
            customerAddress = editor.customerAddress.trim(),
            items = validItems,
            discount = editor.discountText.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0,
            discountType = editor.discountType,
            taxPercentage = calc.taxRate,
            taxAmount = calc.taxAmount,
            previousDues = if (editor.isOrder) 0.0 else calc.previousDues,
            subtotal = calc.subtotal,
            grandTotal = calc.grandTotal,
            paidAmount = calc.effectivePaid,
            balanceDue = calc.balanceDue,
            paymentMethod = editor.paymentMethod,
            paymentStatusNote = statusNote,
            notes = editor.notes,
            rawOcrText = editor.rawOcrText,
            sourceImageUri = editor.sourceImageUri,
            isWholesale = editor.isWholesaleMode,
            memoType = editor.memoType,
            orderStatus = if (editor.isOrder) editor.orderStatus else OrderStatus.PENDING,
            linkedMemoId = if (editor.isOrder) editor.linkedMemoId else "",
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

    fun setHistoryTab(tab: Int) {
        historyTabFlow.value = tab
        _uiState.update { it.copy(historyTab = tab) }
    }

    fun editMemo(memo: CashMemo) {
        go(AppDestination.EDITOR) {
            it.copy(
                editor = memo.toEditorState(isNewDraft = false)
            )
        }
    }

    fun duplicateMemo(memo: CashMemo) {
        val nextNum = if (memo.isOrder) {
            _uiState.value.settings.nextOrderNumber.toString()
        } else {
            _uiState.value.settings.nextMemoNumber.toString()
        }
        go(AppDestination.EDITOR) {
            it.copy(
                editor = memo.toEditorState(isNewDraft = true).copy(memoNumber = nextNum)
            )
        }
    }

    fun startDeliverOrder(order: CashMemo) {
        val settings = _uiState.value.settings
        val hasAdvance = order.paidAmount > 0.009
        go(AppDestination.EDITOR) {
            it.copy(
                editor = EditorUiState(
                    editingMemoId = null,
                    memoType = MemoType.CASH,
                    orderStatus = OrderStatus.PENDING,
                    linkedMemoId = "",
                    deliveringOrderId = order.id,
                    memoNumber = settings.nextMemoNumber.toString(),
                    date = CashMemo.todayFormatted(),
                    dueDate = "",
                    customerName = order.customerName,
                    customerMobile = order.customerMobile,
                    customerAddress = order.customerAddress,
                    items = if (order.items.isNotEmpty()) {
                        order.items.mapIndexed { idx, item -> item.copy(id = MemoItem().id, serialNo = idx + 1) }
                    } else {
                        listOf(MemoItem(serialNo = 1))
                    },
                    discountText = if (order.discount > 0.0) formatEditableNumber(order.discount) else "",
                    discountType = order.discountType,
                    taxPercentageText = if (order.taxPercentage > 0.0) formatEditableNumber(order.taxPercentage) else "",
                    previousDuesText = "",
                    paidAmountText = if (hasAdvance) formatEditableNumber(order.paidAmount) else "",
                    hasCustomPayment = hasAdvance,
                    paymentMethod = if (order.paymentMethod.equals("Credit", ignoreCase = true)) "Cash" else order.paymentMethod.ifBlank { "Cash" },
                    paymentStatusNote = "Carry Forward",
                    notes = order.notes,
                    isWholesaleMode = order.isWholesale,
                    tierTouchedByUser = true
                )
            )
        }
        viewModelScope.launch {
            _events.emit(UiEvent.ShowMessage("Converting Order #${order.memoNumber} to Cash Memo"))
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
                .onSuccess { _events.emit(UiEvent.ShowMessage("Order marked ${status.label}")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Update failed: ${err.localizedMessage}")) }
        }
    }

    fun deleteMemo(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMemo(id) }
                .onSuccess { _events.emit(UiEvent.ShowMessage("Memo deleted")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Delete failed: ${err.localizedMessage}")) }
        }
    }

    private fun CashMemo.withEffectiveAmounts(): CashMemo = copy(
        paidAmount = effectivePaid,
        balanceDue = effectiveDue,
        paymentStatusNote = if (hasEffectivePendingDue) paymentStatusNote else "Fully Paid"
    )

    fun printMemo(memo: CashMemo) {
        viewModelScope.launch {
            _events.emit(UiEvent.TriggerPrint(memo.withEffectiveAmounts(), _uiState.value.settings))
        }
    }

    fun shareMemoAsJpeg(memo: CashMemo) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShareMemoImages(memo.withEffectiveAmounts(), _uiState.value.settings))
        }
    }

    fun shareMemoAsPdf(memo: CashMemo) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShareMemoPdf(memo.withEffectiveAmounts(), _uiState.value.settings))
        }
    }

    fun shareMemoSummary(memo: CashMemo) {
        val effective = memo.withEffectiveAmounts()
        val settings = _uiState.value.settings
        val summary = buildString {
            appendLine("${settings.shopNameHindi} (${settings.shopNameEnglish})")
            val docPrefix = if (effective.isOrder) "Order" else "Memo"
            appendLine("$docPrefix #${effective.memoNumber} | Date: ${effective.date}")
            if (effective.isOrder && effective.dueDate.isNotBlank()) {
                appendLine("Expected Delivery: ${effective.dueDate}")
            }
            if (effective.customerName.isNotBlank()) appendLine("Customer: ${effective.customerName}")
            appendLine("--------------------------------")
            for (item in effective.items) {
                appendLine("${item.description} x ${item.quantity} = ${IndianCurrencyFormatter.formatInr(item.amount, settings.currencySymbol)}")
            }
            appendLine("--------------------------------")
            appendLine("Total: ${IndianCurrencyFormatter.formatInr(effective.totalPayable, settings.currencySymbol)}")
            appendLine("${if (effective.isOrder) "Advance Paid" else "Paid"}: ${IndianCurrencyFormatter.formatInr(effective.paidAmount, settings.currencySymbol)}")
            appendLine("Balance: ${IndianCurrencyFormatter.formatInr(effective.balanceDue, settings.currencySymbol)}")
        }
        viewModelScope.launch {
            val filePrefix = if (effective.isOrder) "OrderMemo" else "Cashmemo"
            _events.emit(
                UiEvent.ShareFile(
                    fileName = "${filePrefix}_${effective.memoNumber}.txt",
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
        val initialEditor = EditorUiState(
            editingMemoId = null,
            memoNumber = settings.nextMemoNumber.toString(),
            date = CashMemo.todayFormatted(),
            customerName = customer.name,
            customerMobile = customer.phoneNumber,
            customerAddress = customer.address,
            previousDuesText = if (customer.outstandingBalance > 0.009) formatEditableNumber(customer.outstandingBalance) else "",
            rateLinks = emptyMap(),
            tierTouchedByUser = false
        )
        val lastTier = lastTierFor(customer.name, customer.phoneNumber)
        val finalEditor = if (lastTier != null && lastTier) {
            initialEditor.copy(isWholesaleMode = true)
        } else initialEditor

        go(AppDestination.EDITOR) {
            it.copy(
                editor = finalEditor
            )
        }
        if (lastTier != null && lastTier) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("${customer.name}: Wholesale (from last bill)"))
            }
        }
    }

    fun saveCustomer(customer: CustomerProfile) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            _events.emit(UiEvent.ShowMessage("Customer saved: ${customer.name}"))
        }
    }

    fun receiveCustomerPayment(
        customerId: String,
        amount: Double,
        method: String,
        date: String,
        note: String,
        selectedMemoIds: Set<String>?
    ) {
        viewModelScope.launch {
            repository.recordCustomerPayment(
                customerId = customerId,
                amountPaid = amount,
                promiseNote = note,
                method = method,
                date = date,
                selectedMemoIds = selectedMemoIds
            ).onSuccess {
                _events.emit(UiEvent.ShowMessage("Payment recorded"))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage("Payment failed: ${err.localizedMessage}"))
            }
        }
    }

    fun settleCustomerDue(customerId: String, amount: Double, promiseNote: String) {
        receiveCustomerPayment(
            customerId = customerId,
            amount = amount,
            method = "Cash",
            date = CashMemo.todayFormatted(),
            note = promiseNote,
            selectedMemoIds = null
        )
    }

    fun settleCustomerDue(customer: CustomerProfile, amount: Double, promiseNote: String) {
        settleCustomerDue(customer.id, amount, promiseNote)
    }

    fun deleteCustomerPayment(paymentId: String) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
                .onSuccess { _events.emit(UiEvent.ShowMessage("Payment deleted and balance updated")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Delete failed: ${err.localizedMessage}")) }
        }
    }

    fun recalculateCustomerBalance(customerId: String) {
        viewModelScope.launch {
            repository.recalculateCustomerFromBills(customerId)
                .onSuccess { _events.emit(UiEvent.ShowMessage("Customer balance recalculated from bills")) }
                .onFailure { err -> _events.emit(UiEvent.ShowMessage("Recalculation failed: ${err.localizedMessage}")) }
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch { repository.deleteCustomer(id) }
    }

    fun savePlantRate(rate: PlantRate) {
        viewModelScope.launch {
            repository.savePlantRate(rate)
            val tierSuffix = when {
                rate.wholesalePrice > 0.0 && rate.retailPrice == 0.0 -> " (Wholesale)"
                rate.retailPrice > 0.0 && rate.wholesalePrice == 0.0 -> " (Retail)"
                else -> ""
            }
            _events.emit(UiEvent.ShowMessage("Saved plant rate: ${rate.name}$tierSuffix"))
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

    // --- Plant Rate Scanning & AI Paste (Part 5) ---

    fun scanRateListImage(uri: Uri) {
        val settings = _uiState.value.settings
        if (settings.scanEngine == ScanEngine.OTHER_AI) {
            openRatePaste(uri.toString())
            return
        }
        if (settings.geminiApiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    rateScan = it.rateScan.copy(
                        imageUri = uri.toString(),
                        errorMessage = "Add your Gemini API key in Settings, or use another AI."
                    )
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                rateScan = it.rateScan.copy(
                    isScanning = true,
                    imageUri = uri.toString(),
                    errorMessage = null
                )
            )
        }

        viewModelScope.launch {
            geminiService.analyzeRateList(
                uri = uri,
                apiKey = settings.geminiApiKey,
                preferredModel = settings.geminiModel,
                nameStyle = settings.rateNameStyle
            ).onSuccess { rows ->
                if (rows.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            rateScan = it.rateScan.copy(
                                isScanning = false,
                                errorMessage = "Gemini couldn't find any plants in this image."
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            rateScan = it.rateScan.copy(
                                isScanning = false,
                                pendingRows = rows,
                                pendingSource = RateReviewSource.GEMINI,
                                errorMessage = null
                            )
                        )
                    }
                    _events.emit(UiEvent.ShowMessage("Gemini found ${rows.size} plants. Check the prices before saving."))
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        rateScan = it.rateScan.copy(
                            isScanning = false,
                            errorMessage = mapGeminiError(err)
                        )
                    )
                }
            }
        }
    }

    fun openRatePaste(imageUri: String? = null) {
        _uiState.update { current ->
            val uriToUse = imageUri ?: current.rateScan.imageUri
            current.copy(
                rateScan = current.rateScan.copy(
                    isPasteOpen = true,
                    imageUri = uriToUse,
                    errorMessage = null
                )
            )
        }
    }

    fun closeRatePaste() {
        _uiState.update { it.copy(rateScan = it.rateScan.copy(isPasteOpen = false, pasteText = "")) }
    }

    fun updateRatePasteText(text: String) {
        _uiState.update { it.copy(rateScan = it.rateScan.copy(pasteText = text)) }
    }

    fun submitRatePaste() {
        val text = _uiState.value.rateScan.pasteText
        val rows = AiTextParser.parseRateList(text)
        if (rows.isEmpty()) {
            viewModelScope.launch { _events.emit(UiEvent.ShowMessage("No plants found in the pasted text")) }
        } else {
            _uiState.update {
                it.copy(
                    rateScan = it.rateScan.copy(
                        isPasteOpen = false,
                        pendingRows = rows,
                        pendingSource = RateReviewSource.PASTE
                    )
                )
            }
        }
    }

    fun showRateReview(rows: List<ParsedRateRow>, source: RateReviewSource) {
        _uiState.update {
            it.copy(
                rateScan = it.rateScan.copy(
                    pendingRows = rows,
                    pendingSource = source,
                    errorMessage = null
                )
            )
        }
    }

    fun dismissRateReview() {
        val current = _uiState.value.rateScan
        if (current.pendingSource == RateReviewSource.PASTE) {
            _uiState.update { it.copy(rateScan = it.rateScan.copy(isPasteOpen = true, pendingRows = null)) }
        } else {
            _uiState.update { it.copy(rateScan = it.rateScan.copy(pendingRows = null)) }
        }
    }

    fun confirmRateImport(rows: List<PlantRate>, policy: PlantRateImportPolicy) {
        importPlantRates(rows, policy) {
            _uiState.update {
                it.copy(
                    rateScan = it.rateScan.copy(
                        pendingRows = null,
                        pasteText = "",
                        imageUri = null
                    )
                )
            }
        }
    }

    fun clearRateScanError() {
        _uiState.update { it.copy(rateScan = it.rateScan.copy(errorMessage = null)) }
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

    fun saveSettings(settings: BusinessSettings, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.saveSettings(settings)
                .onSuccess {
                    _events.emit(UiEvent.ShowMessage("Settings saved"))
                    onResult(true)
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage("Error: ${err.localizedMessage}"))
                    onResult(false)
                }
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
                MemoFilter.PENDING_DUE -> memo.hasEffectivePendingDue
                MemoFilter.PAID -> !memo.hasEffectivePendingDue
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
        val hasCustom = isOrder || balanceDue > 0.009 || (paidAmount > 0.0 && kotlin.math.abs(paidAmount - totalPayable) > 0.01)
        return EditorUiState(
            editingMemoId = if (isNewDraft) null else id,
            memoType = memoType,
            orderStatus = if (isNewDraft) OrderStatus.PENDING else orderStatus,
            linkedMemoId = if (isNewDraft) "" else linkedMemoId,
            deliveringOrderId = null,
            memoNumber = memoNumber,
            date = date,
            dueDate = dueDate,
            customerName = customerName,
            customerMobile = customerMobile,
            customerAddress = customerAddress,
            items = if (items.isNotEmpty()) items else listOf(MemoItem(serialNo = 1)),
            discountText = if (discount > 0.0) formatEditableNumber(discount) else "",
            discountType = discountType,
            taxPercentageText = if (taxPercentage > 0.0) formatEditableNumber(taxPercentage) else "",
            previousDuesText = if (!isOrder && previousDues > 0.0) formatEditableNumber(previousDues) else "",
            paidAmountText = if (hasCustom && paidAmount > 0.009) formatEditableNumber(paidAmount) else if (!isOrder && hasCustom) formatEditableNumber(paidAmount) else "",
            hasCustomPayment = hasCustom,
            paymentMethod = paymentMethod,
            paymentStatusNote = paymentStatusNote,
            notes = notes,
            isWholesaleMode = isWholesale,
            tierTouchedByUser = !isNewDraft,
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

    private data class HistoryControls(
        val query: String,
        val selectedDate: String,
        val filter: MemoFilter,
        val historyTab: Int
    )

    private data class QuadrupleResult(
        val memos: List<CashMemo>,
        val orders: List<CashMemo>,
        val payments: List<CustomerPayment>,
        val filtered: List<CashMemo>,
        val customers: List<CustomerProfile>,
        val rates: List<PlantRate>,
        val settings: BusinessSettings,
        val query: String,
        val selectedDate: String,
        val filter: MemoFilter,
        val historyTab: Int,
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
