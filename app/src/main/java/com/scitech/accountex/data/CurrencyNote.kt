package com.scitech.accountex.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

// 1. Define the explicit type to prevent misclassification
enum class CurrencyType {
    NOTE,
    COIN
}

@Entity(tableName = "currency_notes")
data class CurrencyNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // 2. The critical fix: Explicitly store the type
    val type: CurrencyType = CurrencyType.NOTE,

    val serialNumber: String, // Empty string for COIN
    val amount: Double,
    val denomination: Int,

    val accountId: Int,
    val receivedTransactionId: Int,
    val spentTransactionId: Int? = null,

    val receivedDate: Long,
    val spentDate: Long? = null,

    val isThirdParty: Boolean = false,
    val thirdPartyName: String? = null
) {
    // 3. Helper to determine if this specific item needs a serial number
    fun requiresSerialNumber(): Boolean {
        return type == CurrencyType.NOTE
    }
}

// 4. STEP 4 PREP: Official RBI Color Palette for Indian Currency
// We define it here to keep data & presentation logic aligned by denomination
object CurrencyColors {
    val Note10 = Color(0xFFC68958)   // Chocolate Brown
    val Note20 = Color(0xFF91A95E)   // Greenish Yellow
    val Note50 = Color(0xFF66DDAA)   // Fluorescent Blue
    val Note100 = Color(0xFFE6E6FA)  // Lavender
    val Note200 = Color(0xFFFFD700)  // Bright Yellow
    val Note500 = Color(0xFF808080)  // Stone Grey
    val Coin = Color(0xFFB0B0B0)     // Silver/Steel for Coins
}