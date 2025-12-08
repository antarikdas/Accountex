package com.scitech.accountex.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
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
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Transactions")

                val headerStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont()
                    font.bold = true
                    setFont(font)
                    fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                }

                val headerRow = sheet.createRow(0)
                val headers = listOf("Date", "Type", "Category", "Amount", "Account", "Description")
                headers.forEachIndexed { index, header ->
                    headerRow.createCell(index).apply {
                        setCellValue(header)
                        cellStyle = headerStyle
                    }
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                transactions.value.forEachIndexed { index, transaction ->
                    val row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue(sdf.format(Date(transaction.date)))
                    row.createCell(1).setCellValue(transaction.type.name)
                    row.createCell(2).setCellValue(transaction.category)
                    row.createCell(3).setCellValue(transaction.amount)
                    val account = accounts.value.find { it.id == transaction.accountId }
                    row.createCell(4).setCellValue(account?.name ?: "")
                    row.createCell(5).setCellValue(transaction.description)
                }

                for (i in 0 until headers.size) {
                    sheet.autoSizeColumn(i)
                }

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "Accountex_Export_${System.currentTimeMillis()}.xlsx"
                val file = File(downloadsDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                shareFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun shareFile(file: File) {
        val context = getApplication<Application>()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Share Excel File").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

data class DailySummary(
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
}