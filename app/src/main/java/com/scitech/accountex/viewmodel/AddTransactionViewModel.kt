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

data class DraftNote(val serial: String, val denomination: Int)

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

    // --- SAVE LOGIC (WITH IMAGE PERSISTENCE) ---
    fun addTransaction(originalImageUris: List<String>) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            // FIX: Save images to internal storage so they don't disappear
            val permanentImagePaths = saveImagesToInternalStorage(originalImageUris)

            val tx = Transaction(
                type = state.selectedType,
                amount = amount,
                category = state.category,
                description = state.description,
                date = state.selectedDate,
                accountId = state.selectedAccountId,
                imageUris = permanentImagePaths, // Save the permanent paths
                thirdPartyName = state.thirdPartyName
            )
            val txId = transactionDao.insertTransaction(tx).toInt()

            when (state.selectedType) {
                TransactionType.INCOME -> {
                    _incomingNotes.value.forEach { draft ->
                        noteDao.insertNote(CurrencyNote(
                            serialNumber = draft.serial,
                            amount = draft.denomination.toDouble(),
                            denomination = draft.denomination,
                            accountId = state.selectedAccountId,
                            receivedTransactionId = txId,
                            receivedDate = state.selectedDate,
                            isThirdParty = false
                        ))
                    }
                    accountDao.updateBalance(state.selectedAccountId, amount)
                }
                TransactionType.EXPENSE -> {
                    _selectedNoteIds.value.forEach { noteId -> noteDao.markAsSpent(noteId, txId, state.selectedDate) }
                    _incomingNotes.value.forEach { draft ->
                        noteDao.insertNote(CurrencyNote(
                            serialNumber = draft.serial,
                            amount = draft.denomination.toDouble(),
                            denomination = draft.denomination,
                            accountId = state.selectedAccountId,
                            receivedTransactionId = txId,
                            receivedDate = state.selectedDate,
                            isThirdParty = false
                        ))
                    }
                    accountDao.updateBalance(state.selectedAccountId, -amount)
                }
                TransactionType.THIRD_PARTY_IN -> {
                    _incomingNotes.value.forEach { draft ->
                        noteDao.insertNote(CurrencyNote(
                            serialNumber = draft.serial,
                            amount = draft.denomination.toDouble(),
                            denomination = draft.denomination,
                            accountId = state.selectedAccountId,
                            receivedTransactionId = txId,
                            receivedDate = state.selectedDate,
                            isThirdParty = true,
                            thirdPartyName = state.thirdPartyName
                        ))
                    }
                }
                TransactionType.THIRD_PARTY_OUT -> {
                    _selectedNoteIds.value.forEach { noteId -> noteDao.markAsSpent(noteId, txId, state.selectedDate) }
                }
                else -> {}
            }

            _uiState.value = TransactionFormState()
            clearNoteData()
        }
    }

    // --- IMAGE HELPER FUNCTION ---
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
                    newFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                savedPaths.add(Uri.fromFile(newFile).toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
    val selectedDate: Long = System.currentTimeMillis()
)