package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        RoomEntity::class,
        RoomMemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        PaymentRecordEntity::class,
        RoomNoteEntity::class,
        AuditLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VwsDatabase : RoomDatabase() {
    abstract fun vwsDao(): VwsDao

    companion object {
        @Volatile
        private var INSTANCE: VwsDatabase? = null

        fun getDatabase(context: Context): VwsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VwsDatabase::class.java,
                    "vws_expense_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
