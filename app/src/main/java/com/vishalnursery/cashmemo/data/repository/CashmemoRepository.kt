package com.vishalnursery.cashmemo.data.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vishalnursery.cashmemo.data.local.CashmemoDatabase
import com.vishalnursery.cashmemo.data.local.SettingsDataStore
import com.vishalnursery.cashmemo.data.remote.DynamicBaseUrlClient
import com.vishalnursery.cashmemo.data.remote.dto.BackupPayloadDto
import com.vishalnursery.cashmemo.data.remote.dto.PaymentRequestDto
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.round2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Offline-first repository combining Room, Jetpack DataStore, and optional Flask REST sync.
 */
class CashmemoRepository(
    private val database: CashmemoDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    private val memoDao = database.memoDao()
    private val customerDao = database.customerDao()
    private val rateDao = database.plantRateDao()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    val memosFlow: Flow<List<CashMemo>> =
        memoDao.observeAllMemos().map { list -> list.map { it.toDomain() } }

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

    suspend fun saveMemo(memo: CashMemo): Result<CashMemo> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val cleanMemo = memo.copy(
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
            if (previousVersion == null) {
                syncCustomerFromBillLocally(cleanMemo)
            } else {
                reconcileCustomerFromMemos(cleanMemo.customerName, cleanMemo.customerMobile, cleanMemo.customerAddress)
                if (!sameCustomerKey(previousVersion.customerName, previousVersion.customerMobile, cleanMemo.customerName, cleanMemo.customerMobile)) {
                    reconcileCustomerFromMemos(previousVersion.customerName, previousVersion.customerMobile, previousVersion.customerAddress)
                }
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

    suspend fun deleteMemo(id: String) = withContext(Dispatchers.IO) {
        val existingMemo = memoDao.getMemoById(id)?.toDomain()
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
        promiseNote: String = "Carry Forward"
    ) = withContext(Dispatchers.IO) {
        val entity = customerDao.getCustomerById(customerId) ?: return@withContext
        val oldBalance = entity.outstandingBalance
        val newBalance = round2(max(0.0, oldBalance - amountPaid))
        val updated = entity.copy(
            outstandingBalance = newBalance,
            totalPaid = round2(entity.totalPaid + amountPaid),
            lastPromiseNote = if (newBalance <= 0.009) "Fully Paid" else promiseNote.ifBlank { "Carry Forward" },
            lastTransactionAt = System.currentTimeMillis()
        )
        customerDao.upsertCustomer(updated)

        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching {
                DynamicBaseUrlClient.getService(settings.serverBaseUrl)
                    ?.recordCustomerPayment(customerId, PaymentRequestDto(amountPaid, promiseNote))
            }
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
        rateDao.upsertRate(rate.toEntity())
        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching { DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.saveRate(rate.toDto()) }
        }
    }

    suspend fun deletePlantRate(id: String) = withContext(Dispatchers.IO) {
        rateDao.deleteRateById(id)
        val settings = settingsDataStore.getSettingsOnce()
        if (settings.autoSyncEnabled && settings.serverBaseUrl.isNotBlank()) {
            runCatching { DynamicBaseUrlClient.getService(settings.serverBaseUrl)?.deleteRate(id) }
        }
    }

    /**
     * Bidirectional sync with Cashmemo_AI_Web Flask backend.
     */
    suspend fun syncWithBackend(serverUrlOverride: String? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val currentSettings = settingsDataStore.getSettingsOnce()
            val targetUrl = serverUrlOverride ?: currentSettings.serverBaseUrl
            val api = DynamicBaseUrlClient.getService(targetUrl)
                ?: error("Enter a valid server URL (e.g. http://192.168.1.10:8080)")

            // 1. Push unsynced local memos first
            val localMemos = memoDao.getAllMemosOnce().map { it.toDomain() }
            var pushedCount = 0
            for (memo in localMemos.filter { !it.isSynced }) {
                api.saveBill(memo.toDto())
                memoDao.markSynced(memo.id, true)
                pushedCount++
            }

            // 2. Pull full remote snapshot
            val remoteBackup = api.exportBackup()
            remoteBackup.settings?.let { remoteSettings ->
                settingsDataStore.saveSettings(remoteSettings.mergeInto(currentSettings))
            }

            val remoteMemos = remoteBackup.bills.map { it.toDomain(synced = true) }
            for (remote in remoteMemos) {
                val existing = memoDao.getMemoById(remote.id)
                if (existing == null || remote.updatedAt >= existing.memo.updatedAt) {
                    memoDao.upsertMemoWithItems(remote.toEntity(), remote.toItemEntities())
                }
            }

            if (remoteBackup.customers.isNotEmpty()) {
                customerDao.upsertAll(remoteBackup.customers.map { it.toDomain().toEntity() })
            }
            if (remoteBackup.rates.isNotEmpty()) {
                rateDao.upsertAll(remoteBackup.rates.map { it.toDomain().toEntity() })
            }

            "Synced ($pushedCount pushed, ${remoteMemos.size} bills on server)"
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.getSettingsOnce()
        val memos = memoDao.getAllMemosOnce().map { it.toDomain().toDto() }
        val customers = customerDao.getAllCustomersOnce().map { it.toDomain().toDto() }
        val rates = rateDao.getAllRatesOnce().map { it.toDomain().toDto() }
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val payload = BackupPayloadDto(
            version = 6,
            appName = "Vishal_Nurshri_Cashmemo",
            exportDate = isoFormatter.format(Date()),
            settings = settings.toDto(),
            bills = memos,
            customers = customers,
            rates = rates
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
            val importedMemos = payload.bills.map { it.toDomain(synced = false) }
            for (m in importedMemos) {
                memoDao.upsertMemoWithItems(m.toEntity(), m.toItemEntities())
            }
            customerDao.upsertAll(payload.customers.map { it.toDomain().toEntity() })
            rateDao.upsertAll(payload.rates.map { it.toDomain().toEntity() })
            "Imported ${importedMemos.size} memos, ${payload.customers.size} customers, ${payload.rates.size} rates"
        }
    }

    suspend fun exportMemosCsv(): String = withContext(Dispatchers.IO) {
        val memos = memoDao.getAllMemosOnce().map { it.toDomain() }
        val sb = StringBuilder()
        sb.appendLine("MemoNumber,Date,CustomerName,Mobile,Address,Subtotal,Discount,Tax,PreviousDues,GrandTotal,PaidAmount,BalanceDue,PaymentMethod,Status")
        for (m in memos) {
            fun esc(value: String): String = "\"${value.replace("\"", "\"\"")}\""
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
                    m.paidAmount.toString(),
                    m.balanceDue.toString(),
                    esc(m.paymentMethod),
                    esc(m.paymentStatusNote)
                ).joinToString(",")
            )
        }
        sb.toString()
    }

    private suspend fun syncCustomerFromBillLocally(bill: CashMemo) {
        val name = bill.customerName.trim()
        val phone = bill.customerMobile.trim()
        val addr = bill.customerAddress.trim()
        if (name.isEmpty() && phone.isEmpty()) return

        val existing = customerDao.getAllCustomersOnce()
        val matched = existing.firstOrNull { c ->
            (phone.isNotEmpty() && c.phoneNumber.trim() == phone) ||
                (name.isNotEmpty() && c.name.trim().equals(name, ignoreCase = true))
        }

        val now = System.currentTimeMillis()
        if (matched != null) {
            val newBal = if (bill.previousDues >= matched.outstandingBalance && matched.outstandingBalance > 0.0) {
                bill.balanceDue
            } else {
                round2(max(0.0, matched.outstandingBalance + (bill.grandTotal - bill.paidAmount)))
            }
            customerDao.upsertCustomer(
                matched.copy(
                    name = name.ifEmpty { matched.name },
                    phoneNumber = phone.ifEmpty { matched.phoneNumber },
                    address = addr.ifEmpty { matched.address },
                    outstandingBalance = newBal,
                    totalPurchased = round2(matched.totalPurchased + bill.grandTotal),
                    totalPaid = round2(matched.totalPaid + bill.paidAmount),
                    lastPromiseNote = if (newBal > 0.009) "Carry Forward" else "Fully Paid",
                    lastTransactionAt = now
                )
            )
        } else {
            val newCustomer = CustomerProfile(
                name = name.ifEmpty { "Customer ($phone)" },
                phoneNumber = phone,
                address = addr,
                outstandingBalance = bill.balanceDue,
                totalPurchased = bill.grandTotal,
                totalPaid = bill.paidAmount,
                lastPromiseNote = if (bill.balanceDue > 0.009) "Carry Forward" else "Fully Paid",
                lastTransactionAt = now,
                createdAt = now
            )
            customerDao.upsertCustomer(newCustomer.toEntity())
        }
    }

    private fun sameCustomerKey(nameA: String, phoneA: String, nameB: String, phoneB: String): Boolean {
        val cleanPhoneA = phoneA.trim()
        val cleanPhoneB = phoneB.trim()
        if (cleanPhoneA.isNotEmpty() && cleanPhoneB.isNotEmpty()) {
            return cleanPhoneA == cleanPhoneB
        }
        return nameA.trim().isNotEmpty() && nameA.trim().equals(nameB.trim(), ignoreCase = true)
    }

    private suspend fun reconcileCustomerFromMemos(rawName: String, rawPhone: String, rawAddress: String) {
        val name = rawName.trim()
        val phone = rawPhone.trim()
        if (name.isEmpty() && phone.isEmpty()) return

        val allCustomers = customerDao.getAllCustomersOnce()
        val matchedCustomer = allCustomers.firstOrNull { c ->
            (phone.isNotEmpty() && c.phoneNumber.trim() == phone) ||
                (name.isNotEmpty() && c.name.trim().equals(name, ignoreCase = true))
        } ?: return

        val customerMemos = memoDao.getAllMemosOnce()
            .map { it.toDomain() }
            .filter { m ->
                (phone.isNotEmpty() && m.customerMobile.trim() == phone) ||
                    (name.isNotEmpty() && m.customerName.trim().equals(name, ignoreCase = true)) ||
                    (matchedCustomer.phoneNumber.trim().isNotEmpty() && m.customerMobile.trim() == matchedCustomer.phoneNumber.trim()) ||
                    (matchedCustomer.name.trim().isNotEmpty() && m.customerName.trim().equals(matchedCustomer.name.trim(), ignoreCase = true))
            }
            .sortedBy { it.createdAt }

        if (customerMemos.isEmpty()) {
            customerDao.upsertCustomer(
                matchedCustomer.copy(
                    outstandingBalance = 0.0,
                    totalPurchased = 0.0,
                    totalPaid = 0.0,
                    lastPromiseNote = "Fully Paid",
                    lastTransactionAt = System.currentTimeMillis()
                )
            )
            return
        }

        var runningBal = 0.0
        var sumPurchased = 0.0
        var sumPaid = 0.0
        for ((idx, m) in customerMemos.withIndex()) {
            sumPurchased += m.grandTotal
            sumPaid += m.paidAmount
            runningBal = if (idx == 0) {
                m.balanceDue
            } else if (m.previousDues > 0.009 || (m.previousDues >= runningBal && runningBal > 0.009)) {
                m.balanceDue
            } else {
                round2(max(0.0, runningBal + (m.grandTotal - m.paidAmount)))
            }
        }
        val cleanBal = round2(max(0.0, runningBal))
        val latest = customerMemos.last()
        customerDao.upsertCustomer(
            matchedCustomer.copy(
                name = name.ifEmpty { matchedCustomer.name },
                phoneNumber = phone.ifEmpty { matchedCustomer.phoneNumber },
                address = rawAddress.trim().ifEmpty { matchedCustomer.address },
                outstandingBalance = cleanBal,
                totalPurchased = round2(sumPurchased),
                totalPaid = round2(sumPaid),
                lastPromiseNote = if (cleanBal > 0.009) latest.paymentStatusNote.ifBlank { "Carry Forward" } else "Fully Paid",
                lastTransactionAt = latest.updatedAt
            )
        )
    }
}
