package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scitech.accountex.data.Account
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.InventoryItem
import com.scitech.accountex.viewmodel.NoteInventoryViewModel

// Theme Colors
private val VaultBg = Color(0xFFF1F5F9)
private val SlateText = Color(0xFF1E293B)
private val NoteColor = Color(0xFF10B981)
private val CoinColor = Color(0xFFF59E0B)
private val SelectedChip = Color(0xFF1E293B) // Dark Slate for selected
private val UnselectedChip = Color(0xFFFFFFFF) // White for unselected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInventoryScreen(
    viewModel: NoteInventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.inventoryState.collectAsState()
    val accounts by viewModel.availableAccounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = VaultBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cash Inventory", fontWeight = FontWeight.Bold, color = SlateText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SlateText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = VaultBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // 1. ACCOUNT FILTER CHIPS (Horizontal Scroll)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "All" Chip
                item {
                    InventoryFilterChip(
                        label = "All Vaults",
                        isSelected = selectedAccountId == 0,
                        onClick = { viewModel.selectAccount(0) }
                    )
                }

                // Individual Account Chips
                items(accounts) { account ->
                    InventoryFilterChip(
                        label = account.name,
                        isSelected = selectedAccountId == account.id,
                        onClick = { viewModel.selectAccount(account.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // 2. TOTAL SUMMARY CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateText),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // Dynamic Label based on selection
                            val currentAccountName = if (selectedAccountId == 0) "All Accounts" else accounts.find { it.id == selectedAccountId }?.name ?: "Account"

                            Text("Total Value ($currentAccountName)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                formatCurrency(state.grandTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                state.totalNotes.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NoteColor
                            )
                        }
                    }
                }

                // 3. HEADERS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DENOMINATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("TOTAL VALUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                // 4. INVENTORY LIST
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (state.items.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No cash notes found in this wallet.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(state.items) { item ->
                            InventoryItemCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) SelectedChip else UnselectedChip,
        contentColor = if (isSelected) Color.White else SlateText,
        shape = CircleShape,
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun InventoryItemCard(item: InventoryItem) {
    val isCoin = item.isCoin
    val mainColor = if (isCoin) CoinColor else NoteColor
    // Using a different icon for notes vs coins creates a nice visual distinction
    val icon = if (isCoin) Icons.Default.MonetizationOn else Icons.Outlined.Description

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Stack
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(mainColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "₹${item.denomination}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = mainColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isCoin) "Coin Stack" else "Currency Note",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    "${item.count} pcs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText
                )
            }

            // Total Value
            Text(
                formatCurrency(item.totalValue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SlateText
            )
        }
    }
}