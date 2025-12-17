package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyNoteDao {
    // 1. ALL ACTIVE NOTES (For General Inventory View - "Physical Cash")
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId ORDER BY denomination DESC")
    fun getActiveNotesByAccount(accountId: Int): Flow<List<CurrencyNote>>

    // 2. ONLY PERSONAL NOTES (For Standard Expenses - "Your Money")
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 0 ORDER BY denomination DESC")
    fun getActivePersonalNotes(accountId: Int): Flow<List<CurrencyNote>>

    // 3. ONLY THIRD-PARTY NOTES (For Handing Over - "Held Money")
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 1 ORDER BY denomination DESC")
    fun getActiveThirdPartyNotes(accountId: Int): Flow<List<CurrencyNote>>

    // 4. TOTAL HELD LIABILITY (To display "Held: ₹5000" badge on Dashboard)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 1")
    fun getTotalHeldAmount(accountId: Int): Flow<Double>

    // --- EXISTING QUERIES ---

    @Query("SELECT * FROM currency_notes WHERE serialNumber = :serial")
    suspend fun getNoteBySerial(serial: String): CurrencyNote?

    @Insert
    suspend fun insertNote(note: CurrencyNote)

    @Query("UPDATE currency_notes SET spentTransactionId = :txId, spentDate = :date WHERE id = :noteId")
    suspend fun markAsSpent(noteId: Int, txId: Int, date: Long)

    @Query("SELECT * FROM currency_notes WHERE receivedTransactionId = :txId OR spentTransactionId = :txId")
    fun getNotesByTransaction(txId: Int): Flow<List<CurrencyNote>>

    @Query("UPDATE currency_notes SET spentTransactionId = NULL, spentDate = NULL WHERE spentTransactionId = :txId")
    suspend fun unspendNotesForTransaction(txId: Int)

    @Query("DELETE FROM currency_notes WHERE receivedTransactionId = :txId")
    suspend fun deleteNotesFromTransaction(txId: Int)

    @Query("SELECT COUNT(*) FROM currency_notes WHERE receivedTransactionId = :txId AND spentTransactionId IS NOT NULL")
    suspend fun countSpentNotesFromTransaction(txId: Int): Int
}