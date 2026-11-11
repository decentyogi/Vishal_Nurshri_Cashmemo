package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.model.round2

/**
 * Extracts structured cashmemo fields (vendor/customer, invoice #, date, phone, line items,
 * tax, discount, and totals) from raw ML Kit OCR text.
 */
object ReceiptParser {

    data class ExtractedReceipt(
        val memoNumber: String?,
        val date: String?,
        val partyName: String?,
        val phoneNumber: String?,
        val items: List<MemoItem>,
        val discount: Double,
        val taxPercentage: Double,
        val previousDues: Double,
        val detectedTotal: Double?,
        val paidAmount: Double?,
        val rawText: String
    )

    private val memoNumRegex = Regex(
        """(?i)(?:bill|memo|invoice|inv|receipt|cash\s*memo|क्रमांक)\s*(?:no\.?|number|#|:|-)?\s*[:#-]?\s*([A-Z0-9-]{2,16})"""
    )
    private val dateRegex = Regex(
        """\b(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}|\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2})\b"""
    )
    private val phoneRegex = Regex(
        """(?:\+91[\s-]?)?([6-9]\d{9})\b"""
    )
    private val qtyRateAmountRegex = Regex(
        """^(.{2,55}?)\s+(\d+(?:\.\d+)?)\s*[xX×*]\s*(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d+)?)(?:\s*=?\s*(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d+)?))?$"""
    )
    private val threeColItemRegex = Regex(
        """^(?:\d+[.)]\s*)?(.{2,55}?)\s+(\d+(?:\.\d+)?)\s+(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d+)?)\s+(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d+)?)$"""
    )
    private val singlePriceItemRegex = Regex(
        """^(?:\d+[.)]\s*)?([A-Za-z\u0900-\u097F][A-Za-z0-9\u0900-\u097F\s/&(),.-]{1,50}?)\s+(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)$"""
    )
    private val taxPercentRegex = Regex(
        """(?i)(?:gst|cgst|sgst|igst|tax)\s*@?\s*(\d{1,2}(?:\.\d+)?)\s*%"""
    )
    private val discountAmountRegex = Regex(
        """(?i)(?:discount|disc|छूट)\s*[:=-]?\s*(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)"""
    )
    private val prevDueRegex = Regex(
        """(?i)(?:prev(?:ious)?\s*dues?|old\s*bal(?:ance)?|बकाया)\s*[:=-]?\s*(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)"""
    )
    private val grandTotalRegex = Regex(
        """(?i)(?:grand\s*total|total\s*payable|net\s*total|total\s*amount|total|कुल)\s*[:=-]?\s*(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)"""
    )
    private val paidRegex = Regex(
        """(?i)(?:paid|received|cash\s*paid|जमा)\s*[:=-]?\s*(?:₹|Rs\.?|INR)?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)"""
    )
    private val partyLabelRegex = Regex(
        """(?i)(?:bill\s*to|customer|party|vendor|name|M/s|श्री|नाम)\s*[:.-]\s*(.+)"""
    )

    private val summaryKeywords = listOf(
        "subtotal", "sub total", "total", "grand total", "discount", "gst", "cgst", "sgst",
        "tax", "paid", "balance", "due", "carry forward", "amount in words", "authorized",
        "signatory", "phone", "mob", "gstin", "invoice", "memo", "date", "upi", "thanks"
    )

    fun parse(rawText: String): ExtractedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var memoNumber: String? = null
        var date: String? = null
        var partyName: String? = null
        var phoneNumber: String? = null
        var discount = 0.0
        var taxPercentage = 0.0
        var previousDues = 0.0
        var detectedTotal: Double? = null
        var paidAmount: Double? = null
        val items = mutableListOf<MemoItem>()

        for (line in lines) {
            if (memoNumber == null) {
                memoNumRegex.find(line)?.groupValues?.getOrNull(1)?.let { candidate ->
                    if (candidate.any { it.isDigit() }) memoNumber = candidate.trim()
                }
            }
            if (date == null) {
                dateRegex.find(line)?.groupValues?.getOrNull(1)?.let { rawDate ->
                    date = normalizeDate(rawDate)
                }
            }
            if (phoneNumber == null) {
                phoneRegex.find(line)?.groupValues?.getOrNull(1)?.let { phone ->
                    phoneNumber = phone
                }
            }
            if (partyName == null) {
                partyLabelRegex.find(line)?.groupValues?.getOrNull(1)?.let { name ->
                    val cleaned = name.trim()
                    if (cleaned.length >= 2) partyName = cleaned
                }
            }

            taxPercentRegex.find(line)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { pct ->
                if (pct in 0.0..50.0) taxPercentage = pct
            }
            discountAmountRegex.find(line)?.groupValues?.getOrNull(1)?.let { raw ->
                discount = parseNumber(raw) ?: discount
            }
            prevDueRegex.find(line)?.groupValues?.getOrNull(1)?.let { raw ->
                previousDues = parseNumber(raw) ?: previousDues
            }
            val totalRaw = grandTotalRegex.find(line)?.groupValues?.getOrNull(1)
            val totalVal = totalRaw?.let(::parseNumber)
            val currentDetected = detectedTotal
            if (totalVal != null && (currentDetected == null || totalVal > currentDetected)) {
                detectedTotal = totalVal
            }
            val paidRaw = paidRegex.find(line)?.groupValues?.getOrNull(1)
            if (paidRaw != null) {
                paidAmount = parseNumber(paidRaw) ?: paidAmount
            }

            // Skip header/summary lines before attempting line-item extraction
            val lower = line.lowercase()
            if (summaryKeywords.any { lower.contains(it) }) continue

            parseLineItem(line, items.size + 1)?.let { items.add(it) }
        }

        // Fallback: use the first clean non-numeric header line as vendor/party name if none labeled
        if (partyName == null) {
            partyName = lines.firstOrNull { line ->
                val lower = line.lowercase()
                line.length in 3..48 &&
                    !lower.contains("cash memo") &&
                    !lower.contains("tax invoice") &&
                    !dateRegex.containsMatchIn(line) &&
                    line.count { it.isLetter() } >= 3
            }
        }

        // If no structured items were found, keep one editable row prefilled if a total exists
        val finalTotal = detectedTotal
        val finalItems = if (items.isNotEmpty()) {
            items
        } else if (finalTotal != null && finalTotal > 0.0) {
            listOf(MemoItem(serialNo = 1, description = "Scanned Memo Items", quantity = 1.0, rate = finalTotal))
        } else {
            listOf(MemoItem(serialNo = 1))
        }

        return ExtractedReceipt(
            memoNumber = memoNumber,
            date = date,
            partyName = partyName,
            phoneNumber = phoneNumber,
            items = finalItems,
            discount = round2(discount),
            taxPercentage = round2(taxPercentage),
            previousDues = round2(previousDues),
            detectedTotal = detectedTotal,
            paidAmount = paidAmount,
            rawText = rawText
        )
    }

    fun toDraftMemo(
        extracted: ExtractedReceipt,
        defaultMemoNumber: String,
        sourceImageUri: String = ""
    ): CashMemo {
        val cleanItems = extracted.items.mapIndexed { idx, item -> item.copy(serialNo = idx + 1) }
        val subtotal = round2(cleanItems.sumOf { it.amount })
        val taxable = (subtotal - extracted.discount).coerceAtLeast(0.0)
        val taxAmount = round2((taxable * extracted.taxPercentage) / 100.0)
        val grandTotal = round2(taxable + taxAmount)
        val totalPayable = round2(grandTotal + extracted.previousDues)
        val paid = extracted.paidAmount ?: totalPayable
        val due = round2((totalPayable - paid).coerceAtLeast(0.0))

        return CashMemo(
            memoNumber = extracted.memoNumber ?: defaultMemoNumber,
            date = extracted.date ?: CashMemo.todayFormatted(),
            customerName = extracted.partyName.orEmpty(),
            customerMobile = extracted.phoneNumber.orEmpty(),
            items = cleanItems,
            discount = extracted.discount,
            discountType = DiscountType.AMOUNT,
            taxPercentage = extracted.taxPercentage,
            taxAmount = taxAmount,
            previousDues = extracted.previousDues,
            subtotal = subtotal,
            grandTotal = grandTotal,
            paidAmount = paid,
            balanceDue = due,
            paymentStatusNote = if (due > 0.009) "Carry Forward" else "Fully Paid",
            rawOcrText = extracted.rawText,
            sourceImageUri = sourceImageUri
        )
    }

    private fun parseLineItem(line: String, serialNo: Int): MemoItem? {
        qtyRateAmountRegex.matchEntire(line)?.let { match ->
            val desc = match.groupValues[1].trim()
            val qty = match.groupValues[2].toDoubleOrNull() ?: 1.0
            val rate = parseNumber(match.groupValues[3]) ?: return null
            if (desc.length >= 2 && qty > 0.0 && rate >= 0.0) {
                return MemoItem(serialNo = serialNo, description = desc, quantity = qty, rate = rate)
            }
        }

        threeColItemRegex.matchEntire(line)?.let { match ->
            val desc = match.groupValues[1].trim()
            val qty = match.groupValues[2].toDoubleOrNull() ?: 1.0
            val rate = parseNumber(match.groupValues[3])
            val amount = parseNumber(match.groupValues[4])
            if (desc.length >= 2 && qty > 0.0 && rate != null && amount != null) {
                // Verify consistency when possible (qty * rate approx == amount)
                val effectiveRate = if (kotlin.math.abs(qty * rate - amount) <= 2.0) rate else round2(amount / qty)
                return MemoItem(serialNo = serialNo, description = desc, quantity = qty, rate = effectiveRate)
            }
        }

        singlePriceItemRegex.matchEntire(line)?.let { match ->
            val desc = match.groupValues[1].trim()
            val price = parseNumber(match.groupValues[2]) ?: return null
            if (desc.length >= 2 && price > 0.0 && price < 1_000_000.0) {
                return MemoItem(serialNo = serialNo, description = desc, quantity = 1.0, rate = price)
            }
        }
        return null
    }

    private fun parseNumber(raw: String): Double? =
        raw.replace(",", "").trim().toDoubleOrNull()

    private fun normalizeDate(raw: String): String {
        val parts = raw.split('/', '-', '.')
        if (parts.size != 3) return raw
        return if (parts[0].length == 4) {
            "${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[0]}"
        } else {
            val year = if (parts[2].length == 2) "20${parts[2]}" else parts[2]
            "${parts[0].padStart(2, '0')}/${parts[1].padStart(2, '0')}/$year"
        }
    }
}
