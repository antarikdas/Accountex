package com.scitech.accountex.data

enum class TransactionType {
    INCOME,
    EXPENSE,
    THIRD_PARTY_IN, // (If you use these for debt)
    THIRD_PARTY_OUT,
    EXCHANGE,
    TRANSFER // <--- NEW TYPE
}