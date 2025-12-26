package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    // --- STANDARD OPERATIONS ---
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSync(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    // [FIXED] ADDED BACK: Needed for ManageAccountsViewModel
    @Delete
    suspend fun deleteAccount(account: Account)

    // Fast Write (Optimistic Update)
    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :id")
    suspend fun updateBalance(id: Int, amount: Double)

    // --- 🛡️ THE TRUTH ENGINE (High-Scale Forensic Audit) 🛡️ ---
    @Query("""
        SELECT 
            (
                SELECT COALESCE(SUM(amount), 0.0) 
                FROM transactions 
                WHERE accountId = :accId AND type IN ('INCOME', 'THIRD_PARTY_IN')
            ) 
            - 
            (
                SELECT COALESCE(SUM(amount), 0.0) 
                FROM transactions 
                WHERE accountId = :accId AND type IN ('EXPENSE', 'THIRD_PARTY_OUT', 'TRANSFER')
            )
            +
            (
                SELECT COALESCE(SUM(amount), 0.0) 
                FROM transactions 
                WHERE toAccountId = :accId AND type = 'TRANSFER'
            )
    """)
    suspend fun calculateTrueBalanceFromHistory(accId: Int): Double
}