package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()

    // Expose accounts as StateFlow so Compose can observe them
    val accounts: StateFlow<List<Account>> =
        accountDao
            .getAllAccounts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /**
     * Add a new transaction to the database.
     *
     * @param type INCOME / EXPENSE / TRANSFER
     * @param amount Amount of the transaction
     * @param category Category text (e.g. Food, Salary)
     * @param description Optional description
     * @param accountId Main account affected
     * @param imageUri Uri string of attached bill image (if any)
     *
     * For now, we treat TRANSFER = no balance change here.
     * Later we will handle transfers with a dedicated function.
     */
    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        accountId: Int,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                date = System.currentTimeMillis(),
                category = category,
                description = description,
                accountId = accountId,
                toAccountId = null,      // we’ll handle transfers separately later
                imageUri = imageUri
            )

            // Save transaction, Room returns the new row id (we may use it later)
            transactionDao.insertTransaction(transaction)

            // Update account balance
            val balanceChange = when (type) {
                TransactionType.INCOME  -> amount
                TransactionType.EXPENSE -> -amount
                TransactionType.TRANSFER -> 0.0 // TODO: handle transfer logic separately
            }

            if (balanceChange != 0.0) {
                accountDao.updateBalance(accountId, balanceChange)
            }
        }
    }
}
