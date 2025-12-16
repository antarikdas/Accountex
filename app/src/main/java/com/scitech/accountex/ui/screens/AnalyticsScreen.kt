package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Financial Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Period Filter (Chips)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AnalyticsPeriod.values().forEach { p ->
                        val isSelected = period == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.setPeriod(p) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                p.name.replace("THIS_", "").replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Spending Trend Graph
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Spending Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (summary.graphData.isNotEmpty()) {
                            BarChart(data = summary.graphData, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No data for this period", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // 3. Summary Cards (Net, Income, Expense)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Income Card
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Income",
                        amount = summary.income,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Expense Card
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Expense",
                        amount = summary.expense,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 4. Category Breakdown (Modern Progress Bars)
            if (summary.categoryBreakdown.isNotEmpty()) {
                item {
                    Text("Top Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(summary.categoryBreakdown) { (category, amount) ->
                    ModernCategoryRow(category, amount, summary.expense)
                }
            }
        }
    }
}

// --- CUSTOM COMPONENTS ---

@Composable
fun BarChart(data: List<Pair<String, Double>>, color: Color) {
    val maxAmount = data.maxOfOrNull { it.second } ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.takeLast(7).forEach { (label, amount) ->
            // Animation for bar height
            val heightFraction by animateFloatAsState(
                targetValue = (amount / maxAmount).toFloat().coerceIn(0.1f, 1f),
                label = "barHeight"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight(heightFraction) // Uses animated fraction
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(color, color.copy(alpha = 0.5f))
                            )
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, amount: Double, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ModernCategoryRow(category: String, amount: Double, totalExpense: Double) {
    val percentage = if (totalExpense > 0) (amount / totalExpense) else 0.0

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, fontWeight = FontWeight.SemiBold)
            Text(formatCurrency(amount), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}