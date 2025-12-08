package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyNoteDao {
    @Query("SELECT * FROM currency_notes WHERE status = 'ACTIVE' ORDER BY denomination DESC")
    fun getAllActiveNotes(): Flow<List<CurrencyNote>>

    @Query("SELECT * FROM currency_notes WHERE accountId = :accountId AND status = 'ACTIVE' ORDER BY denomination DESC")
    fun getActiveNotesByAccount(accountId: Int): Flow<List<CurrencyNote>>

    @Query("SELECT * FROM currency_notes WHERE serialNumber = :serialNumber")
    suspend fun getNoteBySerialNumber(serialNumber: String): CurrencyNote?

    @Query("SELECT * FROM currency_notes WHERE id = :id")
    suspend fun getNoteById(id: Int): CurrencyNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CurrencyNote)

    @Update
    suspend fun updateNote(note: CurrencyNote)

    @Delete
    suspend fun deleteNote(note: CurrencyNote)

    @Query("UPDATE currency_notes SET status = 'SPENT', spentDate = :spentDate, spentTransactionId = :transactionId WHERE id = :noteId")
    suspend fun markNoteAsSpent(noteId: Int, spentDate: Long, transactionId: Int)

    @Query("SELECT SUM(denomination) FROM currency_notes WHERE accountId = :accountId AND status = 'ACTIVE'")
    suspend fun getTotalActiveAmount(accountId: Int): Int?
}