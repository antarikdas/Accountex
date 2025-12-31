package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.scitech.accountex.ui.theme.AccountexColors
import com.scitech.accountex.ui.theme.AppTheme
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.InventoryItem
import com.scitech.accountex.viewmodel.NoteInventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInventoryScreen(
    viewModel: NoteInventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.inventoryState.collectAsState()
    val accounts by viewModel.availableAccounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()

    // 🧠 SYSTEM THEME ACCESS
    val colors = AppTheme.colors

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cash Vault", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. FILTERS (Account Selection)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    InventoryFilterChip(
                        label = "All Vaults",
                        isSelected = selectedAccountId == 0,
                        colors = colors,
                        onClick = { viewModel.selectAccount(0) }
                    )
                }
                items(accounts) { account ->
                    InventoryFilterChip(
                        label = account.name,
                        isSelected = selectedAccountId == account.id,
                        colors = colors,
                        onClick = { viewModel.selectAccount(account.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // 2. SUMMARY CARD (High Contrast Asset Overview)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceCard
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, colors.divider)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val currentAccountName = if (selectedAccountId == 0) "All Accounts" else accounts.find { it.id == selectedAccountId }?.name ?: "Account"
                            Text("Total Cash ($currentAccountName)", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatCurrency(state.grandTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Box(modifier = Modifier.size(56.dp).background(colors.brandPrimary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.totalNotes.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                                Text("Items", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // 3. HEADERS
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 8.dp, end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DENOMINATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("VALUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }

                // 4. LIST (With Premium Color Logic)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (state.items.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No cash found.", color = colors.textSecondary)
                            }
                        }
                    } else {
                        items(state.items) { item -> InventoryItemCard(item, colors) }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryFilterChip(label: String, isSelected: Boolean, colors: AccountexColors, onClick: () -> Unit) {
    // Adaptive Filter Chip Colors
    val selectedContainer = colors.brandPrimary
    val unselectedContainer = colors.surfaceCard
    val selectedText = colors.textInverse
    val unselectedText = colors.textSecondary

    Surface(
        color = if (isSelected) selectedContainer else unselectedContainer,
        contentColor = if (isSelected) selectedText else unselectedText,
        shape = CircleShape,
        border = if(!isSelected) BorderStroke(1.dp, colors.divider) else null,
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
fun InventoryItemCard(item: InventoryItem, colors: AccountexColors) {
    val isCoin = item.isCoin

    // 🧠 CENTRALIZED CURRENCY LOGIC
    val brandColor = if (isCoin) colors.currencyCoin else getCurrencyTierColor(item.denomination, colors)

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)) // Glow border for value perception
    ) {
        Column {
            // MAIN ROW
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Denomination Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(if(isCoin) CircleShape else RoundedCornerShape(10.dp))
                        .background(brandColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "₹${item.denomination}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = brandColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isCoin) "Coin Stack" else "Bank Note",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${item.count} pcs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                // Value & Expand Icon
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatCurrency(item.totalValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = brandColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // DROPDOWN DETAILS
            if (expanded && !isCoin) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(colors.surfaceHighlight, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Serial Numbers", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))

                    item.notes.forEach { note ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(brandColor, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    note.serialNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (note.isThirdParty) {
                                Text("Held (${note.thirdPartyName})", style = MaterialTheme.typography.labelSmall, color = colors.expense, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🧠 TIERED COLOR RESOLVER
fun getCurrencyTierColor(denom: Int, colors: AccountexColors): Color = when (denom) {
    500 -> colors.currencyTier1
    200 -> colors.currencyTier2
    100 -> colors.currencyTier3
    50 -> colors.currencyTier4
    20 -> colors.currencyTier5
    10 -> colors.currencyTier5
    else -> colors.textSecondary
}