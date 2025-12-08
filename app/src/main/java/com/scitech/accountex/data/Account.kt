package com.scitech.accountex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0
)

enum class AccountType {
    BANK,
    CASH_DAILY,
    CASH_RESERVE
}