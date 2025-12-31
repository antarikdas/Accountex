package com.scitech.accountex.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AccountType
import com.scitech.accountex.ui.theme.AppTheme
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.ManageAccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    viewModel: ManageAccountsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val errorMsg by viewModel.errorEvent.collectAsState()
    val context = LocalContext.current

    // 🧠 SYSTEM THEME
    val colors = AppTheme.colors

    // Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(errorMsg) {
        if (errorMsg != null) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Wallets", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.actionDominant, // Gold/Attention
                contentColor = colors.textInverse,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, "Add Account")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            items(accounts) { account ->
                PremiumAccountItem(
                    account = account,
                    onEdit = { accountToEdit = account },
                    onDelete = { viewModel.deleteAccount(account) }
                )
            }
        }
    }

    // --- DIALOGS ---
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, balance ->
                viewModel.addAccount(name, type, balance)
                showAddDialog = false
            }
        )
    }

    if (accountToEdit != null) {
        EditAccountDialog(
            account = accountToEdit!!,
            onDismiss = { accountToEdit = null },
            onConfirm = { newName ->
                viewModel.updateAccountName(accountToEdit!!, newName)
                accountToEdit = null
            }
        )
    }
}

@Composable
fun PremiumAccountItem(account: Account, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = AppTheme.colors

    // 🧠 PSYCHOLOGICAL MAPPING
    // Bank = Trust (Primary/Teal)
    // Cash = Flow (Income/Green)
    // Reserve = Vault (Secondary/Purple)
    val themeColor = when(account.type) {
        AccountType.BANK -> colors.brandPrimary
        AccountType.CASH_DAILY -> colors.income
        else -> colors.brandSecondary
    }

    val icon = when(account.type) {
        AccountType.BANK -> Icons.Outlined.AccountBalance
        else -> Icons.Outlined.Wallet
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(themeColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = themeColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    account.type.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            // Actions & Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(account.balance), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    // Edit Action
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                        .background(colors.surfaceHighlight), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Delete Action
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .background(colors.expense.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = colors.expense, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onConfirm: (String, AccountType, Double) -> Unit) {
    val colors = AppTheme.colors
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.dp, colors.divider)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("New Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.brandPrimary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )

                Column {
                    Text("Type", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeChip(selectedType == AccountType.BANK, "Bank") { selectedType = AccountType.BANK }
                        TypeChip(selectedType == AccountType.CASH_DAILY, "Cash") { selectedType = AccountType.CASH_DAILY }
                        TypeChip(selectedType == AccountType.CASH_RESERVE, "Reserve") { selectedType = AccountType.CASH_RESERVE }
                    }
                }

                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Initial Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.brandPrimary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotEmpty()) onConfirm(name, selectedType, balance.toDoubleOrNull() ?: 0.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                    ) { Text("Create Wallet", color = colors.textInverse) }
                }
            }
        }
    }
}

@Composable
fun EditAccountDialog(account: Account, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val colors = AppTheme.colors
    var name by remember { mutableStateOf(account.name) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.dp, colors.divider)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Rename Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.brandPrimary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (name.isNotEmpty()) onConfirm(name) }, colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)) { Text("Update", color = colors.textInverse) }
                }
            }
        }
    }
}

@Composable
fun TypeChip(isSelected: Boolean, label: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        color = if (isSelected) colors.brandPrimary else colors.surfaceHighlight,
        contentColor = if (isSelected) colors.textInverse else colors.textSecondary,
        shape = CircleShape,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}