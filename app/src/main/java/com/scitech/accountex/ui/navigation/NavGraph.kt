package com.scitech.accountex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.scitech.accountex.ui.screens.*
import com.scitech.accountex.viewmodel.*

// Define Routes
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Security : Screen("security")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")

    // UPDATED: Now supports optional txId argument
    object AddTransaction : Screen("add_transaction?txId={txId}") {
        fun createRoute(txId: Int? = null) =
            if (txId != null) "add_transaction?txId=$txId" else "add_transaction"
    }

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
    ledgerViewModel: LedgerViewModel,
    settingsViewModel: SettingsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // --- SPLASH ---
        composable(Screen.Splash.route) {
            SplashScreen(
                onAnimationFinished = {
                    navController.navigate(Screen.Security.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // --- SECURITY ---
        composable(Screen.Security.route) {
            SecurityScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Security.route) { inclusive = true }
                    }
                }
            )
        }

        // --- DASHBOARD ---
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddTransactionClick = { navController.navigate(Screen.AddTransaction.createRoute(null)) },
                onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                onNoteInventoryClick = { navController.navigate(Screen.NoteInventory.route) },
                onTransactionClick = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                onManageAccountsClick = { navController.navigate(Screen.ManageAccounts.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToLedger = { navController.navigate(Screen.Ledger.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        // --- SETTINGS ---
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- ADD TRANSACTION (UPDATED) ---
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(navArgument("txId") {
                type = NavType.IntType
                defaultValue = -1 // Default to -1 (Create Mode)
            })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getInt("txId") ?: -1

            // INITIALIZE SANDBOX OR RESET
            LaunchedEffect(txId) {
                if (txId != -1) {
                    addTransactionViewModel.loadTransactionForEdit(txId)
                } else {
                    // Logic to ensure clean state for new entry
                    // (Assuming User resets manual inputs if needed, or we add a reset() later)
                }
            }

            AddTransactionScreen(
                viewModel = addTransactionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- LEDGER ---
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
                    addTransactionViewModel.applyTemplate(template)
                    navController.navigate(Screen.AddTransaction.createRoute(null))
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

        // --- TRANSACTION DETAIL (UPDATED) ---
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("txId") { type = NavType.IntType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getInt("txId") ?: 0

            TransactionDetailScreen(
                transactionId = txId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.AddTransaction.createRoute(id))
                }
            )
        }
    }
}