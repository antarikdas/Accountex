package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    // USE REPOSITORY
    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database, application)

    // 1. Filters
    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<TransactionType?>(null) // Null = All
    private val _dateRange = MutableStateFlow(DateRange.ALL_TIME)

    // 2. Data Source (From Repository)
    private val _allTransactions = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Processed List (Optimized)
    val ledgerState = combine(_allTransactions, _searchQuery, _selectedType, _dateRange) { list, query, type, range ->

        // OPTIMIZATION: Calculate Date Boundaries ONCE, outside the loop
        val (startTime, endTime) = calculateDateBoundaries(range)

        // A. High-Speed Filter
        val filtered = list.filter { tx ->
            // 1. Date Check (Fast Long comparison)
            if (tx.date < startTime || tx.date > endTime) return@filter false

            // 2. Type Check
            if (type != null && tx.type != type) return@filter false

            // 3. Search Check (Only run regex if query exists)
            if (query.isNotEmpty()) {
                val q = query.lowercase()
                !(tx.description.lowercase().contains(q) ||
                        tx.category.lowercase().contains(q) ||
                        tx.amount.toInt().toString().contains(q))
            } else {
                true
            }
        }

        // B. Grouping (Optimized Header Generation)
        // We use a helper to avoid creating SimpleDateFormat for every row
        val grouped = filtered.groupBy { tx -> getSmartDateHeader(tx.date) }

        // C. Financial Summary (Corrected Logic)
        val netFlow = filtered.sumOf { tx ->
            when (tx.type) {
                TransactionType.INCOME -> tx.amount
                TransactionType.EXPENSE -> -tx.amount
                TransactionType.THIRD_PARTY_IN -> tx.amount   // Cash In
                TransactionType.THIRD_PARTY_OUT -> -tx.amount // Cash Out
                // Transfer & Exchange are Neutral (0) for Net Flow
                TransactionType.TRANSFER -> 0.0
                TransactionType.EXCHANGE -> 0.0
            }
        }

        LedgerData(grouped, filtered.size, netFlow)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerData(emptyMap(), 0, 0.0))

    // --- ACTIONS ---
    fun onSearch(query: String) { _searchQuery.value = query }
    fun onFilterType(type: TransactionType?) { _selectedType.value = type }
    fun onDateRange(range: DateRange) { _dateRange.value = range }

    // --- PERFORMANCE HELPERS ---

    // Calculates simple Long timestamps for the range
    private fun calculateDateBoundaries(range: DateRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        // Reset to end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        val start = when(range) {
            DateRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            DateRange.LAST_MONTH -> {
                // First day of current month minus 1 second is end of last month
                // But simpler: Go to 1st of this month, subtract 1 month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            DateRange.ALL_TIME -> 0L
        }

        // If Last Month, we also need to clamp the End Time
        val finalEnd = if (range == DateRange.LAST_MONTH) {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.add(Calendar.SECOND, -1) // Last second of previous month
            c.timeInMillis
        } else {
            end
        }

        return Pair(start, finalEnd)
    }

    private fun getSmartDateHeader(dateMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - dateMillis
        val oneDay = 24 * 60 * 60 * 1000L

        // Fast approximation for Today/Yesterday (ignores timezone edge cases for speed)
        // For strict correctness, use Calendar, but instantiate it sparingly.
        return if (diff < oneDay && isSameDay(now, dateMillis)) {
            "Today"
        } else if (diff < 2 * oneDay && isYesterday(now, dateMillis)) {
            "Yesterday"
        } else {
            // Only create formatter for older dates
            SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) &&
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
    }

    private fun isYesterday(now: Long, target: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = now }
        c1.add(Calendar.DAY_OF_YEAR, -1)
        val c2 = Calendar.getInstance().apply { timeInMillis = target }
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) &&
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
    }
}

enum class DateRange { THIS_MONTH, LAST_MONTH, ALL_TIME }
data class LedgerData(
    val groupedTransactions: Map<String, List<Transaction>>,
    val count: Int,
    val netFlow: Double
)