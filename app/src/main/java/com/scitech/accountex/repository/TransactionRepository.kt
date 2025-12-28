package com.scitech.accountex.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.scitech.accountex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.abs

class TransactionRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    private val transactionDao = db.transactionDao()
    private val accountDao = db.accountDao()
    private val noteDao = db.currencyNoteDao()

    // ============================================================================================
    // 🛡️ FORENSIC SELF-HEALING SYSTEM
    // ============================================================================================
    suspend fun verifyAndRepairLedger() {
        withContext(Dispatchers.IO) {
            val accounts = accountDao.getAllAccountsSync()
            accounts.forEach { account ->
                val cachedBalance = account.balance
                val trueBalance = accountDao.calculateTrueBalanceFromHistory(account.id)

                if (abs(cachedBalance - trueBalance) > 0.001) {
                    Log.e("AccountexAudit", "CRITICAL: Integrity Drift detected. Stored: $cachedBalance, Actual: $trueBalance. Repairing.")
                    val fixedAccount = account.copy(balance = trueBalance)
                    accountDao.updateAccount(fixedAccount)
                }
            }
        }
    }

    // ============================================================================================
    // 🚀 ANALYTICS
    // ============================================================================================
    fun getTotalsByType(start: Long, end: Long): Flow<List<TypeTotal>> = transactionDao.getTotalsByType(start, end)
    fun getTopExpenseCategories(start: Long, end: Long): Flow<List<CategoryTotal>> = transactionDao.getTopExpenseCategories(start, end)
    fun getAllTransactions() = transactionDao.getAllTransactions()

    // ============================================================================================
    // ✍️ ATOMIC WRITE OPERATIONS (The Engine)
    // ============================================================================================

    // 1. CREATE NEW
    suspend fun saveTransactionWithNotes(
        transaction: Transaction,
        spentNoteIds: Set<Int>,
        newNotes: List<CurrencyNote>
    ) {
        withContext(Dispatchers.IO) {
            val permanentImagePaths = saveImagesToInternalStorage(transaction.imageUris)
            val finalTransaction = transaction.copy(imageUris = permanentImagePaths)

            db.withTransaction {
                val txId = transactionDao.insertTransaction(finalTransaction).toInt()

                // Inventory: Spend selected notes
                spentNoteIds.forEach { noteId ->
                    noteDao.markAsSpent(noteId, txId, finalTransaction.date)
                }
                // Inventory: Create new notes (Income/Change)
                newNotes.forEach { note ->
                    noteDao.insertNote(note.copy(receivedTransactionId = txId, receivedDate = finalTransaction.date))
                }

                applyBalanceUpdate(finalTransaction, finalTransaction.amount)
            }
        }
    }

    // 2. UPDATE EXISTING (THE "ATOMIC SWAP")
    suspend fun updateTransactionWithInventory(
        oldTx: Transaction,
        newTx: Transaction,
        newNotes: List<CurrencyNote>,
        spentNoteIds: Set<Int>
    ) {
        withContext(Dispatchers.IO) {
            val processedImages = saveImagesToInternalStorage(newTx.imageUris)
            val finalNewTx = newTx.copy(imageUris = processedImages)

            db.withTransaction {
                // A. REVERSE OLD STATE (Undo)
                reverseBalanceUpdate(oldTx)

                // Inventory Reversal:
                // 1. Release the notes we spent (Give them back to the wallet)
                noteDao.unspendNotesForTransaction(oldTx.id)
                // 2. Destroy the notes we created (Remove income/change)
                noteDao.deleteNotesFromTransaction(oldTx.id)

                // B. APPLY NEW STATE (Redo)
                applyBalanceUpdate(finalNewTx, finalNewTx.amount)

                // Inventory Application:
                // 1. Spend the NEW selected notes
                spentNoteIds.forEach { noteId ->
                    noteDao.markAsSpent(noteId, finalNewTx.id, finalNewTx.date)
                }
                // 2. Create the NEW notes
                newNotes.forEach { note ->
                    noteDao.insertNote(note.copy(receivedTransactionId = finalNewTx.id, receivedDate = finalNewTx.date))
                }

                // C. UPDATE RECORD
                transactionDao.updateTransaction(finalNewTx)
            }
        }
    }

    // 3. DELETE
    suspend fun deleteTransaction(txId: Int) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val tx = transactionDao.getTransactionById(txId) ?: return@withTransaction
                reverseBalanceUpdate(tx)
                noteDao.unspendNotesForTransaction(txId)
                noteDao.deleteNotesFromTransaction(txId)
                transactionDao.deleteTransaction(tx)
            }
        }
    }

    // ============================================================================================
    // 🔧 INTERNAL HELPERS
    // ============================================================================================
    private suspend fun applyBalanceUpdate(tx: Transaction, amount: Double) {
        when (tx.type) {
            TransactionType.INCOME -> accountDao.updateBalance(tx.accountId, amount)
            TransactionType.EXPENSE -> accountDao.updateBalance(tx.accountId, -amount)
            TransactionType.TRANSFER -> {
                accountDao.updateBalance(tx.accountId, -amount)
                tx.toAccountId?.let { accountDao.updateBalance(it, amount) }
            }
            TransactionType.THIRD_PARTY_IN -> accountDao.updateBalance(tx.accountId, amount)
            TransactionType.THIRD_PARTY_OUT -> accountDao.updateBalance(tx.accountId, -amount)
            else -> {}
        }
    }

    private suspend fun reverseBalanceUpdate(tx: Transaction) {
        applyBalanceUpdate(tx, -tx.amount)
    }

    private fun saveImagesToInternalStorage(uris: List<String>): List<String> {
        val savedPaths = mutableListOf<String>()
        for (uriString in uris) {
            try {
                if (uriString.contains("com.scitech.accountex")) {
                    savedPaths.add(uriString)
                    continue
                }
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val folder = File(context.filesDir, "Accountex_Images")
                if (!folder.exists()) folder.mkdirs()

                val newFile = File(folder, "IMG_${UUID.randomUUID()}.jpg")
                inputStream?.use { input ->
                    newFile.outputStream().use { output -> input.copyTo(output) }
                }
                savedPaths.add(Uri.fromFile(newFile).toString())
            } catch (e: Exception) { e.printStackTrace() }
        }
        return savedPaths
    }
}