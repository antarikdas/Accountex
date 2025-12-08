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

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appliedTemplate = MutableStateFlow<TransactionTemplate?>(null)
    val appliedTemplate: StateFlow<TransactionTemplate?> = _appliedTemplate.asStateFlow()

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        accountId: Int
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

            val balanceChange = when (type) {
                TransactionType.INCOME -> amount
                TransactionType.EXPENSE -> -amount
                TransactionType.TRANSFER -> 0.0
            }

            accountDao.updateBalance(accountId, balanceChange)
            _appliedTemplate.value = null
        }
    }

    fun applyTemplate(template: TransactionTemplate) {
        _appliedTemplate.value = template
    }
}
