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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.ui.components.UpiQrDialog
import com.vishalnursery.cashmemo.ui.theme.NurseryColors

@Composable
fun SettingsScreen(
    settings: BusinessSettings,
    isSyncing: Boolean,
    availableFlashModels: List<String>,
    isDetectingModels: Boolean,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSavePermanentQr: (String, String, String) -> Unit,
    onDetectFlashModels: (String?) -> Unit,
    onSyncNow: (String?) -> Unit,
    onExportJsonBackup: () -> Unit,
    onImportJsonBackup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

        // Cashmemo_AI_Web Flask Sync Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CASHMEMO WEB SERVER SYNC (OPTIONAL)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NurseryColors.BotanicalGreen
                )
                OutlinedTextField(
                    value = draft.serverBaseUrl,
                    onValueChange = { draft = draft.copy(serverBaseUrl = it) },
                    label = { Text("Flask Server URL (e.g. http://192.168.1.10:8080)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSaveSettings(draft) },
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Settings", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveSettings(draft)
                            onSyncNow(draft.serverBaseUrl)
                        },
                        enabled = !isSyncing && draft.serverBaseUrl.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Sync Now", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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
}
