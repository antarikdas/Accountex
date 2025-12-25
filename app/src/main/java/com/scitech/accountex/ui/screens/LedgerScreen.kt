package com.scitech.accountex.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

    // Theme Colors shortcuts
    val bg = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = bg,
        topBar = {
            if (showSearch) {
                // SEARCH MODE
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; viewModel.onSearch(it) },
                            placeholder = { Text("Search description, amount...", style = MaterialTheme.typography.bodyLarge) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            viewModel.onSearch("")
                        }) { Icon(Icons.Default.Close, "Close") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
                )
            } else {
                // TITLE MODE
                TopAppBar(
                    title = { Text("Transaction History", style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        // FIX: Use Default ArrowBack to avoid version issues
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
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
                FilterChip(
                    selected = true,
                    onClick = { viewModel.onDateRange(DateRange.ALL_TIME) },
                    label = { Text("All Time") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.onDateRange(DateRange.THIS_MONTH) },
                    label = { Text("This Month") }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.onDateRange(DateRange.LAST_MONTH) },
                    label = { Text("Last Month") }
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // 2. TIMELINE LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                state.groupedTransactions.forEach { (dateHeader, transactions) ->

                    // Sticky Header (Clean Date)
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = dateHeader.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Timeline Items
                    items(transactions) { tx ->
                        TimelineTransactionItem(tx) { onTransactionClick(tx.id) }
                    }
                }

                if (state.groupedTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No transactions found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTransactionItem(tx: Transaction, onClick: () -> Unit) {
    // Semantic Colors
    val isIncome = tx.type == TransactionType.INCOME
    val isExpense = tx.type == TransactionType.EXPENSE

    val color = when {
        isIncome -> MaterialTheme.colorScheme.tertiary // Gold
        isExpense -> MaterialTheme.colorScheme.error // Coral
        else -> MaterialTheme.colorScheme.primary // Emerald/Blue
    }

    val sign = if (isExpense) "-" else if (isIncome) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min) // Essential for the vertical line to stretch
    ) {
        // --- TIMELINE LEFT ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(56.dp)
        ) {
            // Top Line
            Box(modifier = Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))

            // Dot/Icon (Fixed Border Issue)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color = color, shape = CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )

            // Bottom Line
            Box(modifier = Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
        }

        // --- CONTENT RIGHT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Description & Category
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.description.ifEmpty { tx.category },
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = tx.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount (Monospace)
                Text(
                    text = "$sign${formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
    }
}