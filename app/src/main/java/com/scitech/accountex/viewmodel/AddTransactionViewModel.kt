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

data class DraftNote(val serial: String, val denomination: Int, val isCoin: Boolean = false)

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val noteDao = database.currencyNoteDao()

    private val _uiState = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    // --- SUGGESTIONS ---
    val categorySuggestions: StateFlow<List<String>> = transactionDao.getUniqueCategories()
        .map { history -> combineAndDedup(CoreData.allCategories, history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allCategories)

    val descriptionSuggestions: StateFlow<List<String>> = transactionDao.getRecentsDescriptions()
        .map { history -> combineAndDedup(CoreData.allDescriptions, history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoreData.allDescriptions)

    private fun combineAndDedup(defaults: List<String>, history: List<String>): List<String> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (item in defaults) {
            val key = item.trim().lowercase()
            if (key.isNotEmpty() && !seenKeys.contains(key)) { seenKeys.add(key); result.add(item.trim()) }
        }
        for (item in history) {
            val key = item.trim().lowercase()
            if (key.isNotEmpty() && !seenKeys.contains(key)) { seenKeys.add(key); result.add(item.trim()) }
        }
        return result.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    // --- ACCOUNTS & INVENTORY ---
    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _availableNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val availableNotes: StateFlow<List<CurrencyNote>> = _availableNotes.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Int>> = _selectedNoteIds.asStateFlow()

    private val _incomingNotes = MutableStateFlow<List<DraftNote>>(emptyList())
    val incomingNotes: StateFlow<List<DraftNote>> = _incomingNotes.asStateFlow()

    private var inventoryJob: Job? = null

    fun loadNotesForAccount(accountId: Int, type: TransactionType) {
        inventoryJob?.cancel()
        // LOAD LOGIC:
        // For Transfer, we MUST load notes from the Source Account
        // so the user can pick exactly which physical cash to move.
        inventoryJob = viewModelScope.launch {
            val flow = if (type == TransactionType.THIRD_PARTY_OUT) {
                noteDao.getActiveThirdPartyNotes(accountId)
            } else {
                noteDao.getActivePersonalNotes(accountId)
            }
            flow.collect { notes -> _availableNotes.value = notes }
        }
    }

    // --- FORM ACTIONS ---
    fun updateAmount(amount: String) { _uiState.update { it.copy(amountInput = amount) } }
    fun updateCategory(category: String) { _uiState.update { it.copy(category = category) } }
    fun updateDescription(description: String) { _uiState.update { it.copy(description = description) } }
    fun updateThirdPartyName(name: String) { _uiState.update { it.copy(thirdPartyName = name) } }
    fun updateDate(date: Long) { _uiState.update { it.copy(selectedDate = date) } }

    fun updateType(type: TransactionType) {
        _uiState.update { it.copy(selectedType = type) }
        clearNoteData()
        if (_uiState.value.selectedAccountId != 0) {
            loadNotesForAccount(_uiState.value.selectedAccountId, type)
        }
    }

    fun updateAccount(accountId: Int) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
        loadNotesForAccount(accountId, _uiState.value.selectedType)
    }

    fun updateToAccount(accountId: Int) {
        _uiState.update { it.copy(toAccountId = accountId) }
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

    // --- INVENTORY HELPERS ---
    fun toggleNoteSelection(noteId: Int) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) current.remove(noteId) else current.add(noteId)
        _selectedNoteIds.value = current
    }

    fun addIncomingNote(serial: String, denomination: Int, isCoin: Boolean) {
        val finalSerial = if (isCoin) "COIN-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}" else serial
        val current = _incomingNotes.value.toMutableList()
        current.add(DraftNote(finalSerial, denomination, isCoin))
        _incomingNotes.value = current
    }

    fun addBulkIncomingNotes(denomination: Int, count: Int, isCoin: Boolean) {
        val newNotes = List(count) {
            val serial = if (isCoin) "COIN-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}" else ""
            DraftNote(serial, denomination, isCoin)
        }
        val current = _incomingNotes.value.toMutableList()
        current.addAll(newNotes)
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

    // --- SAVE LOGIC ---
    fun addTransaction(originalImageUris: List<String>) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val permanentImagePaths = saveImagesToInternalStorage(originalImageUris)

            val tx = Transaction(
                type = state.selectedType,
                amount = amount,
                category = if(state.selectedType == TransactionType.EXCHANGE) "Currency Exchange"
                else if (state.selectedType == TransactionType.TRANSFER) "Transfer"
                else state.category,
                description = state.description,
                date = state.selectedDate,
                accountId = state.selectedAccountId,
                toAccountId = if(state.selectedType == TransactionType.TRANSFER) state.toAccountId else null,
                imageUris = permanentImagePaths,
                thirdPartyName = state.thirdPartyName
            )
            val txId = transactionDao.insertTransaction(tx).toInt()

            // 1. Helper: Insert NEW notes (Manual Entry)
            suspend fun insertIncomingNotes(isThirdParty: Boolean, targetAccountId: Int) {
                _incomingNotes.value.forEach { draft ->
                    noteDao.insertNote(CurrencyNote(
                        serialNumber = draft.serial,
                        amount = draft.denomination.toDouble(),
                        denomination = draft.denomination,
                        accountId = targetAccountId,
                        receivedTransactionId = txId,
                        receivedDate = state.selectedDate,
                        isThirdParty = isThirdParty,
                        thirdPartyName = if(isThirdParty) state.thirdPartyName else null
                    ))
                }
            }

            // 2. Helper: Mark notes as SPENT (Removes from Source)
            suspend fun spendSelectedNotes() {
                _selectedNoteIds.value.forEach { noteId ->
                    noteDao.markAsSpent(noteId, txId, state.selectedDate)
                }
            }

            when (state.selectedType) {
                TransactionType.INCOME -> {
                    insertIncomingNotes(isThirdParty = false, targetAccountId = state.selectedAccountId)
                    accountDao.updateBalance(state.selectedAccountId, amount)
                }
                TransactionType.EXPENSE -> {
                    spendSelectedNotes()
                    insertIncomingNotes(isThirdParty = false, targetAccountId = state.selectedAccountId)
                    accountDao.updateBalance(state.selectedAccountId, -amount)
                }
                TransactionType.TRANSFER -> {
                    // A. Update Balances
                    accountDao.updateBalance(state.selectedAccountId, -amount)
                    if(state.toAccountId != null) {
                        accountDao.updateBalance(state.toAccountId, amount)

                        // B. THE MAGIC: Move EXACT Selected Notes/Coins
                        val notesToMove = _availableNotes.value.filter { it.id in _selectedNoteIds.value }
                        notesToMove.forEach { oldNote ->
                            noteDao.insertNote(CurrencyNote(
                                serialNumber = oldNote.serialNumber, // KEEP SERIAL (Works for Coins too)
                                amount = oldNote.amount,
                                denomination = oldNote.denomination,
                                accountId = state.toAccountId, // NEW OWNER (Destination)
                                receivedTransactionId = txId,
                                receivedDate = state.selectedDate,
                                isThirdParty = false,
                                thirdPartyName = null
                            ))
                        }

                        // C. Handle Incoming Manual Notes (e.g. Withdrawal Bank -> Cash)
                        insertIncomingNotes(isThirdParty = false, targetAccountId = state.toAccountId)
                    }
                    // D. Remove the notes from Source
                    spendSelectedNotes()
                }
                TransactionType.THIRD_PARTY_IN -> {
                    insertIncomingNotes(isThirdParty = true, targetAccountId = state.selectedAccountId)
                }
                TransactionType.THIRD_PARTY_OUT -> {
                    spendSelectedNotes()
                }
                TransactionType.EXCHANGE -> {
                    spendSelectedNotes()
                    insertIncomingNotes(isThirdParty = false, targetAccountId = state.selectedAccountId)
                }
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
                inputStream?.use { input ->
                    newFile.outputStream().use { output -> input.copyTo(output) }
                }
                savedPaths.add(Uri.fromFile(newFile).toString())
            } catch (e: Exception) { e.printStackTrace() }
        }
        return savedPaths
    }
}

data class TransactionFormState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val category: String = "",
    val description: String = "",
    val thirdPartyName: String = "",
    val selectedAccountId: Int = 0,
    val toAccountId: Int? = null,
    val selectedDate: Long = System.currentTimeMillis()
)