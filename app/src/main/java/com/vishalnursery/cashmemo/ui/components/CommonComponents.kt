package com.vishalnursery.cashmemo.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.domain.util.QrCodeGenerator
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import java.io.File
import java.io.FileOutputStream

@Composable
fun UpiQrDialog(
    initialAmount: Double,
    settings: BusinessSettings,
    billLabel: String = "",
    initialTab: Int = 0,
    onSavePermanentQr: (upiVpa: String, payeeName: String, defaultNote: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var showFullScreen by remember { mutableStateOf(false) }

    // Permanent Store QR state
    var storeVpa by remember(settings.upiVpa) { mutableStateOf(settings.upiVpa) }
    var storePayeeName by remember(settings.shopNameEnglish) { mutableStateOf(settings.shopNameEnglish) }
    var storeNote by remember(settings.upiDefaultNote) { mutableStateOf(settings.upiDefaultNote) }
    var storeSavedFeedback by remember { mutableStateOf(false) }

    // Custom One-Time QR state (blank by default with placeholder hint)
    var customAmountText by remember { mutableStateOf("") }
    var customVpa by remember(settings.upiVpa) { mutableStateOf(settings.upiVpa) }
    var customPayeeName by remember(settings.shopNameEnglish) { mutableStateOf(settings.shopNameEnglish) }
    var customNote by remember(billLabel, settings.upiDefaultNote) {
        mutableStateOf(settings.upiDefaultNote.ifBlank { billLabel })
    }

    val activeVpa = if (activeTab == 0) storeVpa else customVpa
    val activePayee = if (activeTab == 0) storePayeeName else customPayeeName
    val activeNote = if (activeTab == 0) {
        storeNote.ifBlank { billLabel }
    } else {
        customNote
    }
    val activeAmount = if (activeTab == 0) {
        if (initialAmount.isFinite() && initialAmount > 0.009) initialAmount else 0.0
    } else {
        customAmountText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    }

    val hasValidVpa = IndianCurrencyFormatter.isValidUpiVpa(activeVpa)
    val upiUri = remember(activeVpa, activePayee, activeAmount, activeNote) {
        IndianCurrencyFormatter.buildUpiUri(
            upiVpa = activeVpa,
            payeeName = activePayee,
            amount = activeAmount,
            note = activeNote
        )
    }
    val qrBitmap = remember(upiUri) {
        if (upiUri.isNotEmpty()) QrCodeGenerator.generateBitmap(upiUri, sizePx = 640) else null
    }

    if (showFullScreen && qrBitmap != null) {
        Dialog(
            onDismissRequest = { showFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeTab == 0) "Store Payment QR" else "Custom UPI QR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showFullScreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Full Screen")
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = activePayee.ifBlank { "UPI Payment" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (activeAmount > 0.009) {
                            Text(
                                text = IndianCurrencyFormatter.formatInr(activeAmount, settings.currencySymbol),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text(
                                text = "Scan & Enter Any Amount",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Full Screen UPI QR",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(300.dp)
                            )
                        }
                        Text(
                            text = activeVpa.trim(),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (activeNote.isNotBlank()) {
                            Text(
                                text = activeNote.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "Accepted by GPay • PhonePe • Paytm • BHIM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFullScreen = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                ShareExportHelper.shareUpiQrCard(
                                    context = context,
                                    upiUri = upiUri,
                                    payeeName = activePayee,
                                    upiVpa = activeVpa,
                                    amount = activeAmount,
                                    note = activeNote,
                                    currencySymbol = settings.currencySymbol
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share QR")
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar (Title + QR Icon on Left, ✕ Close on Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = NurseryColors.Maroon,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "UPI QR Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NurseryColors.Maroon
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sleek Single-Line Tab Switcher (Segmented Pill)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    color = NurseryColors.CreamBackground,
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (activeTab == 0) NurseryColors.Maroon else Color.Transparent)
                                .clickable { activeTab = 0 }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Permanent QR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 0) Color.White else NurseryColors.Maroon,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (activeTab == 1) NurseryColors.Maroon else Color.Transparent)
                                .clickable { activeTab = 1 }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Custom Amount QR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 1) Color.White else NurseryColors.Maroon,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // QR Preview Card (16dp corners, 16dp–20dp padding, no clipped edges)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = qrBitmap != null) { showFullScreen = true }
                ) {
                    if (qrBitmap != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(4.dp)
                            )
                            Text(
                                text = activeVpa.trim(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NurseryColors.Maroon,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap QR for full-screen counter display",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Enter a valid UPI ID (e.g. name@bank) below to generate a scannable QR.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Balanced Action Buttons (50% / 50% width, 12dp gap, single line)
                if (qrBitmap != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFullScreen = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.2.dp, NurseryColors.Maroon),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = NurseryColors.Maroon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Full Screen",
                                color = NurseryColors.Maroon,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Button(
                            onClick = {
                                ShareExportHelper.shareUpiQrCard(
                                    context = context,
                                    upiUri = upiUri,
                                    payeeName = activePayee,
                                    upiVpa = activeVpa,
                                    amount = activeAmount,
                                    note = activeNote,
                                    currencySymbol = settings.currencySymbol
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Save / Share",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                HorizontalDivider(color = NurseryColors.OutlineSoft)

                // Tab Contents with 12dp spacing and rounded input boxes
                if (activeTab == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = storeVpa,
                            onValueChange = {
                                storeVpa = it
                                storeSavedFeedback = false
                            },
                            label = { Text("Store UPI ID (e.g. name@ybl)") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = storePayeeName,
                            onValueChange = {
                                storePayeeName = it
                                storeSavedFeedback = false
                            },
                            label = { Text("Payee / Nursery Name") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = storeNote,
                            onValueChange = {
                                storeNote = it
                                storeSavedFeedback = false
                            },
                            label = { Text("Default Payment Note (Optional)") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSavePermanentQr(storeVpa.trim(), storePayeeName.trim(), storeNote.trim())
                                storeSavedFeedback = true
                            },
                            enabled = hasValidVpa || storeVpa.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (storeSavedFeedback) "Permanent Store QR Saved ✓" else "Save Permanent Store QR",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customAmountText,
                            onValueChange = { customAmountText = it },
                            label = { Text("Custom Amount (₹)") },
                            placeholder = { Text("Enter amount (optional)") },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customVpa,
                            onValueChange = { customVpa = it },
                            label = { Text("UPI ID") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customPayeeName,
                            onValueChange = { customPayeeName = it },
                            label = { Text("Payee Name") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("Transaction Note (Optional)") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiSummaryPill(
    label: String,
    value: String,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

object ShareExportHelper {
    fun shareTextFile(context: Context, fileName: String, mimeType: String, content: String) {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(content, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            clipData = ClipData.newRawUri(fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export $fileName"))
    }

    fun shareImageFiles(context: Context, files: List<File>, subject: String) {
        if (files.isEmpty()) return
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList(files.map { FileProvider.getUriForFile(context, authority, it) })

        val clipData = ClipData.newRawUri(subject, uris.first()).apply {
            for (i in 1 until uris.size) {
                addItem(ClipData.Item(uris[i]))
            }
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "$subject (${uris.size} Pages)")
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share $subject"))
    }

    fun sharePdfFile(context: Context, file: File, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            clipData = ClipData.newRawUri(subject, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $subject"))
    }

    fun shareUpiQrCard(
        context: Context,
        upiUri: String,
        payeeName: String,
        upiVpa: String,
        amount: Double,
        note: String,
        currencySymbol: String
    ) {
        if (upiUri.isBlank()) return
        val qrBitmap = QrCodeGenerator.generateBitmap(upiUri, sizePx = 760) ?: return
        val cardW = 1080
        val cardH = 1360
        val cardBitmap = Bitmap.createBitmap(cardW, cardH, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(cardBitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val maroon = android.graphics.Color.parseColor("#7A1120")
            val darkInk = android.graphics.Color.parseColor("#1F1A1C")
            val mutedInk = android.graphics.Color.parseColor("#5A4E52")
            val green = android.graphics.Color.parseColor("#1B6B3A")

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = maroon
                style = Paint.Style.STROKE
                strokeWidth = 10f
            }
            canvas.drawRoundRect(RectF(28f, 28f, cardW - 28f, cardH - 28f), 32f, 32f, borderPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = maroon
                textSize = 54f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(payeeName.ifBlank { "UPI Payment" }, cardW / 2f, 130f, titlePaint)

            if (amount > 0.009) {
                val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = green
                    textSize = 62f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(
                    IndianCurrencyFormatter.formatInr(amount, currencySymbol),
                    cardW / 2f,
                    220f,
                    amtPaint
                )
            } else {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mutedInk
                    textSize = 36f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Scan & Pay Any Amount", cardW / 2f, 212f, subPaint)
            }

            val qrRect = RectF(180f, 270f, 900f, 990f)
            canvas.drawBitmap(qrBitmap, null, qrRect, Paint(Paint.FILTER_BITMAP_FLAG))

            val vpaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 40f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("UPI: ${upiVpa.trim()}", cardW / 2f, 1080f, vpaPaint)

            if (note.isNotBlank()) {
                val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mutedInk
                    textSize = 32f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(note.trim(), cardW / 2f, 1146f, notePaint)
            }

            val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedInk
                textSize = 30f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("GPay • PhonePe • Paytm • BHIM", cardW / 2f, 1255f, footPaint)

            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val safeVpa = upiVpa.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "upi" }
            val file = File(exportDir, "UPI_QR_${safeVpa}.jpg")
            FileOutputStream(file).use { stream ->
                cardBitmap.compress(Bitmap.CompressFormat.JPEG, 94, stream)
                stream.flush()
            }
            shareImageFiles(context, listOf(file), "UPI Payment QR - $upiVpa")
        } finally {
            qrBitmap.recycle()
            cardBitmap.recycle()
        }
    }
}

object CashmemoPrintHelper {

    fun printCashMemo(context: Context, memo: CashMemo, businessSettings: BusinessSettings) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "CashMemo_${memo.memoNumber}"

        val adapter = object : PrintDocumentAdapter() {
            private var activeAttributes: PrintAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                activeAttributes = newAttributes
                val pageCount = BillDocumentRenderer.calculatePageCount(memo, businessSettings.billExportLayout)
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pageCount)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                val pdfDoc = PrintedPdfDocument(context, activeAttributes)
                try {
                    val mediaSize = activeAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
                    val widthPt = ((mediaSize.widthMils / 1000f) * 72f).toInt().coerceAtLeast(BillDocumentRenderer.PDF_A4_W)
                    val heightPt = ((mediaSize.heightMils / 1000f) * 72f).toInt().coerceAtLeast(BillDocumentRenderer.PDF_A4_H)

                    BillDocumentRenderer.renderToPdfDocument(
                        pdfDocument = pdfDoc,
                        pageWidthPt = widthPt,
                        pageHeightPt = heightPt,
                        memo = memo,
                        settings = businessSettings
                    )

                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                        out.flush()
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.localizedMessage ?: "Print rendering failed")
                } finally {
                    pdfDoc.close()
                }
            }
        }

        val defaultAttrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()
        printManager.print(jobName, adapter, defaultAttrs)
    }
}
