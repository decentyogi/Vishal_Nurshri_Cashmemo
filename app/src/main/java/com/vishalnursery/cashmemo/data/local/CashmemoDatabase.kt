package com.vishalnursery.cashmemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vishalnursery.cashmemo.data.local.dao.CustomerDao
import com.vishalnursery.cashmemo.data.local.dao.MemoDao
import com.vishalnursery.cashmemo.data.local.dao.PlantRateDao
import com.vishalnursery.cashmemo.data.local.entity.CashMemoEntity
import com.vishalnursery.cashmemo.data.local.entity.CustomerEntity
import com.vishalnursery.cashmemo.data.local.entity.MemoItemEntity
import com.vishalnursery.cashmemo.data.local.entity.PlantRateEntity

@Database(
    entities = [
        CashMemoEntity::class,
        MemoItemEntity::class,
        CustomerEntity::class,
        PlantRateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CashmemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun customerDao(): CustomerDao
    abstract fun plantRateDao(): PlantRateDao

    companion object {
        @Volatile
        private var INSTANCE: CashmemoDatabase? = null

        fun getInstance(context: Context): CashmemoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CashmemoDatabase::class.java,
                    "vishal_cashmemo_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
