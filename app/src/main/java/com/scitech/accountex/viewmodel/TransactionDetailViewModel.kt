package com.scitech.accountex.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import com.scitech.accountex.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database, application)

    // Read-only DAOs for UI Binding
    private val transactionDao = database.transactionDao()
    private val noteDao = database.currencyNoteDao()
    private val accountDao = database.accountDao()

    // STATE
    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction: StateFlow<Transaction?> = _transaction.asStateFlow()

    private val _relatedNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val relatedNotes: StateFlow<List<CurrencyNote>> = _relatedNotes.asStateFlow()

    private val _accounts = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    // Suggestions
    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { (CoreData.allCategories + it).distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    fun loadTransaction(id: Int) {
        viewModelScope.launch {
            val tx = transactionDao.getTransactionById(id)
            _transaction.value = tx

            if (tx != null) {
                // High-Performance Query for linked notes
                noteDao.getNotesByTransaction(id).collect { notes ->
                    _relatedNotes.value = notes
                }
            }
        }
    }

    // --- 1. FULL EDIT CAPABILITY ---
    // This function accepts ALL fields, including Amount and Account.
    fun updateFullTransaction(
        newAmount: Double,
        newDate: Long,
        newCategory: String,
        newDescription: String,
        newAccountId: Int
    ) {
        val currentTx = _transaction.value ?: return

        viewModelScope.launch {
            val updatedTx = currentTx.copy(
                amount = newAmount,
                date = newDate,
                category = newCategory,
                description = newDescription,
                accountId = newAccountId
            )

            try {
                // The Repository handles the complex Math (Reversing old balance, applying new)
                repository.updateTransaction(
                    oldTx = currentTx,
                    newTx = updatedTx
                )

                // Refresh local state immediately
                _transaction.value = updatedTx

            } catch (e: Exception) {
                e.printStackTrace()
                // You could add an error state here if needed
            }
        }
    }

    // --- 2. DELETE ---
    fun deleteTransaction() {
        val tx = _transaction.value ?: return
        viewModelScope.launch {
            repository.deleteTransaction(tx.id)
            _navigationEvent.value = true // Close Screen
        }
    }

    // --- 3. IMAGE MANAGEMENT ---
    // These methods now update the Live Transaction object but persist via Repository
    fun addImageUri(uriStr: String) {
        updateImages { current -> current + uriStr }
    }

    fun removeImageUri(uri: String) {
        updateImages { current -> current - uri }
    }

    private fun updateImages(transform: (List<String>) -> List<String>) {
        val currentTx = _transaction.value ?: return
        val newUris = transform(currentTx.imageUris)
        val updatedTx = currentTx.copy(imageUris = newUris)

        viewModelScope.launch {
            // Using the smart update to ensure persistence
            repository.updateTransaction(currentTx, updatedTx)
            _transaction.value = updatedTx
        }
    }
}