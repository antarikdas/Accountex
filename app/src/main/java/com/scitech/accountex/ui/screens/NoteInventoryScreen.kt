package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scitech.accountex.viewmodel.NoteInventoryViewModel
import com.scitech.accountex.utils.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInventoryScreen(
    viewModel: NoteInventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val activeNotes by viewModel.activeNotes.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Inventory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back") }
                },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, "Search")
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select Account", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { viewModel.selectAccount(account.id) },
                        label = { Text(account.name) }
                    )
                }
            }

            if (selectedAccountId != 0) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Notes: ${activeNotes.size}", fontWeight = FontWeight.Bold)
                        Text("Total Value: ₹${activeNotes.sumOf { it.amount.toInt() }}")                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val grouped = activeNotes.groupBy { it.denomination }.toSortedMap(reverseOrder())
                    grouped.forEach { (denom, notes) ->
                        item {
                            Text("₹$denom (${notes.size} notes)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(notes) { note ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(note.serialNumber, fontWeight = FontWeight.Medium)
                                        Text("Received: ${formatDate(note.receivedDate)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("₹${note.amount.toInt()}", style = MaterialTheme.typography.titleLarge)                                }
                            }
                        }
                    }
                }
            }
        }
    }
}