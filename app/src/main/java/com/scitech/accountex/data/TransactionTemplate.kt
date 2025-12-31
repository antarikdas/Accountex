package com.scitech.accountex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val defaultAmount: Double,
    val accountId: Int,

    // [NEW] Critical for Premium Color Logic (Gold vs Coral)
    val type: TransactionType
)