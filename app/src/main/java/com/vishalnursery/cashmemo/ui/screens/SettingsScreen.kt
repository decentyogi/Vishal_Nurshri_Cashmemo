package com.vishalnursery.cashmemo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishalnursery.cashmemo.BuildConfig
import com.vishalnursery.cashmemo.data.repository.SyncMode
import com.vishalnursery.cashmemo.data.repository.SyncScopes
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.RateNameStyle
import com.vishalnursery.cashmemo.domain.model.ScanEngine
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.ui.components.LocalBottomOverlayInset
import com.vishalnursery.cashmemo.ui.components.SettingsSection
import com.vishalnursery.cashmemo.ui.components.SettingsSwitchRow
import com.vishalnursery.cashmemo.ui.components.SettingsTextField
import com.vishalnursery.cashmemo.ui.components.SyncActionTile
import com.vishalnursery.cashmemo.ui.components.UpiQrDialog
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.SyncDryRunResult
import com.vishalnursery.cashmemo.ui.viewmodel.UpdateState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: BusinessSettings,
    isSyncing: Boolean,
    syncProgressText: String?,
    lastSyncStatus: String,
    dryRunPreview: SyncDryRunResult?,
    availableFlashModels: List<String>,
    isDetectingModels: Boolean,
    safetySnapshots: List<File>,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSavePermanentQr: (String, String, String) -> Unit,
    onDetectFlashModels: (String?) -> Unit,
    onTestConnection: (String) -> Unit,
    onRequestSync: (SyncMode, SyncScopes) -> Unit,
    onConfirmDryRun: () -> Unit,
    onDismissDryRun: () -> Unit,
    onRestoreSnapshot: (File) -> Unit,
    onExportJsonBackup: () -> Unit,
    onImportJsonBackup: (String) -> Unit,
    updateState: UpdateState = UpdateState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onStartDownloadAndInstall: (UpdateState.Available) -> Unit = {},
    onCancelUpdateDownload: () -> Unit = {},
    onInstallDownloadedApk: (File) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var draft by remember(settings) { mutableStateOf(settings) }
    var nextMemoText by remember(settings.nextMemoNumber) {
        mutableStateOf(settings.nextMemoNumber.toString())
    }

    var syncBills by remember { mutableStateOf(true) }
    var syncCustomers by remember { mutableStateOf(true) }
    var syncRates by remember { mutableStateOf(true) }
    var syncSettings by remember { mutableStateOf(false) }

    var isAdvancedSyncOpen by remember { mutableStateOf(false) }
    var showSnapshotPicker by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var pendingReplaceMode by remember { mutableStateOf<SyncMode?>(null) }
    var replaceConfirmText by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var showQrManager by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Section expanded states (rememberSaveable, only "Shop & bill" starts expanded)
    var shopExpanded by rememberSaveable { mutableStateOf(true) }
    var upiExpanded by rememberSaveable { mutableStateOf(false) }
    var aiExpanded by rememberSaveable { mutableStateOf(false) }
    var syncExpanded by rememberSaveable { mutableStateOf(false) }
    var backupExpanded by rememberSaveable { mutableStateOf(false) }
    var updatesExpanded by rememberSaveable { mutableStateOf(true) }

    val isModified = draft != settings || nextMemoText != settings.nextMemoNumber.toString()
    val bottomOverlayInset = LocalBottomOverlayInset.current
    val density = LocalDensity.current

    DisposableEffect(Unit) {
        onDispose {
            bottomOverlayInset.value = 0.dp
        }
    }

    LaunchedEffect(isModified) {
        if (!isModified) {
            bottomOverlayInset.value = 0.dp
        }
    }

    fun handleBack() {
        if (isModified) {
            showDiscardDialog = true
        } else {
            onClose()
        }
    }

    BackHandler(onBack = { handleBack() })

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("You have unsaved changes in Settings. Do you want to discard them?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        draft = settings
                        nextMemoText = settings.nextMemoNumber.toString()
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.DueRed)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    if (showQrManager) {
        UpiQrDialog(
            initialAmount = 0.0,
            settings = draft,
            billLabel = "",
            initialTab = 0,
            onSavePermanentQr = { vpa, payee, note ->
                draft = draft.copy(
                    upiVpa = vpa,
                    shopNameEnglish = payee.ifBlank { draft.shopNameEnglish },
                    upiDefaultNote = note
                )
                onSavePermanentQr(vpa, payee, note)
            },
            onDismiss = { showQrManager = false }
        )
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let(onImportJsonBackup)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // A. In-Page Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { handleBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NurseryColors.DarkInk
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.DarkInk
                    )
                    Text(
                        text = "Shop, AI scanning, sync and backup",
                        style = MaterialTheme.typography.labelSmall,
                        color = NurseryColors.MutedInk
                    )
                }
            }

            // B.1 Shop & bill Section
            val shopSummary = "${draft.shopNameEnglish} • ${draft.billExportLayout.shortLabel}"
            SettingsSection(
                title = "Shop & bill",
                summary = shopSummary,
                icon = Icons.Default.Store,
                expanded = shopExpanded,
                onToggle = { shopExpanded = !shopExpanded }
            ) {
                SettingsTextField(
                    value = draft.shopNameHindi,
                    onValueChange = { draft = draft.copy(shopNameHindi = it) },
                    label = "Shop name (Hindi)"
                )
                SettingsTextField(
                    value = draft.shopNameEnglish,
                    onValueChange = { draft = draft.copy(shopNameEnglish = it) },
                    label = "Shop name (English)"
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsTextField(
                            value = draft.phone,
                            onValueChange = { draft = draft.copy(phone = it) },
                            label = "Phone",
                            keyboardType = KeyboardType.Phone
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsTextField(
                            value = draft.gstin,
                            onValueChange = { draft = draft.copy(gstin = it) },
                            label = "GSTIN (optional)"
                        )
                    }
                }
                SettingsTextField(
                    value = nextMemoText,
                    onValueChange = { nextMemoText = it.filter { ch -> ch.isDigit() } },
                    label = "Next memo no.",
                    keyboardType = KeyboardType.Number
                )
                SettingsTextField(
                    value = draft.termsNote,
                    onValueChange = { draft = draft.copy(termsNote = it) },
                    label = "Bill footer message",
                    singleLine = false,
                    maxLines = 3
                )

                Text(
                    text = "Default bill layout:",
                    style = MaterialTheme.typography.bodySmall,
                    color = NurseryColors.MutedInk,
                    modifier = Modifier.padding(top = 4.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BillExportLayout.entries.forEach { layout ->
                        FilterChip(
                            selected = draft.billExportLayout == layout,
                            onClick = { draft = draft.copy(billExportLayout = layout) },
                            label = { Text(layout.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NurseryColors.MaroonLight,
                                selectedLabelColor = NurseryColors.Maroon
                            )
                        )
                    }
                }
            }

            // B.2 UPI & QR Section
            val upiSummary = if (draft.upiVpa.isNotBlank()) draft.upiVpa else "Not set"
            SettingsSection(
                title = "UPI & QR",
                summary = upiSummary,
                icon = Icons.Default.QrCode2,
                expanded = upiExpanded,
                onToggle = { upiExpanded = !upiExpanded }
            ) {
                SettingsTextField(
                    value = draft.upiVpa,
                    onValueChange = { draft = draft.copy(upiVpa = it) },
                    label = "UPI ID"
                )
                SettingsTextField(
                    value = draft.upiDefaultNote,
                    onValueChange = { draft = draft.copy(upiDefaultNote = it) },
                    label = "Default UPI note (optional)"
                )
                OutlinedButton(
                    onClick = { showQrManager = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("UPI QR Manager")
                }
            }

            // B.3 AI scanning Section
            val aiSummary = if (draft.scanEngine == ScanEngine.OTHER_AI) {
                "Other AI"
            } else {
                "Gemini • ${draft.geminiModel} • ${if (draft.geminiApiKey.isNotBlank()) "key saved" else "no key"}"
            }
            SettingsSection(
                title = "AI scanning",
                summary = aiSummary,
                icon = Icons.Default.AutoAwesome,
                expanded = aiExpanded,
                onToggle = { aiExpanded = !aiExpanded }
            ) {
                // Segmented Pill for Gemini | Other AI
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NurseryColors.CreamBackground,
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (draft.scanEngine == ScanEngine.GEMINI) NurseryColors.Maroon else Color.Transparent,
                            onClick = { draft = draft.copy(scanEngine = ScanEngine.GEMINI) }
                        ) {
                            Text(
                                text = "Gemini",
                                color = if (draft.scanEngine == ScanEngine.GEMINI) Color.White else NurseryColors.DarkInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (draft.scanEngine == ScanEngine.OTHER_AI) NurseryColors.Maroon else Color.Transparent,
                            onClick = { draft = draft.copy(scanEngine = ScanEngine.OTHER_AI) }
                        ) {
                            Text(
                                text = "Other AI",
                                color = if (draft.scanEngine == ScanEngine.OTHER_AI) Color.White else NurseryColors.DarkInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Used for memo scanning and rate-list scanning",
                    style = MaterialTheme.typography.labelSmall,
                    color = NurseryColors.MutedInk
                )

                // Gemini API Key
                OutlinedTextField(
                    value = draft.geminiApiKey,
                    onValueChange = { draft = draft.copy(geminiApiKey = it) },
                    label = { Text("Gemini API key") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Hide" else "Show"
                            )
                        }
                    },
                    supportingText = {
                        Text("Stored on this phone only", style = MaterialTheme.typography.labelSmall, color = NurseryColors.MutedInk)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Gemini Model + Detect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsTextField(
                            value = draft.geminiModel,
                            onValueChange = { draft = draft.copy(geminiModel = it) },
                            label = "Gemini model"
                        )
                    }
                    OutlinedButton(
                        onClick = { onDetectFlashModels(draft.geminiApiKey) },
                        enabled = !isDetectingModels,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (isDetectingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Detect")
                        }
                    }
                }

                // Quick model chips
                val quickModels = remember(availableFlashModels) {
                    (availableFlashModels + listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.0-flash")).distinct().take(4)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    quickModels.forEach { model ->
                        FilterChip(
                            selected = draft.geminiModel == model,
                            onClick = { draft = draft.copy(geminiModel = model) },
                            label = { Text(model, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NurseryColors.MaroonLight,
                                selectedLabelColor = NurseryColors.Maroon
                            )
                        )
                    }
                }

                // Memo item names chips (TranslationMode)
                Text("Memo item names:", style = MaterialTheme.typography.bodySmall, color = NurseryColors.MutedInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TranslationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = draft.translationMode == mode,
                            onClick = { draft = draft.copy(translationMode = mode) },
                            label = { Text(mode.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NurseryColors.GreenSoft,
                                selectedLabelColor = NurseryColors.GreenHeader
                            )
                        )
                    }
                }

                // Plant names in rate lists chips (RateNameStyle)
                Text("Plant names in rate lists:", style = MaterialTheme.typography.bodySmall, color = NurseryColors.MutedInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RateNameStyle.entries.forEach { style ->
                        FilterChip(
                            selected = draft.rateNameStyle == style,
                            onClick = { draft = draft.copy(rateNameStyle = style) },
                            label = { Text(style.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NurseryColors.GreenSoft,
                                selectedLabelColor = NurseryColors.GreenHeader
                            )
                        )
                    }
                }
            }

            // B.4 Web sync Section
            val serverSummary = if (draft.serverBaseUrl.isNotBlank()) {
                val host = draft.serverBaseUrl.removePrefix("http://").removePrefix("https://").take(25)
                "$host • $lastSyncStatus"
            } else {
                "Not set up"
            }
            SettingsSection(
                title = "Web sync",
                summary = serverSummary,
                icon = Icons.Default.CloudSync,
                expanded = syncExpanded,
                onToggle = { syncExpanded = !syncExpanded }
            ) {
                SettingsTextField(
                    value = draft.serverBaseUrl,
                    onValueChange = { draft = draft.copy(serverBaseUrl = it) },
                    label = "Server URL",
                    placeholder = "http://192.168.1.10:8080"
                )

                OutlinedButton(
                    onClick = {
                        val nextNum = nextMemoText.toIntOrNull() ?: draft.nextMemoNumber
                        onSaveSettings(draft.copy(nextMemoNumber = nextNum))
                        onTestConnection(draft.serverBaseUrl)
                    },
                    enabled = !isSyncing && draft.serverBaseUrl.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test connection")
                }

                Text("Include:", style = MaterialTheme.typography.bodySmall, color = NurseryColors.MutedInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = syncBills,
                        onClick = { syncBills = !syncBills },
                        label = { Text("Bills", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NurseryColors.GreenSoft,
                            selectedLabelColor = NurseryColors.GreenHeader
                        )
                    )
                    FilterChip(
                        selected = syncCustomers,
                        onClick = { syncCustomers = !syncCustomers },
                        label = { Text("Customers", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NurseryColors.GreenSoft,
                            selectedLabelColor = NurseryColors.GreenHeader
                        )
                    )
                    FilterChip(
                        selected = syncRates,
                        onClick = { syncRates = !syncRates },
                        label = { Text("Plant rates", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NurseryColors.GreenSoft,
                            selectedLabelColor = NurseryColors.GreenHeader
                        )
                    )
                    FilterChip(
                        selected = syncSettings,
                        onClick = { syncSettings = !syncSettings },
                        label = { Text("Shop settings", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NurseryColors.GreenSoft,
                            selectedLabelColor = NurseryColors.GreenHeader
                        )
                    )
                }

                val canSync = !isSyncing && draft.serverBaseUrl.isNotBlank()
                val scopes = SyncScopes(syncBills, syncCustomers, syncRates, syncSettings)
                fun triggerSync(mode: SyncMode) {
                    val nextNum = nextMemoText.toIntOrNull() ?: draft.nextMemoNumber
                    onSaveSettings(draft.copy(nextMemoNumber = nextNum))
                    onRequestSync(mode, scopes)
                }

                SyncActionTile(
                    title = "Push to web",
                    description = "Send this phone's data to the web. Adds and updates only.",
                    icon = Icons.Default.CloudUpload,
                    onClick = { triggerSync(SyncMode.PUSH) },
                    enabled = canSync
                )
                SyncActionTile(
                    title = "Pull from web",
                    description = "Bring web data to this phone. Adds and updates only.",
                    icon = Icons.Default.CloudDownload,
                    onClick = { triggerSync(SyncMode.PULL) },
                    enabled = canSync
                )
                SyncActionTile(
                    title = "Sync both ways",
                    description = "Merge both sides. Newest edit wins. Nothing is deleted.",
                    icon = Icons.Default.Sync,
                    onClick = { triggerSync(SyncMode.BOTH_WAYS) },
                    enabled = canSync,
                    isHighlighted = true,
                    badgeText = "Recommended"
                )

                if (isSyncing && syncProgressText != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = syncProgressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = NurseryColors.InkDark
                        )
                    }
                }

                SettingsSwitchRow(
                    title = "Auto-sync bills when saving",
                    subtitle = "Silently sends new bills to web server",
                    checked = draft.autoSyncEnabled,
                    onCheckedChange = { draft = draft.copy(autoSyncEnabled = it) }
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Last sync: $lastSyncStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = NurseryColors.MutedInk,
                        maxLines = 2
                    )
                    if (safetySnapshots.isNotEmpty()) {
                        TextButton(
                            onClick = { showSnapshotPicker = true },
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text("Snapshots (${safetySnapshots.size})", color = NurseryColors.Maroon)
                        }
                    }
                }

                // Collapsed red Advanced block
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NurseryColors.DueRed.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, NurseryColors.DueRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAdvancedSyncOpen = !isAdvancedSyncOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = NurseryColors.DueRed, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Advanced (Destructive Replace)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NurseryColors.DueRed
                                )
                            }
                            Icon(
                                if (isAdvancedSyncOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = NurseryColors.DueRed
                            )
                        }

                        if (isAdvancedSyncOpen) {
                            Text(
                                text = "Use with extreme caution. These actions permanently delete data on one side to make it an exact clone of the other.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NurseryColors.InkDark
                            )

                            OutlinedButton(
                                onClick = {
                                    pendingReplaceMode = SyncMode.DELIBERATE_MAKE_WEB_MATCH_PHONE
                                    replaceConfirmText = ""
                                    showReplaceDialog = true
                                },
                                enabled = canSync,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NurseryColors.DueRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Make web match this phone", fontWeight = FontWeight.Bold)
                                    Text("Deletes web records that are not on this phone", fontSize = 11.sp, color = NurseryColors.MutedInk)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    pendingReplaceMode = SyncMode.DELIBERATE_MAKE_PHONE_MATCH_WEB
                                    replaceConfirmText = ""
                                    showReplaceDialog = true
                                },
                                enabled = canSync,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NurseryColors.DueRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Make this phone match the web", fontWeight = FontWeight.Bold)
                                    Text("Deletes phone records that are not on the web", fontSize = 11.sp, color = NurseryColors.MutedInk)
                                }
                            }
                        }
                    }
                }
            }

            // B.5 Backup & restore Section
            SettingsSection(
                title = "Backup & restore",
                summary = "Export or restore JSON",
                icon = Icons.Default.Backup,
                expanded = backupExpanded,
                onToggle = { backupExpanded = !backupExpanded }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportJsonBackup,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export JSON")
                    }
                    OutlinedButton(
                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore JSON")
                    }
                }
                Text(
                    text = "Export a backup before restoring or syncing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NurseryColors.MutedInk
                )
            }

            // 6. App Updates Section (GitHub Releases)
            SettingsSection(
                title = "App updates",
                summary = "GitHub Releases (v${BuildConfig.VERSION_NAME})",
                icon = Icons.Default.CloudDownload,
                expanded = updatesExpanded,
                onToggle = { updatesExpanded = !updatesExpanded }
            ) {
                Text(
                    text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NurseryColors.DarkInk
                )

                when (updateState) {
                    is UpdateState.Idle -> {
                        Text(
                            text = "Check for official updates directly from GitHub Releases.",
                            style = MaterialTheme.typography.labelSmall,
                            color = NurseryColors.MutedInk
                        )
                    }
                    is UpdateState.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NurseryColors.Maroon)
                            Text(
                                text = "Checking GitHub Releases...",
                                style = MaterialTheme.typography.bodySmall,
                                color = NurseryColors.MutedInk
                            )
                        }
                    }
                    is UpdateState.UpToDate -> {
                        Text(
                            text = "✓ App is up to date (v${BuildConfig.VERSION_NAME})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = NurseryColors.BotanicalGreen
                        )
                    }
                    is UpdateState.Available -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Update available: v${updateState.version}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NurseryColors.BotanicalGreen
                            )
                            if (updateState.sizeBytes > 0) {
                                val sizeMb = String.format(Locale.US, "%.1f MB", updateState.sizeBytes / (1024f * 1024f))
                                Text(
                                    text = "Download size: $sizeMb",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NurseryColors.MutedInk
                                )
                            }
                        }
                    }
                    is UpdateState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val percent = (updateState.progress * 100).toInt()
                            val mbRead = String.format(Locale.US, "%.1f", updateState.bytesRead / (1024f * 1024f))
                            val mbTotal = if (updateState.totalBytes > 0) {
                                String.format(Locale.US, "%.1f MB", updateState.totalBytes / (1024f * 1024f))
                            } else "..."
                            Text(
                                text = "Downloading update: $percent% ($mbRead / $mbTotal)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = NurseryColors.DarkInk
                            )
                            LinearProgressIndicator(
                                progress = { updateState.progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = NurseryColors.BotanicalGreen,
                                trackColor = NurseryColors.OutlineSoft
                            )
                        }
                    }
                    is UpdateState.ReadyToInstall -> {
                        Text(
                            text = "Download complete. Ready to install.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = NurseryColors.BotanicalGreen
                        )
                    }
                    is UpdateState.Error -> {
                        Text(
                            text = updateState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = NurseryColors.DueRed
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (updateState is UpdateState.Available) {
                        Button(
                            onClick = { onStartDownloadAndInstall(updateState) },
                            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download & Install", fontWeight = FontWeight.Bold)
                        }
                    } else if (updateState is UpdateState.ReadyToInstall) {
                        Button(
                            onClick = { onInstallDownloadedApk(updateState.apkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Install Now", fontWeight = FontWeight.Bold)
                        }
                    } else if (updateState is UpdateState.Downloading) {
                        OutlinedButton(
                            onClick = onCancelUpdateDownload,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("Cancel")
                        }
                    }

                    if (updateState !is UpdateState.Downloading) {
                        OutlinedButton(
                            onClick = onCheckForUpdates,
                            enabled = updateState !is UpdateState.Checking,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(if (updateState is UpdateState.Checking) "Checking..." else "Check for updates")
                        }
                    }
                }
            }

            // Small centered muted Build line footer
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Vishal Nursery Cashmemo AI • v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = NurseryColors.MutedInk,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // C. Bottom Unsaved Changes Bar
        AnimatedVisibility(
            visible = isModified,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        if (isModified) {
                            bottomOverlayInset.value = with(density) { size.height.toDp() }
                        }
                    },
                color = NurseryColors.PaperSurface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Unsaved changes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.DarkInk
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                draft = settings
                                nextMemoText = settings.nextMemoNumber.toString()
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Discard")
                        }

                        Button(
                            onClick = {
                                val nextNum = nextMemoText.toIntOrNull() ?: draft.nextMemoNumber
                                val cleanDraft = draft.copy(
                                    geminiApiKey = draft.geminiApiKey.trim(),
                                    geminiModel = draft.geminiModel.trim().ifBlank { "gemini-3.5-flash" },
                                    nextMemoNumber = nextNum
                                )
                                onSaveSettings(cleanDraft)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Save changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showSnapshotPicker && safetySnapshots.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showSnapshotPicker = false },
            title = { Text("Restore From Safety Snapshot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a pre-sync automatic snapshot to restore:", style = MaterialTheme.typography.bodySmall)
                    safetySnapshots.take(5).forEach { file ->
                        val dateFormatted = runCatching {
                            val epoch = file.nameWithoutExtension.removePrefix("snapshot_").toLong()
                            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date(epoch))
                        }.getOrDefault(file.name)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NurseryColors.PaperSurface,
                            border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSnapshotPicker = false
                                    onRestoreSnapshot(file)
                                }
                        ) {
                            Text(
                                text = "Snapshot: $dateFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnapshotPicker = false }) { Text("Close") }
            }
        )
    }

    if (showReplaceDialog && pendingReplaceMode != null) {
        val isWebTarget = pendingReplaceMode == SyncMode.DELIBERATE_MAKE_WEB_MATCH_PHONE
        AlertDialog(
            onDismissRequest = {
                showReplaceDialog = false
                pendingReplaceMode = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = NurseryColors.DueRed) },
            title = {
                Text(
                    text = if (isWebTarget) "Replace Web with Phone?" else "Replace Phone with Web?",
                    fontWeight = FontWeight.Bold,
                    color = NurseryColors.DueRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isWebTarget)
                            "DANGER: This will permanently DELETE records on the server that do not exist on this phone!"
                        else
                            "DANGER: This will permanently DELETE records on this phone that do not exist on the server!",
                        style = MaterialTheme.typography.bodySmall,
                        color = NurseryColors.DueRed
                    )
                    Text(
                        text = "To confirm this operation, type REPLACE in the box below:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = replaceConfirmText,
                        onValueChange = { replaceConfirmText = it },
                        placeholder = { Text("REPLACE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mode = pendingReplaceMode
                        showReplaceDialog = false
                        pendingReplaceMode = null
                        if (mode != null) {
                            onRequestSync(mode, SyncScopes(syncBills, syncCustomers, syncRates, syncSettings))
                        }
                    },
                    enabled = replaceConfirmText.trim() == "REPLACE",
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.DueRed)
                ) {
                    Text("Proceed with Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReplaceDialog = false
                    pendingReplaceMode = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (dryRunPreview != null) {
        SyncDryRunBottomSheet(
            result = dryRunPreview,
            isSyncing = isSyncing,
            progressText = syncProgressText,
            onConfirm = onConfirmDryRun,
            onDismiss = onDismissDryRun
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDryRunBottomSheet(
    result: SyncDryRunResult,
    isSyncing: Boolean,
    progressText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NurseryColors.CreamBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (result.isDeliberateReplace) NurseryColors.DueRed else NurseryColors.Maroon
            )

            if (result.description.isNotBlank()) {
                Text(
                    text = result.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NurseryColors.InkMedium
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NurseryColors.PaperSurface,
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (result.billsToAdd > 0 || result.billsToUpdate > 0 || result.billsToSkipDuplicates > 0) {
                        Text(
                            text = "• Bills: +${result.billsToAdd} new, ~${result.billsToUpdate} update, ${result.billsToSkipDuplicates} skip",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (result.customersToAdd > 0 || result.customersToUpdate > 0) {
                        Text(
                            text = "• Customers: +${result.customersToAdd} new, ~${result.customersToUpdate} update",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (result.ratesToAdd > 0 || result.ratesToUpdate > 0) {
                        Text(
                            text = "• Plant Rates: +${result.ratesToAdd} new, ~${result.ratesToUpdate} update",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (result.billsToDelete > 0 || result.customersToDelete > 0 || result.ratesToDelete > 0) {
                        Text(
                            text = "• Records to DELETE: ${result.billsToDelete} bills, ${result.customersToDelete} customers, ${result.ratesToDelete} rates",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = NurseryColors.DueRed
                        )
                    }
                    if (!result.isDeliberateReplace && result.billsToDelete == 0 && result.customersToDelete == 0 && result.ratesToDelete == 0) {
                        Text(
                            text = "✓ Safe sync: No local data will be deleted.",
                            style = MaterialTheme.typography.labelSmall,
                            color = NurseryColors.BotanicalGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isSyncing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = progressText ?: "Executing sync...",
                        style = MaterialTheme.typography.bodySmall,
                        color = NurseryColors.InkDark
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!result.isAppliedResult) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (result.isDeliberateReplace) NurseryColors.DueRed else NurseryColors.BotanicalGreen
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Proceed with Sync")
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
