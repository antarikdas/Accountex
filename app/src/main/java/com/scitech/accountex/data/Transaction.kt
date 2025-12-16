package com.scitech.accountex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: TransactionType,
    val amount: Double,
    val date: Long,
    val category: String,
    val description: String = "",
    val accountId: Int,
    val toAccountId: Int? = null,
    val imageUris: List<String> = emptyList() // CHANGED: Supports multiple images
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}