package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
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

    // --- SUGGESTIONS FOR EDITING ---
    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { history -> (CoreData.allCategories + history).distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    val descriptionSuggestions: StateFlow<List<String>> = transactionDao.getRecentsDescriptions()
        .map { history -> (CoreData.allDescriptions + history).distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allDescriptions)

    fun loadTransaction(id: Int) {
        viewModelScope.launch {
            _transaction.value = transactionDao.getTransactionById(id)
        }
    }

    // UPDATED: Now accepts Category and Description
    fun updateTransaction(id: Int, newAmount: Double, newDate: Long, newCategory: String, newDescription: String) {
        viewModelScope.launch {
            val currentTx = _transaction.value ?: return@launch

            val diff = newAmount - currentTx.amount

            val updatedTx = currentTx.copy(
                amount = newAmount,
                date = newDate,
                category = newCategory,
                description = newDescription
                // imageUris remains the same in this path
            )
            transactionDao.updateTransaction(updatedTx)

            val balanceChange = if (currentTx.type == TransactionType.INCOME) diff else -diff
            accountDao.updateBalance(currentTx.accountId, balanceChange)

            loadTransaction(id)
        }
    }

    // --- NEW IMAGE MANAGEMENT FUNCTIONS ---

    private fun updateTransactionImageUris(newUris: List<String>) {
        viewModelScope.launch {
            val currentTx = _transaction.value ?: return@launch
            val updatedTx = currentTx.copy(imageUris = newUris)
            transactionDao.updateTransaction(updatedTx)
            _transaction.value = updatedTx // Update state immediately
        }
    }

    fun addImageUri(uri: String) {
        val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
        if (!currentUris.contains(uri)) {
            currentUris.add(uri)
            updateTransactionImageUris(currentUris)
        }
    }

    fun removeImageUri(uri: String) {
        val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
        currentUris.remove(uri)
        updateTransactionImageUris(currentUris)
    }

    fun replaceImageUri(oldUri: String, newUri: String) {
        val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
        val index = currentUris.indexOf(oldUri)
        if (index != -1) {
            currentUris[index] = newUri
            updateTransactionImageUris(currentUris)
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val tx = _transaction.value ?: return@launch

            if (tx.type == TransactionType.INCOME) {
                val spentCount = noteDao.countSpentNotesFromTransaction(tx.id)
                if (spentCount > 0) {
                    _errorEvent.value = "Cannot delete: $spentCount notes from this income have already been spent."
                    return@launch
                }
                noteDao.deleteNotesFromTransaction(tx.id)
                accountDao.updateBalance(tx.accountId, -tx.amount)
            } else {
                noteDao.unspendNotesForTransaction(tx.id)
                accountDao.updateBalance(tx.accountId, tx.amount)
            }

            transactionDao.deleteTransaction(tx)
            _navigationEvent.value = true
        }
    }

    fun clearError() { _errorEvent.value = null }
}