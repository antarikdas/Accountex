package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DraftNote(val serial: String, val denomination: Int)

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val noteDao = database.currencyNoteDao()

    private val _uiState = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    // --- MERGED SMART SUGGESTIONS ---
    // Combines Core Defaults + History from DB
    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { history -> (CoreData.allCategories + history).distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    val descriptionSuggestions: StateFlow<List<String>> = transactionDao.getRecentsDescriptions()
        .map { history -> (CoreData.allDescriptions + history).distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allDescriptions)

    // 1. Load Accounts
    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Inventory State
    private val _availableNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val availableNotes: StateFlow<List<CurrencyNote>> = _availableNotes.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Int>> = _selectedNoteIds.asStateFlow()

    private val _incomingNotes = MutableStateFlow<List<DraftNote>>(emptyList())
    val incomingNotes: StateFlow<List<DraftNote>> = _incomingNotes.asStateFlow()

    fun loadNotesForAccount(accountId: Int) {
        viewModelScope.launch {
            noteDao.getActiveNotesByAccount(accountId).collect { notes ->
                _availableNotes.value = notes
            }
        }
    }

    // --- FORM ACTIONS ---
    fun updateAmount(amount: String) { _uiState.update { it.copy(amountInput = amount) } }
    fun updateCategory(category: String) { _uiState.update { it.copy(category = category) } }
    fun updateDescription(description: String) { _uiState.update { it.copy(description = description) } }
    fun updateDate(date: Long) { _uiState.update { it.copy(selectedDate = date) } }

    fun updateType(type: TransactionType) {
        _uiState.update { it.copy(selectedType = type) }
        clearNoteData()
    }

    fun updateAccount(accountId: Int) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
        loadNotesForAccount(accountId)
    }

    fun applyTemplate(template: TransactionTemplate) {
        _uiState.update {
            it.copy(
                amountInput = template.defaultAmount.toInt().toString(),
                category = template.category,
                description = template.name,
                selectedAccountId = template.accountId,
                selectedType = TransactionType.EXPENSE
            )
        }
        loadNotesForAccount(template.accountId)
    }

    // --- INVENTORY ACTIONS ---
    fun toggleNoteSelection(noteId: Int) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) current.remove(noteId) else current.add(noteId)
        _selectedNoteIds.value = current
    }

    fun addIncomingNote(serial: String, denomination: Int) {
        val current = _incomingNotes.value.toMutableList()
        current.add(DraftNote(serial, denomination))
        _incomingNotes.value = current
    }

    fun removeIncomingNote(index: Int) {
        val current = _incomingNotes.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _incomingNotes.value = current
        }
    }

    fun clearNoteData() {
        _selectedNoteIds.value = emptySet()
        _incomingNotes.value = emptyList()
    }

    // 3. Save Transaction
    fun addTransaction(imageUris: List<String>) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val tx = Transaction(
                type = state.selectedType,
                amount = amount,
                category = state.category,
                description = state.description,
                date = state.selectedDate,
                accountId = state.selectedAccountId,
                imageUris = imageUris
            )
            val txId = transactionDao.insertTransaction(tx).toInt()

            if (state.selectedType == TransactionType.INCOME) {
                _incomingNotes.value.forEach { draft ->
                    noteDao.insertNote(CurrencyNote(serialNumber = draft.serial, amount = draft.denomination.toDouble(), denomination = draft.denomination, accountId = state.selectedAccountId, receivedTransactionId = txId, receivedDate = state.selectedDate))
                }
                accountDao.updateBalance(state.selectedAccountId, amount)
            } else {
                _selectedNoteIds.value.forEach { noteId -> noteDao.markAsSpent(noteId, txId, state.selectedDate) }
                _incomingNotes.value.forEach { draft ->
                    noteDao.insertNote(CurrencyNote(serialNumber = draft.serial, amount = draft.denomination.toDouble(), denomination = draft.denomination, accountId = state.selectedAccountId, receivedTransactionId = txId, receivedDate = state.selectedDate))
                }
                accountDao.updateBalance(state.selectedAccountId, -amount)
            }

            _uiState.value = TransactionFormState()
            clearNoteData()
        }
    }
}

data class TransactionFormState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val category: String = "",
    val description: String = "",
    val selectedAccountId: Int = 0,
    val selectedDate: Long = System.currentTimeMillis()
)