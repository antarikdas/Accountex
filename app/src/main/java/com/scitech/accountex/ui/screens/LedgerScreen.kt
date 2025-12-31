package com.scitech.accountex.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.theme.AccountexColors
import com.scitech.accountex.ui.theme.AppTheme
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

    // 🧠 SYSTEM THEME ACCESS
    val colors = AppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            if (showSearch) {
                // SEARCH MODE
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; viewModel.onSearch(it) },
                            placeholder = { Text("Search history...", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                cursorColor = colors.brandPrimary
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
                        }) { Icon(Icons.Default.Close, "Close", tint = colors.textPrimary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
            } else {
                // TITLE MODE
                TopAppBar(
                    title = { Text("Transaction History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary) }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, "Search", tint = colors.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
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
                // FIX: Used BorderStroke directly as requested by compiler
                AssistChip(
                    onClick = { viewModel.onDateRange(DateRange.ALL_TIME) },
                    label = { Text("All Time") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = colors.surfaceCard,
                        labelColor = colors.textPrimary
                    ),
                    border = BorderStroke(1.dp, colors.divider)
                )
                AssistChip(
                    onClick = { viewModel.onDateRange(DateRange.THIS_MONTH) },
                    label = { Text("This Month") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = colors.surfaceCard,
                        labelColor = colors.textSecondary
                    ),
                    border = BorderStroke(1.dp, colors.divider)
                )
            }

            // FIX: Replaced deprecated Divider with HorizontalDivider
            HorizontalDivider(color = colors.divider)

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
                            color = colors.surfaceHighlight // Subtle distinction
                        ) {
                            Text(
                                text = dateHeader.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Timeline Items
                    items(transactions) { tx ->
                        TimelineTransactionItem(tx, colors) { onTransactionClick(tx.id) }
                    }
                }

                if (state.groupedTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = colors.textSecondary.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No transactions found", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTransactionItem(tx: Transaction, colors: AccountexColors, onClick: () -> Unit) {
    // Semantic Colors
    val isIncome = tx.type == TransactionType.INCOME
    val isExpense = tx.type == TransactionType.EXPENSE

    val color = when {
        isIncome -> colors.income
        isExpense -> colors.expense
        else -> colors.brandPrimary
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
            Box(modifier = Modifier.width(2.dp).weight(1f).background(colors.divider))

            // Dot/Icon (Fixed Border Issue)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color = color, shape = CircleShape)
                    .border(2.dp, colors.background, CircleShape) // Border matches background to create "cutout" effect
            )

            // Bottom Line
            Box(modifier = Modifier.width(2.dp).weight(1f).background(colors.divider))
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = tx.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount (Monospace)
                Text(
                    text = "$sign${formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
    }
}