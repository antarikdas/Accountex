package com.scitech.accountex.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class DataManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val noteDao = database.currencyNoteDao()

    private val _uiState = MutableStateFlow(DataManagementState())
    val uiState: StateFlow<DataManagementState> = _uiState.asStateFlow()

    // --- EXPORT LOGIC ---
    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = DataManagementState(isLoading = true, statusMessage = "Preparing Backup...")
            try {
                val jsonString = generateBackupJson()
                writeJsonToFile(uri, jsonString)
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Backup Successful!", isSuccess = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Export Failed: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. Export Accounts
        val accounts = accountDao.getAllAccountsSync()
        val accArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("balance", acc.balance)
            obj.put("type", acc.type.name) // Save Enum name as String
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        // 2. Export Transactions
        val transactions = transactionDao.getAllTransactionsSync()
        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type.name)
            obj.put("amount", tx.amount)
            obj.put("date", tx.date)
            obj.put("category", tx.category)
            obj.put("description", tx.description)
            obj.put("accountId", tx.accountId)
            if (tx.toAccountId != null) obj.put("toAccountId", tx.toAccountId)

            val imgArray = JSONArray()
            tx.imageUris.forEach { imgArray.put(it) }
            obj.put("imageUris", imgArray)

            if (tx.thirdPartyName != null) obj.put("thirdPartyName", tx.thirdPartyName)

            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // 3. Export Currency Notes
        val notes = noteDao.getAllNotesSync()
        val noteArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("serialNumber", note.serialNumber)
            obj.put("amount", note.amount)
            obj.put("denomination", note.denomination)
            obj.put("accountId", note.accountId)
            obj.put("receivedTransactionId", note.receivedTransactionId)
            if (note.spentTransactionId != null) obj.put("spentTransactionId", note.spentTransactionId)
            obj.put("receivedDate", note.receivedDate)
            if (note.spentDate != null) obj.put("spentDate", note.spentDate)

            obj.put("isThirdParty", note.isThirdParty)
            if (note.thirdPartyName != null) obj.put("thirdPartyName", note.thirdPartyName)

            noteArray.put(obj)
        }
        root.put("currency_notes", noteArray)

        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        return@withContext root.toString(4)
    }

    private suspend fun writeJsonToFile(uri: Uri, json: String) = withContext(Dispatchers.IO) {
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(json.toByteArray())
        }
    }

    // --- IMPORT LOGIC ---
    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = DataManagementState(isLoading = true, statusMessage = "Reading Backup...")
            try {
                val jsonString = readJsonFromFile(uri)
                restoreBackup(jsonString)
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Restore Complete!", isSuccess = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Import Failed: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun readJsonFromFile(uri: Uri): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return@withContext stringBuilder.toString()
    }

    private suspend fun restoreBackup(jsonString: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)

        database.clearAllTables()

        // 2. Restore Accounts
        val accArray = root.getJSONArray("accounts")
        for (i in 0 until accArray.length()) {
            val obj = accArray.getJSONObject(i)
            val acc = Account(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                balance = obj.getDouble("balance"),
                // FIXED: Convert String back to Enum
                type = AccountType.valueOf(obj.getString("type"))
            )
            accountDao.insertAccount(acc)
        }

        // 3. Restore Transactions
        val txArray = root.getJSONArray("transactions")
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)

            val imgJsonArray = obj.optJSONArray("imageUris")
            val imgList = mutableListOf<String>()
            if (imgJsonArray != null) {
                for (j in 0 until imgJsonArray.length()) {
                    imgList.add(imgJsonArray.getString(j))
                }
            }

            val tx = Transaction(
                id = obj.getInt("id"),
                type = TransactionType.valueOf(obj.getString("type")),
                amount = obj.getDouble("amount"),
                date = obj.getLong("date"),
                category = obj.getString("category"),
                description = if (obj.has("description")) obj.getString("description") else "",
                accountId = obj.getInt("accountId"),
                toAccountId = if (obj.has("toAccountId")) obj.getInt("toAccountId") else null,
                imageUris = imgList,
                // FIXED: Safe nullable retrieval
                thirdPartyName = if (obj.has("thirdPartyName")) obj.getString("thirdPartyName") else null
            )
            transactionDao.insertTransaction(tx)
        }

        // 4. Restore Notes
        val noteArray = root.optJSONArray("currency_notes")
        if (noteArray != null) {
            for (i in 0 until noteArray.length()) {
                val obj = noteArray.getJSONObject(i)
                val note = CurrencyNote(
                    id = obj.getInt("id"),
                    serialNumber = obj.getString("serialNumber"),
                    amount = obj.getDouble("amount"),
                    denomination = obj.getInt("denomination"),
                    accountId = obj.getInt("accountId"),
                    receivedTransactionId = obj.getInt("receivedTransactionId"),
                    spentTransactionId = if (obj.has("spentTransactionId")) obj.getInt("spentTransactionId") else null,
                    receivedDate = obj.getLong("receivedDate"),
                    spentDate = if (obj.has("spentDate")) obj.getLong("spentDate") else null,
                    isThirdParty = obj.optBoolean("isThirdParty", false),
                    // FIXED: Safe nullable retrieval
                    thirdPartyName = if (obj.has("thirdPartyName")) obj.getString("thirdPartyName") else null
                )
                noteDao.insertNote(note)
            }
        }
    }

    fun resetState() {
        _uiState.value = DataManagementState()
    }
}

data class DataManagementState(
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val isSuccess: Boolean = false,
    val isError: Boolean = false
)