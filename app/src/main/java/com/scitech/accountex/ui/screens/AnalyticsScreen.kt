package com.scitech.accountex.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet // FIXED IMPORT
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.AnalyticsPeriod
import com.scitech.accountex.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val summary by viewModel.periodSummary.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { PeriodSelector(current = selectedPeriod, onSelect = { viewModel.setPeriod(it) }) }
            item { SummaryGrid(summary.income, summary.expense, summary.net, summary.savingsRate) }
            item {
                if (summary.graphData.isNotEmpty()) {
                    Text("Expense Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    CustomBarChart(data = summary.graphData)
                }
            }
            item {
                if (summary.categoryBreakdown.isNotEmpty()) {
                    Text("Top Spending Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    CategoryBreakdownList(summary.categoryBreakdown, summary.expense)
                }
            }
        }
    }
}

@Composable
fun PeriodSelector(current: AnalyticsPeriod, onSelect: (AnalyticsPeriod) -> Unit) {
    val options = listOf(AnalyticsPeriod.THIS_WEEK to "Week", AnalyticsPeriod.THIS_MONTH to "Month", AnalyticsPeriod.THIS_YEAR to "Year", AnalyticsPeriod.ALL_TIME to "All")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { (period, label) ->
            val isSelected = current == period
            Box(
                modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent).clickable { onSelect(period) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SummaryGrid(income: Double, expense: Double, net: Double, savingsRate: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Income", income, Icons.Rounded.TrendingUp, Color(0xFF4CAF50), Modifier.weight(1f))
            StatCard("Expense", expense, Icons.Rounded.TrendingDown, MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Net Savings", net, Icons.Default.AccountBalanceWallet, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) // FIXED ICON
            SavingsRateCard(savingsRate, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(title: String, amount: Double, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(formatCurrency(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SavingsRateCard(rate: Double, modifier: Modifier) {
    // FIX: Convert Double to Float explicitly for logic to avoid type mismatch errors
    val rateFloat = rate.toFloat()

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Savings Rate", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${rate.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (rate >= 20) Color(0xFF4CAF50) else if (rate > 0) Color(0xFFFFA000) else MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                // FIXED MATH:
                progress = (rateFloat / 100f).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (rate >= 20) Color(0xFF4CAF50) else if (rate > 0) Color(0xFFFFA000) else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun CustomBarChart(data: List<Pair<String, Double>>) {
    if (data.isEmpty()) return
    val maxAmount = data.maxOf { it.second }
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(200.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            data.forEach { (label, amount) ->
                val barHeightRatio = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f
                val animatedHeight by animateFloatAsState(targetValue = barHeightRatio, label = "barHeight")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight(animatedHeight.coerceAtLeast(0.02f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(primaryColor))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownList(categories: List<Pair<String, Double>>, totalExpense: Double) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            categories.take(5).forEachIndexed { index, (name, amount) ->
                val percentage = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(progress = (percentage / 100f).toFloat(), modifier = Modifier.fillMaxWidth(0.5f).height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatCurrency(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                if (index < categories.size - 1 && index < 4) { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
            }
        }
    }
}