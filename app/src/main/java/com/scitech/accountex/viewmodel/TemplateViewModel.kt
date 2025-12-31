package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.TransactionTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val templateDao = db.transactionTemplateDao()
    private val accountDao = db.accountDao()

    // 1. TEMPLATES FLOW
    val templates: StateFlow<List<TransactionTemplate>> = templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. [FIX] ACCOUNTS FLOW (Added this to fix the error)
    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. DELETE ACTION
    fun deleteTemplate(template: TransactionTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            templateDao.deleteTemplate(template)
        }
    }
}