package com.scitech.accountex.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.text.bold
import androidx.glance.layout.width
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.slice.builders.range
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()

    val accounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySummary: StateFlow<DailySummary> = transactions.map { txList ->
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val todayStart = calendar.timeInMillis

        val todayTransactions = txList.filter { it.date >= todayStart }

        val income = todayTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expense = todayTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        DailySummary(income, expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummary(0.0, 0.0))

    init {
        viewModelScope.launch {
            val existingAccounts = accountDao.getAllAccounts().first()
            if (existingAccounts.isEmpty()) {
                accountDao.insertAccount(Account(name = "Bank Account", type = AccountType.BANK))
                accountDao.insertAccount(Account(name = "Daily Cash", type = AccountType.CASH_DAILY))
                accountDao.insertAccount(Account(name = "Cash Reserve", type = AccountType.CASH_RESERVE))
            }
        }
    }

    fun getTotalBalance(): Double {
        return accounts.value.sumOf { it.balance }
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun exportToExcel() {
        viewModelScope.launch {
            try {
                // Define where the file will be saved
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "Accountex_Export_${System.currentTimeMillis()}.xlsx"
                val file = File(downloadsDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    // Use FastExcel to create the workbook and worksheet
                    val workbook = Workbook(outputStream, "Accountex", "1.0")
                    val worksheet: Worksheet = workbook.newWorksheet("Transactions")

                    // --- Create and Style the Header Row ---
                    val headers = listOf("Date", "Type", "Category", "Amount", "Account", "Description")
                    worksheet.range(0, 0, 0, headers.size - 1).style().bold().set()

                    headers.forEachIndexed { index, header ->
                        worksheet.value(0, index, header)
                        // Set column widths for better readability
                        worksheet.width(index, 20)
                    }

                    // --- Populate Data Rows ---
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    transactions.value.forEachIndexed { index, transaction ->
                        val row = index + 1 // Data starts on the second row
                        val account = accounts.value.find { it.id == transaction.accountId }

                        worksheet.value(row, 0, sdf.format(Date(transaction.date)))
                        worksheet.value(row, 1, transaction.type.name)
                        worksheet.value(row, 2, transaction.category)
                        worksheet.value(row, 3, transaction.amount)
                        worksheet.value(row, 4, account?.name ?: "N/A")
                        worksheet.value(row, 5, transaction.description)
                    }

                    // Finish writing the workbook
                    workbook.finish()
                }

                // Share the created file
                shareFile(file)
            } catch (e: Exception) {
                // Log the exception to understand what went wrong
                e.printStackTrace()
            }
        }
    }

    private fun shareFile(file: File) {
        val context = getApplication<Application>()
        // Get a content URI using FileProvider for security
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // Make sure this matches your provider_paths.xml authority
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Use a chooser to let the user decide how to share
        val chooser = Intent.createChooser(intent, "Share Excel File").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

data class DailySummary(
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
}
