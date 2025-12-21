package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.TransactionType
import kotlinx.coroutines.flow.*
import java.util.*

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()

    // 1. Time Filter
    private val _timeRange = MutableStateFlow(TimeRange.THIS_MONTH)
    val timeRange = _timeRange.asStateFlow()

    // 2. Raw Data
    private val _allTransactions = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Processed Data for UI
    val analyticsState = combine(_allTransactions, _timeRange) { list, range ->
        // Filter by Date
        val filtered = list.filter { isInRange(it.date, range) }

        // A. Totals
        val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // B. Category Breakdown (for Pie Chart)
        val categories = filtered
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, txs) -> CategoryData(cat, txs.sumOf { it.amount }) }
            .sortedByDescending { it.amount }
            .take(5) // Top 5 Categories only

        // C. Calculate Percentages for Pie Chart
        val totalExpense = categories.sumOf { it.amount }
        val pieData = categories.mapIndexed { index, cat ->
            PieSlice(
                label = cat.name,
                value = cat.amount.toFloat(),
                percentage = if(totalExpense > 0) (cat.amount / totalExpense).toFloat() else 0f,
                color = getChartColor(index)
            )
        }

        AnalyticsData(income, expense, pieData)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsData(0.0, 0.0, emptyList()))

    fun setTimeRange(range: TimeRange) { _timeRange.value = range }

    // --- HELPERS ---
    private fun isInRange(date: Long, range: TimeRange): Boolean {
        val cal = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = date }

        return when(range) {
            TimeRange.THIS_MONTH -> cal.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            TimeRange.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            }
            TimeRange.ALL_TIME -> true
        }
    }

    private fun getChartColor(index: Int): Color {
        val colors = listOf(
            Color(0xFFEF4444), // Red
            Color(0xFFF59E0B), // Amber
            Color(0xFF3B82F6), // Blue
            Color(0xFF10B981), // Green
            Color(0xFF8B5CF6)  // Violet
        )
        return colors.getOrElse(index) { Color.Gray }
    }
}

enum class TimeRange { THIS_MONTH, LAST_MONTH, ALL_TIME }
data class AnalyticsData(val income: Double, val expense: Double, val pieSlices: List<PieSlice>)
data class CategoryData(val name: String, val amount: Double)
data class PieSlice(val label: String, val value: Float, val percentage: Float, val color: Color)