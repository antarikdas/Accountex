package com.scitech.accountex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.scitech.accountex.ui.screens.*
import com.scitech.accountex.ui.theme.AccountexTheme
import com.scitech.accountex.viewmodel.*

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val addTransactionViewModel: AddTransactionViewModel by viewModels()
    private val templateViewModel: TemplateViewModel by viewModels()
    private val noteTrackingViewModel: NoteTrackingViewModel by viewModels()
    private val analyticsViewModel: AnalyticsViewModel by viewModels()
    private val noteInventoryViewModel: NoteInventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccountexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AccountexApp(
                        dashboardViewModel = dashboardViewModel,
                        addTransactionViewModel = addTransactionViewModel,
                        templateViewModel = templateViewModel,
                        noteTrackingViewModel = noteTrackingViewModel,
                        analyticsViewModel = analyticsViewModel,
                        noteInventoryViewModel = noteInventoryViewModel,
                        context = this
                    )
                }
            }
        }
    }
}

@Composable
fun AccountexApp(
    dashboardViewModel: DashboardViewModel,
    addTransactionViewModel: AddTransactionViewModel,
    templateViewModel: TemplateViewModel,
    noteTrackingViewModel: NoteTrackingViewModel,
    analyticsViewModel: AnalyticsViewModel,
    noteInventoryViewModel: NoteInventoryViewModel,
    context: android.content.Context
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var selectedTransactionId by remember { mutableIntStateOf(0) }

    when (currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddTransactionClick = { currentScreen = Screen.AddTransaction },
                onTemplatesClick = { currentScreen = Screen.Templates },
                onNoteTrackingClick = { currentScreen = Screen.NoteTracking },
                onAnalyticsClick = { currentScreen = Screen.Analytics },
                onNoteInventoryClick = { currentScreen = Screen.NoteInventory },
                onTransactionClick = { id ->
                    selectedTransactionId = id
                    currentScreen = Screen.TransactionDetail
                },
                context = context
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
        Screen.NoteTracking -> {
            NoteTrackingScreen(
                viewModel = noteTrackingViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
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
                viewModel = dashboardViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }
        Screen.NoteInventory -> {
            NoteInventoryScreen(
                viewModel = noteInventoryViewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard }
            )
        }
    }
}

sealed class Screen {
    object Dashboard : Screen()
    object AddTransaction : Screen()
    object Templates : Screen()
    object NoteTracking : Screen()
    object Analytics : Screen()
    object TransactionDetail : Screen()

    object NoteInventory : Screen()
}