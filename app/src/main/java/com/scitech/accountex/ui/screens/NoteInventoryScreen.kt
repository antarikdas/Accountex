package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import com.scitech.accountex.viewmodel.NoteInventoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInventoryScreen(
    viewModel: NoteInventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()

    // Consuming the logic-fixed data from ViewModel
    val inventorySummary by viewModel.inventorySummary.collectAsState()
    val totalValue by viewModel.totalValue.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var searchSerial by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var searchResult by remember { mutableStateOf<String?>(null) }

    BackHandler { onNavigateBack() }

    // Helper for date formatting
    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Inventory", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, "Find Note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Account Selector
            Text("Select Account", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { viewModel.selectAccount(account.id) },
                        label = { Text(account.name) }
                    )
                }
            }

            if (selectedAccountId != 0) {
                // 2. Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Total Cash in Hand", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "₹${totalValue.toInt()}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${inventorySummary.values.sumOf { it.size }} active notes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider()

                // 3. Inventory List (Grouped by Denomination)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    inventorySummary.forEach { (denomination, notes) ->
                        item {
                            // Header for Denomination
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "₹$denomination",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${notes.size} pcs • ₹${notes.size * denomination}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // List of Notes for this denomination
                        items(notes) { note ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = note.serialNumber.ifEmpty { "No Serial" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Added: ${formatDate(note.receivedDate)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (inventorySummary.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No cash notes in this account.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select an account to view inventory")
                }
            }
        }
    }

    // Search Dialog
    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false; searchResult = null; searchSerial = "" },
            title = { Text("Find Note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchSerial,
                        onValueChange = { searchSerial = it },
                        label = { Text("Enter Serial Number") },
                        singleLine = true
                    )
                    searchResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(result, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val note = viewModel.searchNote(searchSerial.trim())
                        searchResult = if (note != null) {
                            val status = if (note.spentTransactionId == null) "Active in Account #${note.accountId}" else "Spent on ${formatDate(note.spentDate ?: 0)}"
                            "Found: ₹${note.denomination}\nStatus: $status"
                        } else {
                            "Note not found."
                        }
                    }
                }) { Text("Search") }
            },
            dismissButton = {
                TextButton(onClick = { showSearch = false; searchResult = null; searchSerial = "" }) {
                    Text("Close")
                }
            }
        )
    }
}