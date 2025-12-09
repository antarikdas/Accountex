package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.AnalyticsPeriod
import com.scitech.accountex.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val period by viewModel.selectedPeriod.collectAsState()
    val summary by viewModel.periodSummary.collectAsState()

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Period", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticsPeriod.values().forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { viewModel.setPeriod(p) },
                            label = { Text(p.name.replace("_", " ")) }
                        )
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryRow("Income", summary.income, true)
                        SummaryRow("Expense", summary.expense, false)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow("Net", summary.net, summary.net >= 0)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Transactions: ${summary.transactionCount}", style = MaterialTheme.typography.bodyMedium)
                        if (summary.income > 0) {
                            Text("Savings Rate: ${String.format("%.1f", summary.savingsRate)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (summary.categoryBreakdown.isNotEmpty()) {
                item {
                    Text("Category Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(summary.categoryBreakdown) { (category, amount) ->
                    CategoryBreakdownCard(category, amount, summary.expense)
                }
            }

            if (summary.topExpenses.isNotEmpty()) {
                item {
                    Text("Top Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(summary.topExpenses) { transaction ->
                    TopExpenseCard(transaction)
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(category: String, amount: Double, totalExpense: Double) {
    val percentage = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.bodyLarge)
            }
            Text(formatCurrency(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun TopExpenseCard(transaction: Transaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(transaction.category, fontWeight = FontWeight.Bold)
                if (transaction.description.isNotEmpty()) {
                    Text(transaction.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(formatCurrency(transaction.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    }
}