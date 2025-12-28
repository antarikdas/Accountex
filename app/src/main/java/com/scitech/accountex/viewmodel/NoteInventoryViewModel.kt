package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.data.CurrencyNote
import com.scitech.accountex.data.CurrencyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NoteInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val noteDao = db.currencyNoteDao()
    private val accountDao = db.accountDao()

    // 1. Inputs
    private val _selectedAccountId = MutableStateFlow(0) // 0 = All Accounts
    val selectedAccountId = _selectedAccountId.asStateFlow()

    // 2. Data Sources
    val availableAccounts = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // We use the "All Notes" stream but filter it in memory for complex grouping
    private val _allNotes = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. The "Smart Inventory" Logic
    val inventoryState = combine(_allNotes, _selectedAccountId) { notes, accId ->
        // Step A: Filter logic (Active Notes Only)
        val filteredNotes = if (accId == 0) {
            notes.filter { it.spentTransactionId == null }
        } else {
            notes.filter { it.spentTransactionId == null && it.accountId == accId }
        }

        // Step B: Grouping logic (CRITICAL FIX)
        // We now group by BOTH Denomination AND Type.
        // This separates ₹10 Notes from ₹10 Coins.
        val grouped = filteredNotes.groupBy { Pair(it.denomination, it.type) }
            .map { (key, noteList) ->
                val (denom, type) = key
                InventoryItem(
                    denomination = denom,
                    type = type, // Explicit Type
                    count = noteList.size,
                    totalValue = noteList.sumOf { it.amount },
                    isCoin = type == CurrencyType.COIN, // Logic based on Fact, not Assumption
                    notes = noteList
                )
            }
            // Sort: High value first, then Notes before Coins
            .sortedWith(compareByDescending<InventoryItem> { it.denomination }
                .thenBy { it.isCoin }) // Notes (false) come before Coins (true)

        val totalValue = grouped.sumOf { it.totalValue }
        val totalCount = grouped.sumOf { it.count }

        InventoryData(grouped, totalValue, totalCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryData(emptyList(), 0.0, 0))

    // --- ACTIONS ---
    fun selectAccount(accountId: Int) {
        _selectedAccountId.value = accountId
    }
}

data class InventoryData(
    val items: List<InventoryItem>,
    val grandTotal: Double,
    val totalNotes: Int
)

data class InventoryItem(
    val denomination: Int,
    val type: CurrencyType, // Added for UI styling
    val count: Int,
    val totalValue: Double,
    val isCoin: Boolean,
    val notes: List<CurrencyNote> // Holds the specific serials for the dropdown
)