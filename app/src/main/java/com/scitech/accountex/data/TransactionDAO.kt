package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTxCountForAccount(accountId: Int): Int

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // NEW: Synchronous fetch for Backup
    @Query("SELECT * FROM transactions")
    fun getAllTransactionsSync(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT DISTINCT category FROM transactions ORDER BY category ASC")
    fun getUniqueCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT description FROM transactions WHERE description != '' ORDER BY date DESC LIMIT 20")
    fun getRecentsDescriptions(): Flow<List<String>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Double?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}