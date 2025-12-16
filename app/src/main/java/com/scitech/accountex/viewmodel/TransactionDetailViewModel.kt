package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val noteDao = database.currencyNoteDao()
    private val accountDao = database.accountDao()

    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction: StateFlow<Transaction?> = _transaction.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    fun loadTransaction(id: Int) {
        viewModelScope.launch {
            _transaction.value = transactionDao.getTransactionById(id)
        }
    }

    fun updateTransaction(id: Int, newAmount: Double, newDate: Long) {
        viewModelScope.launch {
            val currentTx = _transaction.value ?: return@launch

            // Calculate balance difference
            val diff = newAmount - currentTx.amount

            // Update the Transaction Record
            val updatedTx = currentTx.copy(amount = newAmount, date = newDate)
            transactionDao.updateTransaction(updatedTx)

            // Update Account Balance Logic
            // If Income increased, add diff. If Expense increased, subtract diff.
            val balanceChange = if (currentTx.type == TransactionType.INCOME) diff else -diff
            accountDao.updateBalance(currentTx.accountId, balanceChange)

            // Reload to show changes
            loadTransaction(id)
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val tx = _transaction.value ?: return@launch

            if (tx.type == TransactionType.INCOME) {
                // SAFETY CHECK: Have we already spent the notes from this income?
                val spentCount = noteDao.countSpentNotesFromTransaction(tx.id)
                if (spentCount > 0) {
                    _errorEvent.value = "Cannot delete: $spentCount notes from this income have already been spent."
                    return@launch
                }
                // Safe to delete: Remove the notes from inventory
                noteDao.deleteNotesFromTransaction(tx.id)
                accountDao.updateBalance(tx.accountId, -tx.amount) // Reverse Income
            } else {
                // Deleting an Expense: "Un-spend" the notes (return to active inventory)
                noteDao.unspendNotesForTransaction(tx.id)
                accountDao.updateBalance(tx.accountId, tx.amount) // Refund Expense
            }

            // Finally, delete the transaction record
            transactionDao.deleteTransaction(tx)
            _navigationEvent.value = true // Trigger navigation back
        }
    }

    fun clearError() { _errorEvent.value = null }
}