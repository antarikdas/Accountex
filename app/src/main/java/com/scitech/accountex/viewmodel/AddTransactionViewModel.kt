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
    val error: String? = null,

    // --- EDIT MODE FLAGS ---
    val isEditing: Boolean = false,
    val initialImageUris: List<String> = emptyList()
)

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database, application)
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

    // --- SANDBOX STATE ---
    private var editingTransactionId: Int? = null

    private val _availableNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val availableNotes: StateFlow<List<CurrencyNote>> = _availableNotes.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Int>> = _selectedNoteIds.asStateFlow()

    private val _incomingNotes = MutableStateFlow<List<DraftNote>>(emptyList())
    val incomingNotes: StateFlow<List<DraftNote>> = _incomingNotes.asStateFlow()

    private var inventoryJob: Job? = null

    // --- INITIALIZATION & ACCOUNT LOADING ---

    // Called when User picks an account from dropdown
    fun updateAccount(accountId: Int) {
        val currentState = _uiState.value
        if (currentState.selectedAccountId == accountId) return

        _uiState.update { it.copy(selectedAccountId = accountId) }

        if (editingTransactionId != null) {
            // EDIT MODE: If we switch accounts, we must abandon the "Old Tx Snapshot" notes
            // and load the "Active Notes" of the NEW account.
            // We cannot carry over spent notes from Account A to Account B.
            refreshSandboxInventoryForNewAccount(accountId, currentState.selectedType)
        } else {
            // CREATE MODE: Just switch the live listener
            loadLiveNotesForAccount(accountId, currentState.selectedType)
        }
    }

    // Called when User changes Transaction Type (Income/Expense)
    fun updateType(type: TransactionType) {
        _uiState.update {
            it.copy(
                selectedType = type,
                isManualAmount = false,
                amountInput = "",
                error = null
            )
        }
        clearNoteData() // Reset selection on type change

        val accountId = _uiState.value.selectedAccountId
        if (accountId != 0) {
            if (editingTransactionId != null) {
                refreshSandboxInventoryForNewAccount(accountId, type)
            } else {
                loadLiveNotesForAccount(accountId, type)
            }
        }
    }

    // --- LIVE LOADER (For Creating New Tx) ---
    private fun loadLiveNotesForAccount(accountId: Int, type: TransactionType) {
        inventoryJob?.cancel()
        inventoryJob = viewModelScope.launch {
            val flow = if (type == TransactionType.THIRD_PARTY_OUT) {
                noteDao.getActiveThirdPartyNotes(accountId)
            } else {
                noteDao.getActivePersonalNotes(accountId)
            }
            flow.collect { notes -> _availableNotes.value = notes }
        }
    }

    // --- SANDBOX LOADER (For Edit Mode Switching) ---
    private fun refreshSandboxInventoryForNewAccount(accountId: Int, type: TransactionType) {
        inventoryJob?.cancel()
        // In Edit Mode, we fetch ONCE (Snapshot). We don't want live updates shifting items under our fingers.
        viewModelScope.launch {
            val notes = if (type == TransactionType.THIRD_PARTY_OUT) {
                noteDao.getActiveThirdPartyNotes(accountId).first()
            } else {
                noteDao.getActivePersonalNotes(accountId).first()
            }
            _availableNotes.value = notes
            _selectedNoteIds.value = emptySet() // Reset selection because account changed
        }
    }

    // --- THE CORE: LOAD TRANSACTION INTO SANDBOX ---
    fun loadTransactionForEdit(txId: Int) {
        inventoryJob?.cancel() // Stop live updates

        viewModelScope.launch {
            val tx = transactionDao.getTransactionById(txId) ?: return@launch
            editingTransactionId = txId

            // 1. Fetch Ecosystem (Notes involved in this transaction)
            val allRelatedNotes = noteDao.getNotesByTransactionId(txId) // Uses the SUSPEND method now

            // Notes we GAVE (Spent) - Need to appear in the "Wallet" again, pre-selected
            val spentNotes = allRelatedNotes.filter { it.spentTransactionId == txId }

            // Notes we RECEIVED (Income/Change) - Need to appear as "Drafts"
            val createdNotes = allRelatedNotes.filter { it.receivedTransactionId == txId }

            // 2. Fetch currently Active notes in the wallet (for potential swapping)
            val activeNotes = noteDao.getActivePersonalNotes(tx.accountId).first()

            // 3. Construct the Sandbox Inventory
            // Inventory = (Active Notes) + (Notes we spent in THIS transaction)
            val mergedInventory = (activeNotes + spentNotes)
                .distinctBy { it.id }
                .sortedByDescending { it.denomination }

            _availableNotes.value = mergedInventory

            // 4. Pre-Select the notes that were used
            _selectedNoteIds.value = spentNotes.map { it.id }.toSet()

            // 5. Convert "Created" notes back to UI Drafts
            _incomingNotes.value = createdNotes.map { note ->
                DraftNote(
                    serial = if (note.type == CurrencyType.NOTE) note.serialNumber else "",
                    denomination = note.denomination,
                    isCoin = note.type == CurrencyType.COIN
                )
            }

            // 6. Populate UI State
            _uiState.update {
                it.copy(
                    selectedType = tx.type,
                    amountInput = if (tx.amount % 1.0 == 0.0) tx.amount.toInt().toString() else tx.amount.toString(),
                    isManualAmount = true,
                    category = tx.category,
                    description = tx.description,
                    selectedAccountId = tx.accountId,
                    toAccountId = tx.toAccountId,
                    selectedDate = tx.date,
                    thirdPartyName = tx.thirdPartyName ?: "",
                    // Auto-detect mode: If notes were involved, assume Cash Desk mode
                    inputMode = if (spentNotes.isNotEmpty() || createdNotes.isNotEmpty()) InputMode.CASH_DESK else InputMode.KEYPAD,
                    initialImageUris = tx.imageUris,
                    isEditing = true
                )
            }
        }
    }

    // --- UI HELPERS ---
    fun setInputMode(mode: InputMode) { _uiState.update { it.copy(inputMode = mode, error = null) } }
    fun updateAmount(amount: String) { _uiState.update { it.copy(amountInput = amount, isManualAmount = true, error = null) } }
    fun updateCategory(s: String) { _uiState.update { it.copy(category = s, error = null) } }
    fun updateDescription(s: String) { _uiState.update { it.copy(description = s) } }
    fun updateThirdPartyName(s: String) { _uiState.update { it.copy(thirdPartyName = s) } }
    fun updateDate(l: Long) { _uiState.update { it.copy(selectedDate = l) } }
    fun updateToAccount(id: Int) { _uiState.update { it.copy(toAccountId = id) } }

    fun forceSyncAmount() {
        _uiState.update { it.copy(isManualAmount = false) }
        recalculateAutoAmount()
    }

    private fun recalculateAutoAmount() {
        val state = _uiState.value
        if (state.isManualAmount) return

        val isIncoming = state.selectedType == TransactionType.INCOME || state.selectedType == TransactionType.THIRD_PARTY_IN
        var total = 0.0

        if (isIncoming) {
            total = _incomingNotes.value.sumOf { it.denomination }.toDouble()
        } else {
            val selectedNotesSum = _availableNotes.value
                .filter { it.id in _selectedNoteIds.value }
                .sumOf { it.amount }

            val changeSum = _incomingNotes.value.sumOf { it.denomination }.toDouble()
            total = if (selectedNotesSum > changeSum) selectedNotesSum - changeSum else 0.0
        }

        if (total >= 0) {
            _uiState.update { it.copy(amountInput = total.toInt().toString()) }
        }
    }

    // --- CASH DESK LOGIC ---
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
        val newNotes = List(count) { DraftNote("", denom, isCoin) }
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
        // Template application is treated as a "New Entry", so we load live notes
        editingTransactionId = null
        loadLiveNotesForAccount(template.accountId, TransactionType.EXPENSE)
    }

    // --- SAVE / UPDATE ---
    fun saveTransaction(imageUris: List<String>) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _uiState.update { it.copy(error = "Amount must be greater than 0") }
            return
        }

        // Integrity Check: Cash Desk Math
        if (state.inputMode == InputMode.CASH_DESK &&
            (state.selectedType == TransactionType.EXPENSE || state.selectedType == TransactionType.THIRD_PARTY_OUT)) {

            val selectedNotesSum = _availableNotes.value
                .filter { it.id in _selectedNoteIds.value }
                .sumOf { it.amount }

            val changeSum = _incomingNotes.value.sumOf { it.denomination }.toDouble()
            val expectedChange = selectedNotesSum - amount

            if (abs(expectedChange - changeSum) > 0.01) {
                val msg = "Math Mismatch! Selected: ${selectedNotesSum.toInt()}, Cost: ${amount.toInt()}, Change Expected: ${expectedChange.toInt()}, but Entered: ${changeSum.toInt()}."
                _uiState.update { it.copy(error = msg) }
                return
            }
        }

        viewModelScope.launch {
            val isThirdPartyIn = state.selectedType == TransactionType.THIRD_PARTY_IN

            // 1. Prepare Note Entities
            val newNotes = _incomingNotes.value.map { draft ->
                val type = if (draft.isCoin) CurrencyType.COIN else CurrencyType.NOTE
                val finalSerial = if (type == CurrencyType.NOTE) {
                    draft.serial.ifBlank { "UNKNOWN-${UUID.randomUUID().toString().take(8)}" }
                } else {
                    ""
                }

                CurrencyNote(
                    type = type,
                    serialNumber = finalSerial,
                    amount = draft.denomination.toDouble(),
                    denomination = draft.denomination,
                    // IMPORTANT: If Transfer, incoming notes go to destination account
                    accountId = if (state.selectedType == TransactionType.TRANSFER) state.toAccountId ?: state.selectedAccountId else state.selectedAccountId,
                    receivedTransactionId = 0, // Set by Repo
                    receivedDate = state.selectedDate,
                    isThirdParty = isThirdPartyIn,
                    thirdPartyName = if (isThirdPartyIn) state.thirdPartyName else null
                )
            }

            // 2. Prepare Transaction Entity
            val txId = editingTransactionId ?: 0
            val tx = Transaction(
                id = txId,
                type = state.selectedType,
                amount = amount,
                category = if (state.selectedType == TransactionType.EXCHANGE) "Currency Exchange" else state.category,
                description = state.description,
                date = state.selectedDate,
                accountId = state.selectedAccountId,
                toAccountId = state.toAccountId,
                imageUris = imageUris,
                thirdPartyName = state.thirdPartyName
            )

            try {
                if (editingTransactionId != null) {
                    // --- ATOMIC UPDATE ---
                    val oldTx = transactionDao.getTransactionById(txId)
                    if (oldTx != null) {
                        repository.updateTransactionWithInventory(
                            oldTx = oldTx,
                            newTx = tx,
                            newNotes = newNotes,
                            spentNoteIds = _selectedNoteIds.value
                        )
                    }
                } else {
                    // --- ATOMIC CREATE ---
                    repository.saveTransactionWithNotes(
                        transaction = tx,
                        spentNoteIds = _selectedNoteIds.value,
                        newNotes = newNotes
                    )
                }

                // Cleanup
                _uiState.value = TransactionFormState()
                editingTransactionId = null
                clearNoteData()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Save Failed: ${e.message}") }
            }
        }
    }
}