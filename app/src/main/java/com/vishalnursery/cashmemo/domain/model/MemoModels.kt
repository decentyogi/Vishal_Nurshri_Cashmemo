package com.vishalnursery.cashmemo.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Core domain entity representing a cash memo / invoice, compatible with Cashmemo_AI_Web.
 */
data class CashMemo(
    val id: String = newMemoId(),
    val memoNumber: String = "1001",
    val date: String = todayFormatted(),
    val dueDate: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val customerGstin: String = "",
    val items: List<MemoItem> = listOf(MemoItem()),
    val discount: Double = 0.0,
    val discountType: DiscountType = DiscountType.AMOUNT,
    val taxPercentage: Double = 0.0,
    val taxAmount: Double = 0.0,
    val previousDues: Double = 0.0,
    val subtotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val balanceDue: Double = 0.0,
    val paymentMethod: String = "Cash",
    val paymentStatusNote: String = "Carry Forward",
    val notes: String = "",
    val rawOcrText: String = "",
    val sourceImageUri: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalPayable: Double
        get() = round2(grandTotal + previousDues)

    val hasPendingDue: Boolean
        get() = balanceDue > 0.009

    companion object {
        fun newMemoId(): String =
            "memo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}"

        fun todayFormatted(): String =
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())
    }
}

enum class DiscountType(val wireValue: String, val label: String) {
    AMOUNT("amount", "₹"),
    PERCENT("%", "%");

    companion object {
        fun fromWire(value: String?): DiscountType =
            if (value == "%") PERCENT else AMOUNT
    }
}

data class MemoItem(
    val id: String = "itm_${UUID.randomUUID().toString().take(8)}",
    val serialNo: Int = 1,
    val description: String = "",
    val quantity: Double = 1.0,
    val rate: Double = 0.0
) {
    val amount: Double
        get() = round2(max(0.0, quantity * rate))
}

data class CustomerProfile(
    val id: String = "cust_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
    val name: String,
    val phoneNumber: String = "",
    val address: String = "",
    val outstandingBalance: Double = 0.0,
    val totalPurchased: Double = 0.0,
    val totalPaid: Double = 0.0,
    val lastPromiseNote: String = "Carry Forward",
    val lastTransactionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class PlantRate(
    val id: String = "pr_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
    val name: String,
    val category: String = "Plants",
    val retailPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0
)

data class BusinessSettings(
    val shopNameHindi: String = "विशाल नर्सरी",
    val shopNameEnglish: String = "Vishal Nursery",
    val tagline: String = "हमारे यहाँ हर प्रकार के फूल, पेड़-पौधा जैसे-गुलाब, गुलदावदी, गेंदा, क्रोटन सब गमले में तैयार पौधा भी मिलता है।",
    val phone: String = "7631873743",
    val email: String = "vishalnursery.sasaram@gmail.com",
    val addressLine1: String = "ग्राम-कोटा, माँ ताराचण्डी रोड (माँ सीताराम मंदिर)",
    val addressLine2: String = "पो०-सासाराम (रोहतास), बिहार - 821115",
    val gstin: String = "NaN",
    val currencySymbol: String = "₹",
    val memoPrefix: String = "VN-",
    val nextMemoNumber: Int = 1001,
    val defaultTaxPercentage: Double = 0.0,
    val enableTax: Boolean = false,
    val taxType: String = "GST",
    val upiVpa: String = "dangerousop@ybl",
    val signerName: String = "VISHAL NURSERY",
    val termsNote: String = "Thanks For Visiting Vishal Nurshri !! Visit Again... Thanks",
    val serverBaseUrl: String = "",
    val autoSyncEnabled: Boolean = false
)

enum class MemoFilter(val label: String, val wireValue: String) {
    ALL("All", "ALL"),
    PENDING_DUE("Pending Due", "PENDING_DUE"),
    PAID("Paid", "PAID")
}

enum class OcrScriptMode(val label: String) {
    HINDI_AND_ENGLISH("Hindi + English"),
    ENGLISH_ONLY("English / Numbers")
}

data class MemoCalculationResult(
    val subtotal: Double,
    val overallDiscountAmount: Double,
    val taxableBase: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val previousDues: Double,
    val totalPayable: Double,
    val effectivePaid: Double,
    val balanceDue: Double,
    val changeToReturn: Double
) {
    companion object {
        /**
         * Mirrors Cashmemo_AI_Web's recalculateAll() calculation rules.
         */
        fun compute(
            items: List<MemoItem>,
            discount: Double,
            discountType: DiscountType,
            taxPercentage: Double,
            previousDues: Double,
            paidAmount: Double,
            hasCustomPayment: Boolean
        ): MemoCalculationResult {
            val subtotal = round2(items.sumOf { max(0.0, it.quantity * it.rate) })
            val rawDiscount = max(0.0, discount)
            val effectiveDiscount = when (discountType) {
                DiscountType.PERCENT -> (subtotal * rawDiscount) / 100.0
                DiscountType.AMOUNT -> rawDiscount
            }.coerceIn(0.0, subtotal)

            val taxableBase = round2(max(0.0, subtotal - effectiveDiscount))
            val cleanTaxRate = max(0.0, taxPercentage)
            val taxAmount = round2((taxableBase * cleanTaxRate) / 100.0)
            val grandTotal = round2(taxableBase + taxAmount)
            val cleanPrevDues = max(0.0, previousDues)
            val totalPayable = round2(grandTotal + cleanPrevDues)

            val effectivePaid = if (hasCustomPayment) round2(max(0.0, paidAmount)) else totalPayable
            val balanceDue = if (hasCustomPayment) round2(max(0.0, totalPayable - effectivePaid)) else 0.0
            val changeToReturn = if (hasCustomPayment) round2(max(0.0, effectivePaid - totalPayable)) else 0.0

            return MemoCalculationResult(
                subtotal = subtotal,
                overallDiscountAmount = round2(effectiveDiscount),
                taxableBase = taxableBase,
                taxRate = cleanTaxRate,
                taxAmount = taxAmount,
                grandTotal = grandTotal,
                previousDues = cleanPrevDues,
                totalPayable = totalPayable,
                effectivePaid = effectivePaid,
                balanceDue = balanceDue,
                changeToReturn = changeToReturn
            )
        }
    }
}

data class DashboardStats(
    val totalMemos: Int = 0,
    val totalSales: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalPendingDues: Double = 0.0
)

fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
