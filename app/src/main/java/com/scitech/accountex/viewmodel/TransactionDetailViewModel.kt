package com.scitech.accountex.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val noteDao = database.currencyNoteDao()
    private val accountDao = database.accountDao()

    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction: StateFlow<Transaction?> = _transaction.asStateFlow()

    // NEW: Holds the physical cash linked to this transaction
    private val _relatedNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val relatedNotes: StateFlow<List<CurrencyNote>> = _relatedNotes.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    // --- 1. SMART SUGGESTIONS ---
    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { history -> (CoreData.allCategories + history).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    val descriptionSuggestions: StateFlow<List<String>> = transactionDao.getRecentsDescriptions()
        .map { history -> (CoreData.allDescriptions + history).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allDescriptions)

    fun loadTransaction(id: Int) {
        viewModelScope.launch {
            val tx = transactionDao.getTransactionById(id)
            _transaction.value = tx

            if (tx != null) {
                // Fetch notes involved in this transaction (either Spent by it OR Created by it)
                // We use Sync here for a snapshot, or you can use Flow if you prefer live updates
                val allNotes = noteDao.getAllNotesSync()
                val linked = allNotes.filter {
                    it.spentTransactionId == id || it.receivedTransactionId == id
                }
                _relatedNotes.value = linked
            }
        }
    }

    // --- 2. THE "TRUE UNDO" LOGIC ---
    fun deleteTransaction() {
        viewModelScope.launch {
            val tx = _transaction.value ?: return@launch
            val notes = _relatedNotes.value

            // A. Reverse Balances
            when (tx.type) {
                TransactionType.INCOME -> accountDao.updateBalance(tx.accountId, -tx.amount)
                TransactionType.EXPENSE -> accountDao.updateBalance(tx.accountId, tx.amount)
                TransactionType.TRANSFER -> {
                    accountDao.updateBalance(tx.accountId, tx.amount) // Refund Source
                    tx.toAccountId?.let { accountDao.updateBalance(it, -tx.amount) } // Deduct Dest
                }
                else -> {}
            }

            // B. Restore Inventory (Un-spend notes)
            // Any note marked as SPENT by this transaction becomes ACTIVE again
            notes.filter { it.spentTransactionId == tx.id }.forEach { note ->
                noteDao.updateNote(note.copy(spentTransactionId = null, spentDate = null))
            }

            // C. Delete Created Inventory (Remove incoming notes)
            // Any note CREATED by this transaction (Income, or the Clone in a Transfer) must be deleted
            notes.filter { it.receivedTransactionId == tx.id }.forEach { note ->
                noteDao.deleteNoteById(note.id)
            }

            // D. Delete the Record
            transactionDao.deleteTransaction(tx)
            _navigationEvent.value = true
        }
    }

    fun updateTransaction(id: Int, newAmount: Double, newDate: Long, newCategory: String, newDescription: String) {
        viewModelScope.launch {
            val currentTx = _transaction.value ?: return@launch
            val diff = newAmount - currentTx.amount

            val updatedTx = currentTx.copy(
                amount = newAmount,
                date = newDate,
                category = newCategory,
                description = newDescription
            )
            transactionDao.updateTransaction(updatedTx)

            // Simple balance update for edits.
            // Note: We do NOT re-calculate physical notes on edit to avoid complexity.
            if (currentTx.type == TransactionType.INCOME) accountDao.updateBalance(currentTx.accountId, diff)
            if (currentTx.type == TransactionType.EXPENSE) accountDao.updateBalance(currentTx.accountId, -diff)

            loadTransaction(id)
        }
    }

    // --- 3. IMAGE MANAGEMENT (Preserved) ---
    private suspend fun saveImageToInternalStorage(uriStr: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val sourceUri = Uri.parse(uriStr)
                val directory = File(context.filesDir, "transaction_attachments")
                if (!directory.exists()) directory.mkdirs()
                val fileName = "IMG_${UUID.randomUUID()}.jpg"
                val destinationFile = File(directory, fileName)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destinationFile).use { output -> input.copyTo(output) }
                }
                Uri.fromFile(destinationFile).toString()
            } catch (e: Exception) { null }
        }
    }

    private fun updateTransactionImageUris(newUris: List<String>) {
        viewModelScope.launch {
            val currentTx = _transaction.value ?: return@launch
            val updatedTx = currentTx.copy(imageUris = newUris)
            transactionDao.updateTransaction(updatedTx)
            _transaction.value = updatedTx
        }
    }

    fun addImageUri(uriStr: String) {
        viewModelScope.launch {
            val permanentUri = saveImageToInternalStorage(uriStr)
            if (permanentUri != null) {
                val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
                if (!currentUris.contains(permanentUri)) {
                    currentUris.add(permanentUri)
                    updateTransactionImageUris(currentUris)
                }
            } else { _errorEvent.value = "Failed to save image." }
        }
    }

    fun replaceImageUri(oldUri: String, newUriStr: String) {
        viewModelScope.launch {
            val permanentNewUri = saveImageToInternalStorage(newUriStr)
            if (permanentNewUri != null) {
                val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
                val index = currentUris.indexOf(oldUri)
                if (index != -1) {
                    currentUris[index] = permanentNewUri
                    updateTransactionImageUris(currentUris)
                }
            }
        }
    }

    fun removeImageUri(uri: String) {
        val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
        if (currentUris.remove(uri)) {
            updateTransactionImageUris(currentUris)
        }
    }

    fun clearError() { _errorEvent.value = null }
}