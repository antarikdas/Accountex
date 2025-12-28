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
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.scitech.accountex.data.CurrencyColors
import com.scitech.accountex.data.CurrencyNote
import com.scitech.accountex.data.CurrencyType
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.components.*
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.AddTransactionViewModel
import com.scitech.accountex.viewmodel.DraftNote
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()

    // --- DATA ---
    val accounts by viewModel.accounts.collectAsState()
    val availableNotes by viewModel.availableNotes.collectAsState()
    val incomingNotes by viewModel.incomingNotes.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()

    // --- COLORS ---
    val themeColor = when (uiState.selectedType) {
        TransactionType.EXPENSE -> Color(0xFFE53935)
        TransactionType.INCOME -> Color(0xFF00C853)
        TransactionType.TRANSFER -> Color(0xFF2962FF)
        TransactionType.EXCHANGE -> Color(0xFFAA00FF)
        else -> MaterialTheme.colorScheme.primary
    }

    // --- CALCULATIONS ---
    val isIncoming = uiState.selectedType == TransactionType.INCOME || uiState.selectedType == TransactionType.THIRD_PARTY_IN
    val isExchange = uiState.selectedType == TransactionType.EXCHANGE
    val isTransfer = uiState.selectedType == TransactionType.TRANSFER

    val incomingTotal = incomingNotes.sumOf { it.denomination }
    val walletSelectedTotal = availableNotes.filter { it.id in selectedNoteIds }.sumOf { it.amount }
    val exchangeMismatch = isExchange && (incomingTotal.toDouble() != walletSelectedTotal)
    val activeCashTotal = if(isIncoming || isTransfer) incomingTotal.toDouble() else walletSelectedTotal
    val manualAmount = uiState.amountInput.toDoubleOrNull() ?: 0.0
    val showSyncWarning = uiState.isManualAmount && activeCashTotal > 0 && activeCashTotal != manualAmount

    // --- STATES ---
    var showAccountSheet by remember { mutableStateOf(false) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Sheet States
    var showSerialSheet by remember { mutableStateOf(false) }
    var showCoinCountSheet by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableIntStateOf(-1) }
    var editingDenom by remember { mutableIntStateOf(0) }

    // --- LAUNCHERS ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) selectedImageUris = selectedImageUris + tempPhotoUri!! }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedImageUris = selectedImageUris + it }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val (file, uri) = createImageFile(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // --- FIX 1: LOAD EXISTING IMAGES IN EDIT MODE ---
    LaunchedEffect(uiState.initialImageUris) {
        if (uiState.initialImageUris.isNotEmpty() && selectedImageUris.isEmpty()) {
            selectedImageUris = uiState.initialImageUris.map { Uri.parse(it) }
        }
    }

    LaunchedEffect(accounts) { if (uiState.selectedAccountId == 0 && accounts.isNotEmpty()) viewModel.updateAccount(accounts.first().id) }
    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                // --- FIX 2: DYNAMIC TITLE ---
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if(uiState.isEditing) "Edit Transaction" else "New Transaction", fontWeight = FontWeight.Bold)
                        if(uiState.isEditing) {
                            Text("Editing Mode", style = MaterialTheme.typography.labelSmall, color = themeColor)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.Close, "Close") } },
                actions = {
                    val canSave = if(isExchange) !exchangeMismatch && walletSelectedTotal > 0 else true
                    TextButton(
                        onClick = {
                            if(canSave) {
                                // --- FIX 3: USE SAVE TRANSACTION (Handles Add & Edit) ---
                                viewModel.saveTransaction(selectedImageUris.map { it.toString() })
                                onNavigateBack()
                            }
                        },
                        enabled = canSave
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold, color = if(canSave) themeColor else Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. SPLIT HEADER
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)).padding(4.dp)) {
                    val is3rdParty = uiState.selectedType == TransactionType.THIRD_PARTY_IN || uiState.selectedType == TransactionType.THIRD_PARTY_OUT
                    TabButton("Personal", !is3rdParty, themeColor, Modifier.weight(1f)) { if (is3rdParty) viewModel.updateType(TransactionType.EXPENSE) }
                    TabButton("3rd Party", is3rdParty, themeColor, Modifier.weight(1f)) { if (!is3rdParty) viewModel.updateType(TransactionType.THIRD_PARTY_IN) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = if (uiState.selectedType == TransactionType.THIRD_PARTY_IN || uiState.selectedType == TransactionType.THIRD_PARTY_OUT) {
                        listOf(TransactionType.THIRD_PARTY_IN, TransactionType.THIRD_PARTY_OUT)
                    } else {
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER, TransactionType.EXCHANGE)
                    }
                    items(types) { type ->
                        val selected = uiState.selectedType == type
                        FilterChip(
                            selected = selected, onClick = { viewModel.updateType(type) },
                            label = { Text(type.name.replace("_", " "), fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColor.copy(0.1f), selectedLabelColor = themeColor),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = if(selected) themeColor else Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // 2. HERO AMOUNT
                BasicTextField(
                    value = uiState.amountInput,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) viewModel.updateAmount(it) },
                    textStyle = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, color = themeColor, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    decorationBox = { innerTextField -> Box(contentAlignment = Alignment.Center) { if (uiState.amountInput.isEmpty()) Text("0", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline.copy(0.3f)); innerTextField() } },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = showSyncWarning && !isExchange) {
                    Surface(onClick = { viewModel.forceSyncAmount() }, color = themeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Sync, null, tint = themeColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cash: ${formatCurrency(activeCashTotal)} (Tap to Sync)", style = MaterialTheme.typography.labelSmall, color = themeColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Account Selector
                val currentAcc = accounts.find { it.id == uiState.selectedAccountId }
                Surface(onClick = { showAccountSheet = true }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(if (isIncoming) "Deposit To" else "Spend From", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(currentAcc?.name ?: "Select Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // ====================================================================================
            // 3. CASH SECTIONS
            // ====================================================================================

            // A. WALLET INVENTORY (SPENDING)
            if (!isIncoming) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(if(isExchange) "GIVING (Select Notes)" else "WALLET INVENTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (availableNotes.isEmpty()) {
                        Text("No cash available in this account.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    } else {
                        // Split Notes vs Coins
                        val (notes, coins) = availableNotes.partition { it.type == CurrencyType.NOTE }

                        // 1. NOTES (Select by Serial)
                        if (notes.isNotEmpty()) {
                            notes.groupBy { it.denomination }.forEach { (denom, denomNotes) ->
                                Text("₹$denom Notes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = getPremiumColor(denom), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    denomNotes.forEach { note ->
                                        val isSelected = selectedNoteIds.contains(note.id)
                                        NoteChip(
                                            serial = note.serialNumber,
                                            color = getPremiumColor(denom),
                                            selected = isSelected,
                                            onClick = { viewModel.toggleNoteSelection(note.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. COINS (Select by Count)
                        if (coins.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("COINS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            coins.groupBy { it.denomination }.forEach { (denom, denomCoins) ->
                                val selectedCount = denomCoins.count { selectedNoteIds.contains(it.id) }
                                CoinCounterRow(
                                    denomination = denom,
                                    totalAvailable = denomCoins.size,
                                    currentSelected = selectedCount,
                                    onIncrement = {
                                        val coinToSelect = denomCoins.firstOrNull { !selectedNoteIds.contains(it.id) }
                                        coinToSelect?.let { viewModel.toggleNoteSelection(it.id) }
                                    },
                                    onDecrement = {
                                        val coinToUnselect = denomCoins.firstOrNull { selectedNoteIds.contains(it.id) }
                                        coinToUnselect?.let { viewModel.toggleNoteSelection(it.id) }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isExchange) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SwapVert, null, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(4.dp), tint = themeColor)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // B. CASH STREAM (RECEIVING)
            if (isIncoming || isTransfer || isExchange) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(if(isExchange) "RECEIVING (Add Cash)" else "CASH STREAM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (incomingNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("Tap buttons below to add", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline.copy(0.5f))
                        }
                    } else {
                        LazyRow(contentPadding = PaddingValues(end = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(incomingNotes) { index, note ->
                                DraftNoteCard(
                                    note = note,
                                    color = if (note.isCoin) CurrencyColors.Coin else getPremiumColor(note.denomination),
                                    onClick = {
                                        editingItemIndex = index; editingDenom = note.denomination
                                        if(note.isCoin) showCoinCountSheet = true else showSerialSheet = true
                                    },
                                    onRemove = { viewModel.removeIncomingNote(index) }
                                )
                            }
                        }
                    }
                    if(isExchange && exchangeMismatch) {
                        Text("Mismatch! Giving: ${formatCurrency(walletSelectedTotal)} | Receiving: ${formatCurrency(incomingTotal.toDouble())}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // C. LAUNCHPAD (Add Notes/Coins)
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    val notes = listOf(500, 200, 100, 50, 20, 10)
                    val coins = listOf(20, 10, 5, 2, 1)

                    Text("ADD NOTES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    LaunchpadGrid(items = notes, isCoin = false) { denom -> viewModel.addIncomingNote("", denom, false) }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("ADD COINS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    LaunchpadGrid(items = coins, isCoin = true) { denom -> viewModel.addIncomingNote("", denom, true) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. META DETAILS
            Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val dateLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(uiState.selectedDate))
                CleanDetailRow(Icons.Rounded.CalendarToday, "Date & Time", dateLabel, themeColor) {
                    val c = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                    DatePickerDialog(context, { _, y, m, d -> TimePickerDialog(context, { _, h, min -> c.set(y, m, d, h, min); viewModel.updateDate(c.timeInMillis) }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }
                CleanInput(uiState.category, { viewModel.updateCategory(it) }, "Category", Icons.Rounded.Category, themeColor, categorySuggestions)
                CleanInput(uiState.description, { viewModel.updateDescription(it) }, "Description", Icons.Rounded.Description, themeColor, descriptionSuggestions)
                Row(modifier = Modifier.fillMaxWidth().clickable { showAttachmentOptions = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.AttachFile, null, tint = themeColor); Spacer(modifier = Modifier.width(12.dp)); Text(if(selectedImageUris.isEmpty()) "Add Receipt / Photo" else "${selectedImageUris.size} attached", fontWeight = FontWeight.Bold, color = themeColor) }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // --- SHEETS & DIALOGS ---
    if (showAccountSheet) { ModalBottomSheet(onDismissRequest = { showAccountSheet = false }) { LazyColumn(modifier = Modifier.padding(24.dp)) { items(accounts) { acc -> Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.updateAccount(acc.id); showAccountSheet = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(acc.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text(formatCurrency(acc.balance), fontWeight = FontWeight.Bold) }; HorizontalDivider() } } } }
    if (showSerialSheet && editingItemIndex != -1) { ModalBottomSheet(onDismissRequest = { showSerialSheet = false }) { NoteSerialEntry(denom = editingDenom, brandColor = themeColor) { serial -> val oldNote = incomingNotes[editingItemIndex]; viewModel.removeIncomingNote(editingItemIndex); viewModel.addIncomingNote(serial, oldNote.denomination, false); showSerialSheet = false } } }
    if (showCoinCountSheet && editingItemIndex != -1) { val currentCount = incomingNotes.count { it.isCoin && it.denomination == editingDenom }; ModalBottomSheet(onDismissRequest = { showCoinCountSheet = false }) { CoinQuantitySheet(denom = editingDenom, currentCount = currentCount, brandColor = themeColor) { newCount -> val indicesToRemove = incomingNotes.mapIndexedNotNull { index, note -> if (note.isCoin && note.denomination == editingDenom) index else null }.sortedDescending(); indicesToRemove.forEach { viewModel.removeIncomingNote(it) }; if (newCount > 0) viewModel.addBulkIncomingNotes(editingDenom, newCount, true); showCoinCountSheet = false } } }
    if (showAttachmentOptions) { AlertDialog(onDismissRequest = { showAttachmentOptions = false }, title = { Text("Add Attachment") }, text = { Column { ListItem(headlineContent = { Text("Take Photo") }, leadingContent = { Icon(Icons.Outlined.CameraAlt, null) }, modifier = Modifier.clickable { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val (f, u) = createImageFile(context); tempPhotoUri = u; cameraPermissionLauncher.launch(Manifest.permission.CAMERA) } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }; showAttachmentOptions = false }); ListItem(headlineContent = { Text("Choose from Gallery") }, leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) }, modifier = Modifier.clickable { galleryLauncher.launch("image/*"); showAttachmentOptions = false }) } }, confirmButton = { TextButton(onClick = { showAttachmentOptions = false }) { Text("Cancel") } }) }
}

// --- SUB-COMPONENTS (UX & VISUAL UPGRADE) ---

@Composable
fun NoteChip(serial: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp), // Rectangular for Notes
        color = if (selected) color else color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                serial,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else color.darken(0.4f)
            )
        }
    }
}

@Composable
fun CoinCounterRow(denomination: Int, totalAvailable: Int, currentSelected: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(CurrencyColors.Coin, CircleShape).border(2.dp, Color(0xFF9E9E9E), CircleShape), // Metallic Look
                contentAlignment = Alignment.Center
            ) {
                Text("₹$denomination", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("₹$denomination Coins", fontWeight = FontWeight.Bold)
                Text("$totalAvailable available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // Counter
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, enabled = currentSelected > 0) {
                Icon(Icons.Rounded.Remove, null, tint = if (currentSelected > 0) MaterialTheme.colorScheme.error else Color.Gray)
            }
            Text(
                text = "$currentSelected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 24.dp).wrapContentWidth(Alignment.CenterHorizontally)
            )
            IconButton(onClick = onIncrement, enabled = currentSelected < totalAvailable) {
                Icon(Icons.Rounded.Add, null, tint = if (currentSelected < totalAvailable) MaterialTheme.colorScheme.primary else Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LaunchpadGrid(items: List<Int>, isCoin: Boolean, onClick: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { denom ->
            val color = if(isCoin) CurrencyColors.Coin else getPremiumColor(denom)
            val shape = if(isCoin) CircleShape else RoundedCornerShape(4.dp) // Coin=Circle, Note=Rect

            Surface(
                onClick = { onClick(denom) },
                color = color.copy(alpha = 0.2f),
                shape = shape,
                border = BorderStroke(1.dp, color.copy(alpha = 0.8f)),
                modifier = Modifier.width(if(isCoin) 60.dp else 80.dp).height(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "₹$denom",
                        fontWeight = FontWeight.Black,
                        color = color.darken(0.4f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable fun TabButton(text: String, selected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) { Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent).clickable(onClick = onClick)) { Text(text, fontWeight = FontWeight.Bold, color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
fun DraftNoteCard(note: DraftNote, color: Color, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color,
        shape = if(note.isCoin) CircleShape else RoundedCornerShape(8.dp), // Distinct shapes
        shadowElevation = 4.dp,
        modifier = Modifier.width(140.dp).height(90.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("₹${note.denomination}", fontWeight = FontWeight.Black, color = Color.White)
                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp).clickable { onRemove() })
            }
            if (!note.isCoin) {
                Text(
                    if(note.serial.isEmpty()) "Tap to set SN" else note.serial,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.9f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Color.Black.copy(0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                )
            } else {
                // For coins, don't show "Tap to set Count" inside the card if it's 1,
                // but if we group them later, we can. For now, it's 1 card = 1 coin in this list view.
                Icon(Icons.Rounded.MonetizationOn, null, tint = Color.White.copy(0.5f))
            }
        }
    }
}

@Composable fun NoteSerialEntry(denom: Int, brandColor: Color, onSave: (String) -> Unit) { var serial by remember { mutableStateOf("") }; val focusRequester = remember { FocusRequester() }; LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus() }; Column(modifier = Modifier.padding(24.dp).navigationBarsPadding().imePadding()) { Text("Note Serial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("For ₹$denom Note", color = brandColor); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = serial, onValueChange = { serial = it.uppercase() }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), label = { Text("Serial Number") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (serial.isNotBlank()) onSave(serial) })); Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { if (serial.isNotBlank()) onSave(serial) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = brandColor)) { Text("Save") } } }
@Composable fun CoinQuantitySheet(denom: Int, currentCount: Int, brandColor: Color, onSave: (Int) -> Unit) { var countText by remember { mutableStateOf(if (currentCount > 0) currentCount.toString() else "") }; val focusRequester = remember { FocusRequester() }; LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus() }; Column(modifier = Modifier.padding(24.dp).navigationBarsPadding().imePadding()) { Text("Coin Quantity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("For ₹$denom Coins", color = brandColor); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = countText, onValueChange = { if (it.all { c -> c.isDigit() }) countText = it }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), label = { Text("Count") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onSave(countText.toIntOrNull() ?: 0) })); Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { onSave(countText.toIntOrNull() ?: 0) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = brandColor)) { Text("Set Quantity") } } }

// --- HELPERS ---

// PREMIUM PALETTE (Matched to NoteInventoryScreen)
fun getPremiumColor(denom: Int): Color = when (denom) {
    500 -> Color(0xFF37474F)
    200 -> Color(0xFFF57C00)
    100 -> Color(0xFF512DA8)
    50 -> Color(0xFF00897B)
    20 -> Color(0xFF33691E)
    10 -> Color(0xFF5D4037)
    else -> Color.Gray
}

fun Color.darken(factor: Float): Color = Color(red = (this.red * (1 - factor)).coerceIn(0f, 1f), green = (this.green * (1 - factor)).coerceIn(0f, 1f), blue = (this.blue * (1 - factor)).coerceIn(0f, 1f), alpha = this.alpha)
@Composable private fun CleanInput(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, color: Color, suggestions: List<String>) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }; SmartInput(value = value, onValueChange = onValueChange, suggestions = suggestions, modifier = Modifier.fillMaxWidth(), placeholder = "Enter ${label.lowercase()}...") } }
@Composable private fun CleanDetailRow(icon: ImageVector, label: String, value: String, color: Color, onClick: () -> Unit) { Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }; Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant) } }
private fun createImageFile(context: Context): Pair<File, Uri> { val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()); val storageDir = File(context.getExternalFilesDir(null), "Accountex_Captures"); if (!storageDir.exists()) storageDir.mkdirs(); val file = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir); val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file); return Pair(file, uri) }