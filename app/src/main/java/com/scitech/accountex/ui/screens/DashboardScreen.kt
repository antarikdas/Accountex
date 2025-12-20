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
    context: Context
) {
    // Collect State
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val heldAmount by viewModel.heldAmount.collectAsState()

    // FIX 1: Use Reactive Balance (Fixes the lag)
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
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // === HERO SECTION ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            )
                        )
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Welcome Back,", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                                Text("Accountex", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Total Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))

                        // FIX: Displaying reactive totalBalance variable
                        Text(
                            formatCurrency(totalBalance),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = (-1).sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniStat("Income", todaySummary.income, true)
                            MiniStat("Expense", todaySummary.expense, false)
                        }
                    }
                }

                // === CONTENT ===
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (heldAmount > 0) {
                        item { HeldMoneyCard(amount = heldAmount) }
                    }
                    item {
                        SectionHeader("Quick Actions")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ModernActionBtn(Icons.Default.Star, "Templates", MaterialTheme.colorScheme.tertiary, onTemplatesClick)
                            ModernActionBtn(Icons.Default.Analytics, "Analytics", MaterialTheme.colorScheme.tertiary, onAnalyticsClick)
                            ModernActionBtn(Icons.Default.Inventory, "Inventory", MaterialTheme.colorScheme.tertiary, onNoteInventoryClick)
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader("Your Accounts")
                            TextButton(onClick = onManageAccountsClick) { Text("Manage", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    items(accounts) { account -> NeoAccountCard(account) }

                    item { SectionHeader("Recent Transactions") }
                    items(transactions.take(10)) { transaction ->
                        ModernTransactionItem(transaction, onClick = { onTransactionClick(transaction.id) })
                    }
                }
            }
        }
    }
}

// FIX 2: ModernTransactionItem (Kept Arrow UI, added Paperclip Indicator)
@Composable
private fun ModernTransactionItem(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (transaction.type == TransactionType.INCOME) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                contentDescription = null,
                tint = if (transaction.type == TransactionType.INCOME) Color(0xFF006C5B) else Color(0xFFBA1A1A)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            // New: Row to show Date + optional Paperclip icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (transaction.imageUris.isNotEmpty()) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    formatDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatCurrency(transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else Color(0xFF006C5B)
        )
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
fun HeldMoneyCard(amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFFFFB300), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Held for Others", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6D4C41), fontWeight = FontWeight.Bold)
                Text(formatCurrency(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }
        }
    }
}

// ... (Rest of your helpers like NeoAccountCard, AccountStyle, MiniStat, ModernActionBtn remain exactly as you had them) ...

@Composable
fun NeoAccountCard(account: Account) {
    val style = getAccountStyle(account)
    Card(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(style.containerColor), contentAlignment = Alignment.Center) { Icon(style.icon, contentDescription = account.name, tint = style.color, modifier = Modifier.size(28.dp)) }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(account.type.name, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Current Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text(formatCurrency(account.balance), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = style.color, letterSpacing = (-0.5).sp)
        }
    }
}
data class AccountStyle(val icon: ImageVector, val color: Color, val containerColor: Color)
@Composable
fun getAccountStyle(account: Account): AccountStyle {
    val isDark = isSystemInDarkTheme()
    val baseColor = when (account.type) {
        AccountType.CASH_DAILY -> Color(0xFF66BB6A)
        AccountType.BANK -> Color(0xFF42A5F5)
        AccountType.CASH_RESERVE -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }
    val icon = when (account.type) {
        AccountType.CASH_DAILY -> Icons.Default.Money
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CASH_RESERVE -> Icons.Default.CreditCard
        else -> Icons.Default.AccountBalanceWallet
    }
    val containerColor = if (isDark) baseColor.copy(alpha = 0.2f) else baseColor.copy(alpha = 0.1f)
    return AccountStyle(icon, baseColor, containerColor)
}
@Composable
private fun MiniStat(label: String, amount: Double, isPositive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).background(if(isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) { Icon(if(isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, modifier = Modifier.size(14.dp), tint = if(isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)) }
        Spacer(modifier = Modifier.width(8.dp))
        Column { Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f)); Text(formatCurrency(amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White) }
    }
}
@Composable
private fun ModernActionBtn(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(60.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)) }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
}