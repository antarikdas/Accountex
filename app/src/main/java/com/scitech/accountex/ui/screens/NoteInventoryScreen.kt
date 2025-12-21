package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.InventoryItem
import com.scitech.accountex.viewmodel.NoteInventoryViewModel

// Theme: The Digital Vault
private val VaultBg = Color(0xFFF1F5F9)
private val SlateText = Color(0xFF1E293B)
private val NoteColor = Color(0xFF10B981) // Green for Notes
private val CoinColor = Color(0xFFF59E0B) // Gold for Coins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInventoryScreen(
    viewModel: NoteInventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.inventoryState.collectAsState()
    val selectedAccount by viewModel.selectedAccountId.collectAsState()

    // For Filter Dialog (Optional expansion)
    var showFilterMenu by remember { mutableStateOf(false) }

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
                actions = {
                    IconButton(onClick = { viewModel.selectAccount(if (selectedAccount == 0) 1 else 0) }) {
                        // Simple toggle for now: All vs Filtered (You can expand this to a full menu)
                        Icon(Icons.Default.FilterList, "Filter", tint = if(selectedAccount != 0) NoteColor else Color.Gray)
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
                .padding(horizontal = 24.dp)
        ) {
            // 1. TOTAL SUMMARY CARD
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
                        Text("Total Cash Held", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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

            // 2. HEADERS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("DENOMINATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("TOTAL VALUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            }

            // 3. INVENTORY LIST
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.items) { item ->
                    InventoryItemCard(item)
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(item: InventoryItem) {
    val isCoin = item.isCoin
    val mainColor = if (isCoin) CoinColor else NoteColor
    val icon = if (isCoin) Icons.Default.MonetizationOn else Icons.Default.MonetizationOn // Or use different icons

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
            // Icon
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