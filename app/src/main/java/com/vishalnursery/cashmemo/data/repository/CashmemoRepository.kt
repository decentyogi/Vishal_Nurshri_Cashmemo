package com.vishalnursery.cashmemo.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vishalnursery.cashmemo.data.local.CashmemoDatabase
import com.vishalnursery.cashmemo.data.local.SettingsDataStore
import com.vishalnursery.cashmemo.data.remote.DynamicBaseUrlClient
import com.vishalnursery.cashmemo.data.remote.dto.BackupPayloadDto
import com.vishalnursery.cashmemo.data.remote.dto.PaymentRequestDto
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerPayment
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.MemoType
import com.vishalnursery.cashmemo.domain.model.OrderStatus
import com.vishalnursery.cashmemo.domain.model.PaymentAllocation
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.round2
import com.vishalnursery.cashmemo.domain.util.CustomerLedgerCalculator
import com.vishalnursery.cashmemo.domain.util.CustomerMatcher
import com.vishalnursery.cashmemo.domain.util.PaymentAllocator
import com.vishalnursery.cashmemo.domain.util.PlantNameMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

enum class PlantRateImportPolicy(val label: String) {
    ADD_NEW_AND_UPDATE_EXISTING("Add new + update existing"),
    ADD_NEW_ONLY("Add new only (keep existing)"),
    REPLACE_ALL("Replace all plant rates")
}

data class PlantRateImportSummary(
    val addedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int
)

/**
 * Offline-first repository combining Room, Jetpack DataStore, and optional Flask REST sync.
 */
class CashmemoRepository(
    private val database: CashmemoDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val context: Context
) {
    private val memoDao = database.memoDao()
    private val customerDao = database.customerDao()
    private val rateDao = database.plantRateDao()
    private val paymentDao = database.paymentDao()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    val syncManager: SyncManager = SyncManager(context, database, settingsDataStore)

    val memosFlow: Flow<List<CashMemo>> = combine(
        memoDao.observeAllMemos(),
        paymentDao.observeAllAllocations()
    ) { memos, allocations ->
        val laterPaidByMemo = allocations
            .groupBy { it.memoId }
            .mapValues { (_, list) -> round2(list.sumOf { it.amount }) }
        memos.map { it.toDomain(laterPaid = laterPaidByMemo[it.memo.id] ?: 0.0) }
    }

    val paymentsFlow: Flow<List<CustomerPayment>> =
        paymentDao.observeAllPayments().map { list -> list.map { it.toDomain() } }

    val ordersFlow: Flow<List<CashMemo>> =
        memoDao.observeOrders().map { list -> list.map { it.toDomain() } }

    val customersFlow: Flow<List<CustomerProfile>> =
        customerDao.observeAllCustomers().map { list -> list.map { it.toDomain() } }

    val plantRatesFlow: Flow<List<PlantRate>> =
        rateDao.observeAllRates().map { list -> list.map { it.toDomain() } }

    val settingsFlow: Flow<BusinessSettings> = settingsDataStore.settingsFlow

    suspend fun getSettingsOnce(): BusinessSettings = settingsDataStore.getSettingsOnce()

    suspend fun saveSettings(settings: BusinessSettings): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            settingsDataStore.saveSettings(settings)
            val api = if (settings.autoSyncEnabled) {
                DynamicBaseUrlClient.getService(settings.serverBaseUrl)
            } else null
            api?.let {
                runCatching { it.saveSettings(settings.toDto()) }
            }
            Unit
        }
    }

    suspend fun saveMemo(memo: CashMemo, linkedOrderId: String? = null): Result<CashMemo> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            if (memo.isOrder) {
                val cleanOrder = memo.copy(
                    memoType = MemoType.ORDER,
                    previousDues = 0.0,
                    updatedAt = now
                )
                memoDao.upsertMemoWithItems(cleanOrder.toEntity(), cleanOrder.toItemEntities())
                settingsDataStore.incrementNextOrderIfNeeded(cleanOrder.memoNumber)
                return@runCatching cleanOrder
            }

            val cleanMemo = memo.copy(
                memoType = MemoType.CASH,
                updatedAt = now,
                paymentStatusNote = if (memo.balanceDue > 0.009) {
                    memo.paymentStatusNote.ifBlank { "Carry Forward" }
                } else {
                    "Fully Paid"
                }
            )
            val previousVersion = memoDao.getMemoById(cleanMemo.id)?.toDomain()

            memoDao.upsertMemoWithItems(cleanMemo.toEntity(), cleanMemo.toItemEntities())
            settingsDataStore.incrementNextMemoIfNeeded(cleanMemo.memoNumber)

            if (!linkedOrderId.isNullOrBlank()) {
                val orderEntity = memoDao.getMemoById(linkedOrderId)?.memo
                if (orderEntity != null && orderEntity.memoType == MemoType.ORDER.wireValue) {
                    memoDao.insertMemo(
                        orderEntity.copy(
                            orderStatus = OrderStatus.DELIVERED.wireValue,
                            linkedMemoId = cleanMemo.id,
                            updatedAt = now
                        )
                    )
                }
            }

            reconcileCustomerFromMemos(cleanMemo.customerName, cleanMemo.customerMobile, cleanMemo.customerAddress)
            if (previousVersion != null &&
                !sameCustomerKey(previousVersion.customerName, previousVersion.customerMobile, cleanMemo.customerName, cleanMemo.customerMobile)
            ) {
                reconcileCustomerFromMemos(previousVersion.customerName, previousVersion.customerMobile, previousVersion.customerAddress)
            }

            val settings = settingsDataStore.getSettingsOnce()
            var finalMemo = cleanMemo
            if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
                val synced = runCatching {
                    DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.saveBill(cleanMemo.toDto())
                }.isSuccess
                if (synced) {
                    memoDao.markSynced(cleanMemo.id, true)
                    finalMemo = cleanMemo.copy(isSynced = true)
                }
            }
            finalMemo
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = memoDao.getMemoById(orderId)?.memo ?: error("Order not found")
            val updated = entity.copy(
                orderStatus = status.wireValue,
                linkedMemoId = if (status == OrderStatus.PENDING) "" else entity.linkedMemoId,
                updatedAt = System.currentTimeMillis()
            )
            memoDao.insertMemo(updated)
        }
    }

    suspend fun deleteMemo(id: String) = withContext(Dispatchers.IO) {
        val existingMemo = memoDao.getMemoById(id)?.toDomain()
        if (existingMemo != null && existingMemo.isOrder) {
            memoDao.deleteMemoById(id)
            return@withContext
        }
        val linkedOrders = memoDao.getOrdersByLinkedMemoId(id)
        if (linkedOrders.isNotEmpty()) {
            val now = System.currentTimeMillis()
            for (ord in linkedOrders) {
                memoDao.insertMemo(
                    ord.memo.copy(
                        orderStatus = OrderStatus.PENDING.wireValue,
                        linkedMemoId = "",
                        updatedAt = now
                    )
                )
            }
        }
        paymentDao.deleteAllocationsForMemo(id)
        memoDao.deleteMemoById(id)
        if (existingMemo != null) {
            reconcileCustomerFromMemos(existingMemo.customerName, existingMemo.customerMobile, existingMemo.customerAddress)
        }
        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching { DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.deleteBill(id) }
        }
    }

    suspend fun saveCustomer(customer: CustomerProfile) = withContext(Dispatchers.IO) {
        customerDao.upsertCustomer(customer.toEntity())
        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching { DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.saveCustomer(customer.toDto()) }
        }
    }

    suspend fun recordCustomerPayment(
        customerId: String,
        amountPaid: Double,
        promiseNote: String = "Carry Forward",
        method: String = "Cash",
        date: String = CashMemo.todayFormatted(),
        selectedMemoIds: Set<String>? = null
    ): Result<CustomerPayment> = withContext(Dispatchers.IO) {
        runCatching {
            val customer = customerDao.getCustomerById(customerId)?.toDomain()
                ?: error("Customer not found")
            val cleanAmount = round2(max(0.0, amountPaid))
            require(cleanAmount > 0.009) { "Payment amount must be greater than zero" }

            val laterPaidByMemo = paymentDao.getAllAllocationsOnce()
                .groupBy { it.memoId }
                .mapValues { (_, list) -> round2(list.sumOf { it.amount }) }
            val customerMemos = memoDao.getAllMemosOnce()
                .map { it.toDomain(laterPaid = laterPaidByMemo[it.memo.id] ?: 0.0) }
                .filter { matchesCustomer(it.customerName, it.customerMobile, customer) }
            val openBills = PaymentAllocator.findOpenBills(customerMemos)
            val targetBills = if (selectedMemoIds != null) {
                openBills.filter { it.id in selectedMemoIds }
            } else {
                openBills
            }
            val allocResult = PaymentAllocator.allocate(targetBills, cleanAmount)
            val now = System.currentTimeMillis()
            val paymentId = "pay_${now}_${java.util.UUID.randomUUID().toString().take(6)}"
            val payment = CustomerPayment(
                id = paymentId,
                customerId = customer.id,
                customerName = customer.name,
                customerMobile = customer.phoneNumber,
                amount = cleanAmount,
                method = method.ifBlank { "Cash" },
                note = promiseNote.trim(),
                date = date.ifBlank { CashMemo.todayFormatted() },
                createdAt = now,
                allocations = allocResult.allocations.mapIndexed { idx, share ->
                    PaymentAllocation(
                        id = "alloc_${now}_${idx}_${java.util.UUID.randomUUID().toString().take(6)}",
                        paymentId = paymentId,
                        memoId = share.memoId,
                        amount = share.amount
                    )
                }
            )
            paymentDao.upsertPaymentWithAllocations(payment.toEntity(), payment.toAllocationEntities())
            reconcileMatchedCustomer(customer, customer.name, customer.phoneNumber, customer.address)

            val settings = settingsDataStore.getSettingsOnce()
            if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
                runCatching {
                    DynamicBaseUrlClient.getService(settings.serverBaseUrl)
                        ?.recordCustomerPayment(customerId, PaymentRequestDto(cleanAmount, promiseNote))
                }
            }
            payment
        }
    }

    suspend fun deletePayment(paymentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = paymentDao.getPaymentById(paymentId)?.toDomain() ?: return@runCatching
            paymentDao.deletePaymentById(paymentId)
            val allCustomers = customerDao.getAllCustomersOnce().map { it.toDomain() }
            val matched = allCustomers.firstOrNull { it.id == existing.customerId }
                ?: CustomerMatcher.findMatch(allCustomers, existing.customerName, existing.customerMobile)
            if (matched != null) {
                reconcileMatchedCustomer(matched, matched.name, matched.phoneNumber, matched.address)
            }
        }
    }

    suspend fun recalculateCustomerFromBills(customerId: String): Result<CustomerProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val matched = customerDao.getCustomerById(customerId)?.toDomain()
                ?: error("Customer not found")
            reconcileMatchedCustomer(matched, matched.name, matched.phoneNumber, matched.address)
            customerDao.getCustomerById(customerId)?.toDomain() ?: matched
        }
    }

    suspend fun deleteCustomer(id: String) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomerById(id)
        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching { DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.deleteCustomer(id) }
        }
    }

    suspend fun savePlantRate(rate: PlantRate) = withContext(Dispatchers.IO) {
        val cleanRate = rate.copy(updatedAt = System.currentTimeMillis())
        rateDao.upsertRate(cleanRate.toEntity())
        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching { DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.saveRate(cleanRate.toDto()) }
        }
    }

    suspend fun deletePlantRate(id: String) = withContext(Dispatchers.IO) {
        rateDao.deleteRateById(id)
        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching { DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.deleteRate(id) }
        }
    }

    suspend fun exportPlantRatesJson(): String = withContext(Dispatchers.IO) {
        val rates = rateDao.getAllRatesOnce().map { it.toDomain() }
        gson.toJson(rates)
    }

    suspend fun exportPlantRatesCsv(): String = withContext(Dispatchers.IO) {
        val rates = rateDao.getAllRatesOnce().map { it.toDomain() }
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM so Excel opens Hindi characters properly
        sb.appendLine("PlantName,Category,RetailPrice,WholesalePrice")
        for (r in rates) {
            fun esc(s: String) = "\"${s.replace("\"", "\"\"")}\""
            sb.appendLine("${esc(r.name)},${esc(r.category)},${r.retailPrice},${r.wholesalePrice}")
        }
        sb.toString()
    }

    suspend fun importPlantRates(
        rows: List<PlantRate>,
        policy: PlantRateImportPolicy
    ): Result<PlantRateImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val existing = rateDao.getAllRatesOnce().map { it.toDomain() }
            var added = 0
            var updated = 0
            var unchanged = 0

            when (policy) {
                PlantRateImportPolicy.REPLACE_ALL -> {
                    rateDao.deleteAllRates()
                    val entities = rows.map { it.copy(updatedAt = now).toEntity() }
                    rateDao.upsertAll(entities)
                    added = entities.size
                }
                PlantRateImportPolicy.ADD_NEW_AND_UPDATE_EXISTING -> {
                    for (row in rows) {
                        val match = existing.firstOrNull { PlantNameMatcher.matches(it.name, row.name) }
                        if (match != null) {
                            val pricesEqual = match.retailPrice == row.retailPrice && match.wholesalePrice == row.wholesalePrice
                            if (pricesEqual) {
                                unchanged++
                            } else {
                                val cat = if (match.category.isBlank() && row.category.isNotBlank()) row.category else match.category
                                val updatedRate = match.copy(
                                    retailPrice = row.retailPrice,
                                    wholesalePrice = row.wholesalePrice,
                                    category = cat,
                                    updatedAt = now
                                )
                                rateDao.upsertRate(updatedRate.toEntity())
                                updated++
                            }
                        } else {
                            rateDao.upsertRate(row.copy(updatedAt = now).toEntity())
                            added++
                        }
                    }
                }
                PlantRateImportPolicy.ADD_NEW_ONLY -> {
                    for (row in rows) {
                        val match = existing.firstOrNull { PlantNameMatcher.matches(it.name, row.name) }
                        if (match == null) {
                            rateDao.upsertRate(row.copy(updatedAt = now).toEntity())
                            added++
                        } else {
                            unchanged++
                        }
                    }
                }
            }
            PlantRateImportSummary(added, updated, unchanged)
        }
    }

    /**
     * Bidirectional sync delegated to SyncManager.
     */
    suspend fun syncWithBackend(serverUrlOverride: String? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsDataStore.getSettingsOnce()
            val targetUrl = serverUrlOverride ?: settings.serverBaseUrl
            require(targetUrl.isNotBlank()) { "Enter a valid server URL" }
            val scopes = SyncScopes(syncBills = true, syncCustomers = true, syncRates = true, syncSettings = false)
            val summary = syncManager.executeSync(SyncMode.BOTH_WAYS, scopes, targetUrl) {}.getOrThrow()
            summary.message
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.getSettingsOnce()
        val memos = memoDao.getEverythingOnce().map { it.toDomain().toDto() }
        val customers = customerDao.getAllCustomersOnce().map { it.toDomain().toDto() }
        val rates = rateDao.getAllRatesOnce().map { it.toDomain().toDto() }
        val payments = paymentDao.getAllPaymentsOnce().map { it.toDomain().toDto() }
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val payload = BackupPayloadDto(
            version = 7,
            appName = "Vishal_Nurshri_Cashmemo",
            exportDate = isoFormatter.format(Date()),
            settings = settings.toDto(),
            bills = memos,
            customers = customers,
            rates = rates,
            payments = payments
        )
        gson.toJson(payload)
    }

    suspend fun importBackupJson(json: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = gson.fromJson(json, BackupPayloadDto::class.java)
                ?: error("Invalid backup JSON")
            val currentSettings = settingsDataStore.getSettingsOnce()
            payload.settings?.let {
                settingsDataStore.saveSettings(it.mergeInto(currentSettings))
            }
            val importedMemos = payload.bills.orEmpty().map { it.toDomain(synced = false) }
            for (m in importedMemos) {
                memoDao.upsertMemoWithItems(m.toEntity(), m.toItemEntities())
            }
            val importedCustomers = payload.customers.orEmpty().map { it.toDomain().toEntity() }
            customerDao.upsertAll(importedCustomers)
            val importedRates = payload.rates.orEmpty().map { it.toDomain().toEntity() }
            rateDao.upsertAll(importedRates)
            val importedPayments = payload.payments.orEmpty().map { it.toDomain() }
            for (p in importedPayments) {
                paymentDao.upsertPaymentWithAllocations(p.toEntity(), p.toAllocationEntities())
            }
            "Imported ${importedMemos.size} memos, ${importedCustomers.size} customers, ${importedRates.size} rates, ${importedPayments.size} payments"
        }
    }

    suspend fun exportMemosCsv(): String = withContext(Dispatchers.IO) {
        val laterPaidByMemo = paymentDao.getAllAllocationsOnce()
            .groupBy { it.memoId }
            .mapValues { (_, list) -> round2(list.sumOf { it.amount }) }
        val memos = memoDao.getAllMemosOnce().map {
            it.toDomain(laterPaid = laterPaidByMemo[it.memo.id] ?: 0.0)
        }
        val sb = StringBuilder()
        sb.appendLine("MemoNumber,Date,CustomerName,Mobile,Address,Subtotal,Discount,Tax,PreviousDues,GrandTotal,PaidAmount,BalanceDue,PaymentMethod,Status")
        for (m in memos) {
            fun esc(value: String): String = "\"${value.replace("\"", "\"\"")}\""
            val effectiveStatus = if (m.hasEffectivePendingDue) m.paymentStatusNote else "Fully Paid"
            sb.appendLine(
                listOf(
                    esc(m.memoNumber),
                    esc(m.date),
                    esc(m.customerName),
                    esc(m.customerMobile),
                    esc(m.customerAddress),
                    m.subtotal.toString(),
                    m.discount.toString(),
                    m.taxAmount.toString(),
                    m.previousDues.toString(),
                    m.grandTotal.toString(),
                    m.effectivePaid.toString(),
                    m.effectiveDue.toString(),
                    esc(m.paymentMethod),
                    esc(effectiveStatus)
                ).joinToString(",")
            )
        }
        sb.toString()
    }

    suspend fun addRegularCustomer(
        name: String,
        phone: String,
        address: String
    ): CustomerProfile = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanAddress = address.trim()
        val existing = customerDao.getAllCustomersOnce().map { it.toDomain() }
        val matched = CustomerMatcher.findMatch(existing, cleanName, cleanPhone)
        if (matched != null) {
            return@withContext matched
        }

        val now = System.currentTimeMillis()
        val newCustomer = CustomerProfile(
            name = cleanName.ifEmpty { "Customer ($cleanPhone)" },
            phoneNumber = cleanPhone,
            address = cleanAddress,
            outstandingBalance = 0.0,
            totalPurchased = 0.0,
            totalPaid = 0.0,
            lastPromiseNote = "Fully Paid",
            lastTransactionAt = now,
            createdAt = now
        )
        customerDao.upsertCustomer(newCustomer.toEntity())
        reconcileCustomerFromMemos(newCustomer.name, newCustomer.phoneNumber, newCustomer.address)

        val finalProfile = customerDao.getCustomerById(newCustomer.id)?.toDomain() ?: newCustomer

        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching {
                DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.saveCustomer(finalProfile.toDto())
            }
        }
        finalProfile
    }

    private fun sameCustomerKey(nameA: String, phoneA: String, nameB: String, phoneB: String): Boolean {
        return CustomerMatcher.matches(nameA, phoneA, nameB, phoneB)
    }

    private fun matchesCustomer(rawName: String, rawPhone: String, customer: CustomerProfile): Boolean {
        val name = rawName.trim()
        val phone = rawPhone.trim()
        return CustomerMatcher.matches(name, phone, customer.name, customer.phoneNumber) ||
            (phone.isNotEmpty() && phone == customer.phoneNumber.trim()) ||
            (name.isNotEmpty() && name.equals(customer.name.trim(), ignoreCase = true))
    }

    private suspend fun reconcileCustomerFromMemos(rawName: String, rawPhone: String, rawAddress: String) {
        val name = rawName.trim()
        val phone = rawPhone.trim()
        if (name.isEmpty() && phone.isEmpty()) return

        val allCustomers = customerDao.getAllCustomersOnce().map { it.toDomain() }
        val matchedCustomer = CustomerMatcher.findMatch(allCustomers, name, phone) ?: return
        reconcileMatchedCustomer(matchedCustomer, name, phone, rawAddress)
    }

    private suspend fun reconcileMatchedCustomer(
        matchedCustomer: CustomerProfile,
        rawName: String,
        rawPhone: String,
        rawAddress: String
    ) {
        val name = rawName.trim()
        val phone = rawPhone.trim()
        val customerMemos = memoDao.getAllMemosOnce()
            .map { it.toDomain() }
            .filter { m -> matchesCustomer(m.customerName, m.customerMobile, matchedCustomer) }
        val customerPayments = paymentDao.getAllPaymentsOnce()
            .map { it.toDomain() }
            .filter { p ->
                p.customerId == matchedCustomer.id ||
                    matchesCustomer(p.customerName, p.customerMobile, matchedCustomer)
            }

        if (customerMemos.isEmpty() && customerPayments.isEmpty()) {
            customerDao.upsertCustomer(
                matchedCustomer.copy(
                    name = name.ifEmpty { matchedCustomer.name },
                    phoneNumber = phone.ifEmpty { matchedCustomer.phoneNumber },
                    address = rawAddress.trim().ifEmpty { matchedCustomer.address },
                    outstandingBalance = 0.0,
                    totalPurchased = 0.0,
                    totalPaid = 0.0,
                    lastPromiseNote = "Fully Paid",
                    lastTransactionAt = matchedCustomer.lastTransactionAt
                ).toEntity()
            )
            return
        }

        val summary = CustomerLedgerCalculator.calculate(customerMemos, customerPayments)
        customerDao.upsertCustomer(
            matchedCustomer.copy(
                name = name.ifEmpty { matchedCustomer.name },
                phoneNumber = phone.ifEmpty { matchedCustomer.phoneNumber },
                address = rawAddress.trim().ifEmpty { matchedCustomer.address },
                outstandingBalance = summary.outstanding,
                totalPurchased = summary.totalPurchased,
                totalPaid = summary.totalPaid,
                lastPromiseNote = summary.lastStatusNote,
                lastTransactionAt = if (summary.lastTransactionAt > 0L) summary.lastTransactionAt else matchedCustomer.lastTransactionAt
            ).toEntity()
        )
    }
}
