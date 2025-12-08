package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val noteDao = database.currencyNoteDao()
    private val accountDao = database.accountDao()

    val activeNotes: StateFlow<List<CurrencyNote>> = noteDao.getAllActiveNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNote(
        serialNumber: String,
        denomination: Int,
        accountId: Int,
        transactionId: Int
    ) {
        viewModelScope.launch {
            val note = CurrencyNote(
                serialNumber = serialNumber,
                denomination = denomination,
                status = NoteStatus.ACTIVE,
                receivedDate = System.currentTimeMillis(),
                receivedTransactionId = transactionId,
                accountId = accountId
            )
            noteDao.insertNote(note)
        }
    }

    fun markNoteAsSpent(noteId: Int, transactionId: Int) {
        viewModelScope.launch {
            noteDao.markNoteAsSpent(
                noteId = noteId,
                spentDate = System.currentTimeMillis(),
                transactionId = transactionId
            )
        }
    }

    suspend fun searchNoteBySerial(serialNumber: String): CurrencyNote? {
        return noteDao.getNoteBySerialNumber(serialNumber)
    }

    fun getNotesByAccount(accountId: Int): Flow<List<CurrencyNote>> {
        return noteDao.getActiveNotesByAccount(accountId)
    }

    suspend fun getTotalCashInAccount(accountId: Int): Int {
        return noteDao.getTotalActiveAmount(accountId) ?: 0
    }
}