package com.scitech.accountex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CloudUpload
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
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AccountType
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.theme.AppTheme
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.utils.formatDate
import com.scitech.accountex.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddTransactionClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onNoteInventoryClick: () -> Unit,
    onTransactionClick: (Int) -> Unit,
    onManageAccountsClick: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val heldAmount by viewModel.heldAmount.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()

    val colors = AppTheme.colors

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = colors.actionDominant,
                contentColor = colors.textInverse,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- HEADER ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(colors.headerGradient)
                        // ✅ VISIBILITY FIX: Reduced padding, pushed content up
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp)
                ) {
                    Column {
                        // 1. WELCOME ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                // ✅ VISIBILITY FIX: Hardcoded Color.White for absolute certainty
                                Text(
                                    "Welcome Back,",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    "Accountex",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // ✅ VISIBILITY FIX: Hardcoded Color.White
                                IconButton(onClick = onNavigateToBackup) {
                                    Icon(Icons.Rounded.CloudUpload, "Backup", tint = Color.White)
                                }
                                IconButton(onClick = onSettingsClick) {
                                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 2. TOTAL BALANCE
                        Text(
                            "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            formatCurrency(totalBalance),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                            color = Color.White, // ✅ VISIBILITY FIX
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // 3. SUMMARY CARD
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DashboardMiniStat("Income", todaySummary.income, true, colors.income)
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.3f)))
                            DashboardMiniStat("Expense", todaySummary.expense, false, colors.expense)
                        }
                    }
                }

                // --- CONTENT SCROLL ---
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. Held Amount Alert
                    if (heldAmount > 0) {
                        item { DashboardHeldMoneyCard(amount = heldAmount) }
                    }

                    // 2. Quick Actions
                    item {
                        DashboardSectionHeader("Quick Actions")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DashboardActionBtn(
                                icon = Icons.Default.Star,
                                label = "Templates",
                                bgColor = colors.brandPrimary.copy(alpha = 0.1f),
                                iconColor = colors.brandPrimary,
                                onClick = onTemplatesClick
                            )
                            DashboardActionBtn(
                                icon = Icons.Default.Analytics,
                                label = "Analytics",
                                bgColor = colors.actionDominant.copy(alpha = 0.15f),
                                iconColor = colors.actionDominant,
                                onClick = onAnalyticsClick
                            )
                            DashboardActionBtn(
                                icon = Icons.Default.Inventory,
                                label = "Inventory",
                                bgColor = colors.brandSecondary.copy(alpha = 0.1f),
                                iconColor = colors.brandSecondary,
                                onClick = onNoteInventoryClick
                            )
                        }
                    }

                    // 3. Accounts / Wallets
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            DashboardSectionHeader("Wallets")
                            TextButton(onClick = onManageAccountsClick) {
                                Text("Manage", style = MaterialTheme.typography.labelLarge, color = colors.brandPrimary)
                            }
                        }
                    }
                    items(accounts.take(3)) { account ->
                        DashboardAccountCard(account)
                    }

                    // 4. Recent Activity
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            DashboardSectionHeader("Recent Activity")
                            TextButton(onClick = onNavigateToLedger) {
                                Text("View All", style = MaterialTheme.typography.labelLarge, color = colors.brandPrimary)
                            }
                        }
                    }
                    items(transactions.take(8)) { transaction ->
                        val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown"
                        DashboardTransactionItem(
                            transaction = transaction,
                            accountName = accountName,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
private fun DashboardTransactionItem(transaction: Transaction, accountName: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val isIncome = transaction.type == TransactionType.INCOME
    val isExpense = transaction.type == TransactionType.EXPENSE

    val amountColor = if (isIncome) colors.income else if(isExpense) colors.expense else colors.textPrimary
    val icon = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.divider),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Text(" • ", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Text(
            text = "${if (isExpense) "-" else "+"}${formatCurrency(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = amountColor,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
}

@Composable
fun DashboardHeldMoneyCard(amount: Double) {
    val colors = AppTheme.colors
    val baseColor = colors.actionDominant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(baseColor.copy(alpha = 0.15f))
            .border(1.dp, baseColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = baseColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Info, null, tint = Color.Black) // Keep Black for contrast on Gold
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Held for Others",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary.copy(alpha = 0.7f)
                )
                Text(
                    formatCurrency(amount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
fun DashboardAccountCard(account: Account) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = colors.brandSecondary.copy(alpha = 0.1f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if(account.type == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Wallet,
                        contentDescription = null,
                        tint = colors.brandSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(account.type.name, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            Text(
                formatCurrency(account.balance),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.brandPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DashboardMiniStat(label: String, amount: Double, isIncome: Boolean, indicatorColor: Color) {
    val icon = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = indicatorColor)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DashboardActionBtn(icon: ImageVector, label: String, bgColor: Color, iconColor: Color, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bgColor,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DashboardSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = AppTheme.colors.textPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
    )
}