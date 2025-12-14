package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val noteDao = database.currencyNoteDao()

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // IMPORTANT: Make sure you have this
    private val _appliedTemplate = MutableStateFlow<TransactionTemplate?>(null)
    val appliedTemplate: StateFlow<TransactionTemplate?> = _appliedTemplate.asStateFlow()

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        accountId: Int,
        noteSerials: String = ""
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                date = System.currentTimeMillis(),
                category = category,
                description = description,
                accountId = accountId
            )

            transactionDao.insertTransaction(transaction)

            val txId = transactionDao.insertTransaction(transaction).toInt()

// Save notes if income and serials provided
            if (type == TransactionType.INCOME && noteSerials.isNotBlank()) {
                val serials = noteSerials.split(Regex("[,\n]")).map { it.trim() }.filter { it.isNotEmpty() }
                serials.forEach { serial ->
                    // Extract denomination from serial (last 3 digits typically)
                    val denom = serial.takeLast(3).toIntOrNull() ?: 500
                    noteDao.insertNote(
                        CurrencyNote(
                            serialNumber = serial,
                            denomination = denom,
                            accountId = accountId,
                            receivedTransactionId = txId,
                            receivedDate = System.currentTimeMillis()
                        )
                    )
                }
            }

            val balanceChange = when (type) {
                TransactionType.INCOME -> amount
                TransactionType.EXPENSE -> -amount
                TransactionType.TRANSFER -> 0.0
            }

            accountDao.updateBalance(accountId, balanceChange)
            _appliedTemplate.value = null
        }
    }

    // IMPORTANT: Make sure you have this method
    fun applyTemplate(template: TransactionTemplate) {
        _appliedTemplate.value = template
    }
}