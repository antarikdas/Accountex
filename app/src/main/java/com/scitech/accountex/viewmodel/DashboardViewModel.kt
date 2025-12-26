package com.scitech.accountex.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import com.scitech.accountex.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)

    // --- 1. NEW: REPOSITORY FOR LOGIC ---
    private val repository = TransactionRepository(database, application)

    // --- 2. DAOS FOR READ STREAMS (High Speed UI) ---
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val currencyNoteDao = database.currencyNoteDao()

    // --- UI STATE ---
    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // We keep this direct DAO link because it's optimized for UI observing
    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heldAmount: StateFlow<Double> = currencyNoteDao.getGlobalHeldAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaySummary: StateFlow<DailySummary> = transactions.map { txList ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val todayStart = calendar.timeInMillis
        val todayTransactions = txList.filter { it.date >= todayStart }

        val income = todayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        DailySummary(income, expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummary(0.0, 0.0))

    init {
        viewModelScope.launch {
            // A. Default Accounts (First Run Only)
            val existingAccounts = accountDao.getAllAccountsSync()
            if (existingAccounts.isEmpty()) {
                accountDao.insertAccount(Account(name = "Bank Account", type = AccountType.BANK))
                accountDao.insertAccount(Account(name = "Daily Cash", type = AccountType.CASH_DAILY))
                accountDao.insertAccount(Account(name = "Cash Reserve", type = AccountType.CASH_RESERVE))
            }

            // B. 🛡️ ACTIVATE THE SELF-HEALING AUDIT 🛡️
            // This runs on Dispatchers.IO (Background).
            // It will verify millions of rows without lagging the UI.
            repository.verifyAndRepairLedger()
        }
    }

    // --- EXPORT LOGIC ---
    fun exportToExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val fileName = "Accountex_Export_${System.currentTimeMillis()}.xlsx"
                val file = File(context.cacheDir, fileName)

                // Fetch raw list via Repository (safe)
                val allTx = transactionDao.getAllTransactionsSync()

                FileOutputStream(file).use { outputStream ->
                    val workbook = Workbook(outputStream, "Accountex", "1.0")
                    val worksheet = workbook.newWorksheet("Transactions")

                    val headers = listOf("Date", "Type", "Category", "Amount", "Account", "Description")
                    headers.forEachIndexed { index, header ->
                        worksheet.value(0, index, header)
                        worksheet.width(index, 20.0)
                    }
                    worksheet.range(0, 0, 0, headers.size - 1).style().bold().set()

                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                    allTx.forEachIndexed { index, transaction ->
                        val row = index + 1
                        val account = accounts.value.find { it.id == transaction.accountId }

                        worksheet.value(row, 0, sdf.format(Date(transaction.date)))
                        worksheet.value(row, 1, transaction.type.name)
                        worksheet.value(row, 2, transaction.category)
                        worksheet.value(row, 3, transaction.amount)
                        worksheet.value(row, 4, account?.name ?: "N/A")
                        worksheet.value(row, 5, transaction.description)
                    }
                    workbook.finish()
                }

                withContext(Dispatchers.Main) {
                    shareFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun shareFile(file: File) {
        val context = getApplication<Application>()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Excel File")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

// Ensure this DTO is available if not already in another file
data class DailySummary(val income: Double, val expense: Double)