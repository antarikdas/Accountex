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
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DataManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val noteDao = database.currencyNoteDao()
    private val context = application

    private val _uiState = MutableStateFlow(DataManagementState())
    val uiState: StateFlow<DataManagementState> = _uiState.asStateFlow()

    // --- EXPORT (ZIP: JSON + IMAGES) ---
    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = DataManagementState(isLoading = true, statusMessage = "Packing Images & Data...")
            try {
                createZipBackup(uri)
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Backup Successful!", isSuccess = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Export Failed: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun createZipBackup(uri: Uri) = withContext(Dispatchers.IO) {
        // 1. Fetch Raw Data
        val accounts = accountDao.getAllAccountsSync()
        val rawTransactions = transactionDao.getAllTransactionsSync()
        val notes = noteDao.getAllNotesSync()

        // 2. Prepare JSON Object
        val root = JSONObject()

        // A. Accounts
        val accArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("balance", acc.balance)
            obj.put("type", acc.type.name)
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        // B. Transactions (Sanitize Paths)
        val txArray = JSONArray()
        val imagesToZip = mutableListOf<File>()
        val imagesDir = File(context.filesDir, "Accountex_Images")

        rawTransactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type.name)
            obj.put("amount", tx.amount)
            obj.put("date", tx.date)
            obj.put("category", tx.category)
            obj.put("description", tx.description)
            obj.put("accountId", tx.accountId)
            if (tx.toAccountId != null) obj.put("toAccountId", tx.toAccountId)
            if (tx.thirdPartyName != null) obj.put("thirdPartyName", tx.thirdPartyName)

            // Handle Images: Save only the FILENAME, not the full path
            val imgArray = JSONArray()
            tx.imageUris.forEach { uriStr ->
                val file = File(uriStr)
                val fileName = file.name
                imgArray.put(fileName) // Store "IMG_123.jpg"

                // Add to list if it exists in our app storage
                val actualFile = File(imagesDir, fileName)
                if (actualFile.exists()) {
                    imagesToZip.add(actualFile)
                }
            }
            obj.put("imageFilenames", imgArray) // Changed key to indicate it's just names
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // C. Notes
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

        // 3. Write ZIP File
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // Entry 1: The JSON Data
                zipOut.putNextEntry(ZipEntry("backup_data.json"))
                zipOut.write(root.toString(4).toByteArray())
                zipOut.closeEntry()

                // Entry 2...N: The Images
                // Use a set to avoid duplicates if one image is used in multiple places
                imagesToZip.distinctBy { it.name }.forEach { imgFile ->
                    try {
                        zipOut.putNextEntry(ZipEntry("images/${imgFile.name}"))
                        FileInputStream(imgFile).use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    } catch (e: Exception) {
                        e.printStackTrace() // Skip failed images
                    }
                }
            }
        }
    }

    // --- IMPORT (ZIP) ---
    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = DataManagementState(isLoading = true, statusMessage = "Unpacking & Restoring...")
            try {
                restoreZipBackup(uri)
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Restore Complete!", isSuccess = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DataManagementState(isLoading = false, statusMessage = "Import Failed: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun restoreZipBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "Accountex_Images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        var jsonString: String? = null

        // 1. Unzip Logic
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name

                    if (entryName == "backup_data.json") {
                        // Read JSON
                        val bytes = zipIn.readBytes()
                        jsonString = String(bytes)
                    } else if (entryName.startsWith("images/")) {
                        // Extract Image
                        val fileName = File(entryName).name
                        val outFile = File(imagesDir, fileName)
                        FileOutputStream(outFile).use { output ->
                            zipIn.copyTo(output)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

        if (jsonString == null) throw Exception("Invalid Backup: No 'backup_data.json' found inside ZIP.")

        // 2. Parse JSON & Insert to DB
        val root = JSONObject(jsonString!!)
        database.clearAllTables()

        // Accounts
        val accArray = root.getJSONArray("accounts")
        for (i in 0 until accArray.length()) {
            val obj = accArray.getJSONObject(i)
            val acc = Account(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                balance = obj.getDouble("balance"),
                type = AccountType.valueOf(obj.getString("type"))
            )
            accountDao.insertAccount(acc)
        }

        // Transactions
        val txArray = root.getJSONArray("transactions")
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)

            // Reconstruct Absolute Paths
            // Check both "imageFilenames" (New Format) and "imageUris" (Legacy Format compatibility)
            val imgList = mutableListOf<String>()
            val jsonImages = if (obj.has("imageFilenames")) obj.getJSONArray("imageFilenames") else obj.optJSONArray("imageUris")

            if (jsonImages != null) {
                for (j in 0 until jsonImages.length()) {
                    val rawVal = jsonImages.getString(j)
                    // If it's just a filename, prepend the local path. If it looks like a path, keep it (legacy).
                    val fileName = File(rawVal).name
                    val localUri = Uri.fromFile(File(imagesDir, fileName)).toString()
                    imgList.add(localUri)
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
                thirdPartyName = if (obj.has("thirdPartyName")) obj.getString("thirdPartyName") else null
            )
            transactionDao.insertTransaction(tx)
        }

        // Notes
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
                    thirdPartyName = if (obj.has("thirdPartyName")) obj.getString("thirdPartyName") else null
                )
                noteDao.insertNote(note)
            }
        }
    }

    fun resetState() { _uiState.value = DataManagementState() }
}

data class DataManagementState(
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val isSuccess: Boolean = false,
    val isError: Boolean = false
)