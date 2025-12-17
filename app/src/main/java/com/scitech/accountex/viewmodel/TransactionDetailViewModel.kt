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

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    // --- 1. FIXED SMART SUGGESTIONS (No Duplicates) ---
    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { history ->
            val defaults = CoreData.allCategories
            // Remove history item if it matches a default item (case-insensitive)
            val uniqueHistory = history.filter { histItem ->
                defaults.none { defItem -> defItem.equals(histItem, ignoreCase = true) }
            }
            (defaults + uniqueHistory).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    val descriptionSuggestions: StateFlow<List<String>> = transactionDao.getRecentsDescriptions()
        .map { history ->
            val defaults = CoreData.allDescriptions
            val uniqueHistory = history.filter { histItem ->
                defaults.none { defItem -> defItem.equals(histItem, ignoreCase = true) }
            }
            (defaults + uniqueHistory).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allDescriptions)

    fun loadTransaction(id: Int) {
        viewModelScope.launch {
            _transaction.value = transactionDao.getTransactionById(id)
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

            // Update Balance only if not Third Party (Logic safety)
            if (currentTx.type == TransactionType.INCOME || currentTx.type == TransactionType.EXPENSE) {
                val balanceChange = if (currentTx.type == TransactionType.INCOME) diff else -diff
                accountDao.updateBalance(currentTx.accountId, balanceChange)
            }

            loadTransaction(id)
        }
    }

    // --- 2. REVOLUTIONIZED IMAGE MANAGEMENT (Persistent Storage) ---

    /**
     * Copies the image from the temporary Gallery URI to the App's Private Internal Storage.
     * Returns the permanent "file://" URI string.
     */
    private suspend fun saveImageToInternalStorage(uriStr: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val sourceUri = Uri.parse(uriStr)

                // Create a dedicated folder for images
                val directory = File(context.filesDir, "transaction_attachments")
                if (!directory.exists()) directory.mkdirs()

                // Generate a unique filename
                val fileName = "IMG_${UUID.randomUUID()}.jpg"
                val destinationFile = File(directory, fileName)

                // Open streams
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val outputStream = FileOutputStream(destinationFile)

                // Copy data
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                // Return absolute path URI
                Uri.fromFile(destinationFile).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                null // Return null if copy failed
            }
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
            // 1. Copy image to internal storage first
            val permanentUri = saveImageToInternalStorage(uriStr)

            if (permanentUri != null) {
                // 2. Save the PERMANENT path to DB
                val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
                if (!currentUris.contains(permanentUri)) {
                    currentUris.add(permanentUri)
                    updateTransactionImageUris(currentUris)
                }
            } else {
                _errorEvent.value = "Failed to save image. Please try again."
            }
        }
    }

    fun replaceImageUri(oldUri: String, newUriStr: String) {
        viewModelScope.launch {
            // 1. Copy new image to internal storage
            val permanentNewUri = saveImageToInternalStorage(newUriStr)

            if (permanentNewUri != null) {
                val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
                val index = currentUris.indexOf(oldUri)
                if (index != -1) {
                    currentUris[index] = permanentNewUri
                    updateTransactionImageUris(currentUris)

                    // Optional: Delete the old file from internal storage to save space
                    try {
                        val oldFile = File(Uri.parse(oldUri).path ?: "")
                        if (oldFile.exists()) oldFile.delete()
                    } catch (e: Exception) { /* Ignore deletion errors */ }
                }
            } else {
                _errorEvent.value = "Failed to replace image."
            }
        }
    }

    fun removeImageUri(uri: String) {
        val currentUris = _transaction.value?.imageUris.orEmpty().toMutableList()
        if (currentUris.remove(uri)) {
            updateTransactionImageUris(currentUris)
            // Optional: Delete actual file
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val file = File(Uri.parse(uri).path ?: "")
                    if (file.exists()) file.delete()
                } catch (e: Exception) { /* Ignore */ }
            }
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val tx = _transaction.value ?: return@launch

            // Check if Income notes are spent
            if (tx.type == TransactionType.INCOME) {
                val spentCount = noteDao.countSpentNotesFromTransaction(tx.id)
                if (spentCount > 0) {
                    _errorEvent.value = "Cannot delete: $spentCount notes from this income have already been spent."
                    return@launch
                }
                noteDao.deleteNotesFromTransaction(tx.id)
                accountDao.updateBalance(tx.accountId, -tx.amount)
            }
            // Handle Expense/Third Party
            else if (tx.type == TransactionType.EXPENSE) {
                noteDao.unspendNotesForTransaction(tx.id)
                accountDao.updateBalance(tx.accountId, tx.amount)
            }
            else if (tx.type == TransactionType.THIRD_PARTY_IN) {
                noteDao.deleteNotesFromTransaction(tx.id)
                // No Balance Update
            }
            else if (tx.type == TransactionType.THIRD_PARTY_OUT) {
                noteDao.unspendNotesForTransaction(tx.id)
                // No Balance Update
            }

            transactionDao.deleteTransaction(tx)
            _navigationEvent.value = true
        }
    }

    fun clearError() { _errorEvent.value = null }
}