package com.scitech.accountex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.ui.screens.*
import com.scitech.accountex.ui.theme.AccountexTheme
import com.scitech.accountex.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccountexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AccountexApp()
                }
            }
        }
    }
}

// 1. Define all possible screens here
enum class Screen {
    Dashboard,
    AddTransaction,
    Analytics,
    NoteInventory,
    Templates,
    TransactionDetail,
    ManageAccounts,
    Backup // <--- 1. ADDED BACKUP SCREEN
}

@Composable
fun AccountexApp() {
    // 2. Navigation State
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var selectedTransactionId by remember { mutableIntStateOf(0) }

    // 3. Initialize ViewModels
    val dashboardViewModel: DashboardViewModel = viewModel()
    val addTransactionViewModel: AddTransactionViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val noteInventoryViewModel: NoteInventoryViewModel = viewModel()
    val templateViewModel: TemplateViewModel = viewModel()
    val manageAccountsViewModel: ManageAccountsViewModel = viewModel()
    val dataManagementViewModel: DataManagementViewModel = viewModel() // <--- 2. ADDED BACKUP VIEWMODEL

    // 4. Navigation Logic
    when (currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddTransactionClick = { currentScreen = Screen.AddTransaction },
                onTemplatesClick = { currentScreen = Screen.Templates },
                onAnalyticsClick = { currentScreen = Screen.Analytics },
                onNoteInventoryClick = { currentScreen = Screen.NoteInventory },
                onTransactionClick = { id ->
                    selectedTransactionId = id
                    currentScreen = Screen.TransactionDetail
                },
                onManageAccountsClick = { currentScreen = Screen.ManageAccounts },
                onNavigateToBackup = { currentScreen = Screen.Backup }, // <--- 3. CONNECTED THE BRIDGE
                context = androidx.compose.ui.platform.LocalContext.current
            )
        }

        Screen.AddTransaction -> {
            AddTransactionScreen(
                viewModel = addTransactionViewModel,
                templateViewModel = templateViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }

        Screen.Templates -> {
            TemplatesScreen(
                viewModel = templateViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                onTemplateSelect = { template ->
                    addTransactionViewModel.applyTemplate(template)
                    currentScreen = Screen.AddTransaction
                }
            )
        }

        Screen.Analytics -> {
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }

        Screen.TransactionDetail -> {
            TransactionDetailScreen(
                transactionId = selectedTransactionId,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }

        Screen.NoteInventory -> {
            NoteInventoryScreen(
                viewModel = noteInventoryViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }

        Screen.ManageAccounts -> {
            ManageAccountsScreen(
                viewModel = manageAccountsViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }

        Screen.Backup -> { // <--- 4. ADDED BACKUP SCREEN UI
            DataManagementScreen(
                viewModel = dataManagementViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }
    }
}