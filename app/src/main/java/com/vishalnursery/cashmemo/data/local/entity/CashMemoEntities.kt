package com.vishalnursery.cashmemo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "cash_memos")
data class CashMemoEntity(
    @PrimaryKey val id: String,
    val memoNumber: String,
    val date: String,
    val dueDate: String,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String,
    val customerGstin: String,
    val discount: Double,
    val discountType: String,
    val taxPercentage: Double,
    val taxAmount: Double,
    val previousDues: Double,
    val subtotal: Double,
    val grandTotal: Double,
    val paidAmount: Double,
    val balanceDue: Double,
    val paymentMethod: String,
    val paymentStatusNote: String,
    val notes: String,
    val rawOcrText: String,
    val sourceImageUri: String,
    val isSynced: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "memo_items",
    foreignKeys = [
        ForeignKey(
            entity = CashMemoEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memoId"])]
)
data class MemoItemEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val serialNo: Int,
    val description: String,
    val quantity: Double,
    val rate: Double
)

data class MemoWithItems(
    @Embedded val memo: CashMemoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "memoId"
    )
    val items: List<MemoItemEntity>
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val address: String,
    val outstandingBalance: Double,
    val totalPurchased: Double,
    val totalPaid: Double,
    val lastPromiseNote: String,
    val lastTransactionAt: Long,
    val createdAt: Long
)

@Entity(tableName = "plant_rates")
data class PlantRateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val retailPrice: Double,
    val wholesalePrice: Double,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)
