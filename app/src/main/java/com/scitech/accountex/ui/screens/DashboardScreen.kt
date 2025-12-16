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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
    context: Context
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // -- Permission Logic (Kept same as before) --
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
        containerColor = MaterialTheme.colorScheme.background, // Uses our new off-white/charcoal
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.secondary, // Amber color
                contentColor = MaterialTheme.colorScheme.onSecondary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Background curve decoration

            Column(modifier = Modifier.fillMaxSize()) {

                // === 1. HERO SECTION (Custom Header) ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp)
                ) {
                    Column {
                        // Top Row: Title & Menu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Welcome Back,",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    "Accountex",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "Menu", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(text = { Text("Export to Excel") }, onClick = { showMenu = false; requestExport() })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Balance Display
                        Text(
                            "Total Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        Text(
                            formatCurrency(viewModel.getTotalBalance()),
                            style = MaterialTheme.typography.displayMedium, // Much bigger font
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = (-1).sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mini Daily Summary Inside Header
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

                // === 2. SCROLLABLE CONTENT ===
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // QUICK ACTIONS (Floating Row)
                    item {
                        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ModernActionBtn(Icons.Default.Star, "Templates", MaterialTheme.colorScheme.tertiary, onTemplatesClick)
                            ModernActionBtn(Icons.Default.Analytics, "Analytics", MaterialTheme.colorScheme.tertiary, onAnalyticsClick)
                            ModernActionBtn(Icons.Default.Inventory, "Inventory", MaterialTheme.colorScheme.tertiary, onNoteInventoryClick) // Changed Icon
                        }
                    }

                    // ACCOUNTS LIST
                    item {
                        SectionHeader("Your Accounts")
                    }
                    items(accounts) { account ->
                        ModernAccountCard(account)
                    }

                    // RECENT TRANSACTIONS
                    item {
                        SectionHeader("Recent Transactions")
                    }
                    items(transactions.take(10)) { transaction ->
                        ModernTransactionItem(transaction, onClick = { onTransactionClick(transaction.id) })
                    }
                }
            }
        }
    }
}

// --- NEW COMPONENTS ---

@Composable
fun MiniStat(label: String, amount: Double, isPositive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if(isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if(isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, // Down arrow for income (incoming)
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if(isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Text(formatCurrency(amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ModernActionBtn(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ModernAccountCard(account: com.scitech.accountex.data.Account) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    account.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(account.type.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                formatCurrency(account.balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ModernTransactionItem(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
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

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                formatDate(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Amount
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
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}