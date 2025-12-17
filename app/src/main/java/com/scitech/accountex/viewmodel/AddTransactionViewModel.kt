package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.Job
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

    // --- MERGED SMART SUGGESTIONS (FIXED LOGIC) ---
    // Logic: Normalize inputs (trim + lowercase) to detect duplicates.
    // Prioritize CoreData formatting.

    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { history ->
            combineAndDedup(CoreData.allCategories, history)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    val descriptionSuggestions: StateFlow<List<String>> = transactionDao.getRecentsDescriptions()
        .map { history ->
            combineAndDedup(CoreData.allDescriptions, history)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allDescriptions)

    /**
     * Helper to robustly de-duplicate lists.
     * 1. Trims and lowercases to find matches.
     * 2. Preserves the first casing found (Defaults > History).
     */
    private fun combineAndDedup(defaults: List<String>, history: List<String>): List<String> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<String>()

        // 1. Process Defaults First (Preferred Casing)
        for (item in defaults) {
            val key = item.trim().lowercase()
            if (key.isNotEmpty() && !seenKeys.contains(key)) {
                seenKeys.add(key)
                result.add(item.trim())
            }
        }

        // 2. Process History (Skip if already exists in defaults)
        for (item in history) {
            val key = item.trim().lowercase()
            if (key.isNotEmpty() && !seenKeys.contains(key)) {
                seenKeys.add(key)
                result.add(item.trim())
            }
        }

        return result.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

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

    // JOB to handle switching inventory streams
    private var inventoryJob: Job? = null

    // REFACTORED: Loads specific notes based on what the user is doing
    fun loadNotesForAccount(accountId: Int, type: TransactionType) {
        inventoryJob?.cancel()
        inventoryJob = viewModelScope.launch {
            val flow = if (type == TransactionType.THIRD_PARTY_OUT) {
                // If handing over money, only show held notes
                noteDao.getActiveThirdPartyNotes(accountId)
            } else {
                // If spending normally, only show personal notes
                noteDao.getActivePersonalNotes(accountId)
            }
            flow.collect { notes ->
                _availableNotes.value = notes
            }
        }
    }

    // --- FORM ACTIONS ---
    fun updateAmount(amount: String) { _uiState.update { it.copy(amountInput = amount) } }
    fun updateCategory(category: String) { _uiState.update { it.copy(category = category) } }
    fun updateDescription(description: String) { _uiState.update { it.copy(description = description) } }
    fun updateThirdPartyName(name: String) { _uiState.update { it.copy(thirdPartyName = name) } } // NEW
    fun updateDate(date: Long) { _uiState.update { it.copy(selectedDate = date) } }

    fun updateType(type: TransactionType) {
        _uiState.update { it.copy(selectedType = type) }
        clearNoteData()
        // Reload inventory based on the new type (Personal vs Third Party)
        if (_uiState.value.selectedAccountId != 0) {
            loadNotesForAccount(_uiState.value.selectedAccountId, type)
        }
    }

    fun updateAccount(accountId: Int) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
        loadNotesForAccount(accountId, _uiState.value.selectedType)
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
        loadNotesForAccount(template.accountId, TransactionType.EXPENSE)
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

    // 3. Save Transaction (UPDATED FOR THIRD PARTY)
    fun addTransaction(imageUris: List<String>) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            // 1. Create Transaction Record
            val tx = Transaction(
                type = state.selectedType,
                amount = amount,
                category = state.category,
                description = state.description,
                date = state.selectedDate,
                accountId = state.selectedAccountId,
                imageUris = imageUris,
                thirdPartyName = state.thirdPartyName // Save the name
            )
            val txId = transactionDao.insertTransaction(tx).toInt()

            // 2. Handle Logic based on Type
            when (state.selectedType) {
                TransactionType.INCOME -> {
                    // Standard Income: Update Balance, Add Personal Notes
                    _incomingNotes.value.forEach { draft ->
                        noteDao.insertNote(CurrencyNote(
                            serialNumber = draft.serial,
                            amount = draft.denomination.toDouble(),
                            denomination = draft.denomination,
                            accountId = state.selectedAccountId,
                            receivedTransactionId = txId,
                            receivedDate = state.selectedDate,
                            isThirdParty = false // PERSONAL
                        ))
                    }
                    accountDao.updateBalance(state.selectedAccountId, amount)
                }

                TransactionType.EXPENSE -> {
                    // Standard Expense: Update Balance, Spend Notes
                    _selectedNoteIds.value.forEach { noteId -> noteDao.markAsSpent(noteId, txId, state.selectedDate) }
                    // Handle change if any (incoming notes during expense)
                    _incomingNotes.value.forEach { draft ->
                        noteDao.insertNote(CurrencyNote(
                            serialNumber = draft.serial,
                            amount = draft.denomination.toDouble(),
                            denomination = draft.denomination,
                            accountId = state.selectedAccountId,
                            receivedTransactionId = txId,
                            receivedDate = state.selectedDate,
                            isThirdParty = false // PERSONAL
                        ))
                    }
                    accountDao.updateBalance(state.selectedAccountId, -amount)
                }

                TransactionType.THIRD_PARTY_IN -> {
                    // Holding Money: NO Balance Update, Add Third-Party Notes
                    _incomingNotes.value.forEach { draft ->
                        noteDao.insertNote(CurrencyNote(
                            serialNumber = draft.serial,
                            amount = draft.denomination.toDouble(),
                            denomination = draft.denomination,
                            accountId = state.selectedAccountId,
                            receivedTransactionId = txId,
                            receivedDate = state.selectedDate,
                            isThirdParty = true, // THIRD PARTY
                            thirdPartyName = state.thirdPartyName
                        ))
                    }
                    // ZERO IMPACT ON NET WORTH
                }

                TransactionType.THIRD_PARTY_OUT -> {
                    // Handing Over: NO Balance Update, Spend Third-Party Notes
                    _selectedNoteIds.value.forEach { noteId -> noteDao.markAsSpent(noteId, txId, state.selectedDate) }
                    // ZERO IMPACT ON NET WORTH
                }

                else -> {} // Transfer logic if needed later
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
    val thirdPartyName: String = "", // NEW
    val selectedAccountId: Int = 0,
    val selectedDate: Long = System.currentTimeMillis()
)