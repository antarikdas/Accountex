package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyNoteDao {

    // --- NEW: REQUIRED FOR INVENTORY SCREEN ---
    @Query("SELECT * FROM currency_notes ORDER BY denomination DESC")
    fun getAllNotes(): Flow<List<CurrencyNote>>

    // --- MIGRATION LOGIC ---
    @Query("UPDATE currency_notes SET accountId = :newAccountId WHERE receivedTransactionId = :txId")
    suspend fun migrateNotes(txId: Int, newAccountId: Int)

    // --- STANDARD QUERIES ---
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId ORDER BY denomination DESC")
    fun getActiveNotesByAccount(accountId: Int): Flow<List<CurrencyNote>>

    @Query("SELECT * FROM currency_notes")
    suspend fun getAllNotesSync(): List<CurrencyNote>

    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 0 ORDER BY denomination DESC")
    fun getActivePersonalNotes(accountId: Int): Flow<List<CurrencyNote>>

    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 1 ORDER BY denomination DESC")
    fun getActiveThirdPartyNotes(accountId: Int): Flow<List<CurrencyNote>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId AND isThirdParty = 1")
    fun getTotalHeldAmount(accountId: Int): Flow<Double>

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

    // --- [NEW] CRITICAL FIX FOR VIEWMODEL ---
    // The ViewModel needs a one-shot List (Suspend), not a Flow, for the "Edit" check.
    @Query("SELECT * FROM currency_notes WHERE receivedTransactionId = :txId OR spentTransactionId = :txId")
    suspend fun getNotesByTransactionId(txId: Int): List<CurrencyNote>

    @Query("UPDATE currency_notes SET spentTransactionId = NULL, spentDate = NULL WHERE spentTransactionId = :txId")
    suspend fun unspendNotesForTransaction(txId: Int)

    @Query("DELETE FROM currency_notes WHERE receivedTransactionId = :txId")
    suspend fun deleteNotesFromTransaction(txId: Int)

    @Query("SELECT COUNT(*) FROM currency_notes WHERE receivedTransactionId = :txId AND spentTransactionId IS NOT NULL")
    suspend fun countSpentNotesFromTransaction(txId: Int): Int

    @Query("DELETE FROM currency_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)
}