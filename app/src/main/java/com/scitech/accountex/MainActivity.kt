package com.scitech.accountex

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.ui.screens.*
import com.scitech.accountex.ui.theme.AccountexTheme
import com.scitech.accountex.utils.BiometricAuth
import com.scitech.accountex.viewmodel.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccountexTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var isUnlocked by remember { mutableStateOf(false) }

                    // Trigger Auth on Launch
                    LaunchedEffect(Unit) {
                        BiometricAuth.authenticate(
                            activity = this@MainActivity,
                            onSuccess = { isUnlocked = true },
                            onError = { /* Keep locked */ }
                        )
                    }

                    if (isUnlocked) {
                        AccountexApp()
                    } else {
                        LockedScreen(onUnlockClick = {
                            BiometricAuth.authenticate(
                                activity = this@MainActivity,
                                onSuccess = { isUnlocked = true },
                                onError = { }
                            )
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun LockedScreen(onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1E293B)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Lock, "Locked", tint = Color.White, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Accountex is Locked", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Your financial data is secure.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onUnlockClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) {
            Text("Unlock App")
        }
    }
}

// 1. Defined Screen Enum
enum class Screen {
    Dashboard,
    AddTransaction,
    Analytics,
    NoteInventory,
    Templates,
    TransactionDetail,
    ManageAccounts,
    Backup,
    Ledger // Use Ledger for "View All"
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
    val dataManagementViewModel: DataManagementViewModel = viewModel()
    val ledgerViewModel: LedgerViewModel = viewModel()

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
                onNavigateToBackup = { currentScreen = Screen.Backup },
                onNavigateToLedger = { currentScreen = Screen.Ledger }, // <--- FIX: Correct parameter name
                context = LocalContext.current
            )
        }

        Screen.AddTransaction -> {
            AddTransactionScreen(
                viewModel = addTransactionViewModel,
                templateViewModel = templateViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }

        Screen.Ledger -> {
            LedgerScreen(
                viewModel = ledgerViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                onTransactionClick = { id ->
                    selectedTransactionId = id
                    currentScreen = Screen.TransactionDetail
                }
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

        Screen.Backup -> {
            DataManagementScreen(
                viewModel = dataManagementViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }
    }
}