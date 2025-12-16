package com.scitech.accountex.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.viewmodel.AddTransactionViewModel
import com.scitech.accountex.viewmodel.TemplateViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    templateViewModel: TemplateViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val templates by templateViewModel.templates.collectAsState()
    val appliedTemplate by viewModel.appliedTemplate.collectAsState()

    // Form State
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableIntStateOf(0) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    // New Features State
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedDenomination by remember { mutableIntStateOf(500) } // Default to 500
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Note Inventory State
    var noteSerials by remember { mutableStateOf("") }
    var showNoteInput by remember { mutableStateOf(false) }
    var showNoteSelector by remember { mutableStateOf(false) }
    val availableNotes by viewModel.availableNotes.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()

    // Image Picker Launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImageUris = uris
    }

    // Handle back button
    BackHandler { onNavigateBack() }

    // Apply template
    LaunchedEffect(appliedTemplate) {
        appliedTemplate?.let { template ->
            amount = template.defaultAmount.toString()
            category = template.category
            selectedAccountId = template.accountId
        }
    }

    // Set default account
    LaunchedEffect(accounts) {
        if (selectedAccountId == 0 && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Templates
            if (templates.isNotEmpty()) {
                Text("Quick Templates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates.take(5)) { template ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                amount = template.defaultAmount.toString()
                                category = template.category
                                selectedAccountId = template.accountId
                            },
                            label = { Text("${template.name} ₹${template.defaultAmount.toInt()}") }
                        )
                    }
                }
            }

            // Transaction Type Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER).forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name.lowercase().capitalize(Locale.ROOT)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹ ") },
                singleLine = true
            )

            // ✅ Date & Time Picker (NEW)
            OutlinedButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = selectedDate
                    DatePickerDialog(context, { _, year, month, day ->
                        TimePickerDialog(context, { _, hour, minute ->
                            calendar.set(year, month, day, hour, minute)
                            selectedDate = calendar.timeInMillis
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(selectedDate))}")
            }

            // Note Input for INCOME
            if (selectedType == TransactionType.INCOME) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Track Note Numbers", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showNoteInput, onCheckedChange = { showNoteInput = it })
                }

                if (showNoteInput) {
                    // ✅ Denomination Chips (NEW - Critical Logic Fix)
                    Text("Select Denomination:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(2000, 500, 200, 100, 50, 20, 10).forEach { denom ->
                            FilterChip(
                                selected = selectedDenomination == denom,
                                onClick = { selectedDenomination = denom },
                                label = { Text("₹$denom") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteSerials,
                        onValueChange = { noteSerials = it },
                        label = { Text("Note Serial Numbers") },
                        placeholder = { Text("Enter serials (comma or new line separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }

            // Note Selector for EXPENSE
            if (selectedType == TransactionType.EXPENSE && selectedAccountId != 0) {
                LaunchedEffect(selectedAccountId) { viewModel.loadNotesForAccount(selectedAccountId) }
                if (availableNotes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Notes to Spend (${selectedNoteIds.size})", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { showNoteSelector = !showNoteSelector }) {
                            Text(if (showNoteSelector) "Hide" else "Show")
                        }
                    }
                    if (showNoteSelector) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                availableNotes.groupBy { it.denomination }.forEach { (denom, notes) ->
                                    Text("₹$denom (${notes.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                    notes.forEach { note ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = selectedNoteIds.contains(note.id),
                                                onCheckedChange = { viewModel.toggleNoteSelection(note.id) }
                                            )
                                            Text(note.serialNumber, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category & Description
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // ✅ Image Attachments (NEW)
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedImageUris.isEmpty()) "Attach Images" else "${selectedImageUris.size} Images Selected")
            }

            if (selectedImageUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris - uri },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Account Selector
            ExposedDropdownMenuBox(
                expanded = showAccountDropdown,
                onExpandedChange = { showAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.name} (₹${account.balance})") },
                            onClick = {
                                selectedAccountId = account.id
                                showAccountDropdown = false
                            }
                        )
                    }
                }
            }

            // Save Template Button
            OutlinedButton(
                onClick = {
                    if (amount.isNotBlank() && category.isNotBlank()) {
                        templateViewModel.saveAsTemplate(category, category, amount.toDoubleOrNull() ?: 0.0, selectedAccountId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank() && category.isNotBlank()
            ) { Text("Save as Template") }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Transaction Button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0 && category.isNotBlank() && selectedAccountId != 0) {
                        viewModel.addTransaction(
                            type = selectedType,
                            amount = amountValue,
                            category = category.trim(),
                            description = description.trim(),
                            accountId = selectedAccountId,
                            noteSerials = if (selectedType == TransactionType.INCOME) noteSerials else "",
                            date = selectedDate,                // ✅ Pass Date
                            noteDenomination = selectedDenomination, // ✅ Pass Denomination (Fix)
                            imageUris = selectedImageUris.map { it.toString() } // ✅ Pass Images
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0 && category.isNotBlank() && selectedAccountId != 0
            ) {
                Text("Save Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}