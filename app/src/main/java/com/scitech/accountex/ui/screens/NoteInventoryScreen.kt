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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.data.CurrencyColors
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.InventoryItem
import com.scitech.accountex.viewmodel.NoteInventoryViewModel

// Theme Colors - Clean & Premium
private val VaultBg = Color(0xFFF8FAFC)
private val SlateText = Color(0xFF1E293B)
private val SelectedChip = Color(0xFF1E293B)
private val UnselectedChip = Color(0xFFFFFFFF)

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
            // 1. FILTERS (Account Selection)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    InventoryFilterChip(
                        label = "All Vaults",
                        isSelected = selectedAccountId == 0,
                        onClick = { viewModel.selectAccount(0) }
                    )
                }
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
                // 2. SUMMARY CARD (Premium Dark)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateText),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val currentAccountName = if (selectedAccountId == 0) "All Accounts" else accounts.find { it.id == selectedAccountId }?.name ?: "Account"
                            Text("Total Cash ($currentAccountName)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatCurrency(state.grandTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Box(modifier = Modifier.size(56.dp).background(Color.White.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.totalNotes.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Items", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // 3. HEADERS
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 8.dp, end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DENOMINATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("VALUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                // 4. LIST (With Premium Color Logic)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (state.items.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No cash found.", color = Color.Gray) } }
                    } else {
                        items(state.items) { item -> InventoryItemCard(item) }
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
        border = if(isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.4f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
    }
}

@Composable
fun InventoryItemCard(item: InventoryItem) {
    val isCoin = item.isCoin

    // PREMIUM COLOR LOGIC
    val brandColor = if (isCoin) CurrencyColors.Coin else getPremiumCurrencyColor(item.denomination)
    // Subtle background tint for notes, pure white for coins to look cleaner
    val containerColor = if (isCoin) Color.White else brandColor.copy(alpha = 0.05f)

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(alpha = 0.3f))
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
                        color = SlateText.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${item.count} pcs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText
                    )
                }

                // Value & Expand Icon
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatCurrency(item.totalValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = brandColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // DROPDOWN DETAILS (Only for Notes with Serials)
            if (expanded && !isCoin) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = brandColor.copy(alpha = 0.2f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.White.copy(0.6f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Serial Numbers", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

                    item.notes.forEach { note ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(brandColor, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(note.serialNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = SlateText, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }

                            if (note.isThirdParty) {
                                Text("Held (${note.thirdPartyName})", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PREMIUM COLOR PALETTE (Rich, Deep, Saturated) ---
fun getPremiumCurrencyColor(denom: Int): Color = when (denom) {
    // 500: Deep Slate Blue (Secure, Solid)
    500 -> Color(0xFF37474F)

    // 200: Rich Amber/Saffron (Wealth, Gold)
    200 -> Color(0xFFF57C00)

    // 100: Royal Deep Violet (Premium)
    100 -> Color(0xFF512DA8)

    // 50: Ocean Teal (Fresh, Modern)
    50 -> Color(0xFF00897B)

    // 20: Forest Green (Vibrant)
    20 -> Color(0xFF33691E)

    // 10: Copper Brown (Earthy)
    10 -> Color(0xFF5D4037)

    // Fallback
    else -> Color.Gray
}