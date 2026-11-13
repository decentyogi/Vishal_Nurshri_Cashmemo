package com.vishalnursery.cashmemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vishalnursery.cashmemo.data.local.entity.CashMemoEntity
import com.vishalnursery.cashmemo.data.local.entity.CustomerEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoItemEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoWithItems
import com.vishalnursery.cashmemo.data.local.entity.PlantRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Transaction
    @Query("SELECT * FROM cash_memos ORDER BY createdAt DESC")
    fun observeAllMemos(): Flow<List<MemoWithItems>>

    @Transaction
    @Query("SELECT * FROM cash_memos ORDER BY createdAt DESC")
    suspend fun getAllMemosOnce(): List<MemoWithItems>

    @Transaction
    @Query("SELECT * FROM cash_memos WHERE id = :id LIMIT 1")
    suspend fun getMemoById(id: String): MemoWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: CashMemoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MemoItemEntity>)

    @Query("DELETE FROM memo_items WHERE memoId = :memoId")
    suspend fun deleteItemsForMemo(memoId: String)

    @Transaction
    suspend fun upsertMemoWithItems(memo: CashMemoEntity, items: List<MemoItemEntity>) {
        insertMemo(memo)
        deleteItemsForMemo(memo.id)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }

    @Query("DELETE FROM cash_memos WHERE id = :id")
    suspend fun deleteMemoById(id: String)

    @Query("DELETE FROM cash_memos WHERE id IN (:ids)")
    suspend fun deleteMemosByIds(ids: List<String>)

    @Query("UPDATE cash_memos SET isSynced = :synced WHERE id = :id")
    suspend fun markSynced(id: String, synced: Boolean)
}

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY lastTransactionAt DESC")
    fun observeAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY lastTransactionAt DESC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("DELETE FROM customers WHERE id IN (:ids)")
    suspend fun deleteCustomersByIds(ids: List<String>)
}

@Dao
interface PlantRateDao {

    @Query("SELECT * FROM plant_rates ORDER BY name COLLATE NOCASE ASC")
    fun observeAllRates(): Flow<List<PlantRateEntity>>

    @Query("SELECT * FROM plant_rates ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllRatesOnce(): List<PlantRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: PlantRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rates: List<PlantRateEntity>)

    @Query("DELETE FROM plant_rates WHERE id = :id")
    suspend fun deleteRateById(id: String)

    @Query("DELETE FROM plant_rates WHERE id IN (:ids)")
    suspend fun deleteRatesByIds(ids: List<String>)

    @Query("DELETE FROM plant_rates")
    suspend fun deleteAllRates()
}
