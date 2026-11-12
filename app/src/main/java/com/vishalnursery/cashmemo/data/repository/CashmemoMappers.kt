package com.vishalnursery.cashmemo.data.repository

import com.vishalnursery.cashmemo.data.local.entity.CashMemoEntity
import com.vishalnursery.cashmemo.data.local.entity.CustomerEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoItemEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoWithItems
import com.vishalnursery.cashmemo.data.local.entity.PlantRateEntity
import com.vishalnursery.cashmemo.data.remote.dto.BillDto
import com.vishalnursery.cashmemo.data.remote.dto.CustomerDto
import com.vishalnursery.cashmemo.data.remote.dto.MemoItemDto
import com.vishalnursery.cashmemo.data.remote.dto.PlantRateDto
import com.vishalnursery.cashmemo.data.remote.dto.SettingsDto
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.model.PlantRate

fun MemoWithItems.toDomain(): CashMemo = CashMemo(
    id = memo.id,
    memoNumber = memo.memoNumber,
    date = memo.date,
    dueDate = memo.dueDate,
    customerName = memo.customerName,
    customerMobile = memo.customerMobile,
    customerAddress = memo.customerAddress,
    customerGstin = memo.customerGstin,
    items = items.sortedBy { it.serialNo }.map { it.toDomain() },
    discount = memo.discount,
    discountType = DiscountType.fromWire(memo.discountType),
    taxPercentage = memo.taxPercentage,
    taxAmount = memo.taxAmount,
    previousDues = memo.previousDues,
    subtotal = memo.subtotal,
    grandTotal = memo.grandTotal,
    paidAmount = memo.paidAmount,
    balanceDue = memo.balanceDue,
    paymentMethod = memo.paymentMethod,
    paymentStatusNote = memo.paymentStatusNote,
    notes = memo.notes,
    rawOcrText = memo.rawOcrText,
    sourceImageUri = memo.sourceImageUri,
    isSynced = memo.isSynced,
    createdAt = memo.createdAt,
    updatedAt = memo.updatedAt
)

fun MemoItemEntity.toDomain(): MemoItem = MemoItem(
    id = id,
    serialNo = serialNo,
    description = description,
    quantity = quantity,
    rate = rate
)

fun CashMemo.toEntity(): CashMemoEntity = CashMemoEntity(
    id = id,
    memoNumber = memoNumber,
    date = date,
    dueDate = dueDate,
    customerName = customerName,
    customerMobile = customerMobile,
    customerAddress = customerAddress,
    customerGstin = customerGstin,
    discount = discount,
    discountType = discountType.wireValue,
    taxPercentage = taxPercentage,
    taxAmount = taxAmount,
    previousDues = previousDues,
    subtotal = subtotal,
    grandTotal = grandTotal,
    paidAmount = paidAmount,
    balanceDue = balanceDue,
    paymentMethod = paymentMethod,
    paymentStatusNote = paymentStatusNote,
    notes = notes,
    rawOcrText = rawOcrText,
    sourceImageUri = sourceImageUri,
    isSynced = isSynced,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CashMemo.toItemEntities(): List<MemoItemEntity> = items.mapIndexed { idx, item ->
    MemoItemEntity(
        id = item.id,
        memoId = id,
        serialNo = idx + 1,
        description = item.description,
        quantity = item.quantity,
        rate = item.rate
    )
}

fun CashMemo.toDto(): BillDto = BillDto(
    id = id,
    memoNumber = memoNumber,
    date = date,
    dueDate = dueDate,
    customerName = customerName,
    customerMobile = customerMobile,
    customerAddress = customerAddress,
    customerGstin = customerGstin,
    items = items.mapIndexed { idx, item ->
        MemoItemDto(
            id = item.id,
            serialNo = idx + 1,
            description = item.description,
            quantity = item.quantity,
            rate = item.rate,
            amount = item.amount
        )
    },
    subtotal = subtotal,
    discount = discount,
    discountType = discountType.wireValue,
    taxPercentage = taxPercentage,
    taxAmount = taxAmount,
    previousDues = previousDues,
    grandTotal = grandTotal,
    paidAmount = paidAmount,
    balanceDue = balanceDue,
    paymentMethod = paymentMethod,
    paymentStatusNote = paymentStatusNote,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BillDto.toDomain(synced: Boolean = true): CashMemo = CashMemo(
    id = id.ifBlank { CashMemo.newMemoId() },
    memoNumber = memoNumber,
    date = date,
    dueDate = dueDate,
    customerName = customerName,
    customerMobile = customerMobile,
    customerAddress = customerAddress,
    customerGstin = customerGstin,
    items = items.mapIndexed { idx, dto ->
        MemoItem(
            id = dto.id.ifBlank { "itm_${idx}_${System.nanoTime()}" },
            serialNo = if (dto.serialNo > 0) dto.serialNo else idx + 1,
            description = dto.description,
            quantity = dto.quantity,
            rate = dto.rate
        )
    },
    discount = discount,
    discountType = DiscountType.fromWire(discountType),
    taxPercentage = taxPercentage,
    taxAmount = taxAmount,
    previousDues = previousDues,
    subtotal = subtotal,
    grandTotal = grandTotal,
    paidAmount = paidAmount,
    balanceDue = balanceDue,
    paymentMethod = paymentMethod,
    paymentStatusNote = paymentStatusNote,
    notes = notes,
    isSynced = synced,
    createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
    updatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
)

fun CustomerEntity.toDomain(): CustomerProfile = CustomerProfile(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    address = address,
    outstandingBalance = outstandingBalance,
    totalPurchased = totalPurchased,
    totalPaid = totalPaid,
    lastPromiseNote = lastPromiseNote,
    lastTransactionAt = lastTransactionAt,
    createdAt = createdAt
)

fun CustomerProfile.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    address = address,
    outstandingBalance = outstandingBalance,
    totalPurchased = totalPurchased,
    totalPaid = totalPaid,
    lastPromiseNote = lastPromiseNote,
    lastTransactionAt = lastTransactionAt,
    createdAt = createdAt
)

fun CustomerProfile.toDto(): CustomerDto = CustomerDto(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    address = address,
    outstandingBalance = outstandingBalance,
    totalPurchased = totalPurchased,
    totalPaid = totalPaid,
    lastPromiseNote = lastPromiseNote,
    lastTransactionAt = lastTransactionAt,
    createdAt = createdAt
)

fun CustomerDto.toDomain(): CustomerProfile = CustomerProfile(
    id = id.ifBlank { "cust_${System.currentTimeMillis()}" },
    name = name,
    phoneNumber = phoneNumber,
    address = address,
    outstandingBalance = outstandingBalance,
    totalPurchased = totalPurchased,
    totalPaid = totalPaid,
    lastPromiseNote = lastPromiseNote,
    lastTransactionAt = lastTransactionAt,
    createdAt = createdAt
)

fun PlantRateEntity.toDomain(): PlantRate = PlantRate(
    id = id,
    name = name,
    category = category,
    retailPrice = retailPrice,
    wholesalePrice = wholesalePrice
)

fun PlantRate.toEntity(): PlantRateEntity = PlantRateEntity(
    id = id,
    name = name,
    category = category,
    retailPrice = retailPrice,
    wholesalePrice = wholesalePrice
)

fun PlantRate.toDto(): PlantRateDto = PlantRateDto(
    id = id,
    name = name,
    category = category,
    retailPrice = retailPrice,
    wholesalePrice = wholesalePrice
)

fun PlantRateDto.toDomain(): PlantRate = PlantRate(
    id = id.ifBlank { "pr_${System.currentTimeMillis()}" },
    name = name,
    category = category,
    retailPrice = retailPrice,
    wholesalePrice = wholesalePrice
)

fun BusinessSettings.toDto(): SettingsDto = SettingsDto(
    shopNameHindi = shopNameHindi,
    shopNameEnglish = shopNameEnglish,
    tagline = tagline,
    phone = phone,
    email = email,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    gstin = gstin.trim().takeUnless { it.equals("NaN", ignoreCase = true) } ?: "",
    currencySymbol = currencySymbol,
    memoPrefix = memoPrefix,
    nextMemoNumber = nextMemoNumber,
    defaultTaxPercentage = defaultTaxPercentage,
    enableTax = enableTax,
    taxType = taxType,
    upiVpa = upiVpa,
    upiDefaultNote = upiDefaultNote,
    signerName = signerName,
    termsNote = termsNote
)

fun SettingsDto.mergeInto(current: BusinessSettings): BusinessSettings = current.copy(
    shopNameHindi = shopNameHindi.ifBlank { current.shopNameHindi },
    shopNameEnglish = shopNameEnglish.ifBlank { current.shopNameEnglish },
    tagline = tagline.ifBlank { current.tagline },
    phone = phone.ifBlank { current.phone },
    email = email.ifBlank { current.email },
    addressLine1 = addressLine1.ifBlank { current.addressLine1 },
    addressLine2 = addressLine2.ifBlank { current.addressLine2 },
    gstin = gstin.trim().takeIf { it.isNotBlank() && !it.equals("NaN", ignoreCase = true) } ?: current.gstin,
    currencySymbol = currencySymbol.ifBlank { "₹" },
    memoPrefix = memoPrefix.ifBlank { current.memoPrefix },
    nextMemoNumber = maxOf(current.nextMemoNumber, nextMemoNumber),
    defaultTaxPercentage = defaultTaxPercentage,
    enableTax = enableTax,
    taxType = taxType.ifBlank { "GST" },
    upiVpa = upiVpa.ifBlank { current.upiVpa },
    upiDefaultNote = upiDefaultNote.ifBlank { current.upiDefaultNote },
    signerName = signerName.ifBlank { current.signerName },
    termsNote = termsNote.ifBlank { current.termsNote }
)
