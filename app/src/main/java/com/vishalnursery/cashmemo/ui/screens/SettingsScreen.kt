package com.vishalnursery.cashmemo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vishalnursery.cashmemo.BuildConfig
import com.vishalnursery.cashmemo.data.repository.SyncMode
import com.vishalnursery.cashmemo.data.repository.SyncScopes
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.ui.components.UpiQrDialog
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.SyncDryRunResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var syncBills by remember { mutableStateOf(true) }
    var syncCustomers by remember { mutableStateOf(true) }
    var syncRates by remember { mutableStateOf(true) }
    var syncSettings by remember { mutableStateOf(false) }

    var isAdvancedSyncOpen by remember { mutableStateOf(false) }
    var showSnapshotPicker by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var pendingReplaceMode by remember { mutableStateOf<SyncMode?>(null) }
    var replaceConfirmText by remember { mutableStateOf("") }
    var draft by remember(settings) { mutableStateOf(settings) }
    var nextMemoText by remember(settings.nextMemoNumber) {
        mutableStateOf(settings.nextMemoNumber.toString())
    }
    var showApiKey by remember { mutableStateOf(false) }
    var showQrManager by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Store Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BUSINESS & SLIP SETTINGS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NurseryColors.Maroon
                )
                OutlinedTextField(
                    value = draft.shopNameHindi,
                    onValueChange = { draft = draft.copy(shopNameHindi = it) },
                    label = { Text("Shop Name (Hindi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.shopNameEnglish,
                    onValueChange = { draft = draft.copy(shopNameEnglish = it) },
                    label = { Text("Shop Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.phone,
                        onValueChange = { draft = draft.copy(phone = it) },
                        label = { Text("Phone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.gstin,
                        onValueChange = { draft = draft.copy(gstin = it) },
                        label = { Text("GSTIN (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.upiVpa,
                        onValueChange = { draft = draft.copy(upiVpa = it) },
                        label = { Text("Store UPI ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = nextMemoText,
                        onValueChange = {
                            nextMemoText = it
                            it.toIntOrNull()?.let { n -> draft = draft.copy(nextMemoNumber = n) }
                        },
                        label = { Text("Next Memo #") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = draft.upiDefaultNote,
                    onValueChange = { draft = draft.copy(upiDefaultNote = it) },
                    label = { Text("Default UPI Payment Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.termsNote,
                    onValueChange = { draft = draft.copy(termsNote = it) },
                    label = { Text("Bill Footer Greeting Message") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showQrManager = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open UPI QR Manager / Custom QR Generator")
                }

                Text(
                    text = "Default Bill Export / Print Layout",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillExportLayout.entries.forEach { layout ->
                        FilterChip(
                            selected = draft.billExportLayout == layout,
                            onClick = { draft = draft.copy(billExportLayout = layout) },
                            label = {
                                Text(
                                    text = layout.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = { onSaveSettings(draft) },
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Store & Layout Settings", maxLines = 1, softWrap = false)
                }
            }
        }

        // Gemini AI Vision Scan & Translation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NurseryColors.BotanicalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "GEMINI AI SCAN & TRANSLATION",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.BotanicalGreen
                    )
                }

                OutlinedTextField(
                    value = draft.geminiApiKey,
                    onValueChange = { draft = draft.copy(geminiApiKey = it) },
                    label = { Text("Free Gemini API Key (Google AI Studio)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft.geminiModel,
                        onValueChange = { draft = draft.copy(geminiModel = it) },
                        label = { Text("Gemini Model ID", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onDetectFlashModels(draft.geminiApiKey) },
                        enabled = !isDetectingModels && draft.geminiApiKey.isNotBlank()
                    ) {
                        if (isDetectingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Detect", maxLines = 1, softWrap = false)
                    }
                }

                val quickModels = remember(availableFlashModels) {
                    (availableFlashModels + listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite"))
                        .distinct()
                        .take(5)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickModels.forEach { preset ->
                        FilterChip(
                            selected = draft.geminiModel.trim() == preset,
                            onClick = { draft = draft.copy(geminiModel = preset) },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        )
                    }
                }

                Text(
                    text = "Handwritten Hindi Output Translation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TranslationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = draft.translationMode == mode,
                            onClick = { draft = draft.copy(translationMode = mode) },
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = { onSaveSettings(draft.copy(geminiApiKey = draft.geminiApiKey.trim(), geminiModel = draft.geminiModel.trim().ifBlank { "gemini-3.5-flash" })) },
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Gemini AI & Store Settings", maxLines = 1, softWrap = false)
                }
            }
        }

        // Cashmemo_AI_Web Flask Sync Card (Rich Sync Controls)
        Card(
            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = NurseryColors.BotanicalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CASHMEMO WEB SERVER SYNC",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.BotanicalGreen
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft.serverBaseUrl,
                        onValueChange = { draft = draft.copy(serverBaseUrl = it) },
                        label = { Text("Server URL (e.g. http://192.168.1.10:8080)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            onSaveSettings(draft)
                            onTestConnection(draft.serverBaseUrl)
                        },
                        enabled = !isSyncing && draft.serverBaseUrl.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text("Test Connection", maxLines = 1)
                    }
                }

                // Scope Checkboxes
                Text(
                    text = "Collections to Sync:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NurseryColors.InkDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = syncBills,
                            onCheckedChange = { syncBills = it },
                            colors = CheckboxDefaults.colors(checkedColor = NurseryColors.Maroon)
                        )
                        Text("Bills", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = syncCustomers,
                            onCheckedChange = { syncCustomers = it },
                            colors = CheckboxDefaults.colors(checkedColor = NurseryColors.Maroon)
                        )
                        Text("Customers", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = syncRates,
                            onCheckedChange = { syncRates = it },
                            colors = CheckboxDefaults.colors(checkedColor = NurseryColors.Maroon)
                        )
                        Text("Plant Rates", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = syncSettings,
                        onCheckedChange = { syncSettings = it },
                        colors = CheckboxDefaults.colors(checkedColor = NurseryColors.Maroon)
                    )
                    Text("Shop Settings (device settings remain local if unchecked)", style = MaterialTheme.typography.bodySmall)
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onSaveSettings(draft)
                            onRequestSync(SyncMode.PUSH, SyncScopes(syncBills, syncCustomers, syncRates, syncSettings))
                        },
                        enabled = !isSyncing && draft.serverBaseUrl.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Push to Web", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveSettings(draft)
                            onRequestSync(SyncMode.PULL, SyncScopes(syncBills, syncCustomers, syncRates, syncSettings))
                        },
                        enabled = !isSyncing && draft.serverBaseUrl.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pull from Web", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Button(
                    onClick = {
                        onSaveSettings(draft)
                        onRequestSync(SyncMode.BOTH_WAYS, SyncScopes(syncBills, syncCustomers, syncRates, syncSettings))
                    },
                    enabled = !isSyncing && draft.serverBaseUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sync Both Ways (Recommended)", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-sync bills when saving", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = draft.autoSyncEnabled,
                        onCheckedChange = { draft = draft.copy(autoSyncEnabled = it) }
                    )
                }

                // Last sync status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last sync: $lastSyncStatus",
                        style = MaterialTheme.typography.labelSmall,
                        color = NurseryColors.InkMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (safetySnapshots.isNotEmpty()) {
                        TextButton(
                            onClick = { showSnapshotPicker = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Snapshots (${safetySnapshots.size})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (isSyncing && syncProgressText != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(
                            text = syncProgressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = NurseryColors.BotanicalGreen
                        )
                    }
                }

                HorizontalDivider(color = NurseryColors.OutlineSoft.copy(alpha = 0.5f))

                // Advanced / Dangerous Sync Operations (Collapsed by default)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NurseryColors.DueRed.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, NurseryColors.DueRed.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
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
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = NurseryColors.DueRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Advanced / Dangerous Sync Operations",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NurseryColors.DueRed
                                )
                            }
                            Icon(
                                imageVector = if (isAdvancedSyncOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = NurseryColors.DueRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isAdvancedSyncOpen) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "These operations permanently overwrite/delete data to match one side completely:",
                                style = MaterialTheme.typography.bodySmall,
                                color = NurseryColors.InkDark
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    pendingReplaceMode = SyncMode.DELIBERATE_MAKE_WEB_MATCH_PHONE
                                    replaceConfirmText = ""
                                    showReplaceDialog = true
                                },
                                border = BorderStroke(1.dp, NurseryColors.DueRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Make web match this phone (delete items on web not present here)",
                                    color = NurseryColors.DueRed,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    pendingReplaceMode = SyncMode.DELIBERATE_MAKE_PHONE_MATCH_WEB
                                    replaceConfirmText = ""
                                    showReplaceDialog = true
                                },
                                border = BorderStroke(1.dp, NurseryColors.DueRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Make this phone match the web (delete items here not present on web)",
                                    color = NurseryColors.DueRed,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // JSON Backup & Restore (Compatible with Cashmemo_AI_Web)
        Card(
            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BACKUP & RESTORE (WEB COMPATIBLE)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NurseryColors.Maroon
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportJsonBackup,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export JSON", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore JSON", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Text(
            text = "Build v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE}) • ${BuildConfig.APPLICATION_ID}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(12.dp))
    }

    if (showSnapshotPicker) {
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showSnapshotPicker = false },
            title = { Text("Safety Snapshots", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Tap a snapshot to restore the database to how it was before that sync:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    safetySnapshots.forEach { file ->
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
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = dateFormat.format(Date(file.lastModified())),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NurseryColors.Maroon
                                )
                                Text(
                                    text = "${file.name} (${file.length() / 1024} KB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NurseryColors.InkMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnapshotPicker = false }) {
                    Text("Close")
                }
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
