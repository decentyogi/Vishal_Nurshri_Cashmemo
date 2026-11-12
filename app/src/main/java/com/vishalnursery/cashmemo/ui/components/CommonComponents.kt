package com.vishalnursery.cashmemo.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.domain.util.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream

@Composable
fun UpiQrDialog(
    amount: Double,
    settings: BusinessSettings,
    onDismiss: () -> Unit
) {
    val upiUri = remember(amount, settings.upiVpa, settings.shopNameEnglish) {
        IndianCurrencyFormatter.buildUpiUri(
            upiVpa = settings.upiVpa,
            payeeName = settings.shopNameEnglish,
            amount = amount
        )
    }
    val qrBitmap = remember(upiUri) {
        QrCodeGenerator.generateBitmap(upiUri, sizePx = 560)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Scan & Pay via UPI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = IndianCurrencyFormatter.formatInr(amount, settings.currencySymbol),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier
                                .padding(12.dp)
                                .size(220.dp)
                        )
                    }
                }
                Text(
                    text = settings.upiVpa,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "GPay / PhonePe / Paytm / BHIM",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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
