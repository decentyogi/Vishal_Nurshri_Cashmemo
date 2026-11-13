package com.vishalnursery.cashmemo.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vishalnursery.cashmemo.data.local.CashmemoDatabase
import com.vishalnursery.cashmemo.data.local.SettingsDataStore
import com.vishalnursery.cashmemo.data.remote.DynamicBaseUrlClient
import com.vishalnursery.cashmemo.data.remote.dto.BackupPayloadDto
import com.vishalnursery.cashmemo.data.remote.dto.BillDto
import com.vishalnursery.cashmemo.data.remote.dto.CustomerDto
import com.vishalnursery.cashmemo.data.remote.dto.PlantRateDto
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.round2
import com.vishalnursery.cashmemo.ui.viewmodel.SyncDryRunResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ServerStats(
    val serverUrl: String,
    val shopName: String,
    val billsCount: Int,
    val customersCount: Int,
    val ratesCount: Int
)

data class SyncScopes(
    val syncBills: Boolean = true,
    val syncCustomers: Boolean = true,
    val syncRates: Boolean = true,
    val syncSettings: Boolean = false
)

enum class SyncMode {
    PUSH,
    PULL,
    BOTH_WAYS,
    DELIBERATE_MAKE_WEB_MATCH_PHONE,
    DELIBERATE_MAKE_PHONE_MATCH_WEB
}

data class SyncExecutionSummary(
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val skippedDuplicates: Int = 0,
    val deletedCount: Int = 0,
    val message: String
)

class SyncManager(
    private val context: Context,
    private val database: CashmemoDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    private val memoDao = database.memoDao()
    private val customerDao = database.customerDao()
    private val rateDao = database.plantRateDao()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val backupDir: File by lazy {
        File(context.filesDir, "backups").apply { mkdirs() }
    }

    suspend fun testConnection(serverUrl: String): Result<ServerStats> = withContext(Dispatchers.IO) {
        runCatching {
            val api = DynamicBaseUrlClient.getService(serverUrl)
                ?: error("Invalid Server URL format. Enter http:// or https:// address.")
            val settings = api.getSettings()
            val bills = runCatching { api.listBills() }.getOrElse { emptyList() }
            val customers = runCatching { api.listCustomers() }.getOrElse { emptyList() }
            val rates = runCatching { api.listRates() }.getOrElse { emptyList() }
            ServerStats(
                serverUrl = serverUrl,
                shopName = settings.shopNameEnglish.ifBlank { settings.shopNameHindi },
                billsCount = bills.size,
                customersCount = customers.size,
                ratesCount = rates.size
            )
        }
    }

    suspend fun createSafetySnapshot(): File = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.getSettingsOnce()
        val memos = memoDao.getAllMemosOnce().map { it.toDomain().toDto() }
        val customers = customerDao.getAllCustomersOnce().map { it.toDomain().toDto() }
        val rates = rateDao.getAllRatesOnce().map { it.toDomain().toDto() }
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val payload = BackupPayloadDto(
            version = 6,
            appName = "Vishal_Nurshri_Cashmemo",
            exportDate = iso.format(Date()),
            settings = settings.toDto(),
            bills = memos,
            customers = customers,
            rates = rates
        )
        val file = File(backupDir, "snapshot_${System.currentTimeMillis()}.json")
        file.writeText(gson.toJson(payload), Charsets.UTF_8)
        pruneOldSnapshots()
        file
    }

    private fun pruneOldSnapshots() {
        val files = backupDir.listFiles { f -> f.isFile && f.name.startsWith("snapshot_") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        if (files.size > 5) {
            for (old in files.drop(5)) {
                runCatching { old.delete() }
            }
        }
    }

    fun listSnapshots(): List<File> {
        return backupDir.listFiles { f -> f.isFile && f.name.startsWith("snapshot_") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun restoreSnapshot(file: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(file.exists()) { "Snapshot file does not exist" }
            val json = file.readText(Charsets.UTF_8)
            val payload = gson.fromJson(json, BackupPayloadDto::class.java)
                ?: error("Corrupt snapshot file")

            var memosReinserted = 0
            for (b in payload.bills) {
                val domain = b.toDomain(synced = false)
                memoDao.upsertMemoWithItems(domain.toEntity(), domain.toItemEntities())
                memosReinserted++
            }
            var customersReinserted = 0
            for (c in payload.customers) {
                customerDao.upsertCustomer(c.toDomain().toEntity())
                customersReinserted++
            }
            var ratesReinserted = 0
            for (r in payload.rates) {
                rateDao.upsertRate(r.toDomain().toEntity())
                ratesReinserted++
            }
            "Restored from snapshot: $memosReinserted bills, $customersReinserted customers, $ratesReinserted plant rates"
        }
    }

    suspend fun computeDryRun(
        mode: SyncMode,
        scopes: SyncScopes,
        serverUrl: String
    ): Result<SyncDryRunResult> = withContext(Dispatchers.IO) {
        runCatching {
            val api = DynamicBaseUrlClient.getService(serverUrl)
                ?: error("Invalid Server URL format. Check settings.")

            val localMemos = memoDao.getAllMemosOnce().map { it.toDomain() }
            val localCustomers = customerDao.getAllCustomersOnce().map { it.toDomain() }
            val localRates = rateDao.getAllRatesOnce().map { it.toDomain() }

            val remoteBills = if (scopes.syncBills) runCatching { api.listBills() }.getOrElse { emptyList() } else emptyList()
            val remoteCustomers = if (scopes.syncCustomers) runCatching { api.listCustomers() }.getOrElse { emptyList() } else emptyList()
            val remoteRates = if (scopes.syncRates) runCatching { api.listRates() }.getOrElse { emptyList() } else emptyList()

            when (mode) {
                SyncMode.PUSH -> {
                    var billsToAdd = 0
                    var billsToUpdate = 0
                    for (local in localMemos) {
                        val matched = findMatchingBill(local, remoteBills)
                        if (matched == null) billsToAdd++ else billsToUpdate++
                    }

                    var custToAdd = 0
                    var custToUpdate = 0
                    for (local in localCustomers) {
                        val matched = findMatchingCustomer(local, remoteCustomers)
                        if (matched == null) custToAdd++ else custToUpdate++
                    }

                    var ratesToAdd = 0
                    var ratesToUpdate = 0
                    for (local in localRates) {
                        val matched = findMatchingRate(local, remoteRates)
                        if (matched == null) ratesToAdd++ else ratesToUpdate++
                    }

                    SyncDryRunResult(
                        title = "Push to Web Preview",
                        description = "Send phone data to server. Adds new & updates existing.",
                        billsToAdd = if (scopes.syncBills) billsToAdd else 0,
                        billsToUpdate = if (scopes.syncBills) billsToUpdate else 0,
                        customersToAdd = if (scopes.syncCustomers) custToAdd else 0,
                        customersToUpdate = if (scopes.syncCustomers) custToUpdate else 0,
                        ratesToAdd = if (scopes.syncRates) ratesToAdd else 0,
                        ratesToUpdate = if (scopes.syncRates) ratesToUpdate else 0
                    )
                }

                SyncMode.PULL -> {
                    var billsToAdd = 0
                    var billsToUpdate = 0
                    var billsToSkip = 0
                    for (remote in remoteBills) {
                        val (matched, isPossibleDup) = findMatchingBillInLocal(remote, localMemos)
                        if (isPossibleDup) {
                            billsToSkip++
                        } else if (matched == null) {
                            billsToAdd++
                        } else if (remote.updatedAt > matched.updatedAt) {
                            billsToUpdate++
                        } else {
                            billsToSkip++
                        }
                    }

                    var custToAdd = 0
                    var custToUpdate = 0
                    for (remote in remoteCustomers) {
                        val matched = findMatchingCustomerInLocal(remote, localCustomers)
                        if (matched == null) {
                            custToAdd++
                        } else if (remote.lastTransactionAt > matched.lastTransactionAt) {
                            custToUpdate++
                        }
                    }

                    var ratesToAdd = 0
                    var ratesToUpdate = 0
                    for (remote in remoteRates) {
                        val matched = findMatchingRateInLocal(remote, localRates)
                        if (matched == null) {
                            ratesToAdd++
                        } else if (remote.updatedAt > matched.updatedAt) {
                            ratesToUpdate++
                        }
                    }

                    SyncDryRunResult(
                        title = "Pull from Web Preview",
                        description = "Bring server updates to phone. Adds and updates only.",
                        billsToAdd = if (scopes.syncBills) billsToAdd else 0,
                        billsToUpdate = if (scopes.syncBills) billsToUpdate else 0,
                        billsToSkipDuplicates = if (scopes.syncBills) billsToSkip else 0,
                        customersToAdd = if (scopes.syncCustomers) custToAdd else 0,
                        customersToUpdate = if (scopes.syncCustomers) custToUpdate else 0,
                        ratesToAdd = if (scopes.syncRates) ratesToAdd else 0,
                        ratesToUpdate = if (scopes.syncRates) ratesToUpdate else 0
                    )
                }

                SyncMode.BOTH_WAYS -> {
                    var billsPullAdd = 0
                    var billsPullUpdate = 0
                    var billsSkip = 0
                    for (remote in remoteBills) {
                        val (matched, isDup) = findMatchingBillInLocal(remote, localMemos)
                        if (isDup) billsSkip++
                        else if (matched == null) billsPullAdd++
                        else if (remote.updatedAt > matched.updatedAt) billsPullUpdate++
                        else billsSkip++
                    }

                    var billsPushAdd = 0
                    var billsPushUpdate = 0
                    for (local in localMemos) {
                        val matched = findMatchingBill(local, remoteBills)
                        if (matched == null) billsPushAdd++
                        else if (local.updatedAt > matched.updatedAt) billsPushUpdate++
                    }

                    var custToAdd = 0
                    for (remote in remoteCustomers) {
                        if (findMatchingCustomerInLocal(remote, localCustomers) == null) custToAdd++
                    }
                    var custToPush = 0
                    for (local in localCustomers) {
                        if (findMatchingCustomer(local, remoteCustomers) == null) custToPush++
                    }

                    var ratesToAdd = 0
                    for (remote in remoteRates) {
                        if (findMatchingRateInLocal(remote, localRates) == null) ratesToAdd++
                    }
                    var ratesToPush = 0
                    for (local in localRates) {
                        if (findMatchingRate(local, remoteRates) == null) ratesToPush++
                    }

                    SyncDryRunResult(
                        title = "Sync Both Ways Preview (Recommended)",
                        description = "Merges both sides safely. Newer edit wins. Nothing is deleted.",
                        billsToAdd = if (scopes.syncBills) billsPullAdd + billsPushAdd else 0,
                        billsToUpdate = if (scopes.syncBills) billsPullUpdate + billsPushUpdate else 0,
                        billsToSkipDuplicates = if (scopes.syncBills) billsSkip else 0,
                        customersToAdd = if (scopes.syncCustomers) custToAdd + custToPush else 0,
                        ratesToAdd = if (scopes.syncRates) ratesToAdd + ratesToPush else 0
                    )
                }

                SyncMode.DELIBERATE_MAKE_WEB_MATCH_PHONE -> {
                    val billsToDelete = remoteBills.filter { r -> localMemos.none { l -> l.id == r.id } }.size
                    val custToDelete = remoteCustomers.filter { r -> localCustomers.none { l -> l.id == r.id } }.size
                    val ratesToDelete = remoteRates.filter { r -> localRates.none { l -> l.id == r.id } }.size

                    SyncDryRunResult(
                        title = "Make Web Match This Phone",
                        description = "WILL DELETE extra records on server that do not exist on phone!",
                        billsToDelete = if (scopes.syncBills) billsToDelete else 0,
                        customersToDelete = if (scopes.syncCustomers) custToDelete else 0,
                        ratesToDelete = if (scopes.syncRates) ratesToDelete else 0,
                        billsToAdd = if (scopes.syncBills) localMemos.size else 0,
                        isDeliberateReplace = true
                    )
                }

                SyncMode.DELIBERATE_MAKE_PHONE_MATCH_WEB -> {
                    val billsToDelete = localMemos.filter { l -> remoteBills.none { r -> r.id == l.id } }.size
                    val custToDelete = localCustomers.filter { l -> remoteCustomers.none { r -> r.id == l.id } }.size
                    val ratesToDelete = localRates.filter { l -> remoteRates.none { r -> r.id == l.id } }.size

                    SyncDryRunResult(
                        title = "Make This Phone Match The Web",
                        description = "WILL DELETE extra local records on phone that do not exist on server!",
                        billsToDelete = if (scopes.syncBills) billsToDelete else 0,
                        customersToDelete = if (scopes.syncCustomers) custToDelete else 0,
                        ratesToDelete = if (scopes.syncRates) ratesToDelete else 0,
                        billsToAdd = if (scopes.syncBills) remoteBills.size else 0,
                        isDeliberateReplace = true
                    )
                }
            }
        }
    }

    suspend fun executeSync(
        mode: SyncMode,
        scopes: SyncScopes,
        serverUrl: String,
        onProgress: (String) -> Unit
    ): Result<SyncExecutionSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val api = DynamicBaseUrlClient.getService(serverUrl)
                ?: error("Invalid server URL")

            // Always take safety snapshot before Pull, Both Ways or Deliberate Replace
            if (mode != SyncMode.PUSH) {
                onProgress("Saving local snapshot...")
                createSafetySnapshot()
            }

            var pushed = 0
            var pulled = 0
            var skipped = 0
            var deleted = 0

            when (mode) {
                SyncMode.PUSH -> {
                    val result = executePush(api, scopes, onProgress)
                    pushed = result.pushedCount
                }

                SyncMode.PULL -> {
                    val result = executePull(api, scopes, onProgress)
                    pulled = result.pulledCount
                    skipped = result.skippedDuplicates
                }

                SyncMode.BOTH_WAYS -> {
                    val pullResult = executePull(api, scopes, onProgress)
                    pulled = pullResult.pulledCount
                    skipped = pullResult.skippedDuplicates

                    val pushResult = executePush(api, scopes, onProgress)
                    pushed = pushResult.pushedCount
                }

                SyncMode.DELIBERATE_MAKE_WEB_MATCH_PHONE -> {
                    onProgress("Inspecting server records to remove...")
                    if (scopes.syncBills) {
                        val remote = runCatching { api.listBills() }.getOrElse { emptyList() }
                        val localIds = memoDao.getAllMemosOnce().map { it.memo.id }.toSet()
                        for (r in remote) {
                            if (!localIds.contains(r.id)) {
                                runCatching { api.deleteBill(r.id) }
                                deleted++
                            }
                        }
                    }
                    if (scopes.syncCustomers) {
                        val remote = runCatching { api.listCustomers() }.getOrElse { emptyList() }
                        val localIds = customerDao.getAllCustomersOnce().map { it.id }.toSet()
                        for (r in remote) {
                            if (!localIds.contains(r.id)) {
                                runCatching { api.deleteCustomer(r.id) }
                                deleted++
                            }
                        }
                    }
                    if (scopes.syncRates) {
                        val remote = runCatching { api.listRates() }.getOrElse { emptyList() }
                        val localIds = rateDao.getAllRatesOnce().map { it.id }.toSet()
                        for (r in remote) {
                            if (!localIds.contains(r.id)) {
                                runCatching { api.deleteRate(r.id) }
                                deleted++
                            }
                        }
                    }
                    val pushResult = executePush(api, scopes, onProgress)
                    pushed = pushResult.pushedCount
                }

                SyncMode.DELIBERATE_MAKE_PHONE_MATCH_WEB -> {
                    onProgress("Inspecting local records to remove...")
                    if (scopes.syncBills) {
                        val remoteIds = runCatching { api.listBills() }.getOrElse { emptyList() }.map { it.id }.toSet()
                        val local = memoDao.getAllMemosOnce()
                        for (m in local) {
                            if (!remoteIds.contains(m.memo.id)) {
                                memoDao.deleteMemoById(m.memo.id)
                                deleted++
                            }
                        }
                    }
                    if (scopes.syncCustomers) {
                        val remoteIds = runCatching { api.listCustomers() }.getOrElse { emptyList() }.map { it.id }.toSet()
                        val local = customerDao.getAllCustomersOnce()
                        for (c in local) {
                            if (!remoteIds.contains(c.id)) {
                                customerDao.deleteCustomerById(c.id)
                                deleted++
                            }
                        }
                    }
                    if (scopes.syncRates) {
                        val remoteIds = runCatching { api.listRates() }.getOrElse { emptyList() }.map { it.id }.toSet()
                        val local = rateDao.getAllRatesOnce()
                        for (r in local) {
                            if (!remoteIds.contains(r.id)) {
                                rateDao.deleteRateById(r.id)
                                deleted++
                            }
                        }
                    }
                    val pullResult = executePull(api, scopes, onProgress)
                    pulled = pullResult.pulledCount
                    skipped = pullResult.skippedDuplicates
                }
            }

            val timeStr = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
            val summaryMsg = when {
                deleted > 0 -> "Last sync: $timeStr - $pushed pushed, $pulled pulled, $deleted deleted"
                else -> "Last sync: $timeStr - $pushed pushed, $pulled pulled, $skipped skipped"
            }
            settingsDataStore.saveLastSyncStatus(summaryMsg)

            SyncExecutionSummary(
                pushedCount = pushed,
                pulledCount = pulled,
                skippedDuplicates = skipped,
                deletedCount = deleted,
                message = summaryMsg
            )
        }
    }

    private suspend fun executePush(
        api: com.vishalnursery.cashmemo.data.remote.CashmemoApiService,
        scopes: SyncScopes,
        onProgress: (String) -> Unit
    ): SyncExecutionSummary {
        var pushed = 0

        if (scopes.syncBills) {
            val memos = memoDao.getAllMemosOnce().map { it.toDomain() }
            for ((idx, memo) in memos.withIndex()) {
                onProgress("Pushing bills (${idx + 1}/${memos.size})...")
                val success = runCatching { api.saveBill(memo.toDto()).success }.getOrElse { false }
                if (success) {
                    memoDao.markSynced(memo.id, true)
                    pushed++
                }
            }
        }

        if (scopes.syncCustomers) {
            val customers = customerDao.getAllCustomersOnce().map { it.toDomain() }
            for ((idx, cust) in customers.withIndex()) {
                onProgress("Pushing customers (${idx + 1}/${customers.size})...")
                val success = runCatching { api.saveCustomer(cust.toDto()).success }.getOrElse { false }
                if (success) pushed++
            }
        }

        if (scopes.syncRates) {
            val rates = rateDao.getAllRatesOnce().map { it.toDomain() }
            for ((idx, rate) in rates.withIndex()) {
                onProgress("Pushing plant rates (${idx + 1}/${rates.size})...")
                val success = runCatching { api.saveRate(rate.toDto()).success }.getOrElse { false }
                if (success) pushed++
            }
        }

        if (scopes.syncSettings) {
            onProgress("Pushing shop settings...")
            val settings = settingsDataStore.getSettingsOnce()
            runCatching { api.saveSettings(settings.toDto()) }
        }

        return SyncExecutionSummary(pushedCount = pushed, message = "Pushed $pushed records")
    }

    private suspend fun executePull(
        api: com.vishalnursery.cashmemo.data.remote.CashmemoApiService,
        scopes: SyncScopes,
        onProgress: (String) -> Unit
    ): SyncExecutionSummary {
        var pulled = 0
        var skipped = 0

        if (scopes.syncBills) {
            onProgress("Pulling bills from server...")
            val remoteBills = runCatching { api.listBills() }.getOrElse { emptyList() }
            val localMemos = memoDao.getAllMemosOnce().map { it.toDomain() }

            for ((idx, remote) in remoteBills.withIndex()) {
                onProgress("Merging bills (${idx + 1}/${remoteBills.size})...")
                val (matched, isPossibleDup) = findMatchingBillInLocal(remote, localMemos)
                if (isPossibleDup) {
                    skipped++
                } else if (matched == null) {
                    // New bill from server
                    val domain = remote.toDomain(synced = true)
                    memoDao.upsertMemoWithItems(domain.toEntity(), domain.toItemEntities())
                    pulled++
                } else if (remote.updatedAt > matched.updatedAt) {
                    // Server is newer: update, but PRESERVE local rawOcrText and sourceImageUri
                    val domain = remote.toDomain(synced = true).copy(
                        rawOcrText = matched.rawOcrText,
                        sourceImageUri = matched.sourceImageUri
                    )
                    memoDao.upsertMemoWithItems(domain.toEntity(), domain.toItemEntities())
                    pulled++
                } else {
                    skipped++
                }
            }
        }

        if (scopes.syncCustomers) {
            onProgress("Pulling customers from server...")
            val remoteCusts = runCatching { api.listCustomers() }.getOrElse { emptyList() }
            val localCusts = customerDao.getAllCustomersOnce().map { it.toDomain() }

            for ((idx, remote) in remoteCusts.withIndex()) {
                onProgress("Merging customers (${idx + 1}/${remoteCusts.size})...")
                val matched = findMatchingCustomerInLocal(remote, localCusts)
                if (matched == null) {
                    customerDao.upsertCustomer(remote.toDomain().toEntity())
                    pulled++
                } else if (remote.lastTransactionAt > matched.lastTransactionAt) {
                    // Remote is strictly newer
                    customerDao.upsertCustomer(remote.toDomain().toEntity())
                    pulled++
                }
            }
        }

        if (scopes.syncRates) {
            onProgress("Pulling plant rates from server...")
            val remoteRates = runCatching { api.listRates() }.getOrElse { emptyList() }
            val localRates = rateDao.getAllRatesOnce().map { it.toDomain() }

            for ((idx, remote) in remoteRates.withIndex()) {
                onProgress("Merging rates (${idx + 1}/${remoteRates.size})...")
                val matched = findMatchingRateInLocal(remote, localRates)
                if (matched == null) {
                    rateDao.upsertRate(remote.toDomain().toEntity())
                    pulled++
                } else if (remote.updatedAt > matched.updatedAt) {
                    rateDao.upsertRate(remote.toDomain().toEntity())
                    pulled++
                }
            }
        }

        if (scopes.syncSettings) {
            onProgress("Pulling shop settings...")
            runCatching {
                val remoteSettings = api.getSettings()
                val localSettings = settingsDataStore.getSettingsOnce()
                // Never touch Gemini key, server URL or scan engine!
                val merged = localSettings.copy(
                    shopNameHindi = remoteSettings.shopNameHindi,
                    shopNameEnglish = remoteSettings.shopNameEnglish,
                    tagline = remoteSettings.tagline,
                    phone = remoteSettings.phone,
                    email = remoteSettings.email,
                    addressLine1 = remoteSettings.addressLine1,
                    addressLine2 = remoteSettings.addressLine2,
                    gstin = remoteSettings.gstin,
                    currencySymbol = remoteSettings.currencySymbol,
                    memoPrefix = remoteSettings.memoPrefix,
                    upiVpa = remoteSettings.upiVpa,
                    upiDefaultNote = remoteSettings.upiDefaultNote,
                    signerName = remoteSettings.signerName,
                    termsNote = remoteSettings.termsNote
                )
                settingsDataStore.saveSettings(merged)
            }
        }

        return SyncExecutionSummary(
            pulledCount = pulled,
            skippedDuplicates = skipped,
            message = "Pulled $pulled records ($skipped skipped)"
        )
    }

    private fun findMatchingBill(local: CashMemo, remoteList: List<BillDto>): BillDto? {
        return remoteList.firstOrNull { it.id == local.id }
            ?: remoteList.firstOrNull {
                it.memoNumber.trim() == local.memoNumber.trim() &&
                    it.date.trim() == local.date.trim() &&
                    it.customerName.trim().equals(local.customerName.trim(), ignoreCase = true)
            }
    }

    private fun findMatchingBillInLocal(remote: BillDto, localList: List<CashMemo>): Pair<CashMemo?, Boolean> {
        val byId = localList.firstOrNull { it.id == remote.id }
        if (byId != null) return byId to false

        val byMemoNumDateName = localList.firstOrNull {
            it.memoNumber.trim() == remote.memoNumber.trim() &&
                it.date.trim() == remote.date.trim() &&
                it.customerName.trim().equals(remote.customerName.trim(), ignoreCase = true)
        }
        if (byMemoNumDateName != null) {
            // Counted as duplicate match
            return byMemoNumDateName to true
        }

        return null to false
    }

    private fun findMatchingCustomer(local: CustomerProfile, remoteList: List<CustomerDto>): CustomerDto? {
        val byId = remoteList.firstOrNull { it.id == local.id }
        if (byId != null) return byId

        val cleanPhone = local.phoneNumber.trim()
        if (cleanPhone.isNotEmpty()) {
            val byPhone = remoteList.firstOrNull { it.phoneNumber.trim() == cleanPhone }
            if (byPhone != null) return byPhone
        }

        val cleanName = local.name.trim()
        if (cleanName.isNotEmpty()) {
            return remoteList.firstOrNull { it.name.trim().equals(cleanName, ignoreCase = true) }
        }
        return null
    }

    private fun findMatchingCustomerInLocal(remote: CustomerDto, localList: List<CustomerProfile>): CustomerProfile? {
        val byId = localList.firstOrNull { it.id == remote.id }
        if (byId != null) return byId

        val cleanPhone = remote.phoneNumber.trim()
        if (cleanPhone.isNotEmpty()) {
            val byPhone = localList.firstOrNull { it.phoneNumber.trim() == cleanPhone }
            if (byPhone != null) return byPhone
        }

        val cleanName = remote.name.trim()
        if (cleanName.isNotEmpty()) {
            return localList.firstOrNull { it.name.trim().equals(cleanName, ignoreCase = true) }
        }
        return null
    }

    private fun findMatchingRate(local: PlantRate, remoteList: List<PlantRateDto>): PlantRateDto? {
        val byId = remoteList.firstOrNull { it.id == local.id }
        if (byId != null) return byId
        val cleanName = local.name.trim()
        return remoteList.firstOrNull { it.name.trim().equals(cleanName, ignoreCase = true) }
    }

    private fun findMatchingRateInLocal(remote: PlantRateDto, localList: List<PlantRate>): PlantRate? {
        val byId = localList.firstOrNull { it.id == remote.id }
        if (byId != null) return byId
        val cleanName = remote.name.trim()
        return localList.firstOrNull { it.name.trim().equals(cleanName, ignoreCase = true) }
    }
}
