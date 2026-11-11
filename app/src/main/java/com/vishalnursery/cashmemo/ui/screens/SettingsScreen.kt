package com.vishalnursery.cashmemo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.vishalnursery.cashmemo.BuildConfig
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.ui.theme.NurseryColors

@Composable
fun SettingsScreen(
    settings: BusinessSettings,
    isSyncing: Boolean,
    onSaveSettings: (BusinessSettings) -> Unit,
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
                        label = { Text("GSTIN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.upiVpa,
                        onValueChange = { draft = draft.copy(upiVpa = it) },
                        label = { Text("UPI VPA") },
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
                    value = draft.termsNote,
                    onValueChange = { draft = draft.copy(termsNote = it) },
                    label = { Text("Terms & Footer Note") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Settings")
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveSettings(draft)
                            onSyncNow(draft.serverBaseUrl)
                        },
                        enabled = !isSyncing && draft.serverBaseUrl.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Sync Now")
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export JSON")
                    }
                    OutlinedButton(
                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore JSON")
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
