package com.scitech.accountex.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.DateRange
import com.scitech.accountex.viewmodel.LedgerViewModel

// Theme Colors
private val Slate = Color(0xFF1E293B)
private val BgGray = Color(0xFFF8FAFC)
private val Green = Color(0xFF10B981)
private val Red = Color(0xFFEF4444)
private val Blue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onNavigateBack: () -> Unit,
    onTransactionClick: (Int) -> Unit
) {
    val state by viewModel.ledgerState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgGray,
        topBar = {
            if (showSearch) {
                // SEARCH MODE BAR
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; viewModel.onSearch(it) },
                            placeholder = { Text("Search transactions...", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            viewModel.onSearch("")
                        }) { Icon(Icons.Default.Close, "Close", tint = Slate) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgGray)
                )
            } else {
                // NORMAL MODE BAR
                TopAppBar(
                    title = { Text("All Transactions", fontWeight = FontWeight.Bold, color = Slate) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate) }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, "Search", tint = Slate)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgGray)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // 1. FILTER CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedgerFilterChip("All Time") { viewModel.onDateRange(DateRange.ALL_TIME) }
                LedgerFilterChip("This Month") { viewModel.onDateRange(DateRange.THIS_MONTH) }
                LedgerFilterChip("Last Month") { viewModel.onDateRange(DateRange.LAST_MONTH) }
            }

            // 2. TRANSACTION LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                state.groupedTransactions.forEach { (dateHeader, transactions) ->

                    // Sticky Header (Date)
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgGray)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = dateHeader.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Transactions for that date
                    items(transactions) { tx ->
                        LedgerTransactionItem(tx) { onTransactionClick(tx.id) }
                        Divider(color = Color(0xFFE2E8F0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                if (state.groupedTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions found.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerTransactionItem(tx: Transaction, onClick: () -> Unit) {
    val color = when(tx.type) {
        TransactionType.INCOME -> Green
        TransactionType.EXPENSE -> Red
        else -> Blue
    }
    val icon = when(tx.type) {
        TransactionType.INCOME -> Icons.Rounded.CallReceived
        TransactionType.EXPENSE -> Icons.Rounded.CallMade
        else -> Icons.Rounded.SwapHoriz
    }
    val sign = if (tx.type == TransactionType.EXPENSE) "-" else if (tx.type == TransactionType.INCOME) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifEmpty { tx.category }, // Fallback to category if desc is empty
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Slate
            )
            if (tx.description.isNotEmpty()) {
                Text(
                    text = tx.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Amount
        Text(
            text = "$sign${formatCurrency(tx.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (tx.type == TransactionType.EXPENSE) Slate else color
        )
    }
}

@Composable
fun LedgerFilterChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}