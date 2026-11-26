package com.vishalnursery.cashmemo.data.repository

import com.vishalnursery.cashmemo.data.local.entity.CashMemoEntity
import com.vishalnursery.cashmemo.data.local.entity.CustomerEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoItemEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoWithItems
import com.vishalnursery.cashmemo.data.local.entity.NoteEntity
import com.vishalnursery.cashmemo.data.local.entity.PaymentAllocationEntity
import com.vishalnursery.cashmemo.data.local.entity.PaymentEntity
import com.vishalnursery.cashmemo.data.local.entity.PaymentWithAllocations
import com.vishalnursery.cashmemo.data.local.entity.PlantRateEntity
import com.vishalnursery.cashmemo.data.local.entity.TodoEntity
import com.vishalnursery.cashmemo.data.remote.dto.BillDto
import com.vishalnursery.cashmemo.data.remote.dto.CustomerDto
import com.vishalnursery.cashmemo.data.remote.dto.MemoItemDto
import com.vishalnursery.cashmemo.data.remote.dto.NoteDto
import com.vishalnursery.cashmemo.data.remote.dto.PaymentAllocationDto
import com.vishalnursery.cashmemo.data.remote.dto.PaymentDto
import com.vishalnursery.cashmemo.data.remote.dto.PlantRateDto
import com.vishalnursery.cashmemo.data.remote.dto.SettingsDto
import com.vishalnursery.cashmemo.data.remote.dto.TodoDto
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerPayment
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.GeneralNote
import com.vishalnursery.cashmemo.domain.model.MemoItem
import com.vishalnursery.cashmemo.domain.model.MemoType
import com.vishalnursery.cashmemo.domain.model.OrderStatus
import com.vishalnursery.cashmemo.domain.model.PaymentAllocation
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.TodoPriority
import com.vishalnursery.cashmemo.domain.model.TodoTask

fun MemoWithItems.toDomain(laterPaid: Double = 0.0): CashMemo = CashMemo(
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
    isWholesale = memo.isWholesale,
    isSynced = memo.isSynced,
    memoType = MemoType.fromWire(memo.memoType),
    orderStatus = OrderStatus.fromWire(memo.orderStatus),
    linkedMemoId = memo.linkedMemoId,
    laterPaid = laterPaid,
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
    isWholesale = isWholesale,
    isSynced = isSynced,
    memoType = memoType.wireValue,
    orderStatus = if (memoType == MemoType.ORDER) orderStatus.wireValue else "",
    linkedMemoId = linkedMemoId,
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
    isWholesale = isWholesale,
    memoType = memoType.wireValue,
    orderStatus = if (memoType == MemoType.ORDER) orderStatus.wireValue else "",
    linkedMemoId = linkedMemoId,
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
    isWholesale = isWholesale ?: false,
    isSynced = synced,
    memoType = MemoType.fromWire(memoType),
    orderStatus = OrderStatus.fromWire(orderStatus),
    linkedMemoId = linkedMemoId ?: "",
    createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
    updatedAt = if (updatedAt > 0L) updatedAt else 0L
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
    wholesalePrice = wholesalePrice,
    updatedAt = updatedAt
)

fun PlantRate.toEntity(): PlantRateEntity = PlantRateEntity(
    id = id,
    name = name,
    category = category,
    retailPrice = retailPrice,
    wholesalePrice = wholesalePrice,
    updatedAt = updatedAt
)

fun PlantRate.toDto(): PlantRateDto = PlantRateDto(
    id = id,
    name = name,
    category = category,
    retailPrice = retailPrice,
    wholesalePrice = wholesalePrice,
    updatedAt = updatedAt
)

fun PlantRateDto.toDomain(): PlantRate = PlantRate(
    id = id.ifBlank { "pr_${System.currentTimeMillis()}" },
    name = name,
    category = category,
    retailPrice = retailPrice,
    wholesalePrice = wholesalePrice,
    updatedAt = updatedAt
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
    nextOrderNumber = nextOrderNumber,
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
    nextOrderNumber = maxOf(current.nextOrderNumber, nextOrderNumber ?: current.nextOrderNumber),
    defaultTaxPercentage = defaultTaxPercentage,
    enableTax = enableTax,
    taxType = taxType.ifBlank { "GST" },
    upiVpa = upiVpa.ifBlank { current.upiVpa },
    upiDefaultNote = upiDefaultNote.ifBlank { current.upiDefaultNote },
    signerName = signerName.ifBlank { current.signerName },
    termsNote = termsNote.ifBlank { current.termsNote }
)

fun PaymentAllocationEntity.toDomain(): PaymentAllocation = PaymentAllocation(
    id = id,
    paymentId = paymentId,
    memoId = memoId,
    amount = amount
)

fun PaymentWithAllocations.toDomain(): CustomerPayment = CustomerPayment(
    id = payment.id,
    customerId = payment.customerId,
    customerName = payment.customerName,
    customerMobile = payment.customerMobile,
    amount = payment.amount,
    method = payment.method,
    note = payment.note,
    date = payment.date,
    createdAt = payment.createdAt,
    allocations = allocations.map { it.toDomain() }
)

fun CustomerPayment.toEntity(): PaymentEntity = PaymentEntity(
    id = id,
    customerId = customerId,
    customerName = customerName,
    customerMobile = customerMobile,
    amount = amount,
    method = method,
    note = note,
    date = date,
    createdAt = createdAt
)

fun CustomerPayment.toAllocationEntities(): List<PaymentAllocationEntity> = allocations.map {
    PaymentAllocationEntity(
        id = it.id,
        paymentId = id,
        memoId = it.memoId,
        amount = it.amount
    )
}

fun CustomerPayment.toDto(): PaymentDto = PaymentDto(
    id = id,
    customerId = customerId,
    customerName = customerName,
    customerMobile = customerMobile,
    amount = amount,
    method = method,
    note = note,
    date = date,
    createdAt = createdAt,
    allocations = allocations.map {
        PaymentAllocationDto(
            id = it.id,
            paymentId = id,
            memoId = it.memoId,
            amount = it.amount
        )
    }
)

fun PaymentDto.toDomain(): CustomerPayment {
    val payId = id.ifBlank { "pay_${System.currentTimeMillis()}" }
    return CustomerPayment(
        id = payId,
        customerId = customerId,
        customerName = customerName,
        customerMobile = customerMobile,
        amount = amount,
        method = method.ifBlank { "Cash" },
        note = note,
        date = date.ifBlank { CashMemo.todayFormatted() },
        createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
        allocations = allocations.orEmpty().map { a ->
            PaymentAllocation(
                id = a.id.ifBlank { "alloc_${System.nanoTime()}" },
                paymentId = payId,
                memoId = a.memoId,
                amount = a.amount
            )
        }
    )
}

fun NoteEntity.toDomain(): GeneralNote = GeneralNote(
    id = id,
    title = title,
    body = body,
    customerId = customerId,
    customerName = customerName,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GeneralNote.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    customerId = customerId,
    customerName = customerName,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GeneralNote.toDto(): NoteDto = NoteDto(
    id = id,
    title = title,
    body = body,
    customerId = customerId,
    customerName = customerName,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun NoteDto.toDomain(): GeneralNote = GeneralNote(
    id = id.ifBlank { "note_${System.currentTimeMillis()}" },
    title = title,
    body = body,
    customerId = customerId,
    customerName = customerName,
    isPinned = isPinned,
    createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
    updatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
)

fun TodoEntity.toDomain(): TodoTask = TodoTask(
    id = id,
    title = title,
    dueDate = dueDate,
    priority = TodoPriority.fromWire(priority),
    customerId = customerId,
    customerName = customerName,
    isDone = isDone,
    doneAt = doneAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TodoTask.toEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    dueDate = dueDate,
    priority = priority.wireValue,
    customerId = customerId,
    customerName = customerName,
    isDone = isDone,
    doneAt = doneAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TodoTask.toDto(): TodoDto = TodoDto(
    id = id,
    title = title,
    dueDate = dueDate,
    priority = priority.wireValue,
    customerId = customerId,
    customerName = customerName,
    isDone = isDone,
    doneAt = doneAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TodoDto.toDomain(): TodoTask = TodoTask(
    id = id.ifBlank { "todo_${System.currentTimeMillis()}" },
    title = title,
    dueDate = dueDate,
    priority = TodoPriority.fromWire(priority),
    customerId = customerId,
    customerName = customerName,
    isDone = isDone,
    doneAt = doneAt,
    createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
    updatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
)
