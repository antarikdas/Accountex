package com.scitech.accountex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.scitech.accountex.ui.screens.*
import com.scitech.accountex.viewmodel.*

// Define Routes
sealed class Screen(val route: String) {
    object Security : Screen("security") // <--- NEW SCREEN
    object Dashboard : Screen("dashboard")
    object AddTransaction : Screen("add_transaction")
    object Ledger : Screen("ledger")
    object Analytics : Screen("analytics")
    object NoteInventory : Screen("note_inventory")
    object Templates : Screen("templates")
    object ManageAccounts : Screen("manage_accounts")
    object Backup : Screen("backup")
    object TransactionDetail : Screen("transaction_detail/{txId}") {
        fun createRoute(txId: Int) = "transaction_detail/$txId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    addTransactionViewModel: AddTransactionViewModel,
    analyticsViewModel: AnalyticsViewModel,
    noteInventoryViewModel: NoteInventoryViewModel,
    templateViewModel: TemplateViewModel,
    manageAccountsViewModel: ManageAccountsViewModel,
    dataManagementViewModel: DataManagementViewModel,
    ledgerViewModel: LedgerViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Security.route // <--- MUST BE SECURITY
    ) {
        // --- 1. SECURITY SCREEN (START) ---
        composable(Screen.Security.route) {
            SecurityScreen(
                onLoginSuccess = {
                    // When login/pin succeeds, go to Dashboard
                    // popUpTo(Security) ensures pressing "Back" from Dashboard exits the app
                    // instead of going back to the lock screen.
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Security.route) { inclusive = true }
                    }
                }
            )
        }

        // --- 2. DASHBOARD ---
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddTransactionClick = { navController.navigate(Screen.AddTransaction.route) },
                onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                onNoteInventoryClick = { navController.navigate(Screen.NoteInventory.route) },
                onTransactionClick = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                onManageAccountsClick = { navController.navigate(Screen.ManageAccounts.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToLedger = { navController.navigate(Screen.Ledger.route) },
                context = LocalContext.current
            )
        }

        // --- 3. ADD TRANSACTION ---
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                viewModel = addTransactionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- 4. LEDGER (HISTORY) ---
        composable(Screen.Ledger.route) {
            LedgerScreen(
                viewModel = ledgerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTransactionClick = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) }
            )
        }

        // --- 5. TEMPLATES ---
        composable(Screen.Templates.route) {
            TemplatesScreen(
                viewModel = templateViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTemplateSelect = { template ->
                    addTransactionViewModel.applyTemplate(template)
                    navController.navigate(Screen.AddTransaction.route)
                }
            )
        }

        // --- 6. ANALYTICS ---
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- 7. NOTE INVENTORY ---
        composable(Screen.NoteInventory.route) {
            NoteInventoryScreen(
                viewModel = noteInventoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- 8. MANAGE ACCOUNTS ---
        composable(Screen.ManageAccounts.route) {
            ManageAccountsScreen(
                viewModel = manageAccountsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- 9. BACKUP ---
        composable(Screen.Backup.route) {
            DataManagementScreen(
                viewModel = dataManagementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- 10. TRANSACTION DETAIL ---
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("txId") { type = NavType.IntType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getInt("txId") ?: 0
            TransactionDetailScreen(
                transactionId = txId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}