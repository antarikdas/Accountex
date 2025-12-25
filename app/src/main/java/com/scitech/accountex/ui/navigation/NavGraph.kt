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
        startDestination = Screen.Dashboard.route
    ) {
        // --- DASHBOARD ---
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

        // --- ADD TRANSACTION ---
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                viewModel = addTransactionViewModel,
                // FIX: Removed templateViewModel argument as it is no longer required by the screen
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- LEDGER (HISTORY) ---
        composable(Screen.Ledger.route) {
            LedgerScreen(
                viewModel = ledgerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTransactionClick = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) }
            )
        }

        // --- TEMPLATES ---
        composable(Screen.Templates.route) {
            TemplatesScreen(
                viewModel = templateViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTemplateSelect = { template ->
                    // Logic: We apply the template to the Shared ViewModel, then navigate
                    addTransactionViewModel.applyTemplate(template)
                    navController.navigate(Screen.AddTransaction.route)
                }
            )
        }

        // --- ANALYTICS ---
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- NOTE INVENTORY ---
        composable(Screen.NoteInventory.route) {
            NoteInventoryScreen(
                viewModel = noteInventoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- MANAGE ACCOUNTS ---
        composable(Screen.ManageAccounts.route) {
            ManageAccountsScreen(
                viewModel = manageAccountsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- BACKUP ---
        composable(Screen.Backup.route) {
            DataManagementScreen(
                viewModel = dataManagementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- TRANSACTION DETAIL ---
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