package com.vishalnursery.cashmemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vishalnursery.cashmemo.ui.components.BillDocumentRenderer
import com.vishalnursery.cashmemo.ui.components.CashmemoPrintHelper
import com.vishalnursery.cashmemo.ui.components.ShareExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.vishalnursery.cashmemo.ui.screens.CatalogLedgerScreen
import com.vishalnursery.cashmemo.ui.screens.HistoryDashboardScreen
import com.vishalnursery.cashmemo.ui.screens.ReviewEditorScreen
import com.vishalnursery.cashmemo.ui.screens.ScannerScreen
import com.vishalnursery.cashmemo.ui.screens.SettingsScreen
import com.vishalnursery.cashmemo.ui.theme.CashmemoTheme
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.AppDestination
import com.vishalnursery.cashmemo.ui.viewmodel.CashmemoViewModel
import com.vishalnursery.cashmemo.ui.viewmodel.UiEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CashmemoApplication
        setContent {
            CashmemoTheme {
                val viewModel: CashmemoViewModel = viewModel(
                    factory = CashmemoViewModel.Factory(app.repository, app.geminiVisionService)
                )
                CashmemoAppRoot(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashmemoAppRoot(viewModel: CashmemoViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.TriggerPrint -> CashmemoPrintHelper.printCashMemo(context, event.memo, event.settings)
                is UiEvent.ShareMemoImages -> {
                    runCatching {
                        val files = withContext(Dispatchers.Default) {
                            BillDocumentRenderer.renderJpegFiles(context, event.memo, event.settings)
                        }
                        ShareExportHelper.shareImageFiles(
                            context = context,
                            files = files,
                            subject = "CashMemo_${event.memo.memoNumber}"
                        )
                    }.onFailure { err ->
                        snackbarHostState.showSnackbar("JPEG export failed: ${err.localizedMessage}")
                    }
                }
                is UiEvent.ShareMemoPdf -> {
                    runCatching {
                        val file = withContext(Dispatchers.Default) {
                            BillDocumentRenderer.renderPdfFile(context, event.memo, event.settings)
                        }
                        ShareExportHelper.sharePdfFile(
                            context = context,
                            file = file,
                            subject = "CashMemo_${event.memo.memoNumber}.pdf"
                        )
                    }.onFailure { err ->
                        snackbarHostState.showSnackbar("PDF export failed: ${err.localizedMessage}")
                    }
                }
                is UiEvent.ShareFile -> ShareExportHelper.shareTextFile(
                    context = context,
                    fileName = event.fileName,
                    mimeType = event.mimeType,
                    content = event.content
                )
            }
        }
    }

    val navItems = listOf(
        AppDestination.SCANNER to Icons.Default.DocumentScanner,
        AppDestination.EDITOR to Icons.Default.EditNote,
        AppDestination.HISTORY to Icons.Default.History,
        AppDestination.LEDGER to Icons.Default.People
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NurseryColors.CreamBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.settings.shopNameHindi,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.settings.shopNameEnglish} • Cashmemo AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NurseryColors.Maroon,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppDestination.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = NurseryColors.PaperSurface) {
                navItems.forEach { (destination, icon) ->
                    NavigationBarItem(
                        selected = uiState.activeDestination == destination,
                        onClick = { viewModel.navigateTo(destination) },
                        icon = { Icon(icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState.activeDestination) {
            AppDestination.SCANNER -> ScannerScreen(
                scannerState = uiState.scanner,
                settings = uiState.settings,
                onSelectScanEngine = viewModel::setScanEngine,
                onSelectTranslationMode = viewModel::setTranslationMode,
                onImageCaptured = viewModel::processImageForOcr,
                onOpenApiKeySetup = viewModel::openApiKeyPrompt,
                onDismissApiKeyPrompt = viewModel::dismissApiKeyPrompt,
                onSaveGeminiConfig = viewModel::saveGeminiConfigAndResumeScan,
                onDetectFlashModels = viewModel::detectAvailableGeminiModels,
                onOpenAiPaste = viewModel::openAiPaste,
                onCloseAiPaste = viewModel::closeAiPaste,
                onUpdateAiPasteText = viewModel::updateAiPasteText,
                onSubmitAiPaste = viewModel::submitAiPaste,
                onConfirmAiReview = viewModel::applyAiReviewToEditor,
                onDismissAiReview = viewModel::dismissAiReviewDialog,
                onOpenReview = viewModel::openReviewFromLastScan,
                onNewBlankMemo = viewModel::startNewBlankMemo,
                modifier = contentModifier
            )

            AppDestination.EDITOR -> ReviewEditorScreen(
                editorState = uiState.editor,
                settings = uiState.settings,
                customers = uiState.customers,
                plantRates = uiState.plantRates,
                onUpdateMemoNumber = viewModel::updateMemoNumber,
                onUpdateDate = viewModel::updateDate,
                onUpdateCustomerName = viewModel::updateCustomerName,
                onUpdateCustomerMobile = viewModel::updateCustomerMobile,
                onUpdateCustomerAddress = viewModel::updateCustomerAddress,
                onSelectCustomer = viewModel::applyCustomerSuggestion,
                onToggleWholesale = viewModel::toggleWholesaleMode,
                onAddItemRow = viewModel::addItemRow,
                onCloneItemRow = viewModel::cloneItemRow,
                onRemoveItemRow = viewModel::removeItemRow,
                onUpdateItemDescription = viewModel::updateItemDescription,
                onUpdateItemQuantity = viewModel::updateItemQuantity,
                onUpdateItemRate = viewModel::updateItemRate,
                onSelectPlantRate = viewModel::applyPlantRateSuggestion,
                onResetItemRate = viewModel::resetItemToSavedRate,
                onSavePlantRate = viewModel::savePlantRate,
                onUpdateDiscount = viewModel::updateDiscount,
                onUpdateDiscountType = viewModel::updateDiscountType,
                onUpdateTaxPercentage = viewModel::updateTaxPercentage,
                onUpdatePreviousDues = viewModel::updatePreviousDues,
                onUpdatePaidAmount = viewModel::updatePaidAmount,
                onUpdatePaymentMethod = viewModel::updatePaymentMethod,
                onUpdatePaymentNote = viewModel::updatePaymentStatusNote,
                onSelectExportLayout = viewModel::setBillExportLayout,
                onSaveMemo = viewModel::saveCurrentMemo,
                onShareAsJpeg = viewModel::shareCurrentEditorAsJpeg,
                onShareAsPdf = viewModel::shareCurrentEditorAsPdf,
                onSavePermanentQr = viewModel::savePermanentUpiConfig,
                onNewMemo = viewModel::startNewBlankMemo,
                modifier = contentModifier
            )

            AppDestination.HISTORY -> HistoryDashboardScreen(
                memos = uiState.filteredMemos,
                stats = uiState.dashboardStats,
                settings = uiState.settings,
                searchQuery = uiState.searchQuery,
                selectedDate = uiState.selectedHistoryDate,
                activeFilter = uiState.historyFilter,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSelectedDateChange = viewModel::updateSelectedHistoryDate,
                onFilterChange = viewModel::updateHistoryFilter,
                onEditMemo = viewModel::editMemo,
                onDuplicateMemo = viewModel::duplicateMemo,
                onPrintMemo = viewModel::printMemo,
                onShareMemo = viewModel::shareMemoSummary,
                onShareMemoJpeg = viewModel::shareMemoAsJpeg,
                onShareMemoPdf = viewModel::shareMemoAsPdf,
                onDeleteMemo = viewModel::deleteMemo,
                onExportCsv = viewModel::exportCsvReport,
                onExportJsonBackup = viewModel::exportJsonBackup,
                modifier = contentModifier
            )

            AppDestination.LEDGER -> CatalogLedgerScreen(
                customers = uiState.customers,
                memos = uiState.allMemos,
                plantRates = uiState.plantRates,
                settings = uiState.settings,
                onSaveCustomer = viewModel::saveCustomer,
                onSettleCustomerDue = viewModel::settleCustomerDue,
                onDeleteCustomer = viewModel::deleteCustomer,
                onCreateBillForCustomer = viewModel::createNewBillForCustomer,
                onEditMemo = viewModel::editMemo,
                onPrintMemo = viewModel::printMemo,
                onShareMemoJpeg = viewModel::shareMemoAsJpeg,
                onShareMemoPdf = viewModel::shareMemoAsPdf,
                onSavePlantRate = viewModel::savePlantRate,
                onDeletePlantRate = viewModel::deletePlantRate,
                onExportPlantRatesJson = viewModel::exportPlantRatesJson,
                onExportPlantRatesCsv = viewModel::exportPlantRatesCsv,
                onImportPlantRates = viewModel::importPlantRates,
                modifier = contentModifier
            )

            AppDestination.SETTINGS -> SettingsScreen(
                settings = uiState.settings,
                isSyncing = uiState.isSyncing,
                syncProgressText = uiState.syncProgressText,
                lastSyncStatus = uiState.lastSyncStatus,
                dryRunPreview = uiState.dryRunPreview,
                availableFlashModels = uiState.scanner.availableFlashModels,
                isDetectingModels = uiState.scanner.isDetectingModels,
                safetySnapshots = viewModel.listSafetySnapshots(),
                onSaveSettings = viewModel::saveSettings,
                onSavePermanentQr = viewModel::savePermanentUpiConfig,
                onDetectFlashModels = viewModel::detectAvailableGeminiModels,
                onTestConnection = viewModel::testConnection,
                onRequestSync = viewModel::requestSyncDryRun,
                onConfirmDryRun = viewModel::confirmDryRun,
                onDismissDryRun = viewModel::dismissDryRun,
                onRestoreSnapshot = viewModel::restoreSnapshotFile,
                onExportJsonBackup = viewModel::exportJsonBackup,
                onImportJsonBackup = viewModel::importBackupJson,
                modifier = contentModifier
            )
        }
    }
}
