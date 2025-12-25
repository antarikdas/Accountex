package com.scitech.accountex.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Financial Health", style = MaterialTheme.typography.titleMedium) },
                // FIX: Use standard ArrowBack to avoid AutoMirrored issues if library is old
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 1. Time Filter Tabs (Premium Toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeTab("This Month", timeRange == TimeRange.THIS_MONTH) { viewModel.setTimeRange(TimeRange.THIS_MONTH) }
                TimeTab("Last Month", timeRange == TimeRange.LAST_MONTH) { viewModel.setTimeRange(TimeRange.LAST_MONTH) }
                TimeTab("All Time", timeRange == TimeRange.ALL_TIME) { viewModel.setTimeRange(TimeRange.ALL_TIME) }
            }

            // 2. Net Savings Card (The "Truth")
            val balance = state.income - state.expense
            val isPositive = balance >= 0

            // Define colors locally for the chart logic, mapped from Theme
            val incomeColor = MaterialTheme.colorScheme.tertiary // Gold
            val expenseColor = MaterialTheme.colorScheme.error // Coral

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("NET SAVINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            formatCurrency(balance),
                            style = MaterialTheme.typography.displayMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) incomeColor else expenseColor
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // The "Spendometer"
                        val progress = if (state.income > 0) (state.expense / state.income).toFloat() else 0f
                        val safeProgress = progress.coerceIn(0f, 1f)

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Spending Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(safeProgress * 100).toInt()}% Used", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if(safeProgress > 0.8f) expenseColor else incomeColor)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { safeProgress },
                                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                                color = expenseColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatColumn("Earned", state.income, incomeColor)
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            StatColumn("Spent", state.expense, expenseColor)
                        }
                    }
                }
            }

            // 3. Category Breakdown (Donut)
            Text("Where did it go?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            if (state.pieSlices.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CHART
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        AnimatedDonutChart(state.pieSlices)
                        // Center Text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(state.expense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // LEGEND
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        state.pieSlices.take(5).forEach { slice ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(slice.color, CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(slice.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${(slice.percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("No spending data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- HELPERS ---

// FIX: Added 'RowScope.' receiver so 'Modifier.weight(1f)' works
@Composable
fun RowScope.TimeTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    // Removed unused 'shadow' variable

    Box(
        modifier = Modifier
            .weight(1f) // Now this works because we are in RowScope
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun StatColumn(label: String, amount: Double, color: Color) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AnimatedDonutChart(slices: List<com.scitech.accountex.viewmodel.PieSlice>) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animatable.animateTo(1f, animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing))
    }

    Canvas(modifier = Modifier.size(160.dp)) {
        val strokeWidth = 50f
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        slices.forEach { slice ->
            val sweepAngle = 360f * slice.percentage * animatable.value

            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            startAngle += sweepAngle
        }
    }
}