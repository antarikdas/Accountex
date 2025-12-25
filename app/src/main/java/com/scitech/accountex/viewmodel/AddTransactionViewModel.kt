package com.scitech.accountex.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class InputMode {
    KEYPAD,     // Typing amount manually
    CASH_DESK   // Counting notes/coins
}

data class DraftNote(val serial: String, val denomination: Int, val isCoin: Boolean = false)

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
    val inputMode: InputMode = InputMode.KEYPAD
)

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val noteDao = database.currencyNoteDao()

    private val _uiState = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    // --- DATA ---
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

    private val _incomingNotes = MutableStateFlow<List<DraftNote>>(emptyList())
    val incomingNotes: StateFlow<List<DraftNote>> = _incomingNotes.asStateFlow()

    private var inventoryJob: Job? = null

    // --- INITIALIZATION ---
    fun loadNotesForAccount(accountId: Int, type: TransactionType) {
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

    // --- ACTIONS ---
    fun setInputMode(mode: InputMode) { _uiState.update { it.copy(inputMode = mode) } }

    fun updateAmount(amount: String) { _uiState.update { it.copy(amountInput = amount, isManualAmount = true) } }

    fun forceSyncAmount() {
        _uiState.update { it.copy(isManualAmount = false) }
        recalculateAutoAmount()
    }

    private fun recalculateAutoAmount() {
        val state = _uiState.value
        if (state.isManualAmount) return

        val isIncoming = state.selectedType == TransactionType.INCOME || state.selectedType == TransactionType.THIRD_PARTY_IN || state.selectedType == TransactionType.EXCHANGE
        val isOutgoing = state.selectedType == TransactionType.EXPENSE || state.selectedType == TransactionType.THIRD_PARTY_OUT || state.selectedType == TransactionType.TRANSFER

        var total = 0.0
        if (isIncoming) {
            total = _incomingNotes.value.sumOf { it.denomination }.toDouble()
        } else if (isOutgoing) {
            val selected = _availableNotes.value.filter { it.id in _selectedNoteIds.value }
            total = selected.sumOf { it.amount }
        }

        if (total > 0) {
            _uiState.update { it.copy(amountInput = total.toInt().toString()) }
        } else if (_incomingNotes.value.isEmpty() && _selectedNoteIds.value.isEmpty()) {
            _uiState.update { it.copy(amountInput = "") }
        }
    }

    fun updateType(type: TransactionType) {
        _uiState.update { it.copy(selectedType = type, isManualAmount = false, amountInput = "") }
        clearNoteData()
        if (_uiState.value.selectedAccountId != 0) loadNotesForAccount(_uiState.value.selectedAccountId, type)
    }

    fun updateAccount(id: Int) {
        _uiState.update { it.copy(selectedAccountId = id) }
        loadNotesForAccount(id, _uiState.value.selectedType)
    }

    fun updateCategory(s: String) { _uiState.update { it.copy(category = s) } }
    fun updateDescription(s: String) { _uiState.update { it.copy(description = s) } }
    fun updateThirdPartyName(s: String) { _uiState.update { it.copy(thirdPartyName = s) } }
    fun updateDate(l: Long) { _uiState.update { it.copy(selectedDate = l) } }
    fun updateToAccount(id: Int) { _uiState.update { it.copy(toAccountId = id) } }

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
        val newNotes = List(count) { DraftNote(if(isCoin) "COIN-${UUID.randomUUID().toString().take(4)}" else "", denom, isCoin) }
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
                isManualAmount = true
            )
        }
        loadNotesForAccount(template.accountId, TransactionType.EXPENSE)
    }

    fun addTransaction(imageUris: List<String>) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val permanentPaths = saveImagesToInternalStorage(imageUris)
            val tx = Transaction(
                type = state.selectedType,
                amount = amount,
                category = if(state.selectedType == TransactionType.EXCHANGE) "Currency Exchange" else state.category,
                description = state.description,
                date = state.selectedDate,
                accountId = state.selectedAccountId,
                toAccountId = state.toAccountId,
                imageUris = permanentPaths,
                thirdPartyName = state.thirdPartyName
            )
            val txId = transactionDao.insertTransaction(tx).toInt()

            val isOutgoing = state.selectedType == TransactionType.EXPENSE || state.selectedType == TransactionType.TRANSFER || state.selectedType == TransactionType.EXCHANGE || state.selectedType == TransactionType.THIRD_PARTY_OUT
            if (isOutgoing) {
                _selectedNoteIds.value.forEach { noteId ->
                    noteDao.markAsSpent(noteId, txId, state.selectedDate)
                }
            }

            suspend fun insertNotes(targetAccId: Int, is3rd: Boolean) {
                _incomingNotes.value.forEach { draft ->
                    noteDao.insertNote(CurrencyNote(
                        serialNumber = draft.serial,
                        amount = draft.denomination.toDouble(),
                        denomination = draft.denomination,
                        accountId = targetAccId,
                        receivedTransactionId = txId,
                        receivedDate = state.selectedDate,
                        isThirdParty = is3rd,
                        thirdPartyName = if(is3rd) state.thirdPartyName else null
                    ))
                }
            }

            when (state.selectedType) {
                TransactionType.INCOME -> {
                    insertNotes(state.selectedAccountId, false)
                    accountDao.updateBalance(state.selectedAccountId, amount)
                }
                TransactionType.EXPENSE -> {
                    accountDao.updateBalance(state.selectedAccountId, -amount)
                }
                TransactionType.TRANSFER -> {
                    accountDao.updateBalance(state.selectedAccountId, -amount)
                    if(state.toAccountId != null) {
                        accountDao.updateBalance(state.toAccountId, amount)
                        insertNotes(state.toAccountId, false)
                    }
                }
                TransactionType.EXCHANGE -> {
                    insertNotes(state.selectedAccountId, false)
                }
                TransactionType.THIRD_PARTY_IN -> {
                    insertNotes(state.selectedAccountId, true)
                }
                TransactionType.THIRD_PARTY_OUT -> { }
            }

            _uiState.value = TransactionFormState()
            clearNoteData()
        }
    }

    private fun saveImagesToInternalStorage(uris: List<String>): List<String> {
        val context = getApplication<Application>()
        val savedPaths = mutableListOf<String>()
        for (uriString in uris) {
            try {
                if (uriString.contains("com.scitech.accountex")) {
                    savedPaths.add(uriString)
                    continue
                }
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val folder = File(context.filesDir, "Accountex_Images")
                if (!folder.exists()) folder.mkdirs()
                val newFile = File(folder, "IMG_${UUID.randomUUID()}.jpg")
                inputStream?.use { input -> newFile.outputStream().use { output -> input.copyTo(output) } }
                savedPaths.add(Uri.fromFile(newFile).toString())
            } catch (e: Exception) { e.printStackTrace() }
        }
        return savedPaths
    }
}