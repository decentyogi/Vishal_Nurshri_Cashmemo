package com.vishalnursery.cashmemo.ui.viewmodel

import com.vishalnursery.cashmemo.domain.model.AiScanPreview
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DashboardStats
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoCalculationResult
import com.vishalnursery.cashmemo.domain.model.MemoFilter
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.model.OcrScriptMode
import com.vishalnursery.cashmemo.domain.model.PlantRate

enum class AppDestination(val label: String) {
    SCANNER("Scan"),
    EDITOR("Review"),
    HISTORY("History"),
    LEDGER("Ledger"),
    SETTINGS("Settings")
}

data class EditorUiState(
    val editingMemoId: String? = null,
    val memoNumber: String = "1001",
    val date: String = CashMemo.todayFormatted(),
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val items: List<MemoItem> = listOf(MemoItem(serialNo = 1)),
    val discountText: String = "",
    val discountType: DiscountType = DiscountType.AMOUNT,
    val taxPercentageText: String = "",
    val previousDuesText: String = "",
    val paidAmountText: String = "",
    val hasCustomPayment: Boolean = false,
    val paymentMethod: String = "Cash",
    val paymentStatusNote: String = "Carry Forward",
    val notes: String = "",
    val isWholesaleMode: Boolean = false,
    val rawOcrText: String = "",
    val sourceImageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val calculation: MemoCalculationResult
        get() = MemoCalculationResult.compute(
            items = items,
            discount = discountText.toDoubleOrNull() ?: 0.0,
            discountType = discountType,
            taxPercentage = taxPercentageText.toDoubleOrNull() ?: 0.0,
            previousDues = previousDuesText.toDoubleOrNull() ?: 0.0,
            paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0,
            hasCustomPayment = hasCustomPayment
        )
}

data class ScannerUiState(
    val isProcessingOcr: Boolean = false,
    val processingLabel: String = "Analyzing with Gemini AI...",
    val ocrScriptMode: OcrScriptMode = OcrScriptMode.HINDI_AND_ENGLISH,
    val lastImageUri: String = "",
    val extractedRawText: String = "",
    val pendingAiReview: AiScanPreview? = null,
    val showApiKeyPrompt: Boolean = false,
    val pendingUriForKeySetup: String = "",
    val availableFlashModels: List<String> = emptyList(),
    val isDetectingModels: Boolean = false,
    val errorMessage: String? = null
)

data class CashmemoUiState(
    val activeDestination: AppDestination = AppDestination.SCANNER,
    val settings: BusinessSettings = BusinessSettings(),
    val scanner: ScannerUiState = ScannerUiState(),
    val editor: EditorUiState = EditorUiState(),
    val allMemos: List<CashMemo> = emptyList(),
    val filteredMemos: List<CashMemo> = emptyList(),
    val searchQuery: String = "",
    val historyFilter: MemoFilter = MemoFilter.ALL,
    val dashboardStats: DashboardStats = DashboardStats(),
    val customers: List<CustomerProfile> = emptyList(),
    val plantRates: List<PlantRate> = emptyList(),
    val isSyncing: Boolean = false
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class TriggerPrint(val memo: CashMemo, val settings: BusinessSettings) : UiEvent
    data class ShareFile(val fileName: String, val mimeType: String, val content: String) : UiEvent
}
