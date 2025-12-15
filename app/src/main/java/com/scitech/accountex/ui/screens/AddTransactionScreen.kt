package com.scitech.accountex.ui.screens
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.viewmodel.AddTransactionViewModel
import com.scitech.accountex.viewmodel.TemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    templateViewModel: TemplateViewModel,  // ADDED: Template support
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val templates by templateViewModel.templates.collectAsState()
    val appliedTemplate by viewModel.appliedTemplate.collectAsState()

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var noteSerials by remember { mutableStateOf("") }
    var showNoteInput by remember { mutableStateOf(false) }
    var showNoteSelector by remember { mutableStateOf(false) }
    val availableNotes by viewModel.availableNotes.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    var selectedAccountId by remember { mutableIntStateOf(0) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    // Handle back button
    BackHandler {
        onNavigateBack()
    }

    // Apply template when selected
    LaunchedEffect(appliedTemplate) {
        appliedTemplate?.let { template ->
            amount = template.defaultAmount.toString()
            category = template.category
            selectedAccountId = template.accountId
        }
    }

    // Set default account when accounts load
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
            // Quick Templates Section (NEW)
            if (templates.isNotEmpty()) {
                Text(
                    "Quick Templates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates.take(5)) { template ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                amount = template.defaultAmount.toString()
                                category = template.category
                                selectedAccountId = template.accountId
                            },
                            label = {
                                Text("${template.name} ₹${template.defaultAmount.toInt()}")
                            }
                        )
                    }
                }
            }

            // Transaction Type Selector
            Text(
                "Transaction Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    label = { Text("Income") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    label = { Text("Expense") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = { selectedType = TransactionType.TRANSFER },
                    label = { Text("Transfer") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Amount Input
            OutlinedTextField(

                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                placeholder = { Text("Enter amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹ ") },
                singleLine = true
            )
// Note input for INCOME
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
            // Note selector for EXPENSE
            if (selectedType == TransactionType.EXPENSE && selectedAccountId != 0) {
                LaunchedEffect(selectedAccountId) {
                    viewModel.loadNotesForAccount(selectedAccountId)
                }

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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
            // Category Input
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text("e.g., Food, Transport, Salary") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Add notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Account Selector
            Text(
                "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = showAccountDropdown,
                onExpandedChange = { showAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(account.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "Balance: ₹${account.balance}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            onClick = {
                                selectedAccountId = account.id
                                showAccountDropdown = false
                            }
                        )
                    }
                }
            }

            // Save as Template Button (NEW)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (amount.isNotBlank() && category.isNotBlank()) {
                            templateViewModel.saveAsTemplate(
                                name = category,
                                category = category,
                                defaultAmount = amount.toDoubleOrNull() ?: 0.0,
                                accountId = selectedAccountId
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = amount.isNotBlank() && category.isNotBlank()
                ) {
                    Text("Save as Template")
                }
            }

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
                            noteSerials = if (selectedType == TransactionType.INCOME) noteSerials else ""
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amount.toDoubleOrNull() != null &&
                        amount.toDoubleOrNull()!! > 0 &&
                        category.isNotBlank() &&
                        selectedAccountId != 0
            ) {
                Text(
                    "Save Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // NOTE: If you have image attachment functionality,
            // add it here in the same column, before the Save button
            // Example:
            // ImageAttachmentSection(...)
        }
    }
}