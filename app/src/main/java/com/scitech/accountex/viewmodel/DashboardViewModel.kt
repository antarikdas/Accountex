package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import com.scitech.accountex.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database, application)

    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val currencyNoteDao = database.currencyNoteDao()

    // --- UI STATE ---
    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heldAmount: StateFlow<Double> = currencyNoteDao.getGlobalHeldAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaySummary: StateFlow<DailySummary> = transactions.map { txList ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val todayStart = calendar.timeInMillis
        val todayTransactions = txList.filter { it.date >= todayStart }

        val income = todayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        DailySummary(income, expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummary(0.0, 0.0))

    init {
        viewModelScope.launch {
            // A. Default Accounts
            val existingAccounts = accountDao.getAllAccountsSync()
            if (existingAccounts.isEmpty()) {
                accountDao.insertAccount(Account(name = "Bank Account", type = AccountType.BANK))
                accountDao.insertAccount(Account(name = "Daily Cash", type = AccountType.CASH_DAILY))
                accountDao.insertAccount(Account(name = "Cash Reserve", type = AccountType.CASH_RESERVE))
            }

            // B. 🛡️ SELF-HEALING AUDIT 🛡️
            repository.verifyAndRepairLedger()
        }
    }
}

data class DailySummary(val income: Double, val expense: Double)