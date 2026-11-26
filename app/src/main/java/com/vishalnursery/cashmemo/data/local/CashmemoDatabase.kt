package com.vishalnursery.cashmemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vishalnursery.cashmemo.data.local.dao.CustomerDao
import com.vishalnursery.cashmemo.data.local.dao.MemoDao
import com.vishalnursery.cashmemo.data.local.dao.PaymentDao
import com.vishalnursery.cashmemo.data.local.dao.PlantRateDao
import com.vishalnursery.cashmemo.data.local.entity.CashMemoEntity
import com.vishalnursery.cashmemo.data.local.entity.CustomerEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoItemEntity
import com.vishalnursery.cashmemo.data.local.entity.PaymentAllocationEntity
import com.vishalnursery.cashmemo.data.local.entity.PaymentEntity
import com.vishalnursery.cashmemo.data.local.entity.PlantRateEntity

@Database(
    entities = [
        CashMemoEntity::class,
        MemoItemEntity::class,
        CustomerEntity::class,
        PlantRateEntity::class,
        PaymentEntity::class,
        PaymentAllocationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CashmemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun customerDao(): CustomerDao
    abstract fun plantRateDao(): PlantRateDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: CashmemoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plant_rates ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cash_memos ADD COLUMN isWholesale INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payments` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `customerId` TEXT NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `customerMobile` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `method` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payment_allocations` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `paymentId` TEXT NOT NULL,
                        `memoId` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        FOREIGN KEY(`paymentId`) REFERENCES `payments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_allocations_paymentId` ON `payment_allocations` (`paymentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_allocations_memoId` ON `payment_allocations` (`memoId`)")
            }
        }

        fun getInstance(context: Context): CashmemoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CashmemoDatabase::class.java,
                    "vishal_cashmemo_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { INSTANCE = it }
            }
        }
    }
}
