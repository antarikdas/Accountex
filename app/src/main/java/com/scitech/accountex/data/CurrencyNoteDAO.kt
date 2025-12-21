package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyNoteDao {

    // --- NEW: REQUIRED FOR INVENTORY SCREEN ---
    // The ViewModel uses this to get a live stream of everything,
    // then filters it (Active vs Spent) in real-time.
    @Query("SELECT * FROM currency_notes ORDER BY denomination DESC")
    fun getAllNotes(): Flow<List<CurrencyNote>>


    // --- YOUR EXISTING SMART QUERIES (KEPT) ---

    // 1. ALL ACTIVE NOTES (Physical Cash)
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId ORDER BY denomination DESC")
    fun getActiveNotesByAccount(accountId: Int): Flow<List<CurrencyNote>>

    // 2. Synchronous fetch for Backup
    @Query("SELECT * FROM currency_notes")
    suspend fun getAllNotesSync(): List<CurrencyNote>

    // 3. ONLY PERSONAL NOTES (Your Money)
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 0 ORDER BY denomination DESC")
    fun getActivePersonalNotes(accountId: Int): Flow<List<CurrencyNote>>

    // 4. ONLY THIRD-PARTY NOTES (Held Money)
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 1 ORDER BY denomination DESC")
    fun getActiveThirdPartyNotes(accountId: Int): Flow<List<CurrencyNote>>

    // 5. TOTAL HELD LIABILITY (Badge)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 1")
    fun getTotalHeldAmount(accountId: Int): Flow<Double>

    // 6. GLOBAL HELD AMOUNT
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM currency_notes WHERE isThirdParty = 1 AND spentTransactionId IS NULL")
    fun getGlobalHeldAmount(): Flow<Double>


    // --- TRANSACTION & LOGIC QUERIES ---

    @Query("SELECT * FROM currency_notes WHERE serialNumber = :serial")
    suspend fun getNoteBySerial(serial: String): CurrencyNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CurrencyNote)

    @Update
    suspend fun updateNote(note: CurrencyNote)

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

    // Safety delete (if needed)
    @Query("DELETE FROM currency_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)
}