package com.scitech.accountex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Account::class,
        Transaction::class,
        CurrencyNote::class,
        TransactionTemplate::class
    ],
    version = 4, // Incremented to 4
    exportSchema = false
)
@TypeConverters(Converters::class) // Added TypeConverters
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun currencyNoteDao(): CurrencyNoteDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accountex_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}