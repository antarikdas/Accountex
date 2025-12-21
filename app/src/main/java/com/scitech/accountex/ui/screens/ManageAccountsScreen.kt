package com.scitech.accountex.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import com.scitech.accountex.data.Account
import com.scitech.accountex.data.AccountType
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.ManageAccountsViewModel

// --- PREMIUM THEME COLORS ---
private val Slate = Color(0xFF1E293B)
private val BackgroundGray = Color(0xFFF8FAFC)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val BankBlue = Color(0xFF3B82F6)
private val CashGreen = Color(0xFF10B981)
private val ReserveAmber = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    viewModel: ManageAccountsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val errorMsg by viewModel.errorEvent.collectAsState()
    val context = LocalContext.current

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
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Wallets", fontWeight = FontWeight.Bold, color = Slate) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundGray)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Slate,
                contentColor = Color.White,
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
    val themeColor = when(account.type) {
        AccountType.BANK -> BankBlue
        AccountType.CASH_DAILY -> CashGreen
        else -> ReserveAmber
    }

    val icon = when(account.type) {
        AccountType.BANK -> Icons.Outlined.AccountBalance
        else -> Icons.Outlined.Wallet
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate)
                Text(
                    account.type.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Actions & Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(account.balance), fontWeight = FontWeight.Bold, color = Slate)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                        .background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = Slate, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .background(Color(0xFFFEF2F2)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onConfirm: (String, AccountType, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("New Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate, unfocusedBorderColor = Color(0xFFE2E8F0))
                )

                Column {
                    Text("Type", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate, unfocusedBorderColor = Color(0xFFE2E8F0))
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotEmpty()) onConfirm(name, selectedType, balance.toDoubleOrNull() ?: 0.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate)
                    ) { Text("Create Wallet") }
                }
            }
        }
    }
}

@Composable
fun EditAccountDialog(account: Account, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(account.name) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Rename Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate, unfocusedBorderColor = Color(0xFFE2E8F0))
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (name.isNotEmpty()) onConfirm(name) }, colors = ButtonDefaults.buttonColors(containerColor = Slate)) { Text("Update") }
                }
            }
        }
    }
}

@Composable
fun TypeChip(isSelected: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Slate else Color(0xFFF1F5F9),
        contentColor = if (isSelected) Color.White else Color.Gray,
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