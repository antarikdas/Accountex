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
                    // We no longer handle locking here.
                    // The NavGraph automatically starts with 'SecurityScreen'.
                    AccountexApp()
                }
            }
        }
    }
}

@Composable
fun AccountexApp() {
    // 1. Setup Navigation Controller
    val navController = rememberNavController()

    // 2. Initialize ViewModels
    // Ideally, use Hilt for dependency injection in the future, but this works perfectly for now.
    val dashboardViewModel: DashboardViewModel = viewModel()
    val addTransactionViewModel: AddTransactionViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val noteInventoryViewModel: NoteInventoryViewModel = viewModel()
    val templateViewModel: TemplateViewModel = viewModel()
    val manageAccountsViewModel: ManageAccountsViewModel = viewModel()
    val dataManagementViewModel: DataManagementViewModel = viewModel()
    val ledgerViewModel: LedgerViewModel = viewModel()

    // 3. Launch the Graph
    // The NavGraph logic inside ensures we start at the Security Screen.
    NavGraph(
        navController = navController,
        dashboardViewModel = dashboardViewModel,
        addTransactionViewModel = addTransactionViewModel,
        analyticsViewModel = analyticsViewModel,
        noteInventoryViewModel = noteInventoryViewModel,
        templateViewModel = templateViewModel,
        manageAccountsViewModel = manageAccountsViewModel,
        dataManagementViewModel = dataManagementViewModel,
        ledgerViewModel = ledgerViewModel
    )
}