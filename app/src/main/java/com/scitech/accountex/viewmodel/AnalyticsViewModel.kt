package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<AnalyticsPeriod> = _selectedPeriod.asStateFlow()

    val periodSummary: StateFlow<PeriodSummary> = combine(
        transactions,
        selectedPeriod
    ) { txList, period ->
        val (startDate, endDate) = getDateRange(period)
        val filteredTx = txList.filter { it.date in startDate..endDate }

        val income = filteredTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = filteredTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val categoryBreakdown = filteredTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val topExpenses = filteredTx
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(5)

        PeriodSummary(
            income = income,
            expense = expense,
            categoryBreakdown = categoryBreakdown,
            topExpenses = topExpenses,
            transactionCount = filteredTx.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    fun setPeriod(period: AnalyticsPeriod) {
        _selectedPeriod.value = period
    }

    private fun getDateRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startDate = when (period) {
            AnalyticsPeriod.TODAY -> {
                calendar.timeInMillis
            }
            AnalyticsPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.timeInMillis
            }
            AnalyticsPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
            AnalyticsPeriod.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            }
            AnalyticsPeriod.ALL_TIME -> {
                0L
            }
        }

        return Pair(startDate, endDate)
    }
}

enum class AnalyticsPeriod {
    TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL_TIME
}

data class PeriodSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val categoryBreakdown: List<Pair<String, Double>> = emptyList(),
    val topExpenses: List<Transaction> = emptyList(),
    val transactionCount: Int = 0
) {
    val net: Double get() = income - expense
    val savingsRate: Double get() = if (income > 0) ((income - expense) / income) * 100 else 0.0
}