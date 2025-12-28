package com.scitech.accountex

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.scitech.accountex.ui.navigation.NavGraph
import com.scitech.accountex.ui.theme.AccountexTheme
import com.scitech.accountex.viewmodel.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccountexTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AccountexApp()
                }
            }
        }
    }
}

@Composable
fun AccountexApp() {
    val navController = rememberNavController()

    // Initialize ViewModels
    val dashboardViewModel: DashboardViewModel = viewModel()
    val addTransactionViewModel: AddTransactionViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val noteInventoryViewModel: NoteInventoryViewModel = viewModel()
    val templateViewModel: TemplateViewModel = viewModel()
    val manageAccountsViewModel: ManageAccountsViewModel = viewModel()
    val dataManagementViewModel: DataManagementViewModel = viewModel()
    val ledgerViewModel: LedgerViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel() // <--- NEW

    NavGraph(
        navController = navController,
        dashboardViewModel = dashboardViewModel,
        addTransactionViewModel = addTransactionViewModel,
        analyticsViewModel = analyticsViewModel,
        noteInventoryViewModel = noteInventoryViewModel,
        templateViewModel = templateViewModel,
        manageAccountsViewModel = manageAccountsViewModel,
        dataManagementViewModel = dataManagementViewModel,
        ledgerViewModel = ledgerViewModel,
        settingsViewModel = settingsViewModel // <--- PASS IT
    )
}