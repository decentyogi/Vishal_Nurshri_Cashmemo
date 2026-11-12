package com.vishalnursery.cashmemo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SettingsDto(
    @SerializedName("shopNameHindi") val shopNameHindi: String = "विशाल नर्सरी",
    @SerializedName("shopNameEnglish") val shopNameEnglish: String = "Vishal Nursery",
    @SerializedName("tagline") val tagline: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("addressLine1") val addressLine1: String = "",
    @SerializedName("addressLine2") val addressLine2: String = "",
    @SerializedName("gstin") val gstin: String = "",
    @SerializedName("currencySymbol") val currencySymbol: String = "₹",
    @SerializedName("memoPrefix") val memoPrefix: String = "VN-",
    @SerializedName("nextMemoNumber") val nextMemoNumber: Int = 1001,
    @SerializedName("defaultTaxPercentage") val defaultTaxPercentage: Double = 0.0,
    @SerializedName("enableTax") val enableTax: Boolean = false,
    @SerializedName("taxType") val taxType: String = "GST",
    @SerializedName("upiVpa") val upiVpa: String = "",
    @SerializedName("upiDefaultNote") val upiDefaultNote: String = "",
    @SerializedName("signerName") val signerName: String = "VISHAL NURSERY",
    @SerializedName("termsNote") val termsNote: String = "Thanks For Visiting Vishal Nurshri !! Visit Again... Thanks"
)

data class MemoItemDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("serialNo") val serialNo: Int = 1,
    @SerializedName("description") val description: String = "",
    @SerializedName("quantity") val quantity: Double = 1.0,
    @SerializedName("rate") val rate: Double = 0.0,
    @SerializedName("amount") val amount: Double = 0.0
)

data class BillDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("memoNumber") val memoNumber: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("dueDate") val dueDate: String = "",
    @SerializedName("customerName") val customerName: String = "",
    @SerializedName("customerMobile") val customerMobile: String = "",
    @SerializedName("customerAddress") val customerAddress: String = "",
    @SerializedName("customerGstin") val customerGstin: String = "",
    @SerializedName("items") val items: List<MemoItemDto> = emptyList(),
    @SerializedName("subtotal") val subtotal: Double = 0.0,
    @SerializedName("discount") val discount: Double = 0.0,
    @SerializedName("discountType") val discountType: String = "amount",
    @SerializedName("taxPercentage") val taxPercentage: Double = 0.0,
    @SerializedName("taxAmount") val taxAmount: Double = 0.0,
    @SerializedName("previousDues") val previousDues: Double = 0.0,
    @SerializedName("grandTotal") val grandTotal: Double = 0.0,
    @SerializedName("paidAmount") val paidAmount: Double = 0.0,
    @SerializedName("balanceDue") val balanceDue: Double = 0.0,
    @SerializedName("paymentMethod") val paymentMethod: String = "Cash",
    @SerializedName("paymentStatusNote") val paymentStatusNote: String = "Carry Forward",
    @SerializedName("notes") val notes: String = "",
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("updatedAt") val updatedAt: Long = 0L
)

data class CustomerDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("phoneNumber") val phoneNumber: String = "",
    @SerializedName("address") val address: String = "",
    @SerializedName("outstandingBalance") val outstandingBalance: Double = 0.0,
    @SerializedName("totalPurchased") val totalPurchased: Double = 0.0,
    @SerializedName("totalPaid") val totalPaid: Double = 0.0,
    @SerializedName("lastPromiseNote") val lastPromiseNote: String = "Carry Forward",
    @SerializedName("lastTransactionAt") val lastTransactionAt: Long = 0L,
    @SerializedName("createdAt") val createdAt: Long = 0L
)

data class PlantRateDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("category") val category: String = "Plants",
    @SerializedName("retailPrice") val retailPrice: Double = 0.0,
    @SerializedName("wholesalePrice") val wholesalePrice: Double = 0.0
)

data class PaymentRequestDto(
    @SerializedName("amount") val amount: Double,
    @SerializedName("promiseNote") val promiseNote: String = "Carry Forward"
)

data class SaveBillResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("bill") val bill: BillDto? = null
)

data class GenericSuccessDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null
)

data class BackupPayloadDto(
    @SerializedName("version") val version: Int = 6,
    @SerializedName("appName") val appName: String = "Vishal_Nurshri_Cashmemo_Android",
    @SerializedName("exportDate") val exportDate: String = "",
    @SerializedName("settings") val settings: SettingsDto? = null,
    @SerializedName("bills") val bills: List<BillDto> = emptyList(),
    @SerializedName("customers") val customers: List<CustomerDto> = emptyList(),
    @SerializedName("rates") val rates: List<PlantRateDto> = emptyList()
)
