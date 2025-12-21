package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class DataHubViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()

    // 1. Inputs
    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow(TypeFilter.ALL)
    private val _dateFilter = MutableStateFlow(DateFilter.ALL_TIME)

    // 2. Raw Data
    private val _allTransactions = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Output (Grouped & Filtered)
    val uiState = combine(
        _allTransactions,
        _searchQuery,
        _typeFilter,
        _dateFilter
    ) { transactions, query, type, dateRange ->
        filterAndGroup(transactions, query, type, dateRange)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- ACTIONS ---
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onTypeFilterChanged(filter: TypeFilter) { _typeFilter.value = filter }
    fun onDateFilterChanged(filter: DateFilter) { _dateFilter.value = filter }

    // --- LOGIC ---
    private fun filterAndGroup(
        list: List<Transaction>,
        query: String,
        typeFilter: TypeFilter,
        dateFilter: DateFilter
    ): Map<String, List<Transaction>> {
        val filtered = list.filter { tx ->
            // 1. Search Filter (Category, Desc, Party, Amount)
            val matchesSearch = if (query.isBlank()) true else {
                tx.category.contains(query, ignoreCase = true) ||
                        tx.description.contains(query, ignoreCase = true) ||
                        (tx.thirdPartyName ?: "").contains(query, ignoreCase = true) ||
                        tx.amount.toString().contains(query)
            }

            // 2. Type Filter
            val matchesType = when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.INCOME -> tx.type == TransactionType.INCOME
                TypeFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                TypeFilter.THIRD_PARTY -> tx.type == TransactionType.THIRD_PARTY_IN || tx.type == TransactionType.THIRD_PARTY_OUT
                TypeFilter.EXCHANGE -> tx.type == TransactionType.EXCHANGE
            }

            // 3. Date Filter
            val matchesDate = when (dateFilter) {
                DateFilter.ALL_TIME -> true
                DateFilter.THIS_MONTH -> isSameMonth(tx.date, System.currentTimeMillis())
                DateFilter.LAST_MONTH -> isLastMonth(tx.date)
            }

            matchesSearch && matchesType && matchesDate
        }

        // 4. Group by Date Header
        val grouped = filtered.groupBy { tx ->
            getHeaderDate(tx.date)
        }
        return grouped
    }

    // --- HELPERS ---
    private fun getHeaderDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val today = sdf.format(Date())
        val yesterday = sdf.format(Date(System.currentTimeMillis() - 86400000))
        val txDate = sdf.format(Date(timestamp))

        return when (txDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> txDate
        }
    }

    private fun isSameMonth(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun isLastMonth(t1: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val now = Calendar.getInstance()
        now.add(Calendar.MONTH, -1)
        return cal1.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
}

// Enums for Filter Logic
enum class TypeFilter { ALL, INCOME, EXPENSE, THIRD_PARTY, EXCHANGE }
enum class DateFilter { ALL_TIME, THIS_MONTH, LAST_MONTH }