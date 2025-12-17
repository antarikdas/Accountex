package com.scitech.accountex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Account::class,
        Transaction::class,
        CurrencyNote::class,
        TransactionTemplate::class
    ],
    version = 5, // INCREMENTED TO 5
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun currencyNoteDao(): CurrencyNoteDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // --- MIGRATION SCRIPT: Version 4 -> 5 ---
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add 'thirdPartyName' to 'transactions' table
                // We use TEXT and it can be NULL
                database.execSQL("ALTER TABLE transactions ADD COLUMN thirdPartyName TEXT DEFAULT NULL")

                // 2. Add 'isThirdParty' to 'currency_notes' table
                // INTEGER 0 = false, 1 = true. Default is 0 (Personal money)
                database.execSQL("ALTER TABLE currency_notes ADD COLUMN isThirdParty INTEGER NOT NULL DEFAULT 0")

                // 3. Add 'thirdPartyName' to 'currency_notes' table
                database.execSQL("ALTER TABLE currency_notes ADD COLUMN thirdPartyName TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accountex_database"
                )
                    .addMigrations(MIGRATION_4_5) // REGISTER THE MIGRATION
                    .fallbackToDestructiveMigration() // Backup: If migration fails, recreate DB (safety net)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}