package com.scitech.accountex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
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
import com.scitech.accountex.viewmodel.DataHubViewModel
import com.scitech.accountex.viewmodel.DateFilter
import com.scitech.accountex.viewmodel.TypeFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DataHubScreen(
    viewModel: DataHubViewModel,
    onNavigateBack: () -> Unit,
    onTransactionClick: (Int) -> Unit
) {
    val groupedTransactions by viewModel.uiState.collectAsState()

    // 🧠 SYSTEM THEME ACCESS
    val colors = AppTheme.colors

    // UI States for Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TypeFilter.ALL) }
    var selectedDate by remember { mutableStateOf(DateFilter.ALL_TIME) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column(modifier = Modifier.background(colors.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("Data Terminal", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.background)
                )

                // TERMINAL SEARCH BAR
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onSearchQueryChanged(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Query Database...", color = colors.textSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.brandPrimary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.surfaceCard,
                        unfocusedContainerColor = colors.surfaceCard,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.brandPrimary
                    ),
                    singleLine = true
                )

                // FILTERS ROW
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type Filters
                    item { FilterChip(selectedType == TypeFilter.ALL, "ALL", colors) { selectedType = TypeFilter.ALL; viewModel.onTypeFilterChanged(TypeFilter.ALL) } }
                    item { FilterChip(selectedType == TypeFilter.EXPENSE, "EXPENSE", colors) { selectedType = TypeFilter.EXPENSE; viewModel.onTypeFilterChanged(TypeFilter.EXPENSE) } }
                    item { FilterChip(selectedType == TypeFilter.INCOME, "INCOME", colors) { selectedType = TypeFilter.INCOME; viewModel.onTypeFilterChanged(TypeFilter.INCOME) } }
                    item { FilterChip(selectedType == TypeFilter.THIRD_PARTY, "3RD PARTY", colors) { selectedType = TypeFilter.THIRD_PARTY; viewModel.onTypeFilterChanged(TypeFilter.THIRD_PARTY) } }

                    item { Spacer(modifier = Modifier.width(1.dp).height(20.dp).background(colors.divider)) }

                    // Date Filters
                    item { FilterChip(selectedDate == DateFilter.ALL_TIME, "GLOBAL", colors) { selectedDate = DateFilter.ALL_TIME; viewModel.onDateFilterChanged(DateFilter.ALL_TIME) } }
                    item { FilterChip(selectedDate == DateFilter.THIS_MONTH, "CURRENT", colors) { selectedDate = DateFilter.THIS_MONTH; viewModel.onDateFilterChanged(DateFilter.THIS_MONTH) } }
                    item { FilterChip(selectedDate == DateFilter.LAST_MONTH, "PREV", colors) { selectedDate = DateFilter.LAST_MONTH; viewModel.onDateFilterChanged(DateFilter.LAST_MONTH) } }
                }
                HorizontalDivider(color = colors.divider)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (groupedTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("NO DATA SIGNALS FOUND", color = colors.textSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                groupedTransactions.forEach { (dateHeader, transactions) ->
                    // STICKY HEADER (Terminal Block)
                    stickyHeader {
                        Surface(color = colors.surfaceHighlight, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = dateHeader.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // ITEMS
                    items(transactions) { tx ->
                        HubTransactionItem(tx, colors) { onTransactionClick(tx.id) }
                    }
                }
            }
        }
    }
}

// Updated Helper: Now uses AppTheme
@Composable
fun FilterChip(isSelected: Boolean, label: String, colors: AccountexColors, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) colors.brandPrimary else colors.surfaceCard,
        contentColor = if (isSelected) colors.textInverse else colors.textSecondary,
        shape = RoundedCornerShape(8.dp), // Tech/Square look
        border = BorderStroke(1.dp, if (isSelected) colors.brandPrimary else colors.divider),
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun HubTransactionItem(tx: Transaction, colors: AccountexColors, onClick: () -> Unit) {
    val isExpense = tx.type == TransactionType.EXPENSE
    val isIncome = tx.type == TransactionType.INCOME

    val color = when(tx.type) {
        TransactionType.INCOME -> colors.income
        TransactionType.EXPENSE -> colors.expense
        TransactionType.EXCHANGE -> colors.brandSecondary
        else -> colors.brandPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon (Terminal Style)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.1f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Outlined.ArrowDownward else if (isExpense) Icons.Outlined.ArrowUpward else Icons.Outlined.SwapHoriz,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            if (tx.description.isNotEmpty()) {
                Text(tx.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
            }
        }

        // Amount (Monospace for Data Precision)
        Text(
            text = "${if (isExpense) "-" else "+"}${formatCurrency(tx.amount)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
    HorizontalDivider(color = colors.divider, thickness = 1.dp)
}