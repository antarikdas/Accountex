package com.scitech.accountex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.scitech.accountex.ui.screens.AddTransactionScreen
import com.scitech.accountex.ui.screens.DashboardScreen
import com.scitech.accountex.ui.theme.AccountexTheme
import com.scitech.accountex.viewmodel.AddTransactionViewModel
import com.scitech.accountex.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val addTransactionViewModel: AddTransactionViewModel by viewModels()

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
                        addTransactionViewModel = addTransactionViewModel
                    )
                }
            }
        }
    }
}

sealed class Screen {
    object Dashboard : Screen()
    object AddTransaction : Screen()
}

@Composable
fun AccountexApp(
    dashboardViewModel: DashboardViewModel,
    addTransactionViewModel: AddTransactionViewModel
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    when (currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddTransactionClick = {
                    currentScreen = Screen.AddTransaction
                }
            )
        }

        Screen.AddTransaction -> {
            AddTransactionScreen(
                viewModel = addTransactionViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Dashboard
                }
            )
        }
    }
}
