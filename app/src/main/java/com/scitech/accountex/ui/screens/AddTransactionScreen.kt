package com.scitech.accountex.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.viewmodel.AddTransactionViewModel
import com.scitech.accountex.viewmodel.TemplateViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    templateViewModel: TemplateViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // OBSERVE STATE FROM VIEWMODEL
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // Suggestion Data
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()

    // Form Local UI State
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    // --- CAMERA LOGIC ---
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            val newList = selectedImageUris.toMutableList()
            newList.add(tempPhotoUri!!)
            selectedImageUris = newList
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val (file, uri) = createImageFile(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newList = selectedImageUris.toMutableList()
        newList.addAll(uris)
        selectedImageUris = newList
    }

    // Inventory State
    val availableNotes by viewModel.availableNotes.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val incomingNotes by viewModel.incomingNotes.collectAsState()

    // Temp Note Input
    var tempSerial by remember { mutableStateOf("") }
    var tempDenomination by remember { mutableIntStateOf(500) }
    var showNoteSelector by remember { mutableStateOf(false) }

    BackHandler { onNavigateBack() }

    // Logic: Calculate totals
    val incomingTotal = incomingNotes.sumOf { it.denomination }
    val selectedSpentTotal = availableNotes.filter { it.id in selectedNoteIds }.sumOf { it.amount }
    val transactionAmount = uiState.amountInput.toDoubleOrNull() ?: 0.0
    val changeRequired = if (uiState.selectedType == TransactionType.EXPENSE && selectedSpentTotal > transactionAmount) selectedSpentTotal - transactionAmount else 0.0

    // Auto-fill amount for income based on notes
    LaunchedEffect(incomingNotes) {
        if (uiState.selectedType == TransactionType.INCOME && incomingNotes.isNotEmpty()) {
            viewModel.updateAmount(incomingTotal.toString())
        }
    }
    // Auto-select first account
    LaunchedEffect(accounts) {
        if (uiState.selectedAccountId == 0 && accounts.isNotEmpty()) {
            viewModel.updateAccount(accounts.first().id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. Transaction Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeButton(
                    text = "Expense",
                    isSelected = uiState.selectedType == TransactionType.EXPENSE,
                    onClick = { viewModel.updateType(TransactionType.EXPENSE) }
                )
                TypeButton(
                    text = "Income",
                    isSelected = uiState.selectedType == TransactionType.INCOME,
                    onClick = { viewModel.updateType(TransactionType.INCOME) }
                )
            }

            // 2. Huge Amount Input
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Enter Amount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = uiState.amountInput,
                    onValueChange = { viewModel.updateAmount(it) },
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        color = if(uiState.selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    placeholder = {
                        Text("0",
                            style = MaterialTheme.typography.displayMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    readOnly = uiState.selectedType == TransactionType.INCOME && incomingNotes.isNotEmpty()
                )
            }

            // 3. Category & Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Date Picker
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = uiState.selectedDate
                            DatePickerDialog(context, { _, y, m, d ->
                                TimePickerDialog(context, { _, h, min ->
                                    calendar.set(y, m, d, h, min)
                                    viewModel.updateDate(calendar.timeInMillis)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(uiState.selectedDate)),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Account Selector
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { showAccountDropdown = true }
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Account", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    accounts.find { it.id == uiState.selectedAccountId }?.name ?: "Select Account",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showAccountDropdown,
                            onDismissRequest = { showAccountDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.name} (₹${account.balance})") },
                                    onClick = { viewModel.updateAccount(account.id); showAccountDropdown = false }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // --- CATEGORY ---
                    Column {
                        CleanInputRow(
                            label = "Category",
                            value = uiState.category,
                            onValueChange = { viewModel.updateCategory(it) },
                            icon = Icons.Default.Category
                        )
                        val filteredCategories = categorySuggestions.filter {
                            it.contains(uiState.category, ignoreCase = true)
                        }

                        if (filteredCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredCategories) { suggestion ->
                                    SuggestionChip(
                                        onClick = { viewModel.updateCategory(suggestion) },
                                        label = { Text(suggestion) }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // --- DESCRIPTION ---
                    Column {
                        CleanInputRow(
                            label = "Description",
                            value = uiState.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            icon = Icons.Default.Description
                        )
                        if (uiState.description.isEmpty() && descriptionSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(descriptionSuggestions) { suggestion ->
                                    SuggestionChip(
                                        onClick = { viewModel.updateDescription(suggestion) },
                                        label = { Text(suggestion) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. CASH INVENTORY LOGIC
            if (uiState.selectedType == TransactionType.INCOME) {
                CashManagementCard(title = "Cash Received (Inventory)", icon = Icons.Default.Money) {
                    NoteInputRow(
                        denomination = tempDenomination,
                        serial = tempSerial,
                        onDenomChange = { tempDenomination = it },
                        onSerialChange = { tempSerial = it },
                        onAdd = { viewModel.addIncomingNote(tempSerial, tempDenomination); tempSerial = "" }
                    )
                    AddedNotesList(incomingNotes) { idx -> viewModel.removeIncomingNote(idx) }
                }
            } else {
                CashManagementCard(title = "Pay with Cash", icon = Icons.Default.Wallet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selected: ₹${selectedSpentTotal.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { showNoteSelector = !showNoteSelector }) {
                            Text(if (showNoteSelector) "Hide Notes" else "Select Notes")
                        }
                    }

                    if (showNoteSelector && availableNotes.isNotEmpty()) {
                        Box(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth().padding(vertical = 8.dp)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                availableNotes.groupBy { it.denomination }.forEach { (denom, notes) ->
                                    Text("₹$denom Notes", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                                    notes.forEach { note ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleNoteSelection(note.id) }) {
                                            Checkbox(
                                                checked = selectedNoteIds.contains(note.id),
                                                onCheckedChange = { viewModel.toggleNoteSelection(note.id) }
                                            )
                                            Text("${note.serialNumber} (ID: ${note.id})", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (changeRequired > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Change Needed: ₹${changeRequired.toInt()}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        NoteInputRow(
                            denomination = tempDenomination,
                            serial = tempSerial,
                            onDenomChange = { tempDenomination = it },
                            onSerialChange = { tempSerial = it },
                            onAdd = { viewModel.addIncomingNote(tempSerial, tempDenomination); tempSerial = "" }
                        )
                        AddedNotesList(incomingNotes) { idx -> viewModel.removeIncomingNote(idx) }
                    }
                }
            }

            // 5. ATTACHMENTS SECTION
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // GALLERY BUTTON
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }

                // CAMERA BUTTON
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val (file, uri) = createImageFile(context)
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.CameraAlt, null)
                }
            }

            if (selectedImageUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray)
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }

        // Floating Save Button
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.BottomCenter) {
            val isValid = if (uiState.selectedType == TransactionType.INCOME) transactionAmount > 0 else (transactionAmount > 0 && (if (changeRequired > 0) incomingTotal.toDouble() == changeRequired else true))

            Button(
                onClick = {
                    if (isValid) {
                        viewModel.addTransaction(selectedImageUris.map { it.toString() })
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = if(uiState.selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            ) {
                Text("Save Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- UPDATED HELPER FUNCTION FOR CAMERA ---
fun createImageFile(context: Context): Pair<File, Uri> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    // Create a dedicated folder "Accountex_Captures" inside the app's external files directory
    val storageDir = File(context.getExternalFilesDir(null), "Accountex_Captures")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    val file = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    return Pair(file, uri)
}

// --- REUSABLE COMPONENTS ---
@Composable
fun TypeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CleanInputRow(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BasicTextField(value: String, onValueChange: (String) -> Unit, textStyle: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun CashManagementCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun NoteInputRow(denomination: Int, serial: String, onDenomChange: (Int) -> Unit, onSerialChange: (String) -> Unit, onAdd: () -> Unit) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(500, 200, 100, 50, 20, 10).forEach { denom ->
                item {
                    FilterChip(
                        selected = denomination == denom,
                        onClick = { onDenomChange(denom) },
                        label = { Text("₹$denom") }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = serial,
                onValueChange = onSerialChange,
                label = { Text("Serial (Optional)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.AddCircle, "Add", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun AddedNotesList(notes: List<com.scitech.accountex.viewmodel.DraftNote>, onRemove: (Int) -> Unit) {
    if (notes.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            notes.forEachIndexed { index, note ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("₹${note.denomination} - ${note.serial.ifEmpty { "No Serial" }}")
                    Icon(
                        Icons.Outlined.Close, "Remove",
                        modifier = Modifier.clickable { onRemove(index) },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}