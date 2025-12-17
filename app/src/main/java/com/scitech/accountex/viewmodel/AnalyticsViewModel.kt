package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
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

        // 1. Filter by Date AND Exclude Third Party for Financial Stats
        val relevantTx = txList.filter {
            it.date in startDate..endDate &&
                    (it.type == TransactionType.INCOME || it.type == TransactionType.EXPENSE)
        }

        val income = relevantTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = relevantTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // 2. Category Breakdown (For Pie Chart)
        val categoryBreakdown = relevantTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        // 3. Dynamic Graph Logic (Bar/Line Chart)
        val graphPattern = when (period) {
            AnalyticsPeriod.THIS_WEEK -> "EEE" // Mon, Tue
            AnalyticsPeriod.THIS_MONTH -> "dd" // 01, 05
            AnalyticsPeriod.THIS_YEAR -> "MMM" // Jan, Feb
            AnalyticsPeriod.ALL_TIME -> "yyyy" // 2024, 2025
            AnalyticsPeriod.TODAY -> "hh a"    // 10 AM, 11 AM
        }

        val dateFormatter = SimpleDateFormat(graphPattern, Locale.getDefault())

        val graphData = relevantTx
            .filter { it.type == TransactionType.EXPENSE } // We usually graph expenses
            .groupBy {
                // Group by the formatted label
                dateFormatter.format(Date(it.date))
            }
            .map { (label, txs) ->
                // We need to keep a representative date for sorting,
                // otherwise "Apr" comes before "Jan" alphabetically.
                val firstTxDate = txs.first().date
                Triple(label, txs.sumOf { it.amount }, firstTxDate)
            }
            .sortedBy { it.third } // Sort Chronologically
            .map { it.first to it.second } // Map back to Label -> Amount

        val topExpenses = relevantTx
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(5)

        PeriodSummary(
            income = income,
            expense = expense,
            categoryBreakdown = categoryBreakdown,
            graphData = graphData,
            topExpenses = topExpenses,
            transactionCount = relevantTx.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    fun setPeriod(period: AnalyticsPeriod) {
        _selectedPeriod.value = period
    }

    private fun getDateRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        // End date is effectively "now" (or end of today if you prefer inclusive filtering)
        // Setting to end of today to catch any future-dated txs for today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endDate = calendar.timeInMillis

        // Reset for Start Date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startDate = when (period) {
            AnalyticsPeriod.TODAY -> calendar.timeInMillis
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
            AnalyticsPeriod.ALL_TIME -> 0L
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
    val graphData: List<Pair<String, Double>> = emptyList(), // Label -> Amount
    val topExpenses: List<Transaction> = emptyList(),
    val transactionCount: Int = 0
) {
    val net: Double get() = income - expense
    val savingsRate: Double get() = if (income > 0) ((income - expense) / income) * 100 else 0.0
}