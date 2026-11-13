package com.vishalnursery.cashmemo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiPasteSheet(
    title: String,
    promptText: String,
    hint: String,
    liveStatus: (String) -> String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    imageUri: String? = null,
    onPickImage: () -> Unit = {},
    currentTranslationMode: TranslationMode,
    onSelectTranslationMode: (TranslationMode) -> Unit,
    pastedText: String,
    onPastedTextChanged: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    var showPromptPreview by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val statusText = remember(pastedText) { liveStatus(pastedText) }
    val canSubmit = !statusText.isNullOrBlank() && !statusText.contains("No items", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NurseryColors.CreamBackground,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                color = NurseryColors.OutlineSoft,
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NurseryColors.DarkInk
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", color = NurseryColors.Maroon)
                }
            }

            // Step 1: Copy Prompt
            StepContainer(stepNumber = 1, title = "Copy prompt") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Script & Language output:",
                        style = MaterialTheme.typography.bodySmall,
                        color = NurseryColors.MutedInk
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TranslationMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentTranslationMode == mode,
                                onClick = { onSelectTranslationMode(mode) },
                                label = { Text(mode.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NurseryColors.GreenSoft,
                                    selectedLabelColor = NurseryColors.GreenHeader,
                                    containerColor = NurseryColors.PaperSurface,
                                    labelColor = NurseryColors.DarkInk
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AI Prompt", promptText)
                                clipboard.setPrimaryClip(clip)
                                isCopied = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCopied) NurseryColors.GreenHeader else NurseryColors.Maroon
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isCopied) "Copied!" else "Copy Prompt")
                        }

                        OutlinedButton(
                            onClick = { showPromptPreview = !showPromptPreview },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(if (showPromptPreview) "Hide Prompt" else "Preview Prompt")
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (showPromptPreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showPromptPreview) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            color = NurseryColors.PaperSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NurseryColors.OutlineSoft)
                        ) {
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = NurseryColors.MutedInk,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Step 2: Open in AI app
            StepContainer(stepNumber = 2, title = "Open your AI app") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NurseryColors.PaperSurface)
                                .border(1.dp, NurseryColors.OutlineSoft, RoundedCornerShape(8.dp))
                                .clickable { onPickImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            val thumbBitmap = remember(imageUri) {
                                if (!imageUri.isNullOrBlank()) {
                                    runCatching {
                                        val u = Uri.parse(imageUri)
                                        context.contentResolver.openInputStream(u)?.use { stream ->
                                            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                                            BitmapFactory.decodeStream(stream, null, opts)
                                        }
                                    }.getOrNull()
                                } else null
                            }

                            if (thumbBitmap != null) {
                                Image(
                                    bitmap = thumbBitmap.asImageBitmap(),
                                    contentDescription = "Memo Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = "Pick Photo",
                                        tint = NurseryColors.MutedInk,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("Pick", fontSize = 10.sp, color = NurseryColors.MutedInk)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = {
                                    shareImageAndPromptToAi(context, imageUri, promptText)
                                },
                                enabled = !imageUri.isNullOrBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (!imageUri.isNullOrBlank()) "Open in AI app" else "No image yet")
                            }

                            Text(
                                text = "If the AI app ignores the shared text, simply paste the copied prompt.",
                                style = MaterialTheme.typography.labelSmall,
                                color = NurseryColors.MutedInk,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Step 3: Paste the AI's reply
            StepContainer(stepNumber = 3, title = "Paste the AI's reply") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clipboard.primaryClip?.getItemAt(0)
                                val text = item?.text?.toString().orEmpty()
                                if (text.isNotBlank()) onPastedTextChanged(text)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Paste")
                        }

                        if (pastedText.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { onPastedTextChanged("") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = onPastedTextChanged,
                        placeholder = { Text(hint, fontSize = 12.sp, color = NurseryColors.MutedInk.copy(alpha = 0.6f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 240.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NurseryColors.PaperSurface,
                            unfocusedContainerColor = NurseryColors.PaperSurface,
                            focusedBorderColor = NurseryColors.Maroon,
                            unfocusedBorderColor = NurseryColors.OutlineSoft
                        )
                    )

                    // Live Status Text
                    statusText?.let { status ->
                        Surface(
                            color = if (canSubmit) NurseryColors.GreenSoft else NurseryColors.WarningSoft,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canSubmit) NurseryColors.GreenHeader else NurseryColors.WarningHeader,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Submit Action Button
                    Button(
                        onClick = { onSubmit(pastedText) },
                        enabled = canSubmit,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NurseryColors.Maroon,
                            disabledContainerColor = NurseryColors.Maroon.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Send to Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepContainer(
    stepNumber: Int,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = NurseryColors.PaperSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NurseryColors.OutlineSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(NurseryColors.Maroon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NurseryColors.DarkInk
                )
            }
            content()
        }
    }
}

private fun shareImageAndPromptToAi(context: Context, imageUriString: String?, prompt: String) {
    if (imageUriString.isNullOrBlank()) return
    runCatching {
        val uri = Uri.parse(imageUriString)
        val cacheFile = File(context.cacheDir, "ai_prompt_image.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            if (bitmap != null) {
                val maxDim = 2000
                val longest = maxOf(bitmap.width, bitmap.height)
                val finalBitmap = if (longest > maxDim) {
                    val scale = maxDim.toFloat() / longest.toFloat()
                    val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                    val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(bitmap, w, h, true)
                } else {
                    bitmap
                }

                FileOutputStream(cacheFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }

                if (finalBitmap !== bitmap) finalBitmap.recycle()
                bitmap.recycle()
            }
        }

        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            clipData = ClipData.newRawUri("Cashmemo Prompt", fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open in AI app"))
    }.onFailure {
        // Fallback: send text only
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
        }
        context.startActivity(Intent.createChooser(intent, "Open in AI app"))
    }
}
