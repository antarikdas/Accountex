package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NoteInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val noteDao = db.currencyNoteDao()

    // 1. Filter State (0 = All, or specific Account ID)
    private val _selectedAccountId = MutableStateFlow(0)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    // 2. Raw Data
    private val _allNotes = noteDao.getAllNotes() // You might need to ensure this Flow exists in DAO
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Processed Inventory (Grouped by Denomination)
    val inventoryState = combine(_allNotes, _selectedAccountId) { notes, accId ->
        // Filter: Active notes (not spent) AND match account (if selected)
        val activeNotes = notes.filter {
            it.spentTransactionId == null && (accId == 0 || it.accountId == accId)
        }

        // Group by Denomination (e.g., all 500s together)
        val grouped = activeNotes.groupBy { it.denomination }
            .map { (denom, noteList) ->
                InventoryItem(
                    denomination = denom,
                    count = noteList.size,
                    totalValue = noteList.sumOf { it.amount },
                    isCoin = denom <= 20 // Auto-detect coin
                )
            }
            .sortedByDescending { it.denomination } // Show big notes first

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
    val count: Int,
    val totalValue: Double,
    val isCoin: Boolean
)