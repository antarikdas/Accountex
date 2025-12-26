package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import com.scitech.accountex.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

enum class InputMode {
    KEYPAD,     // Typing amount manually (Bank/Digital)
    CASH_DESK   // Counting notes/coins (Physical Cash)
}

data class DraftNote(
    val serial: String,
    val denomination: Int,
    val isCoin: Boolean = false
)

data class TransactionFormState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val isManualAmount: Boolean = false,
    val category: String = "",
    val description: String = "",
    val thirdPartyName: String = "",
    val selectedAccountId: Int = 0,
    val toAccountId: Int? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val inputMode: InputMode = InputMode.KEYPAD,
    val error: String? = null // NEW: UI Error feedback
)

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    // --- ARCHITECTURE UPGRADE: Use Repository, not DAOs directly ---
    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database, application)

    // Keep Read-Only DAOs for UI lists (allowed in MVVM)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val noteDao = database.currencyNoteDao()

    private val _uiState = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    // --- DATA STREAMS ---
    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { combineAndDedup(CoreData.allCategories, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    val descriptionSuggestions: StateFlow<List<String>> = transactionDao.getRecentsDescriptions()
        .map { combineAndDedup(CoreData.allDescriptions, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allDescriptions)

    private fun combineAndDedup(defaults: List<String>, history: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return (defaults + history).filter { seen.add(it.trim().lowercase()) }.sorted()
    }

    // --- CASH STATE ---
    private val _availableNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val availableNotes: StateFlow<List<CurrencyNote>> = _availableNotes.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Int>> = _selectedNoteIds.asStateFlow()

    // This holds notes you are RECEIVING (Income) OR notes you are getting as CHANGE (Expense)
    private val _incomingNotes = MutableStateFlow<List<DraftNote>>(emptyList())
    val incomingNotes: StateFlow<List<DraftNote>> = _incomingNotes.asStateFlow()

    private var inventoryJob: Job? = null

    // --- INITIALIZATION ---
    fun loadNotesForAccount(accountId: Int, type: TransactionType) {
        inventoryJob?.cancel()
        inventoryJob = viewModelScope.launch {
            // Logic: If we are paying someone back (Third Party Out), we show Held Notes.
            // Otherwise, we show our Personal Notes.
            val flow = if (type == TransactionType.THIRD_PARTY_OUT) {
                noteDao.getActiveThirdPartyNotes(accountId)
            } else {
                noteDao.getActivePersonalNotes(accountId)
            }
            flow.collect { notes -> _availableNotes.value = notes }
        }
    }

    // --- UI ACTIONS ---
    fun setInputMode(mode: InputMode) {
        _uiState.update { it.copy(inputMode = mode, error = null) }
    }

    fun updateAmount(amount: String) {
        _uiState.update { it.copy(amountInput = amount, isManualAmount = true, error = null) }
    }

    fun forceSyncAmount() {
        _uiState.update { it.copy(isManualAmount = false) }
        recalculateAutoAmount()
    }

    private fun recalculateAutoAmount() {
        val state = _uiState.value
        if (state.isManualAmount) return

        val isIncoming = state.selectedType == TransactionType.INCOME || state.selectedType == TransactionType.THIRD_PARTY_IN
        // Note: For EXPENSE, we calculate amount based on Selected Notes minus Change

        var total = 0.0

        if (isIncoming) {
            // Sum of notes we are adding
            total = _incomingNotes.value.sumOf { it.denomination }.toDouble()
        } else {
            // EXPENSE / TRANSFER / THIRD_PARTY_OUT
            val selectedNotesSum = _availableNotes.value
                .filter { it.id in _selectedNoteIds.value }
                .sumOf { it.amount }

            val changeSum = _incomingNotes.value.sumOf { it.denomination }.toDouble()

            // Logic: Expense Amount = (Notes I gave) - (Change I got back)
            total = if (selectedNotesSum > changeSum) selectedNotesSum - changeSum else 0.0
        }

        if (total >= 0) {
            _uiState.update { it.copy(amountInput = total.toInt().toString()) }
        }
    }

    fun updateType(type: TransactionType) {
        _uiState.update { it.copy(selectedType = type, isManualAmount = false, amountInput = "", error = null) }
        clearNoteData()
        if (_uiState.value.selectedAccountId != 0) loadNotesForAccount(_uiState.value.selectedAccountId, type)
    }

    // Standard field updates
    fun updateAccount(id: Int) {
        _uiState.update { it.copy(selectedAccountId = id) }
        loadNotesForAccount(id, _uiState.value.selectedType)
    }
    fun updateCategory(s: String) { _uiState.update { it.copy(category = s, error = null) } }
    fun updateDescription(s: String) { _uiState.update { it.copy(description = s) } }
    fun updateThirdPartyName(s: String) { _uiState.update { it.copy(thirdPartyName = s) } }
    fun updateDate(l: Long) { _uiState.update { it.copy(selectedDate = l) } }
    fun updateToAccount(id: Int) { _uiState.update { it.copy(toAccountId = id) } }

    // --- CASH HANDLING ---
    fun toggleNoteSelection(noteId: Int) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) current.remove(noteId) else current.add(noteId)
        _selectedNoteIds.value = current
        recalculateAutoAmount()
    }

    fun addIncomingNote(serial: String, denom: Int, isCoin: Boolean) {
        val list = _incomingNotes.value.toMutableList()
        list.add(DraftNote(serial, denom, isCoin))
        _incomingNotes.value = list
        recalculateAutoAmount()
    }

    fun addBulkIncomingNotes(denom: Int, count: Int, isCoin: Boolean) {
        val newNotes = List(count) {
            DraftNote(if(isCoin) "COIN-${UUID.randomUUID().toString().take(4)}" else "", denom, isCoin)
        }
        val list = _incomingNotes.value.toMutableList()
        list.addAll(newNotes)
        _incomingNotes.value = list
        recalculateAutoAmount()
    }

    fun removeIncomingNote(index: Int) {
        val list = _incomingNotes.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _incomingNotes.value = list
            recalculateAutoAmount()
        }
    }

    fun clearNoteData() {
        _selectedNoteIds.value = emptySet()
        _incomingNotes.value = emptyList()
    }

    fun applyTemplate(template: TransactionTemplate) {
        _uiState.update {
            it.copy(
                amountInput = template.defaultAmount.toInt().toString(),
                category = template.category,
                description = template.name,
                selectedAccountId = template.accountId,
                selectedType = TransactionType.EXPENSE,
                isManualAmount = true,
                error = null
            )
        }
        loadNotesForAccount(template.accountId, TransactionType.EXPENSE)
    }

    // --- SAVE LOGIC (THE BIG CHANGE) ---
    fun addTransaction(imageUris: List<String>) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _uiState.update { it.copy(error = "Amount must be greater than 0") }
            return
        }

        // --- VALIDATION: THE "CHANGE" PROBLEM ---
        // If we are in CASH_DESK mode for an EXPENSE, math must be perfect.
        if (state.inputMode == InputMode.CASH_DESK &&
            (state.selectedType == TransactionType.EXPENSE || state.selectedType == TransactionType.THIRD_PARTY_OUT)) {

            val selectedNotesSum = _availableNotes.value
                .filter { it.id in _selectedNoteIds.value }
                .sumOf { it.amount }

            val changeSum = _incomingNotes.value.sumOf { it.denomination }.toDouble()

            // Expected: (Money I Gave) - (Cost of Item) == (Change I Got)
            val expectedChange = selectedNotesSum - amount

            // Allow 0.01 margin for floating point errors
            if (abs(expectedChange - changeSum) > 0.01) {
                val msg = "Math Mismatch! You gave ${selectedNotesSum.toInt()}, item is ${amount.toInt()}. Change must be ${expectedChange.toInt()}, but you recorded ${changeSum.toInt()}."
                _uiState.update { it.copy(error = msg) }
                return
            }
        }

        // --- PREPARE DATA FOR REPOSITORY ---
        viewModelScope.launch {
            val isThirdPartyIn = state.selectedType == TransactionType.THIRD_PARTY_IN

            // Convert DraftNotes to Real CurrencyNotes
            val newNotes = _incomingNotes.value.map { draft ->
                CurrencyNote(
                    serialNumber = draft.serial.ifBlank { "UNKNOWN-${UUID.randomUUID().toString().take(8)}" },
                    amount = draft.denomination.toDouble(),
                    denomination = draft.denomination,
                    // If Transfer, notes go to Dest. If Income/Change, they go to Source.
                    accountId = if(state.selectedType == TransactionType.TRANSFER) state.toAccountId ?: state.selectedAccountId else state.selectedAccountId,
                    receivedTransactionId = 0, // Repository will fill this
                    receivedDate = state.selectedDate,
                    isThirdParty = isThirdPartyIn,
                    thirdPartyName = if(isThirdPartyIn) state.thirdPartyName else null
                )
            }

            val tx = Transaction(
                type = state.selectedType,
                amount = amount,
                category = if(state.selectedType == TransactionType.EXCHANGE) "Currency Exchange" else state.category,
                description = state.description,
                date = state.selectedDate,
                accountId = state.selectedAccountId,
                toAccountId = state.toAccountId,
                imageUris = imageUris, // Repository handles saving
                thirdPartyName = state.thirdPartyName
            )

            try {
                // ATOMIC SAVE via Repository
                repository.saveTransactionWithNotes(
                    transaction = tx,
                    spentNoteIds = _selectedNoteIds.value,
                    newNotes = newNotes
                )

                // Reset UI on success
                _uiState.value = TransactionFormState()
                clearNoteData()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Save Failed: ${e.message}") }
            }
        }
    }
}