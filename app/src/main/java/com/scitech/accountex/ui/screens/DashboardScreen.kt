package com.scitech.accountex.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AccountType
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.utils.formatDate
import com.scitech.accountex.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    context: Context
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val heldAmount by viewModel.heldAmount.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { viewModel.exportToExcel(); Toast.makeText(context, "Exporting...", Toast.LENGTH_SHORT).show() }
        else { Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show() }
    }
    fun requestExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            viewModel.exportToExcel(); Toast.makeText(context, "Exporting...", Toast.LENGTH_SHORT).show()
        } else { permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.tertiary, // Gold button for "New Value"
                contentColor = MaterialTheme.colorScheme.onTertiary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- 1. PREMIUM HEADER (Gradient Mesh) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary, // Emerald
                                    MaterialTheme.colorScheme.primaryContainer // Lighter Emerald
                                )
                            )
                        )
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 48.dp)
                ) {
                    Column {
                        // Top Bar
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Welcome Back,", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                                Text("Accountex", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onNavigateToBackup) { Icon(Icons.Rounded.CloudUpload, "Backup", tint = MaterialTheme.colorScheme.onPrimary) }
                                Box {
                                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Menu", tint = MaterialTheme.colorScheme.onPrimary) }
                                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                        DropdownMenuItem(text = { Text("Export to Excel") }, onClick = { showMenu = false; requestExport() })
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Total Balance (Monospace for alignment)
                        Text("TOTAL BALANCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), letterSpacing = 2.sp)
                        Text(
                            formatCurrency(totalBalance),
                            style = MaterialTheme.typography.displayLarge, // Use the big new font
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Mini Stats (Glassmorphism)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniStat("Income", todaySummary.income, isIncome = true)
                            // Vertical Divider
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.2f)))
                            MiniStat("Expense", todaySummary.expense, isIncome = false)
                        }
                    }
                }

                // --- 2. SCROLLABLE CONTENT ---
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Alert Card
                    if (heldAmount > 0) { item { HeldMoneyCard(amount = heldAmount) } }

                    // Quick Actions
                    item {
                        SectionHeader("Quick Actions")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ModernActionBtn(Icons.Default.Star, "Templates", MaterialTheme.colorScheme.primary, onTemplatesClick)
                            ModernActionBtn(Icons.Default.Analytics, "Analytics", MaterialTheme.colorScheme.tertiary, onAnalyticsClick)
                            ModernActionBtn(Icons.Default.Inventory, "Inventory", MaterialTheme.colorScheme.secondary, onNoteInventoryClick)
                        }
                    }

                    // Accounts Section
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader("Wallets")
                            TextButton(onClick = onManageAccountsClick) { Text("Manage", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    items(accounts) { account -> NeoAccountCard(account) }

                    // Recent Transactions
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader("Recent Activity")
                            TextButton(onClick = onNavigateToLedger) { Text("View All", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        }
                    }

                    items(transactions.take(10)) { transaction ->
                        ModernTransactionItem(transaction, onClick = { onTransactionClick(transaction.id) })
                    }
                }
            }
        }
    }
}

// --- 3. REFACTORED COMPONENTS (Using Theme Tokens) ---

@Composable
private fun ModernTransactionItem(transaction: Transaction, onClick: () -> Unit) {
    // SEMANTIC COLOR LOGIC: Gold for Income, Coral for Expense
    val isIncome = transaction.type == TransactionType.INCOME
    val isExpense = transaction.type == TransactionType.EXPENSE

    val amountColor = when {
        isIncome -> MaterialTheme.colorScheme.tertiary // Gold
        isExpense -> MaterialTheme.colorScheme.error // Coral
        else -> MaterialTheme.colorScheme.onSurface
    }

    val icon = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatDate(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Amount (Monospace Font applied via Typography.bodyLarge)
        Text(
            text = "${if (isExpense) "-" else "+"}${formatCurrency(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = amountColor
        )
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}

@Composable
fun HeldMoneyCard(amount: Double) {
    // Uses Tertiary Container (Gold/Amber) for "Held" money
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onTertiary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Held for Others", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                Text(formatCurrency(amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NeoAccountCard(account: Account) {
    // Dynamic styles based on Account Type, but mapped to Theme
    val cardColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp), // Subtle shadow
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if(account.type == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Wallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, color = contentColor)
                Text(account.type.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                formatCurrency(account.balance),
                style = MaterialTheme.typography.bodyLarge, // Monospace
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, amount: Double, isIncome: Boolean) {
    // Income = Tertiary (Gold), Expense = Error (Coral)
    val color = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val icon = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f), // Glassy circle
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
            Text(
                formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ModernActionBtn(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}