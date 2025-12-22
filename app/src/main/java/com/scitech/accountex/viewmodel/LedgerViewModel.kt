package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()

    // 1. Filters
    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<TransactionType?>(null) // Null = All
    private val _dateRange = MutableStateFlow(DateRange.ALL_TIME)

    // 2. Data Source
    private val _allTransactions = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Processed List (Filtered & Grouped)
    val ledgerState = combine(_allTransactions, _searchQuery, _selectedType, _dateRange) { list, query, type, range ->
        // A. Filter
        val filtered = list.filter { tx ->
            val matchesQuery = tx.description.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)

            val matchesType = type == null || tx.type == type
            val matchesDate = isInDateRange(tx.date, range)

            matchesQuery && matchesType && matchesDate
        }

        // B. Group by Date (LinkedHashMap maintains insertion order)
        val grouped = filtered.groupBy { tx ->
            formatDateHeader(tx.date)
        }

        LedgerData(grouped, filtered.size, filtered.sumOf { if(it.type == TransactionType.INCOME) it.amount else -it.amount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerData(emptyMap(), 0, 0.0))

    // --- ACTIONS ---
    fun onSearch(query: String) { _searchQuery.value = query }
    fun onFilterType(type: TransactionType?) { _selectedType.value = type }
    fun onDateRange(range: DateRange) { _dateRange.value = range }

    // --- HELPERS ---
    private fun formatDateHeader(dateMillis: Long): String {
        val date = Date(dateMillis)
        val now = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { time = date }

        return when {
            isSameDay(now, txDate) -> "Today"
            isYesterday(now, txDate) -> "Yesterday"
            else -> SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(date)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, txDate: Calendar): Boolean {
        val yesterday = now.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, txDate)
    }

    private fun isInDateRange(date: Long, range: DateRange): Boolean {
        val cal = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = date }
        return when(range) {
            DateRange.THIS_MONTH -> cal.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            DateRange.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            }
            DateRange.ALL_TIME -> true
        }
    }
}

enum class DateRange { THIS_MONTH, LAST_MONTH, ALL_TIME }
data class LedgerData(val groupedTransactions: Map<String, List<Transaction>>, val count: Int, val netFlow: Double)