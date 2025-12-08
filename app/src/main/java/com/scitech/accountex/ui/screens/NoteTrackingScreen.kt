package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scitech.accountex.data.CurrencyNote
import com.scitech.accountex.data.NoteStatus
import com.scitech.accountex.viewmodel.NoteTrackingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTrackingScreen(
    viewModel: NoteTrackingViewModel,
    onNavigateBack: () -> Unit
) {
    val notes by viewModel.activeNotes.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Tracking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, "Search Note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Note")
            }
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No active notes", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Active Notes: ${notes.size}", fontWeight = FontWeight.Bold)
                            Text("Total Value: ₹${notes.sumOf { it.denomination }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val groupedNotes = notes.groupBy { it.denomination }.toSortedMap(reverseOrder())
                groupedNotes.forEach { (denom, noteList) ->
                    item {
                        Text("₹$denom Notes (${noteList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(noteList) { note ->
                        NoteCard(note, accounts.find { it.id == note.accountId }?.name ?: "Unknown")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNoteDialog(
            accounts = accounts,
            onDismiss = { showAddDialog = false },
            onConfirm = { serial, denom, accId ->
                viewModel.addNote(serial, denom, accId, 0)
                showAddDialog = false
            }
        )
    }

    if (showSearchDialog) {
        SearchNoteDialog(
            viewModel = viewModel,
            onDismiss = { showSearchDialog = false }
        )
    }
}

@Composable
fun NoteCard(note: CurrencyNote, accountName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("₹${note.denomination}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(note.status.name, style = MaterialTheme.typography.bodySmall)
            }
            Text("Serial: ${note.serialNumber}", style = MaterialTheme.typography.bodyMedium)
            Text("Account: $accountName", style = MaterialTheme.typography.bodySmall)
            Text("Received: ${formatDate(note.receivedDate)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AddNoteDialog(
    accounts: List<com.scitech.accountex.data.Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    var serial by remember { mutableStateOf("") }
    var denom by remember { mutableStateOf("500") }
    var accId by remember { mutableIntStateOf(if (accounts.isNotEmpty()) accounts.first().id else 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Currency Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = serial, onValueChange = { serial = it }, label = { Text("Serial Number") })
                OutlinedTextField(value = denom, onValueChange = { denom = it }, label = { Text("Denomination") })
                Text("Account:")
                accounts.forEach { acc ->
                    FilterChip(
                        selected = accId == acc.id,
                        onClick = { accId = acc.id },
                        label = { Text(acc.name) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(serial, denom.toIntOrNull() ?: 500, accId) },
                enabled = serial.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SearchNoteDialog(
    viewModel: NoteTrackingViewModel,
    onDismiss: () -> Unit
) {
    var searchSerial by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<CurrencyNote?>(null) }
    var searched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchSerial,
                    onValueChange = { searchSerial = it },
                    label = { Text("Serial Number") }
                )
                Button(
                    onClick = {
                        kotlinx.coroutines.GlobalScope.launch {
                            searchResult = viewModel.searchNoteBySerial(searchSerial)
                            searched = true
                        }
                    },
                    enabled = searchSerial.isNotBlank()
                ) {
                    Text("Search")
                }
                if (searched) {
                    if (searchResult != null) {
                        Text("Found: ₹${searchResult!!.denomination}", fontWeight = FontWeight.Bold)
                        Text("Status: ${searchResult!!.status.name}")
                        Text("Received: ${formatDate(searchResult!!.receivedDate)}")
                    } else {
                        Text("Note not found", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}