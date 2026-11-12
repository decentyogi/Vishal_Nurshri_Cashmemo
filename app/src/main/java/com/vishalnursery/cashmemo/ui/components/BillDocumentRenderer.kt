package com.vishalnursery.cashmemo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.domain.util.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Unified high-resolution vector & bitmap bill renderer supporting both:
 * 1. Classic Nursery Cashmemo (strict 15 items per page, repeating headers/footers, watermark ovals)
 * 2. Modern Receipt (with UPI QR payment block)
 */
object BillDocumentRenderer {

    private const val LOGICAL_W = 1800f
    private const val LOGICAL_H = 2546f

    // High-resolution A4 bitmap output (~266 DPI) so zoomed JPEGs stay sharp (~500-700 KB at 93% quality)
    private const val JPEG_W = 2200
    private const val JPEG_H = 3111
    private const val JPEG_QUALITY = 93

    // Standard A4 PostScript points for crisp vector PDFs
    const val PDF_A4_W = 595
    const val PDF_A4_H = 842

    private const val CLASSIC_ITEMS_PER_PAGE = 15
    private const val MODERN_ITEMS_PER_PAGE = 16

    private val CLASSIC_MAROON = Color.parseColor("#7A1120")
    private val CLASSIC_DARK_MAROON = Color.parseColor("#540C17")
    private val CLASSIC_HEADER_TINT = Color.parseColor("#FDF3F4")
    private val CLASSIC_ROW_DIVIDER = Color.parseColor("#DFC1C6")
    private val CLASSIC_WATERMARK = Color.parseColor("#E9E2DC")

    private val MODERN_MAROON = Color.parseColor("#8B1527")
    private val MODERN_CREAM = Color.parseColor("#FFFDF9")
    private val MODERN_TH_BG = Color.parseColor("#FDF2F4")

    private val INK_DARK = Color.parseColor("#1F1A1C")
    private val INK_MUTED = Color.parseColor("#5A4E52")
    private val GREEN_STAMP = Color.parseColor("#1B6B3A")
    private val DUE_RED = Color.parseColor("#C62828")

    private val inrSymbols = DecimalFormatSymbols(Locale("en", "IN"))
    private val inrPlainDecimal = DecimalFormat("##,##,##0.00", inrSymbols)
    private val inrPlainInteger = DecimalFormat("##,##,##0", inrSymbols)

    fun calculatePageCount(memo: CashMemo, layout: BillExportLayout): Int {
        val perPage = if (layout == BillExportLayout.CLASSIC_NURSERY) {
            CLASSIC_ITEMS_PER_PAGE
        } else {
            MODERN_ITEMS_PER_PAGE
        }
        val count = memo.items.size
        return if (count == 0) 1 else ((count - 1) / perPage) + 1
    }

    fun renderJpegFiles(
        context: Context,
        memo: CashMemo,
        settings: BusinessSettings
    ): List<File> {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeMemoNo = sanitizeFileToken(memo.memoNumber)
        // Clear stale page exports for the same memo so page count reductions never leave orphan files
        exportDir.listFiles { _, name ->
            name.startsWith("CashMemo_${safeMemoNo}_P") && name.endsWith(".jpg")
        }?.forEach { it.delete() }

        val totalPages = calculatePageCount(memo, settings.billExportLayout)
        val outFiles = ArrayList<File>(totalPages)

        for (pageIndex in 0 until totalPages) {
            val bitmap = Bitmap.createBitmap(JPEG_W, JPEG_H, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                canvas.save()
                canvas.scale(JPEG_W / LOGICAL_W, JPEG_H / LOGICAL_H)
                renderPage(canvas, memo, settings, pageIndex, totalPages)
                canvas.restore()

                val file = File(exportDir, "CashMemo_${safeMemoNo}_P${pageIndex + 1}.jpg")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                    stream.flush()
                }
                outFiles.add(file)
            } finally {
                bitmap.recycle()
            }
        }
        return outFiles
    }

    fun renderPdfFile(
        context: Context,
        memo: CashMemo,
        settings: BusinessSettings
    ): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeMemoNo = sanitizeFileToken(memo.memoNumber)
        val file = File(exportDir, "CashMemo_${safeMemoNo}.pdf")

        val pdfDocument = PdfDocument()
        try {
            renderToPdfDocument(pdfDocument, PDF_A4_W, PDF_A4_H, memo, settings)
            FileOutputStream(file).use { stream ->
                pdfDocument.writeTo(stream)
                stream.flush()
            }
        } finally {
            pdfDocument.close()
        }
        return file
    }

    fun renderToPdfDocument(
        pdfDocument: PdfDocument,
        pageWidthPt: Int,
        pageHeightPt: Int,
        memo: CashMemo,
        settings: BusinessSettings
    ) {
        val totalPages = calculatePageCount(memo, settings.billExportLayout)
        val scale = min(pageWidthPt / LOGICAL_W, pageHeightPt / LOGICAL_H)
        val offsetX = (pageWidthPt - LOGICAL_W * scale) / 2f
        val offsetY = (pageHeightPt - LOGICAL_H * scale) / 2f

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)
            renderPage(canvas, memo, settings, pageIndex, totalPages)
            canvas.restore()
            pdfDocument.finishPage(page)
        }
    }

    fun renderPage(
        canvas: Canvas,
        memo: CashMemo,
        settings: BusinessSettings,
        pageIndex: Int,
        totalPages: Int
    ) {
        when (settings.billExportLayout) {
            BillExportLayout.CLASSIC_NURSERY -> drawClassicNurseryPage(canvas, memo, settings, pageIndex, totalPages)
            BillExportLayout.MODERN_RECEIPT -> drawModernReceiptPage(canvas, memo, settings, pageIndex, totalPages)
        }
    }

    // -------------------------------------------------------------------------
    // 1. CLASSIC VISHAL NURSERY CASHMEMO (Exact 15-Item Paginated Format)
    // -------------------------------------------------------------------------

    private fun drawClassicNurseryPage(
        canvas: Canvas,
        memo: CashMemo,
        settings: BusinessSettings,
        pageIndex: Int,
        totalPages: Int
    ) {
        canvas.drawColor(Color.WHITE)

        // Double Maroon Page Frame
        val outerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            style = Paint.Style.STROKE
            strokeWidth = 7f
        }
        val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
        }
        canvas.drawRect(46f, 46f, LOGICAL_W - 46f, LOGICAL_H - 46f, outerStroke)
        canvas.drawRect(60f, 60f, LOGICAL_W - 60f, LOGICAL_H - 60f, innerStroke)

        val cLeft = 92f
        val cRight = LOGICAL_W - 92f
        val contentWidth = cRight - cLeft
        val centerX = LOGICAL_W / 2f

        // --- Header Block (repeats identically on every page) ---
        val maroonPaint = textPaint(30f, CLASSIC_MAROON, bold = true)
        val phoneText = "Mob. ${settings.phone.ifBlank { "7631873743" }}"

        // Top-left rosette emblem, centered Mob., top-right sprout emblem
        drawRosetteEmblem(canvas, cLeft + 26f, 114f, 22f, CLASSIC_MAROON)
        drawCenteredText(canvas, phoneText, centerX, 122f, textPaint(29f, CLASSIC_MAROON, bold = true))
        drawSproutEmblem(canvas, cRight - 30f, 134f, 44f, CLASSIC_MAROON)

        // Shop Hindi Title flanked by crisp vector ✿ rosettes
        val shopHindi = settings.shopNameHindi.ifBlank { "विशाल नर्सरी" }
        val titlePaint = textPaint(66f, CLASSIC_MAROON, bold = true)
        val titleWidth = titlePaint.measureText(shopHindi)
        drawCenteredText(canvas, shopHindi, centerX, 212f, titlePaint)
        drawRosetteEmblem(canvas, centerX - titleWidth / 2f - 44f, 190f, 21f, CLASSIC_MAROON)
        drawRosetteEmblem(canvas, centerX + titleWidth / 2f + 44f, 190f, 21f, CLASSIC_MAROON)

        val shopEnglish = "(${settings.shopNameEnglish.ifBlank { "Vishal Nursery" }})"
        drawCenteredText(canvas, shopEnglish, centerX, 260f, textPaint(33f, CLASSIC_MAROON, bold = true))

        val addr1 = settings.addressLine1.ifBlank { "ग्राम-कोटा, माँ ताराचण्डी रोड (माँ सीताराम मंदिर)" }
        val defaultRawAddr2 = "पो०-सासाराम (रोहतास), बिहार - 821115"
        val addr2 = when {
            settings.addressLine2.isBlank() || settings.addressLine2 == defaultRawAddr2 ->
                "पो०-सासाराम (रोहतास)"
            else -> settings.addressLine2
        }
        val addrPaint = textPaint(26f, CLASSIC_DARK_MAROON, bold = true)
        drawCenteredText(canvas, addr1, centerX, 302f, addrPaint)
        drawCenteredText(canvas, addr2, centerX, 336f, addrPaint)

        val tagline = settings.tagline.ifBlank {
            "हमारे यहाँ हर प्रकार के फूल, पेड़-पौधा जैसे-गुलाब, गुलदावदी, गेंदा, क्रोटन सब गमले में तैयार पौधा भी मिलता है।"
        }
        drawStaticText(
            canvas = canvas,
            text = tagline,
            x = cLeft,
            y = 352f,
            width = contentWidth.toInt(),
            paint = textPaint(22f, CLASSIC_MAROON, bold = false),
            alignment = Layout.Alignment.ALIGN_CENTER,
            maxLines = 2
        )

        val headerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            strokeWidth = 3f
        }
        canvas.drawLine(cLeft, 422f, cRight, 422f, headerLinePaint)

        // --- Memo & Customer Info Block (repeats on every page) ---
        val labelPaint = textPaint(28f, CLASSIC_MAROON, bold = true)
        val valuePaint = textPaint(28f, INK_DARK, bold = true)
        val subtleRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_ROW_DIVIDER
            strokeWidth = 1.6f
        }

        val leftColRight = 1130f
        val rightColLeft = 1165f

        // Memo No bordered rounded box: "नं०  [ 1001 (Page 1/2) ]"
        canvas.drawText("नं०", cLeft, 476f, labelPaint)
        val noBoxLeft = cLeft + 66f
        val noBoxRight = cLeft + 498f
        val noBoxRect = RectF(noBoxLeft, 438f, noBoxRight, 492f)
        val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
        }
        val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_HEADER_TINT
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(noBoxRect, 10f, 10f, boxFill)
        canvas.drawRoundRect(noBoxRect, 10f, 10f, boxStroke)

        val memoBoxText = "${memo.memoNumber} (Page ${pageIndex + 1}/$totalPages)"
        drawCenteredText(
            canvas,
            memoBoxText,
            noBoxRect.centerX(),
            475f,
            textPaint(27f, CLASSIC_DARK_MAROON, bold = true)
        )

        // Left Column: नामः & पता
        val nameLabel = "नामः "
        val nameLabelW = labelPaint.measureText(nameLabel)
        canvas.drawText(nameLabel, cLeft, 536f, labelPaint)
        if (memo.customerName.isNotBlank()) {
            drawSingleLineEllipsized(
                canvas,
                memo.customerName.trim(),
                cLeft + nameLabelW + 6f,
                536f,
                leftColRight - (cLeft + nameLabelW + 16f),
                valuePaint
            )
        }
        canvas.drawLine(cLeft + nameLabelW, 543f, leftColRight, 543f, subtleRulePaint)

        val addrLabel = "पता: "
        val addrLabelW = labelPaint.measureText(addrLabel)
        canvas.drawText(addrLabel, cLeft, 594f, labelPaint)
        if (memo.customerAddress.isNotBlank()) {
            drawSingleLineEllipsized(
                canvas,
                memo.customerAddress.trim(),
                cLeft + addrLabelW + 6f,
                594f,
                leftColRight - (cLeft + addrLabelW + 16f),
                valuePaint
            )
        }
        canvas.drawLine(cLeft + addrLabelW, 601f, leftColRight, 601f, subtleRulePaint)

        // Right Column: दिनांक & मो०
        val dateLabel = "दिनांक: "
        val dateLabelW = labelPaint.measureText(dateLabel)
        canvas.drawText(dateLabel, rightColLeft, 476f, labelPaint)
        if (memo.date.isNotBlank()) {
            drawSingleLineEllipsized(
                canvas,
                memo.date.trim(),
                rightColLeft + dateLabelW + 4f,
                476f,
                cRight - (rightColLeft + dateLabelW + 8f),
                valuePaint
            )
        }
        canvas.drawLine(rightColLeft + dateLabelW, 483f, cRight, 483f, subtleRulePaint)

        val mobLabel = "मो०: "
        val mobLabelW = labelPaint.measureText(mobLabel)
        canvas.drawText(mobLabel, rightColLeft, 536f, labelPaint)
        if (memo.customerMobile.isNotBlank()) {
            drawSingleLineEllipsized(
                canvas,
                memo.customerMobile.trim(),
                rightColLeft + mobLabelW + 4f,
                536f,
                cRight - (rightColLeft + mobLabelW + 8f),
                valuePaint
            )
        }
        canvas.drawLine(rightColLeft + mobLabelW, 543f, cRight, 543f, subtleRulePaint)

        // --- Items Table (Strict 15 Rows Per Page) ---
        val tableTop = 626f
        val thHeight = 68f
        val rowsTop = tableTop + thHeight // 694f
        val rowHeight = 78f
        val rowsBottom = rowsTop + CLASSIC_ITEMS_PER_PAGE * rowHeight // 1864f
        val summaryHeight = 76f
        val tableBottom = rowsBottom + summaryHeight // 1940f

        // Column X boundaries (width total = 1616f)
        val col0 = cLeft            // 92f
        val col1 = cLeft + 104f     // 196f (क्रम.)
        val col2 = col1 + 796f      // 992f (सामग्री / पौधा का विवरण)
        val col3 = col2 + 150f      // 1142f (संख्या)
        val col4 = col3 + 216f      // 1358f (दाम (दर))
        val col5 = col4 + 238f      // 1596f (रू०)
        val col6 = cRight           // 1708f (पै०)

        // 1) Faint overlapping oval watermark rings centered inside table body BEFORE grid/text
        drawWatermarkOvalRings(canvas, centerX, (rowsTop + rowsBottom) / 2f)

        // 2) Table Header Row fill & white text
        val thFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            style = Paint.Style.FILL
        }
        canvas.drawRect(col0, tableTop, col6, rowsTop, thFill)

        val thPaint = textPaint(25f, Color.WHITE, bold = true)
        val thBaseline = tableTop + 44f
        drawCenteredText(canvas, "क्रम.", (col0 + col1) / 2f, thBaseline, thPaint)
        canvas.drawText("सामग्री / पौधा का विवरण", col1 + 18f, thBaseline, thPaint)
        drawCenteredText(canvas, "संख्या", (col2 + col3) / 2f, thBaseline, thPaint)
        drawCenteredText(canvas, "दाम (दर)", (col3 + col4) / 2f, thBaseline, thPaint)
        drawCenteredText(canvas, "रू०", (col4 + col5) / 2f, thBaseline, thPaint)
        drawCenteredText(canvas, "पै०", (col5 + col6) / 2f, thBaseline, thPaint)

        // 3) Summary Row subtle tint fill
        val summaryFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_HEADER_TINT
            style = Paint.Style.FILL
        }
        canvas.drawRect(col0, rowsBottom, col6, tableBottom, summaryFill)

        // 4) Horizontal Row Dividers (1..14)
        for (r in 1 until CLASSIC_ITEMS_PER_PAGE) {
            val ry = rowsTop + r * rowHeight
            canvas.drawLine(col0, ry, col6, ry, subtleRulePaint)
        }

        // 5) Outer Table Frame & Vertical Gridlines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
        }
        val innerGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            strokeWidth = 1.9f
        }
        canvas.drawRect(col0, tableTop, col6, tableBottom, gridPaint)
        canvas.drawLine(col0, rowsBottom, col6, rowsBottom, gridPaint)
        // Columns 1..3 extend down to rowsBottom; columns 4 & 5 extend all the way to tableBottom
        canvas.drawLine(col1, rowsTop, col1, rowsBottom, innerGridPaint)
        canvas.drawLine(col2, rowsTop, col2, rowsBottom, innerGridPaint)
        canvas.drawLine(col3, rowsTop, col3, rowsBottom, innerGridPaint)
        canvas.drawLine(col4, rowsTop, col4, tableBottom, innerGridPaint)
        canvas.drawLine(col5, rowsTop, col5, tableBottom, innerGridPaint)

        // White vertical header separators for crisp header cells
        val thSepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            strokeWidth = 1.6f
        }
        listOf(col1, col2, col3, col4, col5).forEach { cx ->
            canvas.drawLine(cx, tableTop, cx, rowsTop, thSepPaint)
        }

        // 6) Render Strictly This Page's Slice (<= 15 Items)
        val startIndex = pageIndex * CLASSIC_ITEMS_PER_PAGE
        val endIndex = min(memo.items.size, startIndex + CLASSIC_ITEMS_PER_PAGE)
        val pageItems = if (startIndex < endIndex) memo.items.subList(startIndex, endIndex) else emptyList()

        val rowNumPaint = textPaint(26f, INK_DARK, bold = false)
        val rowDescPaint = textPaint(25f, INK_DARK, bold = false)
        val rowAmtPaint = textPaint(26f, INK_DARK, bold = true)

        pageItems.forEachIndexed { localIdx, item ->
            val rTop = rowsTop + localIdx * rowHeight
            val centerY = rTop + rowHeight / 2f + 9f
            val serialNumber = startIndex + localIdx + 1

            drawCenteredText(canvas, serialNumber.toString(), (col0 + col1) / 2f, centerY, rowNumPaint)

            // Description (handles single or wrapped 2-line Hindi/English item descriptions cleanly)
            drawCenteredVerticalStaticText(
                canvas = canvas,
                text = item.description.trim(),
                x = col1 + 16f,
                boxTop = rTop + 4f,
                boxHeight = rowHeight - 8f,
                width = (col2 - col1 - 30f).toInt(),
                paint = rowDescPaint,
                maxLines = 2
            )

            drawCenteredText(canvas, formatCleanQty(item.quantity), (col2 + col3) / 2f, centerY, rowNumPaint)
            drawRightText(canvas, formatRateNoSymbol(item.rate), col4 - 16f, centerY, rowNumPaint)

            val (rupeesStr, paiseStr) = splitRupeesPaise(item.amount)
            drawRightText(canvas, rupeesStr, col5 - 16f, centerY, rowAmtPaint)
            drawCenteredText(canvas, paiseStr, (col5 + col6) / 2f, centerY, rowNumPaint)
        }

        // 7) Table Summary Row (Intermediate vs. Final Page)
        val isFinalPage = pageIndex == totalPages - 1
        val runningSubtotal = memo.items.take(endIndex).sumOf { it.amount }
        val summaryAmount = if (isFinalPage) memo.totalPayable else runningSubtotal

        val summaryLabel = if (isFinalPage) {
            "कुल देय (Total Payable inc. Dues) :-"
        } else {
            "आगे ले जाया गया (Carried Over P${pageIndex + 2}) :-"
        }
        val summaryBaseline = rowsBottom + summaryHeight / 2f + 10f
        drawRightText(
            canvas,
            summaryLabel,
            col4 - 20f,
            summaryBaseline,
            textPaint(26f, CLASSIC_MAROON, bold = true)
        )

        val (sumRupees, sumPaise) = splitRupeesPaise(summaryAmount)
        drawRightText(canvas, sumRupees, col5 - 16f, summaryBaseline, textPaint(29f, CLASSIC_DARK_MAROON, bold = true))
        drawCenteredText(canvas, sumPaise, (col5 + col6) / 2f, summaryBaseline, textPaint(26f, CLASSIC_DARK_MAROON, bold = true))

        // --- Words & Settlement Breakdown Below Table ---
        val wordsY = tableBottom + 18f
        val wordsString = "शब्दों में: ${IndianCurrencyFormatter.convertToIndianRupees(summaryAmount)}"
        val wordsUsedHeight = drawStaticText(
            canvas = canvas,
            text = wordsString,
            x = cLeft,
            y = wordsY,
            width = contentWidth.toInt(),
            paint = textPaint(26f, INK_DARK, bold = true),
            alignment = Layout.Alignment.ALIGN_NORMAL,
            maxLines = 2
        )

        if (isFinalPage) {
            val discountRupees = computeDiscountRupees(memo)
            val line1 = buildString {
                append("Subtotal: ₹ ${formatPlainMoney(memo.subtotal)}")
                append("  |  छूट: -₹ ${formatPlainMoney(discountRupees)}")
                if (memo.taxAmount > 0.009) {
                    append("  |  Tax (${formatCleanQty(memo.taxPercentage)}%): +₹ ${formatPlainMoney(memo.taxAmount)}")
                }
                append("  |  पिछला बकाया (Dues): +₹ ${formatPlainMoney(memo.previousDues)}")
            }
            val dueSuffix = if (memo.balanceDue > 0.009) {
                val note = memo.paymentStatusNote.trim()
                if (note.isNotBlank() &&
                    !note.equals("Carry Forward", ignoreCase = true) &&
                    !note.equals("Fully Paid", ignoreCase = true)
                ) {
                    " [$note]"
                } else {
                    " [Next Day (कल देगा)]"
                }
            } else {
                ""
            }
            val line2 = "जमा राशि (Paid): ₹ ${formatPlainMoney(memo.paidAmount)}  |  शेष बकाया (Balance Due): ₹ ${formatPlainMoney(memo.balanceDue)}$dueSuffix"

            val line1Top = wordsY + wordsUsedHeight + 16f
            val h1 = drawStaticText(
                canvas = canvas,
                text = line1,
                x = cLeft,
                y = line1Top,
                width = contentWidth.toInt(),
                paint = textPaint(24f, INK_MUTED, bold = true),
                alignment = Layout.Alignment.ALIGN_NORMAL,
                maxLines = 2
            )
            val line2Color = if (memo.balanceDue > 0.009) DUE_RED else GREEN_STAMP
            drawStaticText(
                canvas = canvas,
                text = line2,
                x = cLeft,
                y = line1Top + h1 + 8f,
                width = contentWidth.toInt(),
                paint = textPaint(25f, line2Color, bold = true),
                alignment = Layout.Alignment.ALIGN_NORMAL,
                maxLines = 2
            )
        }

        // --- Bottom Footer (Must appear on EVERY page) ---
        drawCenteredTextOrLeft(
            canvas,
            "भूल-चूक लेनी-देनी (E. & O.E.)",
            cLeft,
            2432f,
            textPaint(26f, CLASSIC_MAROON, bold = true)
        )

        // Boxed Digital Stamp (Bottom-Right)
        val stampRect = RectF(cRight - 460f, 2246f, cRight, 2398f)
        val stampFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFDFB")
            style = Paint.Style.FILL
        }
        val stampStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_MAROON
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
        }
        canvas.drawRoundRect(stampRect, 12f, 12f, stampFill)
        canvas.drawRoundRect(stampRect, 12f, 12f, stampStroke)

        val stampCx = stampRect.centerX()
        drawCenteredText(canvas, "✓ Digitally Signed", stampCx, 2292f, textPaint(26f, GREEN_STAMP, bold = true))
        val signer = settings.signerName.ifBlank { "VISHAL NURSERY" }.uppercase(Locale.ENGLISH)
        drawCenteredText(canvas, "BY $signer", stampCx, 2334f, textPaint(25f, CLASSIC_MAROON, bold = true))
        drawCenteredText(canvas, "Date - ${memo.date}", stampCx, 2374f, textPaint(23f, INK_DARK, bold = false))

        drawCenteredText(
            canvas,
            "हस्ताक्षर (Authorized Signatory)",
            stampCx,
            2438f,
            textPaint(25f, CLASSIC_MAROON, bold = true)
        )
    }

    // -------------------------------------------------------------------------
    // 2. MODERN RECEIPT LAYOUT (Retained with UPI QR + Multi-page Support)
    // -------------------------------------------------------------------------

    private fun drawModernReceiptPage(
        canvas: Canvas,
        memo: CashMemo,
        settings: BusinessSettings,
        pageIndex: Int,
        totalPages: Int
    ) {
        canvas.drawColor(MODERN_CREAM)

        val outerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MODERN_MAROON
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
        }
        val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MODERN_MAROON
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRect(50f, 50f, LOGICAL_W - 50f, LOGICAL_H - 50f, outerStroke)
        canvas.drawRect(64f, 64f, LOGICAL_W - 64f, LOGICAL_H - 64f, innerStroke)

        val cLeft = 98f
        val cRight = LOGICAL_W - 98f
        val contentWidth = cRight - cLeft
        val centerX = LOGICAL_W / 2f

        // Header
        drawCenteredText(
            canvas,
            "CASH MEMO | GSTIN: ${settings.gstin} | Page ${pageIndex + 1}/$totalPages",
            centerX,
            122f,
            textPaint(25f, Color.parseColor("#4A1019"), bold = true)
        )
        drawCenteredText(
            canvas,
            "${settings.shopNameHindi} (${settings.shopNameEnglish})",
            centerX,
            192f,
            textPaint(50f, MODERN_MAROON, bold = true)
        )
        drawStaticText(
            canvas = canvas,
            text = settings.tagline,
            x = cLeft,
            y = 212f,
            width = contentWidth.toInt(),
            paint = textPaint(22f, Color.parseColor("#4A1019"), bold = false),
            alignment = Layout.Alignment.ALIGN_CENTER,
            maxLines = 2
        )
        drawCenteredText(
            canvas,
            "${settings.addressLine1}, ${settings.addressLine2} | Mob: ${settings.phone}",
            centerX,
            298f,
            textPaint(24f, Color.parseColor("#4A1019"), bold = true)
        )
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MODERN_MAROON
            strokeWidth = 3f
        }
        canvas.drawLine(cLeft, 322f, cRight, 322f, dividerPaint)

        // Meta row
        val metaPaint = textPaint(27f, MODERN_MAROON, bold = true)
        canvas.drawText("Memo #: ${memo.memoNumber}", cLeft, 368f, metaPaint)
        drawRightText(canvas, "Date: ${memo.date}", cRight, 368f, metaPaint)

        // Customer Box
        val custRect = RectF(cLeft, 392f, cRight, 466f)
        canvas.drawRect(custRect, innerStroke)
        val custSummary = buildString {
            append(memo.customerName.ifBlank { "Walk-in Customer" })
            if (memo.customerAddress.isNotBlank()) append(" — ${memo.customerAddress}")
            if (memo.customerMobile.isNotBlank()) append(" (Mob: ${memo.customerMobile})")
        }
        drawCenteredVerticalStaticText(
            canvas = canvas,
            text = custSummary,
            x = cLeft + 14f,
            boxTop = custRect.top + 6f,
            boxHeight = custRect.height() - 12f,
            width = (contentWidth - 28f).toInt(),
            paint = textPaint(27f, INK_DARK, bold = true),
            alignment = Layout.Alignment.ALIGN_CENTER,
            maxLines = 2
        )

        // Items Table (up to 16 items per page)
        val tableTop = 492f
        val thHeight = 62f
        val rowsTop = tableTop + thHeight
        val rowHeight = 72f
        val startIndex = pageIndex * MODERN_ITEMS_PER_PAGE
        val endIndex = min(memo.items.size, startIndex + MODERN_ITEMS_PER_PAGE)
        val pageItems = if (startIndex < endIndex) memo.items.subList(startIndex, endIndex) else emptyList()
        val displayRows = if (pageItems.isEmpty()) 1 else pageItems.size
        val rowsBottom = rowsTop + displayRows * rowHeight

        val mCol0 = cLeft
        val mCol1 = cLeft + 100f
        val mCol2 = mCol1 + 780f
        val mCol3 = mCol2 + 150f
        val mCol4 = mCol3 + 270f
        val mCol5 = cRight

        val thBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MODERN_TH_BG
            style = Paint.Style.FILL
        }
        canvas.drawRect(mCol0, tableTop, mCol5, rowsTop, thBgPaint)
        canvas.drawRect(mCol0, tableTop, mCol5, rowsBottom, innerStroke)
        canvas.drawLine(mCol0, rowsTop, mCol5, rowsTop, innerStroke)
        listOf(mCol1, mCol2, mCol3, mCol4).forEach { cx ->
            canvas.drawLine(cx, tableTop, cx, rowsBottom, innerStroke)
        }

        val thModernPaint = textPaint(25f, MODERN_MAROON, bold = true)
        val mThBase = tableTop + 41f
        drawCenteredText(canvas, "#", (mCol0 + mCol1) / 2f, mThBase, thModernPaint)
        canvas.drawText("Item / Plant Description", mCol1 + 16f, mThBase, thModernPaint)
        drawCenteredText(canvas, "Qty", (mCol2 + mCol3) / 2f, mThBase, thModernPaint)
        drawRightText(canvas, "Rate", mCol4 - 16f, mThBase, thModernPaint)
        drawRightText(canvas, "Amount", mCol5 - 16f, mThBase, thModernPaint)

        val rowTxtPaint = textPaint(25f, INK_DARK, bold = false)
        val rowBoldPaint = textPaint(25f, INK_DARK, bold = true)
        pageItems.forEachIndexed { localIdx, item ->
            val rTop = rowsTop + localIdx * rowHeight
            if (localIdx > 0) canvas.drawLine(mCol0, rTop, mCol5, rTop, innerStroke)
            val baseY = rTop + rowHeight / 2f + 9f
            drawCenteredText(canvas, "${startIndex + localIdx + 1}", (mCol0 + mCol1) / 2f, baseY, rowTxtPaint)
            drawCenteredVerticalStaticText(
                canvas = canvas,
                text = item.description,
                x = mCol1 + 16f,
                boxTop = rTop + 4f,
                boxHeight = rowHeight - 8f,
                width = (mCol2 - mCol1 - 28f).toInt(),
                paint = rowTxtPaint,
                maxLines = 2
            )
            drawCenteredText(canvas, formatCleanQty(item.quantity), (mCol2 + mCol3) / 2f, baseY, rowTxtPaint)
            drawRightText(
                canvas,
                IndianCurrencyFormatter.formatInr(item.rate, settings.currencySymbol),
                mCol4 - 16f,
                baseY,
                rowTxtPaint
            )
            drawRightText(
                canvas,
                IndianCurrencyFormatter.formatInr(item.amount, settings.currencySymbol),
                mCol5 - 16f,
                baseY,
                rowBoldPaint
            )
        }

        val isFinalPage = pageIndex == totalPages - 1
        if (isFinalPage) {
            val footerTop = (rowsBottom + 36f).coerceAtMost(1830f)
            val words = IndianCurrencyFormatter.convertToIndianRupees(memo.totalPayable)
            drawStaticText(
                canvas = canvas,
                text = "In Words: $words",
                x = cLeft,
                y = footerTop,
                width = 810,
                paint = textPaint(24f, INK_DARK, bold = false),
                maxLines = 3
            )

            // UPI QR block
            val upiUri = IndianCurrencyFormatter.buildUpiUri(
                upiVpa = settings.upiVpa,
                payeeName = settings.shopNameEnglish,
                amount = memo.totalPayable
            )
            val qrBitmap = QrCodeGenerator.generateBitmap(upiUri, sizePx = 280)
            if (qrBitmap != null) {
                val qrTop = footerTop + 100f
                val qrRect = RectF(cLeft, qrTop, cLeft + 220f, qrTop + 220f)
                canvas.drawBitmap(qrBitmap, null, qrRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.drawRect(qrRect, innerStroke)
                qrBitmap.recycle()

                canvas.drawText("UPI: ${settings.upiVpa}", cLeft + 240f, qrTop + 95f, textPaint(24f, INK_DARK, bold = true))
                canvas.drawText("Scan to Pay", cLeft + 240f, qrTop + 132f, textPaint(23f, INK_MUTED, bold = false))
            }

            // Right Totals Column
            val totLeft = 980f
            var ty = footerTop + 24f
            val totPaint = textPaint(25f, INK_DARK, bold = false)
            val totBoldMaroon = textPaint(28f, MODERN_MAROON, bold = true)

            drawSummaryLine(canvas, totLeft, cRight, ty, "Subtotal", IndianCurrencyFormatter.formatInr(memo.subtotal, settings.currencySymbol), totPaint)
            ty += 42f
            val disc = computeDiscountRupees(memo)
            if (disc > 0.009) {
                drawSummaryLine(canvas, totLeft, cRight, ty, "Discount", "- ${IndianCurrencyFormatter.formatInr(disc, settings.currencySymbol)}", totPaint)
                ty += 42f
            }
            if (memo.taxAmount > 0.009) {
                drawSummaryLine(canvas, totLeft, cRight, ty, "Tax (${formatCleanQty(memo.taxPercentage)}%)", IndianCurrencyFormatter.formatInr(memo.taxAmount, settings.currencySymbol), totPaint)
                ty += 42f
            }
            if (memo.previousDues > 0.009) {
                drawSummaryLine(canvas, totLeft, cRight, ty, "Previous Dues", "+ ${IndianCurrencyFormatter.formatInr(memo.previousDues, settings.currencySymbol)}", totPaint)
                ty += 42f
            }

            canvas.drawLine(totLeft, ty - 20f, cRight, ty - 20f, dividerPaint)
            ty += 12f
            drawSummaryLine(canvas, totLeft, cRight, ty, "Total Payable", IndianCurrencyFormatter.formatInr(memo.totalPayable, settings.currencySymbol), totBoldMaroon)
            ty += 20f
            canvas.drawLine(totLeft, ty, cRight, ty, dividerPaint)
            ty += 40f

            drawSummaryLine(canvas, totLeft, cRight, ty, "Paid (${memo.paymentMethod})", IndianCurrencyFormatter.formatInr(memo.paidAmount, settings.currencySymbol), totPaint)
            if (memo.hasPendingDue) {
                ty += 42f
                drawSummaryLine(
                    canvas,
                    totLeft,
                    cRight,
                    ty,
                    "Carry Forward",
                    IndianCurrencyFormatter.formatInr(memo.balanceDue, settings.currencySymbol),
                    textPaint(26f, DUE_RED, bold = true)
                )
            }
        } else {
            drawRightText(
                canvas,
                "Continued on Page ${pageIndex + 2} of $totalPages →",
                cRight,
                rowsBottom + 56f,
                textPaint(26f, MODERN_MAROON, bold = true)
            )
        }

        // Terms & Signatory
        drawStaticText(
            canvas = canvas,
            text = settings.termsNote,
            x = cLeft,
            y = 2388f,
            width = 1050,
            paint = textPaint(22f, INK_MUTED, bold = false),
            maxLines = 2
        )
        drawRightText(
            canvas,
            "For ${settings.signerName.ifBlank { "VISHAL NURSERY" }}",
            cRight,
            2426f,
            textPaint(26f, MODERN_MAROON, bold = true)
        )
    }

    // -------------------------------------------------------------------------
    // Vector Emblems, Watermark & Typography Helpers
    // -------------------------------------------------------------------------

    private fun drawRosetteEmblem(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val petalCenterDist = radius * 0.56f
        val petalRadius = radius * 0.40f
        for (i in 0 until 6) {
            val angleRad = Math.toRadians((i * 60).toDouble())
            val px = cx + (petalCenterDist * cos(angleRad)).toFloat()
            val py = cy + (petalCenterDist * sin(angleRad)).toFloat()
            canvas.drawCircle(px, py, petalRadius, fillPaint)
        }
        val coreHole = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius * 0.24f, coreHole)
        canvas.drawCircle(cx, cy, radius * 0.13f, fillPaint)
    }

    private fun drawSproutEmblem(canvas: Canvas, cx: Float, bottomY: Float, height: Float, color: Int) {
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 3.8f
            strokeCap = Paint.Cap.ROUND
        }
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawLine(cx, bottomY, cx, bottomY - height * 0.72f, stemPaint)
        // Left leaf
        val leftLeaf = Path().apply {
            moveTo(cx, bottomY - height * 0.38f)
            cubicTo(cx - 26f, bottomY - height * 0.44f, cx - 28f, bottomY - height * 0.78f, cx - 4f, bottomY - height * 0.76f)
            cubicTo(cx - 2f, bottomY - height * 0.62f, cx - 2f, bottomY - height * 0.48f, cx, bottomY - height * 0.38f)
            close()
        }
        // Right leaf
        val rightLeaf = Path().apply {
            moveTo(cx, bottomY - height * 0.52f)
            cubicTo(cx + 26f, bottomY - height * 0.58f, cx + 28f, bottomY - height * 0.92f, cx + 4f, bottomY - height * 0.90f)
            cubicTo(cx + 2f, bottomY - height * 0.76f, cx + 2f, bottomY - height * 0.62f, cx, bottomY - height * 0.52f)
            close()
        }
        canvas.drawPath(leftLeaf, leafPaint)
        canvas.drawPath(rightLeaf, leafPaint)
    }

    private fun drawWatermarkOvalRings(canvas: Canvas, cx: Float, cy: Float) {
        val outerOvalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_WATERMARK
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val innerOvalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLASSIC_WATERMARK
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        // Tilted overlapping sepia/light-grey oval rings behind item rows
        canvas.save()
        canvas.rotate(-14f, cx, cy)
        canvas.drawOval(RectF(cx - 310f, cy - 190f, cx + 310f, cy + 190f), outerOvalPaint)
        canvas.restore()

        canvas.save()
        canvas.rotate(14f, cx, cy)
        canvas.drawOval(RectF(cx - 290f, cy - 175f, cx + 290f, cy + 175f), innerOvalPaint)
        canvas.restore()

        canvas.drawOval(RectF(cx - 230f, cy - 140f, cx + 230f, cy + 140f), innerOvalPaint)
        drawSproutEmblem(canvas, cx, cy + 48f, 96f, CLASSIC_WATERMARK)
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean = false): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            this.textSize = size
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, baselineY: Float, paint: Paint) {
        val oldAlign = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, centerX, baselineY, paint)
        paint.textAlign = oldAlign
    }

    private fun drawCenteredTextOrLeft(canvas: Canvas, text: String, x: Float, baselineY: Float, paint: Paint) {
        val oldAlign = paint.textAlign
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, x, baselineY, paint)
        paint.textAlign = oldAlign
    }

    private fun drawRightText(canvas: Canvas, text: String, rightX: Float, baselineY: Float, paint: Paint) {
        val oldAlign = paint.textAlign
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, rightX, baselineY, paint)
        paint.textAlign = oldAlign
    }

    private fun drawSummaryLine(
        canvas: Canvas,
        leftX: Float,
        rightX: Float,
        baselineY: Float,
        label: String,
        value: String,
        paint: Paint
    ) {
        canvas.drawText(label, leftX, baselineY, paint)
        drawRightText(canvas, value, rightX, baselineY, paint)
    }

    private fun drawSingleLineEllipsized(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        paint: TextPaint
    ) {
        if (maxWidth <= 0f) return
        val clipped = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clipped, x, baselineY, paint)
    }

    private fun drawStaticText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        paint: TextPaint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = 2
    ): Float {
        if (text.isEmpty() || width <= 0) return 0f
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun drawCenteredVerticalStaticText(
        canvas: Canvas,
        text: String,
        x: Float,
        boxTop: Float,
        boxHeight: Float,
        width: Int,
        paint: TextPaint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = 2
    ) {
        if (text.isEmpty() || width <= 0) return
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        val yOffset = boxTop + ((boxHeight - layout.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(x, yOffset)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun computeDiscountRupees(memo: CashMemo): Double {
        return if (memo.discountType == DiscountType.PERCENT) {
            ((memo.subtotal * memo.discount) / 100.0 * 100.0).roundToLong() / 100.0
        } else {
            (min(memo.subtotal, memo.discount) * 100.0).roundToLong() / 100.0
        }
    }

    private fun splitRupeesPaise(amount: Double): Pair<String, String> {
        val totalPaise = (abs(amount) * 100.0).roundToLong()
        val rupees = totalPaise / 100L
        val paise = (totalPaise % 100L).toInt()
        val rupeesText = inrPlainInteger.format(rupees)
        val paiseText = if (paise == 0) "--" else String.format(Locale.US, "%02d", paise)
        return rupeesText to paiseText
    }

    private fun formatRateNoSymbol(rate: Double): String {
        return String.format(Locale.US, "%.2f", rate)
    }

    private fun formatPlainMoney(amount: Double): String {
        return inrPlainDecimal.format(amount)
    }

    private fun formatCleanQty(q: Double): String {
        return if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
    }

    private fun sanitizeFileToken(raw: String): String {
        val clean = raw.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")
        return clean.ifBlank { "Bill" }
    }
}
