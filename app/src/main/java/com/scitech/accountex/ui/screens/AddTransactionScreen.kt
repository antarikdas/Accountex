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
import androidx.compose.ui.draw.shadow
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

// --- PREMIUM COLORS ---
val PremiumSlate = Color(0xFF1E293B) // Professional Dark Text
val PremiumGray = Color(0xFFF1F5F9)  // Light Background
val PremiumRed = Color(0xFFD32F2F)   // Expense
val PremiumGreen = Color(0xFF2E7D32) // Income
val PremiumPurple = Color(0xFF673AB7)// Exchange
val PremiumAmber = Color(0xFFF57C00) // Third Party

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
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success && tempPhotoUri != null) selectedImageUris = selectedImageUris + tempPhotoUri!! }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> selectedImageUris = selectedImageUris + uris }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) { val (file, uri) = createImageFile(context); tempPhotoUri = uri; cameraLauncher.launch(uri) } }

    val availableNotes by viewModel.availableNotes.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val incomingNotes by viewModel.incomingNotes.collectAsState()

    // Inputs
    var tempSerial by remember { mutableStateOf("") }
    var tempDenomination by remember { mutableIntStateOf(500) }
    var tempIsCoin by remember { mutableStateOf(false) }
    var tempQuantity by remember { mutableStateOf("1") }

    var showNoteSelector by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedType) {
        if (uiState.selectedType == TransactionType.EXPENSE || uiState.selectedType == TransactionType.THIRD_PARTY_OUT || uiState.selectedType == TransactionType.EXCHANGE) {
            showNoteSelector = true
        }
    }

    BackHandler { onNavigateBack() }

    val isExchange = uiState.selectedType == TransactionType.EXCHANGE
    val isThirdParty = uiState.selectedType == TransactionType.THIRD_PARTY_IN || uiState.selectedType == TransactionType.THIRD_PARTY_OUT
    val isIncomingFlow = uiState.selectedType == TransactionType.INCOME || uiState.selectedType == TransactionType.THIRD_PARTY_IN
    val isOutgoingFlow = uiState.selectedType == TransactionType.EXPENSE || uiState.selectedType == TransactionType.THIRD_PARTY_OUT

    val incomingTotal = incomingNotes.sumOf { it.denomination }
    val selectedSpentTotal = availableNotes.filter { it.id in selectedNoteIds }.sumOf { it.amount }
    val transactionAmount = uiState.amountInput.toDoubleOrNull() ?: 0.0
    val changeRequired = if (isOutgoingFlow && selectedSpentTotal > transactionAmount) selectedSpentTotal - transactionAmount else 0.0

    LaunchedEffect(incomingNotes) { if (isIncomingFlow && incomingNotes.isNotEmpty()) viewModel.updateAmount(incomingTotal.toString()) }
    LaunchedEffect(accounts) { if (uiState.selectedAccountId == 0 && accounts.isNotEmpty()) viewModel.updateAccount(accounts.first().id) }

    // --- DYNAMIC THEME COLOR ---
    val mainColor = when (uiState.selectedType) {
        TransactionType.EXPENSE -> PremiumRed
        TransactionType.INCOME -> PremiumGreen
        TransactionType.EXCHANGE -> PremiumPurple
        else -> PremiumAmber
    }

    Scaffold(
        containerColor = Color.White, // Clean White Background
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isExchange) "Break Change" else if(isThirdParty) "Third Party Money" else "New Transaction", fontWeight = FontWeight.Bold, color = PremiumSlate) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.Close, "Close", tint = PremiumSlate) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Subtle Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(mainColor.copy(alpha = 0.08f), Color.White)
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Master Switcher
                NeoMasterTypeSwitcher(uiState.selectedType, mainColor) { viewModel.updateType(it) }

                // 2. Amount Input
                if (!isExchange) {
                    NeoAmountInput(uiState.amountInput, { viewModel.updateAmount(it) }, mainColor, isIncomingFlow && incomingNotes.isNotEmpty())
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL VALUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                        Text("₹${selectedSpentTotal.toInt()}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = mainColor)
                        if (selectedSpentTotal != incomingTotal.toDouble()) {
                            Text("Mismatch: Added ₹${incomingTotal}", color = PremiumRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        } else if(selectedSpentTotal > 0) {
                            Text("Perfect Match!", color = PremiumGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Date & Account (FIXED COLORS)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeoDetailPanel("Date", SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(uiState.selectedDate)), Icons.Rounded.CalendarToday, mainColor, Modifier.weight(1f)) {
                        val c = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                        DatePickerDialog(context, { _, y, m, d -> TimePickerDialog(context, { _, h, min -> c.set(y, m, d, h, min); viewModel.updateDate(c.timeInMillis) }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        NeoDetailPanel("Account", accounts.find { it.id == uiState.selectedAccountId }?.name ?: "Select", Icons.Rounded.Wallet, mainColor, Modifier.fillMaxWidth()) { showAccountDropdown = true }
                        DropdownMenu(expanded = showAccountDropdown, onDismissRequest = { showAccountDropdown = false }, modifier = Modifier.background(Color.White)) {
                            accounts.forEach { account -> DropdownMenuItem(text = { Text("${account.name} (₹${account.balance})", color = PremiumSlate) }, onClick = { viewModel.updateAccount(account.id); showAccountDropdown = false }) }
                        }
                    }
                }

                // 4. Fields
                if (!isExchange) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        AnimatedVisibility(visible = isThirdParty) { NeoSmartInput("Related Party", uiState.thirdPartyName, { viewModel.updateThirdPartyName(it) }, Icons.Rounded.Person, mainColor, emptyList()) }
                        NeoSmartInput("Category", uiState.category, { viewModel.updateCategory(it) }, Icons.Default.Category, mainColor, categorySuggestions)
                        NeoSmartInput("Description", uiState.description, { viewModel.updateDescription(it) }, Icons.Default.Description, mainColor, descriptionSuggestions)
                    }
                }

                // 5. INVENTORY (FIXED TOGGLE)
                AnimatedVisibility(visible = isIncomingFlow || isOutgoingFlow || isExchange || incomingNotes.isNotEmpty()) {
                    NeoInventoryCard(uiState.selectedType, mainColor) {

                        // OUT FLOW
                        if (isOutgoingFlow || isExchange) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if(isExchange) "1. Select Note(s) to Break" else "Pay with Cash", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PremiumSlate)
                                // FIXED TOGGLE SWITCH COLOR
                                if(!isExchange) {
                                    Switch(
                                        checked = showNoteSelector,
                                        onCheckedChange = { showNoteSelector = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = mainColor,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                            if (showNoteSelector) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Selected: ₹${selectedSpentTotal.toInt()}", fontWeight = FontWeight.Bold, color = mainColor, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (availableNotes.isNotEmpty()) {
                                    Box(modifier = Modifier.heightIn(max = 250.dp).fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).padding(4.dp)) {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                            availableNotes.groupBy { it.denomination }.forEach { (d, n) ->
                                                Text("₹$d Notes", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp), color = Color.Gray)
                                                n.forEach { note ->
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleNoteSelection(note.id) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                        Checkbox(checked = selectedNoteIds.contains(note.id), onCheckedChange = { viewModel.toggleNoteSelection(note.id) }, colors = CheckboxDefaults.colors(checkedColor = mainColor))
                                                        Text("${note.serialNumber} (ID: ${note.id})", style = MaterialTheme.typography.bodyMedium, color = PremiumSlate)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else { Text("No cash available.", style = MaterialTheme.typography.bodySmall, color = PremiumRed) }
                            }
                        }

                        if (isExchange) Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE2E8F0))

                        // IN FLOW
                        if (isIncomingFlow || isExchange || changeRequired > 0) {
                            val headerTitle = if(isExchange) "2. Add Change Received" else if(changeRequired > 0) "Change to Receive: ₹${changeRequired.toInt()}" else "Add Money to Inventory"
                            Text(headerTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if(isExchange || changeRequired > 0) mainColor else PremiumSlate)
                            Spacer(modifier = Modifier.height(12.dp))

                            NoteInputRow(
                                denomination = tempDenomination,
                                serial = tempSerial,
                                isCoin = tempIsCoin,
                                quantity = tempQuantity,
                                mainColor = mainColor, // Pass color
                                onDenomSelect = { d, isCoin -> tempDenomination = d; tempIsCoin = isCoin; tempQuantity = "1" },
                                onSerialChange = { tempSerial = it },
                                onQuantityChange = { tempQuantity = it },
                                onAdd = {
                                    val count = tempQuantity.toIntOrNull() ?: 1
                                    if(count > 0) {
                                        if(count == 1) viewModel.addIncomingNote(tempSerial, tempDenomination, tempIsCoin)
                                        else viewModel.addBulkIncomingNotes(tempDenomination, count, tempIsCoin)
                                        tempSerial = ""; tempQuantity = "1"
                                    }
                                }
                            )
                            AddedNotesList(incomingNotes) { viewModel.removeIncomingNote(it) }
                        }
                    }
                }

                // 6. Attachments
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showAttachmentOptions = true }.padding(8.dp)) {
                        Icon(Icons.Rounded.AttachFile, null, tint = mainColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add attachments", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = PremiumSlate)
                    }
                    if (selectedImageUris.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(selectedImageUris) { uri -> AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }

            val isValid = if (isExchange) selectedSpentTotal > 0 && (selectedSpentTotal == incomingTotal.toDouble()) else if (isIncomingFlow) transactionAmount > 0 else transactionAmount > 0 && (if (changeRequired > 0) incomingTotal.toDouble() == changeRequired else true)
            val buttonText = if(isExchange) "Confirm Exchange" else if(isThirdParty) "Save Record" else "Save Transaction"

            Button(
                onClick = { viewModel.addTransaction(selectedImageUris.map { it.toString() }); onNavigateBack() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = if (isValid) mainColor else Color(0xFFE2E8F0), contentColor = if(isValid) Color.White else Color.Gray)
            ) { Text(buttonText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }

        if (showAttachmentOptions) {
            AlertDialog(
                containerColor = Color.White,
                onDismissRequest = { showAttachmentOptions = false },
                title = { Text("Add Attachment", color = PremiumSlate) },
                text = {
                    Column {
                        ListItem(colors = ListItemDefaults.colors(containerColor = Color.White), headlineContent = { Text("Take Photo", color = PremiumSlate) }, leadingContent = { Icon(Icons.Outlined.CameraAlt, null, tint = mainColor) }, modifier = Modifier.clickable { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val (f, u) = createImageFile(context); tempPhotoUri = u; cameraLauncher.launch(u) } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }; showAttachmentOptions = false })
                        ListItem(colors = ListItemDefaults.colors(containerColor = Color.White), headlineContent = { Text("Choose from Gallery", color = PremiumSlate) }, leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null, tint = mainColor) }, modifier = Modifier.clickable { galleryLauncher.launch("image/*"); showAttachmentOptions = false })
                    }
                },
                confirmButton = { TextButton(onClick = { showAttachmentOptions = false }) { Text("Cancel", color = PremiumSlate) } }
            )
        }
    }
}

// === PREMIUM COMPONENTS ===

@Composable
private fun NeoMasterTypeSwitcher(currentType: TransactionType, mainColor: Color, onTypeSelected: (TransactionType) -> Unit) {
    val isThirdParty = currentType == TransactionType.THIRD_PARTY_IN || currentType == TransactionType.THIRD_PARTY_OUT
    val isExchange = currentType == TransactionType.EXCHANGE
    val isPersonal = !isThirdParty && !isExchange
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(PremiumGray).padding(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (isPersonal) Color.White else Color.Transparent).clickable { onTypeSelected(TransactionType.EXPENSE) }, contentAlignment = Alignment.Center) { Text("Personal", fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Medium, color = if(isPersonal) PremiumSlate else Color.Gray) }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (isThirdParty) Color.White else Color.Transparent).clickable { onTypeSelected(TransactionType.THIRD_PARTY_IN) }, contentAlignment = Alignment.Center) { Text("Third Party", fontWeight = if (isThirdParty) FontWeight.Bold else FontWeight.Medium, color = if(isThirdParty) PremiumAmber else Color.Gray) }
        }
        // Sub-tabs
        Row(modifier = Modifier.width(320.dp).height(40.dp).align(Alignment.CenterHorizontally).clip(CircleShape).background(mainColor.copy(alpha = 0.05f)).border(1.dp, mainColor.copy(alpha = 0.2f), CircleShape)) {
            if (isPersonal || isExchange) {
                TypeTab("Expense", currentType == TransactionType.EXPENSE, PremiumRed) { onTypeSelected(TransactionType.EXPENSE) }
                TypeTab("Income", currentType == TransactionType.INCOME, PremiumGreen) { onTypeSelected(TransactionType.INCOME) }
                TypeTab("Exchange", currentType == TransactionType.EXCHANGE, PremiumPurple) { onTypeSelected(TransactionType.EXCHANGE) }
            } else {
                TypeTab("Receive", currentType == TransactionType.THIRD_PARTY_IN, PremiumAmber) { onTypeSelected(TransactionType.THIRD_PARTY_IN) }
                TypeTab("Hand Over", currentType == TransactionType.THIRD_PARTY_OUT, PremiumAmber) { onTypeSelected(TransactionType.THIRD_PARTY_OUT) }
            }
        }
    }
}
@Composable
private fun RowScope.TypeTab(label: String, isSelected: Boolean, selectedColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (isSelected) selectedColor else Color.Transparent).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Gray, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun NoteInputRow(
    denomination: Int, serial: String, isCoin: Boolean, quantity: String, mainColor: Color,
    onDenomSelect: (Int, Boolean) -> Unit, onSerialChange: (String) -> Unit, onQuantityChange: (String) -> Unit, onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Denomination", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2000, 500, 200, 100, 50, 20, 10).forEach { denom ->
                item {
                    val isSelected = denomination == denom && !isCoin
                    Surface(shape = CircleShape, color = if (isSelected) mainColor else Color.White, border = BorderStroke(1.dp, if(isSelected) mainColor else Color(0xFFE2E8F0)), contentColor = if (isSelected) Color.White else PremiumSlate, modifier = Modifier.clickable { onDenomSelect(denom, false) }) {
                        Text("₹$denom", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
            listOf(20, 10, 5, 2, 1).forEach { denom ->
                item {
                    val isSelected = denomination == denom && isCoin
                    Surface(shape = CircleShape, color = if (isSelected) mainColor else Color(0xFFFFF8E1), border = BorderStroke(1.dp, if(isSelected) mainColor else Color(0xFFFFECB3)), contentColor = if (isSelected) Color.White else Color(0xFFF57C00), modifier = Modifier.clickable { onDenomSelect(denom, true) }) {
                        Text("₹$denom (C)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isCoin) {
                OutlinedTextField(value = quantity, onValueChange = onQuantityChange, label = { Text("Count") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = mainColor, unfocusedBorderColor = Color(0xFFE2E8F0)))
            } else {
                OutlinedTextField(value = serial, onValueChange = onSerialChange, label = { Text("Serial Number") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = mainColor, unfocusedBorderColor = Color(0xFFE2E8F0)))
            }
            FilledIconButton(onClick = onAdd, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = mainColor)) { Icon(Icons.Default.Add, "Add") }
        }
    }
}

// FIXED: Clean white surface instead of Turquoise
@Composable
private fun NeoDetailPanel(label: String, value: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accentColor) }
            Spacer(modifier = Modifier.width(12.dp))
            Column { Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = PremiumSlate, maxLines = 1) }
        }
    }
}

@Composable
private fun NeoSmartInput(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector, accentColor: Color, suggestions: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray) }
        SmartInput(value = value, onValueChange = onValueChange, suggestions = suggestions, modifier = Modifier.fillMaxWidth(), placeholder = "Enter ${label.lowercase()}...")
    }
}

@Composable
private fun NeoInventoryCard(type: TransactionType, mainColor: Color, content: @Composable ColumnScope.() -> Unit) {
    val label = when (type) { TransactionType.INCOME -> "Cash Inventory Entry"; TransactionType.THIRD_PARTY_IN -> "Notes to Hold (Entry)"; TransactionType.THIRD_PARTY_OUT -> "Select Notes to Hand Over"; TransactionType.EXCHANGE -> "Exchange Details"; else -> "Cash Payment Details" }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (type == TransactionType.INCOME || type == TransactionType.THIRD_PARTY_IN) Icons.Default.Money else Icons.Default.Wallet, null, tint = mainColor); Spacer(modifier = Modifier.width(12.dp)); Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PremiumSlate) }
            Divider(color = Color(0xFFF1F5F9)); content()
        }
    }
}

@Composable
private fun AddedNotesList(notes: List<com.scitech.accountex.viewmodel.DraftNote>, onRemove: (Int) -> Unit) {
    if (notes.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            notes.forEachIndexed { index, note ->
                Surface(shape = RoundedCornerShape(12.dp), color = PremiumGray, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("₹${note.denomination}", fontWeight = FontWeight.Bold, color = PremiumSlate)
                                if(note.isCoin) { Spacer(Modifier.width(8.dp)); Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(4.dp)) { Text("COIN", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57C00), modifier = Modifier.padding(horizontal = 4.dp)) } }
                            }
                            if (note.serial.isNotEmpty() && !note.isCoin) Text("Serial: ${note.serial}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { onRemove(index) }) { Icon(Icons.Outlined.Delete, "Remove", tint = PremiumRed) }
                    }
                }
            }
        }
    }
}

// ... (NeoAmountInput and createImageFile remain standard, just ensure imports match) ...
@Composable
private fun NeoAmountInput(value: String, onValueChange: (String) -> Unit, mainColor: Color, readOnly: Boolean) {
    val textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, color = mainColor, textAlign = TextAlign.Center)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ENTER AMOUNT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", style = textStyle.copy(fontWeight = FontWeight.Medium), modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(value = value, onValueChange = onValueChange, textStyle = textStyle, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), readOnly = readOnly, singleLine = true, cursorBrush = SolidColor(mainColor), decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { if (value.isEmpty()) Text("0", style = textStyle.copy(color = Color(0xFFE2E8F0))); inner() } })
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