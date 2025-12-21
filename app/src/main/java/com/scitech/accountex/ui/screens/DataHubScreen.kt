package com.scitech.accountex.ui.screens

import androidx.compose.foundation.BorderStroke // <--- Added missing import
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // <--- Updated Icon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import com.scitech.accountex.viewmodel.DataHubViewModel
import com.scitech.accountex.viewmodel.DateFilter
import com.scitech.accountex.viewmodel.TypeFilter

// Premium Colors
private val Slate = Color(0xFF1E293B)
private val BackgroundGray = Color(0xFFF8FAFC)
private val SurfaceWhite = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DataHubScreen(
    viewModel: DataHubViewModel,
    onNavigateBack: () -> Unit,
    onTransactionClick: (Int) -> Unit
) {
    val groupedTransactions by viewModel.uiState.collectAsState()

    // UI States for Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TypeFilter.ALL) }
    var selectedDate by remember { mutableStateOf(DateFilter.ALL_TIME) }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            Column(modifier = Modifier.background(SurfaceWhite)) {
                CenterAlignedTopAppBar(
                    title = { Text("Data Hub", fontWeight = FontWeight.Bold, color = Slate) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SurfaceWhite)
                )

                // SEARCH BAR
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onSearchQueryChanged(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search transactions...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = BackgroundGray,
                        unfocusedContainerColor = BackgroundGray
                    ),
                    singleLine = true
                )

                // FILTERS ROW
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type Filters (Fixed Logic)
                    item { FilterChip(selectedType == TypeFilter.ALL, "All") { selectedType = TypeFilter.ALL; viewModel.onTypeFilterChanged(TypeFilter.ALL) } }
                    item { FilterChip(selectedType == TypeFilter.EXPENSE, "Expense") { selectedType = TypeFilter.EXPENSE; viewModel.onTypeFilterChanged(TypeFilter.EXPENSE) } }
                    item { FilterChip(selectedType == TypeFilter.INCOME, "Income") { selectedType = TypeFilter.INCOME; viewModel.onTypeFilterChanged(TypeFilter.INCOME) } }
                    item { FilterChip(selectedType == TypeFilter.THIRD_PARTY, "Third Party") { selectedType = TypeFilter.THIRD_PARTY; viewModel.onTypeFilterChanged(TypeFilter.THIRD_PARTY) } }

                    item { Spacer(modifier = Modifier.width(1.dp).height(20.dp).background(Color.LightGray)) }

                    // Date Filters (Fixed Logic)
                    item { FilterChip(selectedDate == DateFilter.ALL_TIME, "All Time") { selectedDate = DateFilter.ALL_TIME; viewModel.onDateFilterChanged(DateFilter.ALL_TIME) } }
                    item { FilterChip(selectedDate == DateFilter.THIS_MONTH, "This Month") { selectedDate = DateFilter.THIS_MONTH; viewModel.onDateFilterChanged(DateFilter.THIS_MONTH) } }
                    item { FilterChip(selectedDate == DateFilter.LAST_MONTH, "Last Month") { selectedDate = DateFilter.LAST_MONTH; viewModel.onDateFilterChanged(DateFilter.LAST_MONTH) } }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0)) // Fixed Divider
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
                        Text("No records found.", color = Color.Gray)
                    }
                }
            } else {
                groupedTransactions.forEach { (dateHeader, transactions) ->
                    // STICKY HEADER
                    stickyHeader {
                        Surface(color = BackgroundGray, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = dateHeader.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // ITEMS
                    items(transactions) { tx ->
                        HubTransactionItem(tx) { onTransactionClick(tx.id) }
                    }
                }
            }
        }
    }
}

// Updated Helper: Now takes a simple click action
@Composable
fun FilterChip(isSelected: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Slate else SurfaceWhite,
        contentColor = if (isSelected) Color.White else Slate,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (isSelected) Slate else Color(0xFFE2E8F0)),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun HubTransactionItem(tx: Transaction, onClick: () -> Unit) {
    val isExpense = tx.type == TransactionType.EXPENSE
    val isIncome = tx.type == TransactionType.INCOME
    val color = when(tx.type) {
        TransactionType.INCOME -> Color(0xFF2E7D32)
        TransactionType.EXPENSE -> Color(0xFFD32F2F)
        TransactionType.EXCHANGE -> Color(0xFF673AB7)
        else -> Color(0xFFF57C00)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Slate)
            if (tx.description.isNotEmpty()) {
                Text(tx.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
            }
        }

        // Amount
        Text(
            text = "${if (isExpense) "-" else "+"}${formatCurrency(tx.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32)
        )
    }
    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp) // Fixed Divider
}