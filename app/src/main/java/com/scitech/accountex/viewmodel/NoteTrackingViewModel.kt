package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*

class NoteTrackingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val noteDao = database.currencyNoteDao()
    private val accountDao = database.accountDao()

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAccountId = MutableStateFlow(0)
    val selectedAccountId: StateFlow<Int> = _selectedAccountId.asStateFlow()

    val activeNotes: StateFlow<List<CurrencyNote>> = _selectedAccountId.flatMapLatest { accountId ->
        if (accountId == 0) flowOf(emptyList())
        else noteDao.getActiveNotesByAccount(accountId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(accountId: Int) {
        _selectedAccountId.value = accountId
    }
}