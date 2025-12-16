package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyNoteDao {
    @Query("SELECT * FROM currency_notes WHERE spentTransactionId IS NULL AND accountId = :accountId ORDER BY denomination DESC")
    fun getActiveNotesByAccount(accountId: Int): Flow<List<CurrencyNote>>

    @Query("SELECT * FROM currency_notes WHERE serialNumber = :serial")
    suspend fun getNoteBySerial(serial: String): CurrencyNote?

    @Insert
    suspend fun insertNote(note: CurrencyNote)

    @Query("UPDATE currency_notes SET spentTransactionId = :txId, spentDate = :date WHERE id = :noteId")
    suspend fun markAsSpent(noteId: Int, txId: Int, date: Long)

    @Query("SELECT * FROM currency_notes WHERE receivedTransactionId = :txId OR spentTransactionId = :txId")
    fun getNotesByTransaction(txId: Int): Flow<List<CurrencyNote>>

    // ... existing functions ...

    // Reverts a note to "Active" state (Un-spend)
    @Query("UPDATE currency_notes SET spentTransactionId = NULL, spentDate = NULL WHERE spentTransactionId = :txId")
    suspend fun unspendNotesForTransaction(txId: Int)

    // Deletes notes created by a specific transaction (Undo Income / Undo Change)
    @Query("DELETE FROM currency_notes WHERE receivedTransactionId = :txId")
    suspend fun deleteNotesFromTransaction(txId: Int)

    // Check if an income transaction's notes have already been spent (Prevent Delete)
    @Query("SELECT COUNT(*) FROM currency_notes WHERE receivedTransactionId = :txId AND spentTransactionId IS NOT NULL")
    suspend fun countSpentNotesFromTransaction(txId: Int): Int
}