package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val noteDao = database.currencyNoteDao()
    private val accountDao = database.accountDao()

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAccountId = MutableStateFlow(0)
    val selectedAccountId: StateFlow<Int> = _selectedAccountId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeNotes: StateFlow<List<CurrencyNote>> = _selectedAccountId.flatMapLatest { accountId ->
        if (accountId == 0) flowOf(emptyList())
        else noteDao.getActiveNotesByAccount(accountId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ✅ NEW: Groups notes by denomination (e.g., 500 -> [Note1, Note2])
    // This provides the UI with the CORRECT values, solving the "Inventory Logic" bug.
    val inventorySummary: StateFlow<Map<Int, List<CurrencyNote>>> = activeNotes.map { notes ->
        notes.groupBy { it.denomination } // This will error until we update CurrencyNote.kt
            .toSortedMap(compareByDescending { it })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectAccount(accountId: Int) {
        _selectedAccountId.value = accountId
    }

    suspend fun searchNote(serial: String): CurrencyNote? {
        return noteDao.getNoteBySerial(serial)
    }
}