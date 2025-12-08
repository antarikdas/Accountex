package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TemplateViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val templateDao = database.transactionTemplateDao()

    val templates: StateFlow<List<TransactionTemplate>> = templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAsTemplate(
        name: String,
        category: String,
        defaultAmount: Double,
        accountId: Int
    ) {
        viewModelScope.launch {
            val template = TransactionTemplate(
                name = name,
                category = category,
                defaultAmount = defaultAmount,
                accountId = accountId
            )
            templateDao.insertTemplate(template)
        }
    }

    fun deleteTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            templateDao.deleteTemplate(template)
        }
    }
}