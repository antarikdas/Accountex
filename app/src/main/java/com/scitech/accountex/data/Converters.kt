package com.scitech.accountex.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // --- 1. IMAGE LIST CONVERTERS (Existing) ---
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    // --- 2. TRANSACTION TYPE CONVERTERS (New Safety Lock) ---
    @TypeConverter
    fun fromTransactionType(value: String?): TransactionType {
        // Default to EXPENSE if data is corrupt or null, to prevent crashes
        return try {
            if (value != null) TransactionType.valueOf(value) else TransactionType.EXPENSE
        } catch (e: IllegalArgumentException) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun toTransactionType(type: TransactionType): String {
        return type.name
    }

    // --- 3. ACCOUNT TYPE CONVERTERS (New Safety Lock) ---
    @TypeConverter
    fun fromAccountType(value: String?): AccountType {
        return try {
            if (value != null) AccountType.valueOf(value) else AccountType.BANK
        } catch (e: IllegalArgumentException) {
            AccountType.BANK
        }
    }

    @TypeConverter
    fun toAccountType(type: AccountType): String {
        return type.name
    }
}