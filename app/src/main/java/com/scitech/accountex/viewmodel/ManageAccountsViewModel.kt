package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AccountType
import com.scitech.accountex.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageAccountsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    fun addAccount(name: String, type: AccountType, initialBalance: Double) {
        viewModelScope.launch {
            val newAccount = Account(name = name, type = type, balance = initialBalance)
            accountDao.insertAccount(newAccount)
        }
    }

    fun updateAccountName(account: Account, newName: String) {
        viewModelScope.launch {
            accountDao.updateAccount(account.copy(name = newName))
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            // Safety Check: Database Integrity
            val count = transactionDao.getTxCountForAccount(account.id)
            if (count > 0) {
                _errorEvent.value = "Cannot delete: This account has $count active transactions. Please delete them first."
            } else {
                accountDao.deleteAccount(account)
            }
        }
    }

    fun clearError() { _errorEvent.value = null }
}