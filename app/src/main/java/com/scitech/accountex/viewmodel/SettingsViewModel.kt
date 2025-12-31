package com.scitech.accountex.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scitech.accountex.data.AppDatabase
import com.scitech.accountex.ui.theme.CurrentTheme
import com.scitech.accountex.ui.theme.ThemeType
import com.scitech.accountex.utils.SecurityPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val accountDao = db.accountDao()
    private val prefs = SecurityPreferences(application)
    private val context = application.applicationContext

    // 🧠 SHARED PREFERENCES (Built-in, No Dependencies needed)
    private val themePrefs = application.getSharedPreferences("accountex_theme_prefs", Context.MODE_PRIVATE)
    private val THEME_KEY = "app_theme"

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        // 🚀 LOAD THEME ON STARTUP
        val savedThemeName = themePrefs.getString(THEME_KEY, ThemeType.Nebula.name) ?: ThemeType.Nebula.name
        CurrentTheme = try {
            ThemeType.valueOf(savedThemeName)
        } catch (e: Exception) {
            ThemeType.Nebula
        }
    }

    // 🧠 SET THEME
    fun setTheme(theme: ThemeType) {
        CurrentTheme = theme
        themePrefs.edit().putString(THEME_KEY, theme.name).apply()
    }

    // --- SECURITY ---
    fun isBiometricEnabled(): Boolean = prefs.isBiometricEnabled()

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.setBiometricEnabled(enabled)
    }

    fun clearPin() {
        val sp = context.getSharedPreferences("accountex_secure_prefs", Context.MODE_PRIVATE)
        sp.edit().remove("user_pin").apply()
        prefs.setBiometricEnabled(false)
    }

    // --- DATA: FACTORY RESET ---
    fun factoryReset() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            // 1. Clear Database
            db.clearAllTables()

            // 2. Wipe Images
            val imagesDir = File(context.filesDir, "Accountex_Images")
            if (imagesDir.exists()) imagesDir.deleteRecursively()

            // 3. Wipe Security
            clearPin()

            // 4. Wipe Theme Preferences
            themePrefs.edit().clear().apply()

            _isLoading.value = false
        }
    }

    // --- DATA: EXPORT TO EXCEL ---
    fun exportToExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val fileName = "Accountex_Report_${System.currentTimeMillis()}.xlsx"
                val file = File(context.cacheDir, fileName)

                val allTx = transactionDao.getAllTransactionsSync()
                val accounts = accountDao.getAllAccountsSync()

                FileOutputStream(file).use { outputStream ->
                    val workbook = Workbook(outputStream, "Accountex", "1.0")
                    val worksheet = workbook.newWorksheet("Transactions")

                    // Headers
                    val headers = listOf("Date", "Type", "Category", "Amount", "Account", "Description", "Third Party")
                    headers.forEachIndexed { i, h ->
                        worksheet.value(0, i, h)
                        worksheet.width(i, 20.0)
                    }
                    worksheet.range(0, 0, 0, headers.size - 1).style().bold().set()

                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                    allTx.forEachIndexed { index, tx ->
                        val row = index + 1
                        val accName = accounts.find { it.id == tx.accountId }?.name ?: "Unknown"

                        worksheet.value(row, 0, sdf.format(Date(tx.date)))
                        worksheet.value(row, 1, tx.type.name)
                        worksheet.value(row, 2, tx.category)
                        worksheet.value(row, 3, tx.amount)
                        worksheet.value(row, 4, accName)
                        worksheet.value(row, 5, tx.description)
                        worksheet.value(row, 6, tx.thirdPartyName ?: "-")
                    }
                    workbook.finish()
                }

                withContext(Dispatchers.Main) {
                    shareFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Excel Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}