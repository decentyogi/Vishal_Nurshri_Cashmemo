package com.vishalnursery.cashmemo.domain.util

import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Indian Numbering System currency & words converter ported from Cashmemo_AI_Web (numberToWords.js).
 */
object IndianCurrencyFormatter {

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private val indianSymbols = DecimalFormatSymbols(Locale("en", "IN"))
    private val indianDecimalFormat = DecimalFormat("##,##,##0.00", indianSymbols)
    private val indianCompactFormat = DecimalFormat("##,##,##0", indianSymbols)

    fun formatInr(amount: Double, symbol: String = "₹", includeDecimals: Boolean = true): String {
        val formatted = if (includeDecimals) {
            indianDecimalFormat.format(amount)
        } else {
            indianCompactFormat.format(amount)
        }
        return "$symbol $formatted"
    }

    fun convertToIndianRupees(
        amount: Double,
        currencyName: String = "Rupees",
        paiseName: String = "Paise"
    ): String {
        if (amount.isNaN() || amount == 0.0) {
            return "Zero $currencyName Only"
        }

        val totalPaise = (abs(amount) * 100.0).roundToLong()
        val rupees = totalPaise / 100L
        val paise = (totalPaise % 100L).toInt()

        val sb = StringBuilder()
        if (rupees > 0L) {
            sb.append(convertIndianNumber(rupees)).append(" ").append(currencyName)
        }
        if (paise > 0) {
            if (sb.isNotEmpty()) sb.append(" and ")
            sb.append(convertLessThanThousand(paise)).append(" ").append(paiseName)
        }
        sb.append(" Only")
        return if (amount < 0) "Minus ${sb.toString().trim()}" else sb.toString().trim()
    }

    fun isValidUpiVpa(upiVpa: String): Boolean {
        val clean = upiVpa.trim()
        return clean.isNotEmpty() && !clean.equals("NaN", ignoreCase = true) && clean.contains("@")
    }

    fun isValidGstin(gstin: String): Boolean {
        val clean = gstin.trim()
        return clean.isNotEmpty() &&
            !clean.equals("NaN", ignoreCase = true) &&
            !clean.equals("N/A", ignoreCase = true)
    }

    fun resolveBillQrAmount(paidAmount: Double, totalPayable: Double): Double {
        val cleanPaid = if (paidAmount.isFinite()) paidAmount else 0.0
        val cleanTotal = if (totalPayable.isFinite()) totalPayable else 0.0
        return when {
            cleanPaid > 0.009 -> cleanPaid
            cleanTotal > 0.009 -> cleanTotal
            else -> 0.0
        }
    }

    fun buildUpiUri(
        upiVpa: String,
        payeeName: String,
        amount: Double,
        note: String = "Plant Purchase"
    ): String {
        val cleanVpa = upiVpa.trim()
        if (!isValidUpiVpa(cleanVpa)) return ""
        val cleanName = payeeName.trim().ifEmpty { "Merchant" }
        val encodedName = URLEncoder.encode(cleanName, "UTF-8").replace("+", "%20")
        val cleanNote = note.trim()
        val encodedNote = URLEncoder.encode(cleanNote, "UTF-8").replace("+", "%20")
        val cleanAmount = if (amount.isFinite()) amount else 0.0
        return buildString {
            append("upi://pay?pa=").append(cleanVpa)
            append("&pn=").append(encodedName)
            if (cleanAmount > 0.009) {
                append("&am=").append(String.format(Locale.US, "%.2f", cleanAmount))
            }
            append("&cu=INR")
            if (cleanNote.isNotEmpty()) {
                append("&tn=").append(encodedNote)
            }
        }
    }

    private fun convertIndianNumber(number: Long): String {
        var num = number
        if (num == 0L) return ""

        val crore = (num / 10_000_000L).toInt()
        num %= 10_000_000L

        val lakh = (num / 100_000L).toInt()
        num %= 100_000L

        val thousand = (num / 1_000L).toInt()
        num %= 1_000L

        val remainder = num.toInt()

        val parts = mutableListOf<String>()
        if (crore > 0) parts.add("${convertLessThanThousand(crore)} Crore")
        if (lakh > 0) parts.add("${convertLessThanThousand(lakh)} Lakh")
        if (thousand > 0) parts.add("${convertLessThanThousand(thousand)} Thousand")
        if (remainder > 0) parts.add(convertLessThanThousand(remainder))
        return parts.joinToString(" ").trim()
    }

    private fun convertLessThanThousand(n: Int): String {
        val parts = mutableListOf<String>()
        if (n >= 100) {
            parts.add("${units[floor(n / 100.0).toInt()]} Hundred")
        }
        val rem = n % 100
        if (rem in 1..19) {
            parts.add(units[rem])
        } else if (rem >= 20) {
            parts.add(tens[rem / 10])
            if (rem % 10 > 0) {
                parts.add(units[rem % 10])
            }
        }
        return parts.joinToString(" ").trim()
    }
}
