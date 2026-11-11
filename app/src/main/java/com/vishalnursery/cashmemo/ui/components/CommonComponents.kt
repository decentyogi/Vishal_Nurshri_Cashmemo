package com.vishalnursery.cashmemo.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Html
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import java.io.ByteArrayOutputStream
import java.io.File

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
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export $fileName"))
    }
}

object CashmemoPrintHelper {
    private var activeWebView: WebView? = null

    fun printCashMemo(context: Context, memo: CashMemo, businessSettings: BusinessSettings) {
        val html = buildSlipHtml(memo, businessSettings)
        val webView = WebView(context).apply {
            this.settings.javaScriptEnabled = false
        }
        activeWebView = webView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val adapter = webView.createPrintDocumentAdapter("CashMemo_${memo.memoNumber}")
                printManager.print(
                    "CashMemo_${memo.memoNumber}",
                    adapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun buildSlipHtml(memo: CashMemo, settings: BusinessSettings): String {
        val upiUri = IndianCurrencyFormatter.buildUpiUri(
            upiVpa = settings.upiVpa,
            payeeName = settings.shopNameEnglish,
            amount = memo.totalPayable
        )
        val qrBase64 = QrCodeGenerator.generateBitmap(upiUri, 260)?.let { bmp ->
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.orEmpty()

        val rows = memo.items.mapIndexed { idx, item ->
            """
            <tr>
              <td class="c">${idx + 1}</td>
              <td>${esc(item.description)}</td>
              <td class="c">${formatQty(item.quantity)}</td>
              <td class="r">${IndianCurrencyFormatter.formatInr(item.rate, settings.currencySymbol)}</td>
              <td class="r"><b>${IndianCurrencyFormatter.formatInr(item.amount, settings.currencySymbol)}</b></td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        val words = IndianCurrencyFormatter.convertToIndianRupees(memo.totalPayable)

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <style>
            body { font-family: sans-serif; color: #8B1527; background: #FFFDF9; padding: 14px; }
            .outer { border: 2.5px solid #8B1527; padding: 4px; }
            .inner { border: 1px solid #8B1527; padding: 12px; }
            .header { text-align: center; border-bottom: 1.5px solid #8B1527; padding-bottom: 8px; }
            .title { font-size: 22px; font-weight: 900; margin: 2px 0; }
            .sub { font-size: 11px; color: #4A1019; }
            .meta { display: flex; justify-content: space-between; font-size: 12px; margin: 8px 0; }
            .cust { text-align: center; border: 1px solid #8B1527; padding: 6px; margin-bottom: 8px; font-size: 12px; color: #1F1A1C; }
            table { width: 100%; border-collapse: collapse; font-size: 12px; color: #1F1A1C; }
            th, td { border: 1px solid #8B1527; padding: 6px; }
            th { background: #FDF2F4; color: #8B1527; }
            .c { text-align: center; }
            .r { text-align: right; font-family: monospace; }
            .footer { display: flex; justify-content: space-between; margin-top: 12px; font-size: 12px; color: #1F1A1C; }
            .totals { width: 45%; }
            .totals div { display: flex; justify-content: space-between; padding: 2px 0; }
            .grand { font-weight: 900; border-top: 1.5px solid #8B1527; border-bottom: 1.5px solid #8B1527; padding: 4px 0 !important; color: #8B1527; }
          </style>
        </head>
        <body>
          <div class="outer"><div class="inner">
            <div class="header">
              <div class="sub">CASH MEMO | GSTIN: ${esc(settings.gstin)}</div>
              <div class="title">${esc(settings.shopNameHindi)} (${esc(settings.shopNameEnglish)})</div>
              <div class="sub">${esc(settings.tagline)}</div>
              <div class="sub">${esc(settings.addressLine1)}, ${esc(settings.addressLine2)} | Mob: ${esc(settings.phone)}</div>
            </div>
            <div class="meta">
              <div><b>Memo #:</b> ${esc(memo.memoNumber)}</div>
              <div><b>Date:</b> ${esc(memo.date)}</div>
            </div>
            <div class="cust">
              <b>${esc(memo.customerName.ifBlank { "Walk-in Customer" })}</b>
              ${if (memo.customerAddress.isNotBlank()) " — ${esc(memo.customerAddress)}" else ""}
              ${if (memo.customerMobile.isNotBlank()) " (Mob: ${esc(memo.customerMobile)})" else ""}
            </div>
            <table>
              <thead>
                <tr>
                  <th style="width:7%">#</th>
                  <th style="width:50%">Item / Plant Description</th>
                  <th style="width:10%">Qty</th>
                  <th style="width:16%">Rate</th>
                  <th style="width:17%">Amount</th>
                </tr>
              </thead>
              <tbody>$rows</tbody>
            </table>
            <div class="footer">
              <div style="width:50%">
                <div><b>In Words:</b> <i>${esc(words)}</i></div>
                ${if (qrBase64.isNotEmpty()) """
                  <div style="margin-top:8px; display:flex; align-items:center; gap:8px;">
                    <img src="data:image/png;base64,$qrBase64" width="84" height="84" style="border:1px solid #8B1527;"/>
                    <div style="font-size:10px;"><b>UPI:</b> ${esc(settings.upiVpa)}<br/>Scan to Pay</div>
                  </div>
                """ else ""}
              </div>
              <div class="totals">
                <div><span>Subtotal</span><span>${IndianCurrencyFormatter.formatInr(memo.subtotal, settings.currencySymbol)}</span></div>
                ${if (memo.discount > 0) "<div><span>Discount</span><span>- ${IndianCurrencyFormatter.formatInr(memo.discount, settings.currencySymbol)}</span></div>" else ""}
                ${if (memo.taxAmount > 0) "<div><span>Tax (${memo.taxPercentage}%)</span><span>${IndianCurrencyFormatter.formatInr(memo.taxAmount, settings.currencySymbol)}</span></div>" else ""}
                ${if (memo.previousDues > 0) "<div><span>Previous Dues</span><span>+ ${IndianCurrencyFormatter.formatInr(memo.previousDues, settings.currencySymbol)}</span></div>" else ""}
                <div class="grand"><span>Total Payable</span><span>${IndianCurrencyFormatter.formatInr(memo.totalPayable, settings.currencySymbol)}</span></div>
                <div><span>Paid (${esc(memo.paymentMethod)})</span><span>${IndianCurrencyFormatter.formatInr(memo.paidAmount, settings.currencySymbol)}</span></div>
                ${if (memo.hasPendingDue) "<div style='color:#C62828;font-weight:bold'><span>Carry Forward</span><span>${IndianCurrencyFormatter.formatInr(memo.balanceDue, settings.currencySymbol)}</span></div>" else ""}
              </div>
            </div>
            <div style="margin-top:16px; display:flex; justify-content:space-between; font-size:11px;">
              <div>${esc(settings.termsNote)}</div>
              <div><b>For ${esc(settings.signerName)}</b></div>
            </div>
          </div></div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun esc(input: String): String = Html.escapeHtml(input)
    private fun formatQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
}
