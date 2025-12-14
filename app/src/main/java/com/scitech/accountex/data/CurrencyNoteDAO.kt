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
}