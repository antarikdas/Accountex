package com.scitech.accountex.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.AnalyticsViewModel
import com.scitech.accountex.viewmodel.TimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.analyticsState.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Time Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeTab("This Month", timeRange == TimeRange.THIS_MONTH) { viewModel.setTimeRange(TimeRange.THIS_MONTH) }
                TimeTab("Last Month", timeRange == TimeRange.LAST_MONTH) { viewModel.setTimeRange(TimeRange.LAST_MONTH) }
                TimeTab("All Time", timeRange == TimeRange.ALL_TIME) { viewModel.setTimeRange(TimeRange.ALL_TIME) }
            }

            // ... inside AnalyticsScreen.kt ...

// 2. Net Balance Card
            val balance = state.income - state.expense
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Keep Midnight Navy
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Net Savings", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatCurrency(balance),
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Premium Progress Bar
                    val progress = if (state.income > 0) (state.expense / state.income).toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),

                        // CHANGE 1: Gold for Expense (The "Spending" part)
                        color = Color(0xFFFFAB40),

                        // CHANGE 2: Royal Blue for Income (The "Capacity" part)
                        trackColor = Color(0xFF448AFF),

                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        // CHANGE 3: Match Text Colors
                        Text("Spent: ${formatCurrency(state.expense)}", color = Color(0xFFFFAB40), style = MaterialTheme.typography.labelSmall)
                        Text("Earned: ${formatCurrency(state.income)}", color = Color(0xFF448AFF), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 3. Spending Habits (Pie Chart)
            Text("Spending Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (state.pieSlices.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CHART
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        AnimatedDonutChart(state.pieSlices)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(formatCurrency(state.expense), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // LEGEND
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.pieSlices.forEach { slice ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(slice.color, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(slice.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${(slice.percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No expense data for this period.", color = Color.Gray)
                }
            }
        }
    }
}

// --- CUSTOM COMPONENTS ---

@Composable
fun TimeTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color.Gray
        )
    }
}

@Composable
fun AnimatedDonutChart(slices: List<com.scitech.accountex.viewmodel.PieSlice>) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animatable.animateTo(1f, animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing))
    }

    Canvas(modifier = Modifier.size(160.dp)) {
        val strokeWidth = 40f
        val radius = size.minDimension / 2 - strokeWidth
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f // Start from top

        slices.forEach { slice ->
            val sweepAngle = 360f * slice.percentage * animatable.value

            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            startAngle += sweepAngle
        }
    }
}