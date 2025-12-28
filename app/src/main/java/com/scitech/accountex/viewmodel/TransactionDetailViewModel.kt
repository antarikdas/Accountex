package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.CurrencyNote
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {

    // 1. USE REPOSITORY (The Engine)
    private val db = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(db, application)

    // We keep DAOs only for READS where Repository overhead isn't needed
    private val transactionDao = db.transactionDao()
    private val accountDao = db.accountDao()
    private val noteDao = db.currencyNoteDao()

    // --- State ---
    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction = _transaction.asStateFlow()

    private val _relatedNotes = MutableStateFlow<List<CurrencyNote>>(emptyList())
    val relatedNotes = _relatedNotes.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts = _accounts.asStateFlow()

    // Used to signal navigation actions (Back after delete, or To-Edit-Screen)
    private val _navigationEvent = MutableStateFlow<NavigationAction?>(null)
    val navigationEvent = _navigationEvent.asStateFlow()

    enum class NavigationAction {
        NAVIGATE_BACK,
        NAVIGATE_TO_EDIT
    }

    // --- Loading Data ---
    fun loadTransaction(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val tx = transactionDao.getTransactionById(id)
            _transaction.value = tx

            if (tx != null) {
                // Load the inventory used in this transaction
                _relatedNotes.value = noteDao.getNotesByTransactionId(tx.id)
            }
            _accounts.value = accountDao.getAllAccountsSync()
        }
    }

    // --- DELETE LOGIC (Now Safe & Atomic) ---
    fun deleteTransaction() {
        val tx = _transaction.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // This calls the Repository method that handles:
            // 1. Balance Reversal
            // 2. Note Un-spending (Inventory Return)
            // 3. Change Note Deletion
            // 4. Record Deletion
            repository.deleteTransaction(tx.id)

            _navigationEvent.value = NavigationAction.NAVIGATE_BACK
        }
    }

    // --- EDIT LOGIC (The Gateway) ---
    // We no longer mutate state here. We ask the UI to open the "Sandbox" (AddTransactionScreen)
    // passing this transaction ID to initialize the sandbox.
    fun onEditClicked() {
        _navigationEvent.value = NavigationAction.NAVIGATE_TO_EDIT
    }

    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }
}