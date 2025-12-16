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

    private val _availableNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val availableNotes: StateFlow<List<CurrencyNote>> = _availableNotes.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Int>> = _selectedNoteIds.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appliedTemplate = MutableStateFlow<TransactionTemplate?>(null)
    val appliedTemplate: StateFlow<TransactionTemplate?> = _appliedTemplate.asStateFlow()

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        accountId: Int,
        noteSerials: String = "",
        date: Long,                  // ✅ Added: User-selected date
        noteDenomination: Int,       // ✅ Added: User-selected denomination (Fixes Inventory Logic)
        imageUris: List<String>      // ✅ Added: Multiple images support
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                date = date,  // Use the passed date
                category = category,
                description = description,
                accountId = accountId,
                imageUris = imageUris // Save the list of images
            )

            val txId = transactionDao.insertTransaction(transaction).toInt()

            // Save notes if income and serials provided
            if (type == TransactionType.INCOME && noteSerials.isNotBlank()) {
                val serials = noteSerials.split(Regex("[,\n]"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                serials.forEach { serial ->
                    noteDao.insertNote(
                        CurrencyNote(
                            serialNumber = serial,
                            amount = noteDenomination.toDouble(), // ✅ Fixed: Use explicit denomination value
                            denomination = noteDenomination,      // ✅ Fixed: Store correct category
                            accountId = accountId,
                            receivedTransactionId = txId,
                            receivedDate = date
                        )
                    )
                }
            }

            val balanceChange = when (type) {
                TransactionType.INCOME -> amount
                TransactionType.EXPENSE -> -amount
                TransactionType.TRANSFER -> 0.0
            }

            // Mark notes as spent if expense
            if (type == TransactionType.EXPENSE && _selectedNoteIds.value.isNotEmpty()) {
                _selectedNoteIds.value.forEach { noteId ->
                    noteDao.markAsSpent(noteId, txId, date)
                }
                clearNoteSelection()
            }

            accountDao.updateBalance(accountId, balanceChange)
            _appliedTemplate.value = null
        }
    }

    fun loadNotesForAccount(accountId: Int) {
        viewModelScope.launch {
            noteDao.getActiveNotesByAccount(accountId).collect { notes ->
                _availableNotes.value = notes
            }
        }
    }

    fun toggleNoteSelection(noteId: Int) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _selectedNoteIds.value = current
    }

    fun clearNoteSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun applyTemplate(template: TransactionTemplate) {
        _appliedTemplate.value = template
    }
}