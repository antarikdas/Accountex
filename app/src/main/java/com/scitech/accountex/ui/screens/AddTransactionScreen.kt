package com.scitech.accountex.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.components.SmartInput
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
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // Suggestion Data
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showAttachmentOptions by remember { mutableStateOf(false) }

    // Camera/Gallery Logic
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) selectedImageUris = selectedImageUris + tempPhotoUri!!
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImageUris = selectedImageUris + uris
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val (file, uri) = createImageFile(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Inventory State
    val availableNotes by viewModel.availableNotes.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val incomingNotes by viewModel.incomingNotes.collectAsState()

    var tempSerial by remember { mutableStateOf("") }
    var tempDenomination by remember { mutableIntStateOf(500) }

    // Auto-show note selector if we are spending (Expense or Hand Over)
    var showNoteSelector by remember { mutableStateOf(false) }

    // LOGIC FIX: Ensure state updates correctly when type changes, but doesn't override manual user interaction
    LaunchedEffect(uiState.selectedType) {
        if (uiState.selectedType == TransactionType.EXPENSE || uiState.selectedType == TransactionType.THIRD_PARTY_OUT) {
            showNoteSelector = true // Default to ON for new expense entry
        }
    }

    BackHandler { onNavigateBack() }

    // Logic Variables
    val isThirdParty = uiState.selectedType == TransactionType.THIRD_PARTY_IN || uiState.selectedType == TransactionType.THIRD_PARTY_OUT
    val isIncomingFlow = uiState.selectedType == TransactionType.INCOME || uiState.selectedType == TransactionType.THIRD_PARTY_IN
    val isOutgoingFlow = uiState.selectedType == TransactionType.EXPENSE || uiState.selectedType == TransactionType.THIRD_PARTY_OUT

    val incomingTotal = incomingNotes.sumOf { it.denomination }
    val selectedSpentTotal = availableNotes.filter { it.id in selectedNoteIds }.sumOf { it.amount }
    val transactionAmount = uiState.amountInput.toDoubleOrNull() ?: 0.0
    val changeRequired = if (isOutgoingFlow && selectedSpentTotal > transactionAmount) selectedSpentTotal - transactionAmount else 0.0

    // Auto-Updates
    LaunchedEffect(incomingNotes) { if (isIncomingFlow && incomingNotes.isNotEmpty()) viewModel.updateAmount(incomingTotal.toString()) }
    LaunchedEffect(accounts) { if (uiState.selectedAccountId == 0 && accounts.isNotEmpty()) viewModel.updateAccount(accounts.first().id) }

    // Theme Color (Red=Expense, Green=Income, Amber=ThirdParty)
    val mainColor = when (uiState.selectedType) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFFFA000) // Amber for Third Party
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isThirdParty) "Third Party Money" else "New Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                mainColor.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Master Type Switcher (Personal vs Third Party)
                NeoMasterTypeSwitcher(
                    currentType = uiState.selectedType,
                    mainColor = mainColor,
                    onTypeSelected = { viewModel.updateType(it) }
                )

                // 2. Hero Amount Input
                NeoAmountInput(
                    value = uiState.amountInput,
                    onValueChange = { viewModel.updateAmount(it) },
                    mainColor = mainColor,
                    readOnly = isIncomingFlow && incomingNotes.isNotEmpty()
                )

                // 3. Date & Account
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeoDetailPanel(
                        label = "Date",
                        value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(uiState.selectedDate)),
                        icon = Icons.Rounded.CalendarToday,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                            DatePickerDialog(context, { _, y, m, d ->
                                TimePickerDialog(context, { _, h, min ->
                                    c.set(y, m, d, h, min)
                                    viewModel.updateDate(c.timeInMillis)
                                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        NeoDetailPanel(
                            label = "Account",
                            value = accounts.find { it.id == uiState.selectedAccountId }?.name ?: "Select",
                            icon = Icons.Rounded.Wallet,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showAccountDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showAccountDropdown,
                            onDismissRequest = { showAccountDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.name} (₹${account.balance})") },
                                    onClick = {
                                        viewModel.updateAccount(account.id)
                                        showAccountDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Input Fields
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // NEW: Third Party Name Field
                    AnimatedVisibility(visible = isThirdParty) {
                        NeoSmartInput(
                            label = "Related Party",
                            value = uiState.thirdPartyName,
                            onValueChange = { viewModel.updateThirdPartyName(it) },
                            icon = Icons.Rounded.Person,
                            suggestions = emptyList(), // Could add contacts later
                            placeholder = "Received from / Giving to..."
                        )
                    }

                    NeoSmartInput(
                        label = "Category",
                        value = uiState.category,
                        onValueChange = { viewModel.updateCategory(it) },
                        icon = Icons.Default.Category,
                        suggestions = categorySuggestions
                    )

                    NeoSmartInput(
                        label = "Description",
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        icon = Icons.Default.Description,
                        suggestions = descriptionSuggestions
                    )
                }

                // 5. Inventory Logic (Adaptive)
                // BUG FIX: We now allow visibility if it's an Outgoing flow, even if user toggled notes OFF.
                // This keeps the "Pay with Cash" header visible so they can toggle it back ON.
                AnimatedVisibility(visible = isIncomingFlow || isOutgoingFlow || incomingNotes.isNotEmpty()) {
                    NeoInventoryCard(
                        type = uiState.selectedType,
                        mainColor = mainColor
                    ) {
                        if (isIncomingFlow) {
                            // Receiving Money (Income OR Third Party In) -> ADD NOTES
                            NoteInputRow(tempDenomination, tempSerial, { tempDenomination = it }, { tempSerial = it }) {
                                viewModel.addIncomingNote(tempSerial, tempDenomination)
                                tempSerial = ""
                            }
                            AddedNotesList(incomingNotes) { viewModel.removeIncomingNote(it) }
                        } else {
                            // Spending Money (Expense OR Third Party Out) -> SELECT NOTES
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if(isThirdParty) "Select Held Cash" else "Pay with Cash",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = showNoteSelector,
                                    onCheckedChange = { showNoteSelector = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = mainColor, checkedTrackColor = mainColor.copy(alpha = 0.5f))
                                )
                            }

                            // The toggle now strictly controls only the LIST content, not the parent card.
                            if (showNoteSelector) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Selected: ₹${selectedSpentTotal.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    color = mainColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (availableNotes.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .heightIn(max = 250.dp)
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                            availableNotes.groupBy { it.denomination }.forEach { (d, n) ->
                                                Text(
                                                    "₹$d Notes",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                n.forEach { note ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { viewModel.toggleNoteSelection(note.id) }
                                                            .padding(4.dp)
                                                    ) {
                                                        Checkbox(
                                                            checked = selectedNoteIds.contains(note.id),
                                                            onCheckedChange = { viewModel.toggleNoteSelection(note.id) },
                                                            colors = CheckboxDefaults.colors(checkedColor = mainColor)
                                                        )
                                                        Text(
                                                            "${note.serialNumber} (ID: ${note.id})",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        if(isThirdParty) "No held notes available." else "No cash available.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (changeRequired > 0) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Change to Receive: ₹${changeRequired.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                NoteInputRow(tempDenomination, tempSerial, { tempDenomination = it }, { tempSerial = it }) {
                                    viewModel.addIncomingNote(tempSerial, tempDenomination)
                                    tempSerial = ""
                                }
                                AddedNotesList(incomingNotes) { viewModel.removeIncomingNote(it) }
                            }
                        }
                    }
                }

                // 6. Attachments
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAttachmentOptions = true }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Rounded.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add attachments", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }

                    if (selectedImageUris.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(selectedImageUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }

            // 7. Save Button
            val isValid = if (isIncomingFlow) transactionAmount > 0 else (transactionAmount > 0 && (if (changeRequired > 0) incomingTotal.toDouble() == changeRequired else true))
            val buttonColor = animateColorAsState(targetValue = if (isValid) mainColor else MaterialTheme.colorScheme.surfaceVariant, label = "btnColor")
            val contentColor = animateColorAsState(targetValue = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "cntColor")

            Button(
                onClick = { viewModel.addTransaction(selectedImageUris.map { it.toString() }); onNavigateBack() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor.value,
                    contentColor = contentColor.value,
                    disabledContainerColor = buttonColor.value,
                    disabledContentColor = contentColor.value
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isValid) 8.dp else 0.dp)
            ) {
                Text(
                    if(isThirdParty) "Save Record" else "Save Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Attachment Options Bottom Sheet / Dialog
        if (showAttachmentOptions) {
            AlertDialog(
                onDismissRequest = { showAttachmentOptions = false },
                title = { Text("Add Attachment") },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("Take Photo") },
                            leadingContent = { Icon(Icons.Outlined.CameraAlt, null) },
                            modifier = Modifier.clickable {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    val (f, u) = createImageFile(context)
                                    tempPhotoUri = u
                                    cameraLauncher.launch(u)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                                showAttachmentOptions = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Choose from Gallery") },
                            leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) },
                            modifier = Modifier.clickable {
                                galleryLauncher.launch("image/*")
                                showAttachmentOptions = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttachmentOptions = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// ==========================================
// NEW & UPDATED COMPONENTS
// ==========================================

@Composable
private fun NeoMasterTypeSwitcher(
    currentType: TransactionType,
    mainColor: Color,
    onTypeSelected: (TransactionType) -> Unit
) {
    val isThirdParty = currentType == TransactionType.THIRD_PARTY_IN || currentType == TransactionType.THIRD_PARTY_OUT
    val isPersonal = !isThirdParty

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Top Level: Personal vs Third Party
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            // Personal Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPersonal) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTypeSelected(TransactionType.EXPENSE) }, // Default to Expense
                contentAlignment = Alignment.Center
            ) {
                Text("Personal", fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Normal, color = if(isPersonal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Third Party Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isThirdParty) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTypeSelected(TransactionType.THIRD_PARTY_IN) }, // Default to Receive
                contentAlignment = Alignment.Center
            ) {
                Text("Third Party", fontWeight = if (isThirdParty) FontWeight.Bold else FontWeight.Normal, color = if(isThirdParty) Color(0xFFFFA000) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 2. Sub Level: Specific Actions
        Row(
            modifier = Modifier
                .width(280.dp)
                .height(40.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(mainColor.copy(alpha = 0.1f))
                .border(1.dp, mainColor.copy(alpha = 0.3f), CircleShape)
        ) {
            if (isPersonal) {
                // Personal: Expense vs Income
                TypeTab(
                    label = "Expense",
                    isSelected = currentType == TransactionType.EXPENSE,
                    selectedColor = MaterialTheme.colorScheme.error,
                    onClick = { onTypeSelected(TransactionType.EXPENSE) }
                )
                TypeTab(
                    label = "Income",
                    isSelected = currentType == TransactionType.INCOME,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = { onTypeSelected(TransactionType.INCOME) }
                )
            } else {
                // Third Party: Receive vs Hand Over
                TypeTab(
                    label = "Receive (Hold)",
                    isSelected = currentType == TransactionType.THIRD_PARTY_IN,
                    selectedColor = Color(0xFFFFA000),
                    onClick = { onTypeSelected(TransactionType.THIRD_PARTY_IN) }
                )
                TypeTab(
                    label = "Hand Over (Out)",
                    isSelected = currentType == TransactionType.THIRD_PARTY_OUT,
                    selectedColor = Color(0xFFFFA000),
                    onClick = { onTypeSelected(TransactionType.THIRD_PARTY_OUT) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.TypeTab(label: String, isSelected: Boolean, selectedColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (isSelected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun NeoAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    mainColor: Color,
    readOnly: Boolean
) {
    val textStyle = MaterialTheme.typography.displayLarge.copy(
        fontWeight = FontWeight.Bold,
        color = mainColor,
        textAlign = TextAlign.Center
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "ENTER AMOUNT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", style = textStyle.copy(fontWeight = FontWeight.Medium), modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                readOnly = readOnly,
                singleLine = true,
                cursorBrush = SolidColor(mainColor),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) Text("0", style = textStyle.copy(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun NeoDetailPanel(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NeoSmartInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    suggestions: List<String>,
    placeholder: String = "Enter ${label.lowercase()}..."
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SmartInput(
            value = value,
            onValueChange = onValueChange,
            suggestions = suggestions,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder
        )
    }
}

@Composable
private fun NeoInventoryCard(type: TransactionType, mainColor: Color, content: @Composable ColumnScope.() -> Unit) {
    val label = when (type) {
        TransactionType.INCOME -> "Cash Inventory Entry"
        TransactionType.THIRD_PARTY_IN -> "Notes to Hold (Entry)"
        TransactionType.THIRD_PARTY_OUT -> "Select Notes to Hand Over"
        else -> "Cash Payment Details"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, mainColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (type == TransactionType.INCOME || type == TransactionType.THIRD_PARTY_IN) Icons.Default.Money else Icons.Default.Wallet, null, tint = mainColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Divider(color = mainColor.copy(alpha = 0.2f))
            content()
        }
    }
}

// === HELPERS ===
@Composable
private fun NoteInputRow(denomination: Int, serial: String, onDenomChange: (Int) -> Unit, onSerialChange: (String) -> Unit, onAdd: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Denomination:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2000, 500, 200, 100, 50, 20, 10).forEach { denom ->
                item {
                    val isSelected = denomination == denom
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(shape = CircleShape, color = color, contentColor = contentColor, modifier = Modifier.clickable { onDenomChange(denom) }) {
                        Text("₹$denom", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = serial, onValueChange = onSerialChange, label = { Text("Serial Number") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
            FilledIconButton(onClick = onAdd, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(56.dp)) { Icon(Icons.Default.Add, "Add") }
        }
    }
}

@Composable
private fun AddedNotesList(notes: List<com.scitech.accountex.viewmodel.DraftNote>, onRemove: (Int) -> Unit) {
    if (notes.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            notes.forEachIndexed { index, note ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("₹${note.denomination}", fontWeight = FontWeight.Bold)
                            if (note.serial.isNotEmpty()) Text("Serial: ${note.serial}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onRemove(index) }) { Icon(Icons.Outlined.Delete, "Remove", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

private fun createImageFile(context: Context): Pair<File, Uri> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.getExternalFilesDir(null), "Accountex_Captures")
    if (!storageDir.exists()) storageDir.mkdirs()
    val file = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    return Pair(file, uri)
}