package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- HELPER DTOs FOR ANALYTICS ---
data class CategoryTotal(
    val category: String,
    val total: Double
)

data class TypeTotal(
    val type: TransactionType,
    val total: Double
)

@Dao
interface TransactionDao {

    // --- STANDARD CRUD ---
    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTxCountForAccount(accountId: Int): Int

    // Used for UI (Live Updates)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // [FIXED] ADDED BACK: Needed for Backup System (DataManagementViewModel)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // --- SUGGESTIONS ---
    @Query("SELECT DISTINCT category FROM transactions ORDER BY category ASC")
    fun getUniqueCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT description FROM transactions WHERE description != '' ORDER BY date DESC LIMIT 20")
    fun getRecentsDescriptions(): Flow<List<String>>

    // --- HIGH-SPEED ANALYTICS ---
    @Query("SELECT type, SUM(amount) as total FROM transactions WHERE date BETWEEN :startDate AND :endDate GROUP BY type")
    fun getTotalsByType(startDate: Long, endDate: Long): Flow<List<TypeTotal>>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC LIMIT 5")
    fun getTopExpenseCategories(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>
}