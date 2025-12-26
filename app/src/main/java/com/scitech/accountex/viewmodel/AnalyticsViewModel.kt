package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(db, application)

    // 1. Time Filter
    private val _timeRange = MutableStateFlow(TimeRange.THIS_MONTH)
    val timeRange = _timeRange.asStateFlow()

    // 2. Reactive Data Pipeline
    // Whenever 'timeRange' changes, we switch the database query immediately.
    // flatMapLatest ensures we cancel the old query and start the new one.
    @OptIn(ExperimentalCoroutinesApi::class)
    val analyticsState = _timeRange.flatMapLatest { range ->
        val (start, end) = calculateDateBoundaries(range)

        // Combine the two fast queries: Totals and Categories
        combine(
            repository.getTotalsByType(start, end),
            repository.getTopExpenseCategories(start, end)
        ) { typeTotals, catTotals ->

            // A. Process Totals
            // Note: We intentionally ignore TRANSFER/EXCHANGE for Profit/Loss calc
            val income = typeTotals.find { it.type == TransactionType.INCOME }?.total ?: 0.0
            val expense = typeTotals.find { it.type == TransactionType.EXPENSE }?.total ?: 0.0

            // B. Process Pie Chart Data
            val totalExpense = catTotals.sumOf { it.total }
            val pieData = catTotals.mapIndexed { index, cat ->
                PieSlice(
                    label = cat.category,
                    value = cat.total.toFloat(),
                    percentage = if(totalExpense > 0) (cat.total / totalExpense).toFloat() else 0f,
                    color = getChartColor(index)
                )
            }

            AnalyticsData(income, expense, pieData)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsData(0.0, 0.0, emptyList()))

    fun setTimeRange(range: TimeRange) { _timeRange.value = range }

    // --- OPTIMIZED DATE MATH (Calculated Once) ---
    private fun calculateDateBoundaries(range: TimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        // Set to End of Today (future-proofing)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        val start = when(range) {
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            TimeRange.LAST_MONTH -> {
                // Go to 1st of current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                // Subtract 1 month to get 1st of last month
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            TimeRange.ALL_TIME -> 0L
        }

        // Fix "End Date" for Last Month (must be last second of previous month)
        val finalEnd = if (range == TimeRange.LAST_MONTH) {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.add(Calendar.SECOND, -1) // Back 1 second
            c.timeInMillis
        } else {
            end
        }

        return Pair(start, finalEnd)
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

// Re-defining data classes here to ensure no imports break
enum class TimeRange { THIS_MONTH, LAST_MONTH, ALL_TIME }
data class AnalyticsData(val income: Double, val expense: Double, val pieSlices: List<PieSlice>)
data class PieSlice(val label: String, val value: Float, val percentage: Float, val color: Color)