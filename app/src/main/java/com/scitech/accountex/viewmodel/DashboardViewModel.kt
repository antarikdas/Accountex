package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DailySummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()

    // All accounts
    val accounts: StateFlow<List<Account>> =
        accountDao
            .getAllAccounts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // All transactions, newest first
    val transactions: StateFlow<List<Transaction>> =
        transactionDao
            .getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // Today's income/expense/net summary
    val todaySummary: StateFlow<DailySummary> =
        transactions
            .map { list -> computeTodaySummary(list) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DailySummary()
            )

    // Total balance across all accounts
    fun getTotalBalance(): Double {
        return accounts.value.sumOf { it.balance }
    }

    // Stub for now – you can fill this later
    fun exportToExcel() {
        // TODO: implement export later if you want
    }

    /**
     * Delete a transaction:
     * 1. Reverse its effect on the account balance.
     * 2. Remove it from the database.
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val amount = transaction.amount
            val accountId = transaction.accountId

            val balanceChange = when (transaction.type) {
                TransactionType.INCOME -> -amount      // undo income
                TransactionType.EXPENSE -> +amount     // undo expense
                TransactionType.TRANSFER -> 0.0        // transfer handling can be added later
            }

            if (balanceChange != 0.0) {
                accountDao.updateBalance(accountId, balanceChange)
            }

            transactionDao.deleteTransaction(transaction)
        }
    }

    private fun computeTodaySummary(transactions: List<Transaction>): DailySummary {
        if (transactions.isEmpty()) return DailySummary()

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Start of today
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // End of today
        calendar.timeInMillis = startOfDay
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val startOfNextDay = calendar.timeInMillis

        var income = 0.0
        var expense = 0.0

        for (t in transactions) {
            if (t.date in startOfDay until startOfNextDay) {
                when (t.type) {
                    TransactionType.INCOME -> income += t.amount
                    TransactionType.EXPENSE -> expense += t.amount
                    TransactionType.TRANSFER -> { /* ignore */ }
                }
            }
        }

        val net = income - expense
        return DailySummary(income = income, expense = expense, net = net)
    }
}
