package com.vishalnursery.cashmemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vishalnursery.cashmemo.ui.components.BillDocumentRenderer
import com.vishalnursery.cashmemo.ui.components.CashmemoPrintHelper
import com.vishalnursery.cashmemo.ui.components.LocalBottomOverlayInset
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
import com.vishalnursery.cashmemo.ui.viewmodel.RateReviewSource
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
    val bottomOverlayInset = remember { mutableStateOf(0.dp) }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
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
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("JPEG export failed: ${err.localizedMessage}", duration = SnackbarDuration.Short)
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
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("PDF export failed: ${err.localizedMessage}", duration = SnackbarDuration.Short)
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

    CompositionLocalProvider(LocalBottomOverlayInset provides bottomOverlayInset) {
        BackHandler(enabled = uiState.canGoBack) {
            viewModel.navigateBack()
        }

        // Non-blocking Startup Update Dialog
        if (uiState.showStartupUpdateDialog && uiState.startupUpdateInfo != null) {
            val info = uiState.startupUpdateInfo!!
            AlertDialog(
                onDismissRequest = viewModel::dismissStartupUpdateDialog,
                title = { Text("Update available: v${info.version}") },
                text = {
                    Text("A newer version of Vishal Nursery Cashmemo is available. Do you want to download and install the update now?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissStartupUpdateDialog()
                            viewModel.startDownloadAndInstall(context, info)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen)
                    ) {
                        Text("Update now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissStartupUpdateDialog) {
                        Text("Later")
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(bottom = bottomOverlayInset.value)
                        .padding(horizontal = 12.dp)
                        .widthIn(max = 480.dp)
                ) { data ->
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    data.dismiss()
                                    true
                                } else false
                            }
                        ),
                        backgroundContent = {}
                    ) {
                        Snackbar(
                            snackbarData = data,
                            shape = RoundedCornerShape(12.dp),
                            containerColor = NurseryColors.InkDark,
                            contentColor = Color.White
                        )
                    }
                }
            },
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
                    onAddCustomerToLedger = viewModel::addCurrentCustomerToLedger,
                    lastTierForCustomer = { name, phone -> viewModel.lastTierFor(name, phone, uiState.editor.editingMemoId) },
                    onCustomerFieldFocusLost = { viewModel.checkAndAutoApplyRememberedTier(uiState.editor.customerName, uiState.editor.customerMobile) },
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
                    onSavePermanentQr = { vpa, payee, note -> viewModel.savePermanentUpiConfig(vpa, payee, note) },
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
                    rateScan = uiState.rateScan,
                    onSelectScanEngine = viewModel::setScanEngine,
                    onSelectRateNameStyle = viewModel::setRateNameStyle,
                    onScanRateImage = viewModel::scanRateListImage,
                    onOpenRatePaste = viewModel::openRatePaste,
                    onCloseRatePaste = viewModel::closeRatePaste,
                    onUpdateRatePasteText = viewModel::updateRatePasteText,
                    onSubmitRatePaste = viewModel::submitRatePaste,
                    onFileRatesParsed = { rows -> viewModel.showRateReview(rows, RateReviewSource.FILE) },
                    onDismissRateReview = viewModel::dismissRateReview,
                    onConfirmRateImport = viewModel::confirmRateImport,
                    onClearRateScanError = viewModel::clearRateScanError,
                    onOpenSettings = { viewModel.navigateTo(AppDestination.SETTINGS) },
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
                    updateState = uiState.updateState,
                    onCheckForUpdates = viewModel::checkForUpdatesManual,
                    onStartDownloadAndInstall = { available -> viewModel.startDownloadAndInstall(context, available) },
                    onCancelUpdateDownload = viewModel::cancelUpdateDownload,
                    onInstallDownloadedApk = { file -> viewModel.installDownloadedApk(context, file) },
                    onSaveSettings = { s, onResult -> viewModel.saveSettings(s, onResult) },
                    onSavePermanentQr = { vpa, payee, note -> viewModel.savePermanentUpiConfig(vpa, payee, note) },
                    onDetectFlashModels = viewModel::detectAvailableGeminiModels,
                    onTestConnection = viewModel::testConnection,
                    onRequestSync = viewModel::requestSyncDryRun,
                    onConfirmDryRun = viewModel::confirmDryRun,
                    onDismissDryRun = viewModel::dismissDryRun,
                    onRestoreSnapshot = viewModel::restoreSnapshotFile,
                    onExportJsonBackup = viewModel::exportJsonBackup,
                    onImportJsonBackup = viewModel::importBackupJson,
                    onClose = viewModel::closeSettings,
                    modifier = contentModifier
                )
            }
        }
    }
}
