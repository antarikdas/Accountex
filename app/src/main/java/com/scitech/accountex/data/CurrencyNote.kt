package com.scitech.accountex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_notes")
data class CurrencyNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serialNumber: String,
    val denomination: Int,
    val accountId: Int,
    val receivedTransactionId: Int,
    val spentTransactionId: Int? = null,
    val receivedDate: Long,
    val spentDate: Long? = null
)