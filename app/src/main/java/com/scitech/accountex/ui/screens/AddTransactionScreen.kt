package com.scitech.accountex.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.widget.Toast
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.scitech.accountex.data.CurrencyType
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.components.*
import com.scitech.accountex.ui.theme.*
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

    // 🧠 NATIVE HAPTICS (Stable)
    val view = LocalView.current
    fun hapticClick() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    fun hapticSuccess() = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

    val uiState by viewModel.uiState.collectAsState()
    val colors = AppTheme.colors

    // --- DATA ---
    val accounts by viewModel.accounts.collectAsState()
    val availableNotes by viewModel.availableNotes.collectAsState()
    val incomingNotes by viewModel.incomingNotes.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()

    // 🎨 SEMANTIC COLOR MAPPING
    val themeColor = when (uiState.selectedType) {
        TransactionType.EXPENSE,
        TransactionType.THIRD_PARTY_OUT -> colors.expense
        TransactionType.INCOME,
        TransactionType.THIRD_PARTY_IN -> colors.income
        TransactionType.TRANSFER -> colors.brandPrimary
        TransactionType.EXCHANGE -> colors.brandSecondary
    }

    // --- CURRENCY RESOLVER HELPER ---
    fun getNoteColor(denom: Int): Color = when (denom) {
        500 -> colors.currencyTier1
        200 -> colors.currencyTier2
        100 -> colors.currencyTier3
        50 -> colors.currencyTier4
        else -> colors.currencyTier5
    }
    fun getCoinColor(denom: Int): Color = colors.currencyCoin

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
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

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
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) { val (file, uri) = createImageFile(context); tempPhotoUri = uri; cameraLauncher.launch(uri) } }

    LaunchedEffect(uiState.initialImageUris) {
        if (uiState.initialImageUris.isNotEmpty() && selectedImageUris.isEmpty()) {
            selectedImageUris = uiState.initialImageUris.map { Uri.parse(it) }
        }
    }

    LaunchedEffect(accounts) { if (uiState.selectedAccountId == 0 && accounts.isNotEmpty()) viewModel.updateAccount(accounts.first().id) }
    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if(uiState.isEditing) "Edit Record" else "New Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        if(uiState.isEditing) {
                            Text("AUDIT MODE", style = MaterialTheme.typography.labelSmall, color = themeColor, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.Close, "Close", tint = colors.textPrimary) } },
                actions = {
                    if (!uiState.isEditing) {
                        IconButton(onClick = { showSaveTemplateDialog = true }) {
                            Icon(Icons.Rounded.BookmarkBorder, "Template", tint = colors.textSecondary)
                        }
                    }

                    val canSave = if(isExchange) !exchangeMismatch && walletSelectedTotal > 0 else true
                    TextButton(
                        onClick = {
                            if(canSave) {
                                hapticSuccess()
                                viewModel.saveTransaction(selectedImageUris.map { it.toString() })
                                onNavigateBack()
                            }
                        },
                        enabled = canSave
                    ) {
                        Text("SAVE", style = MaterialTheme.typography.labelLarge, color = if(canSave) themeColor else colors.textSecondary.copy(alpha=0.5f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. SPLIT HEADER (TYPE SELECTOR)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceCard).padding(4.dp)) {
                    val is3rdParty = uiState.selectedType == TransactionType.THIRD_PARTY_IN || uiState.selectedType == TransactionType.THIRD_PARTY_OUT
                    TabButton(
                        text = "Personal",
                        selected = !is3rdParty,
                        color = themeColor,
                        selectedBg = colors.background,
                        unselectedColor = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    ) { if (is3rdParty) { hapticClick(); viewModel.updateType(TransactionType.EXPENSE) } }

                    TabButton(
                        text = "3rd Party",
                        selected = is3rdParty,
                        color = themeColor,
                        selectedBg = colors.background,
                        unselectedColor = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    ) { if (!is3rdParty) { hapticClick(); viewModel.updateType(TransactionType.THIRD_PARTY_IN) } }
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
                            selected = selected, onClick = { hapticClick(); viewModel.updateType(type) },
                            label = { Text(type.name.replace("_", " "), style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColor.copy(0.1f),
                                selectedLabelColor = themeColor,
                                containerColor = colors.surfaceCard,
                                labelColor = colors.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = if(selected) themeColor else colors.divider)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // 2. HERO AMOUNT (Psychological Focus Zone)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    BasicTextField(
                        value = uiState.amountInput,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() || c == '.' }) {
                                if(it != uiState.amountInput) hapticClick()
                                viewModel.updateAmount(it)
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 64.sp, // HUGE FONT
                            fontWeight = FontWeight.Bold,
                            color = themeColor, // Emotional Color
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(themeColor),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (uiState.amountInput.isEmpty()) Text("0", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary.copy(0.2f));
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedVisibility(visible = showSyncWarning && !isExchange) {
                    Surface(onClick = { hapticClick(); viewModel.forceSyncAmount() }, color = themeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Sync, null, tint = themeColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cash: ${formatCurrency(activeCashTotal)} (Tap to Sync)", style = MaterialTheme.typography.labelSmall, color = themeColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Account Selector
                val currentAcc = accounts.find { it.id == uiState.selectedAccountId }
                Surface(onClick = { hapticClick(); showAccountSheet = true }, shape = RoundedCornerShape(12.dp), color = colors.surfaceCard, border = BorderStroke(1.dp, colors.divider)) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, tint = colors.textPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(if (isIncoming) "Deposit To" else "Spend From", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            Text(currentAcc?.name ?: "Select Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = colors.textSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // ====================================================================================
            // 3. CASH SECTIONS
            // ====================================================================================

            if (!isIncoming) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(if(isExchange) "GIVING (Select Notes)" else "WALLET INVENTORY", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (availableNotes.isEmpty()) {
                        Text("No cash available in this account.", style = MaterialTheme.typography.bodyMedium, color = colors.expense)
                    } else {
                        val (notes, coins) = availableNotes.partition { it.type == CurrencyType.NOTE }

                        // 1. NOTES
                        if (notes.isNotEmpty()) {
                            notes.groupBy { it.denomination }.forEach { (denom, denomNotes) ->
                                val noteColor = getNoteColor(denom)
                                Text("₹$denom Notes", style = MaterialTheme.typography.labelSmall, color = noteColor, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp), fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    denomNotes.forEach { note ->
                                        val isSelected = selectedNoteIds.contains(note.id)
                                        NoteChip(
                                            serial = note.serialNumber,
                                            color = noteColor,
                                            selected = isSelected,
                                            textInverse = if(isSelected) colors.textInverse else noteColor,
                                            onClick = { hapticClick(); viewModel.toggleNoteSelection(note.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. COINS
                        if (coins.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("COINS", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                            coins.groupBy { it.denomination }.forEach { (denom, denomCoins) ->
                                val selectedCount = denomCoins.count { selectedNoteIds.contains(it.id) }
                                CoinCounterRow(
                                    denomination = denom,
                                    totalAvailable = denomCoins.size,
                                    currentSelected = selectedCount,
                                    color = getCoinColor(denom),
                                    textPrimary = colors.textPrimary,
                                    textSecondary = colors.textSecondary,
                                    surfaceColor = colors.surfaceCard,
                                    borderColor = colors.divider,
                                    onIncrement = {
                                        val coinToSelect = denomCoins.firstOrNull { !selectedNoteIds.contains(it.id) }
                                        coinToSelect?.let { hapticClick(); viewModel.toggleNoteSelection(it.id) }
                                    },
                                    onDecrement = {
                                        val coinToUnselect = denomCoins.firstOrNull { selectedNoteIds.contains(it.id) }
                                        coinToUnselect?.let { hapticClick(); viewModel.toggleNoteSelection(it.id) }
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
                    Icon(Icons.Rounded.SwapVert, null, modifier = Modifier.size(32.dp).background(colors.surfaceCard, CircleShape).padding(4.dp), tint = themeColor)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isIncoming || isTransfer || isExchange) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(if(isExchange) "RECEIVING (Add Cash)" else "CASH STREAM", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (incomingNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("Tap buttons below to add", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary.copy(0.5f))
                        }
                    } else {
                        LazyRow(contentPadding = PaddingValues(end = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(incomingNotes) { index, note ->
                                DraftNoteCard(
                                    note = note,
                                    color = if(note.isCoin) getCoinColor(note.denomination) else getNoteColor(note.denomination),
                                    onClick = {
                                        hapticClick()
                                        editingItemIndex = index; editingDenom = note.denomination
                                        if(note.isCoin) showCoinCountSheet = true else showSerialSheet = true
                                    },
                                    onRemove = { hapticClick(); viewModel.removeIncomingNote(index) }
                                )
                            }
                        }
                    }
                    if(isExchange && exchangeMismatch) {
                        Text("Mismatch! Giving: ${formatCurrency(walletSelectedTotal)} | Receiving: ${formatCurrency(incomingTotal.toDouble())}", color = colors.expense, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    val notes = listOf(500, 200, 100, 50, 20, 10)
                    val coins = listOf(20, 10, 5, 2, 1)

                    Text("ADD NOTES", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LaunchpadGrid(items = notes, isCoin = false, colorProvider = { getNoteColor(it) }) { denom -> hapticClick(); viewModel.addIncomingNote("", denom, false) }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("ADD COINS", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LaunchpadGrid(items = coins, isCoin = true, colorProvider = { getCoinColor(it) }) { denom -> hapticClick(); viewModel.addIncomingNote("", denom, true) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. META DETAILS
            Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val dateLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(uiState.selectedDate))
                CleanDetailRow(Icons.Rounded.CalendarToday, "Date & Time", dateLabel, themeColor, colors) {
                    val c = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                    DatePickerDialog(context, { _, y, m, d -> TimePickerDialog(context, { _, h, min -> c.set(y, m, d, h, min); viewModel.updateDate(c.timeInMillis) }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }
                CleanInput(uiState.category, { viewModel.updateCategory(it) }, "Category", Icons.Rounded.Category, themeColor, categorySuggestions, colors)
                CleanInput(uiState.description, { viewModel.updateDescription(it) }, "Description", Icons.Rounded.Description, themeColor, descriptionSuggestions, colors)
                Row(modifier = Modifier.fillMaxWidth().clickable { showAttachmentOptions = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.AttachFile, null, tint = themeColor); Spacer(modifier = Modifier.width(12.dp)); Text(if(selectedImageUris.isEmpty()) "Add Receipt / Photo" else "${selectedImageUris.size} attached", style = MaterialTheme.typography.labelLarge, color = themeColor) }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // --- SHEETS & DIALOGS ---
    if (showAccountSheet) { ModalBottomSheet(onDismissRequest = { showAccountSheet = false }, containerColor = colors.surfaceCard) { LazyColumn(modifier = Modifier.padding(24.dp)) { items(accounts) { acc -> Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.updateAccount(acc.id); showAccountSheet = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(acc.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f)); Text(formatCurrency(acc.balance), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary) }; HorizontalDivider(color = colors.divider) } } } }
    if (showSerialSheet && editingItemIndex != -1) { ModalBottomSheet(onDismissRequest = { showSerialSheet = false }, containerColor = colors.surfaceCard) { NoteSerialEntry(denom = editingDenom, brandColor = themeColor, textColor = colors.textPrimary) { serial -> val oldNote = incomingNotes[editingItemIndex]; viewModel.removeIncomingNote(editingItemIndex); viewModel.addIncomingNote(serial, oldNote.denomination, false); showSerialSheet = false } } }
    if (showCoinCountSheet && editingItemIndex != -1) { val currentCount = incomingNotes.count { it.isCoin && it.denomination == editingDenom }; ModalBottomSheet(onDismissRequest = { showCoinCountSheet = false }, containerColor = colors.surfaceCard) { CoinQuantitySheet(denom = editingDenom, currentCount = currentCount, brandColor = themeColor, textColor = colors.textPrimary) { newCount -> val indicesToRemove = incomingNotes.mapIndexedNotNull { index, note -> if (note.isCoin && note.denomination == editingDenom) index else null }.sortedDescending(); indicesToRemove.forEach { viewModel.removeIncomingNote(it) }; if (newCount > 0) viewModel.addBulkIncomingNotes(editingDenom, newCount, true); showCoinCountSheet = false } } }
    if (showAttachmentOptions) { AlertDialog(containerColor = colors.surfaceCard, titleContentColor = colors.textPrimary, textContentColor = colors.textSecondary, onDismissRequest = { showAttachmentOptions = false }, title = { Text("Add Attachment") }, text = { Column { ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text("Take Photo", color = colors.textPrimary) }, leadingContent = { Icon(Icons.Outlined.CameraAlt, null, tint = colors.textSecondary) }, modifier = Modifier.clickable { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val (f, u) = createImageFile(context); tempPhotoUri = u; cameraPermissionLauncher.launch(Manifest.permission.CAMERA) } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }; showAttachmentOptions = false }); ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text("Choose from Gallery", color = colors.textPrimary) }, leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null, tint = colors.textSecondary) }, modifier = Modifier.clickable { galleryLauncher.launch("image/*"); showAttachmentOptions = false }) } }, confirmButton = { TextButton(onClick = { showAttachmentOptions = false }) { Text("Cancel", color = colors.brandPrimary) } }) }
    if (showSaveTemplateDialog) { var templateName by remember { mutableStateOf("") }; AlertDialog(containerColor = colors.surfaceCard, titleContentColor = colors.textPrimary, textContentColor = colors.textSecondary, onDismissRequest = { showSaveTemplateDialog = false }, title = { Text("Save as Template") }, text = { Column { Text("Give this transaction a name to save it as a template for quick access.", style = MaterialTheme.typography.bodyMedium); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = templateName, onValueChange = { templateName = it }, label = { Text("Template Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, focusedLabelColor = themeColor, unfocusedLabelColor = colors.textSecondary)) } }, confirmButton = { Button(onClick = { if (templateName.isNotBlank()) { viewModel.saveAsTemplate(templateName); showSaveTemplateDialog = false; Toast.makeText(context, "Template Saved", Toast.LENGTH_SHORT).show() } }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Save") } }, dismissButton = { TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Cancel", color = colors.textSecondary) } }) }
}

// --- SUB-COMPONENTS ---

@Composable
fun NoteChip(serial: String, color: Color, selected: Boolean, textInverse: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if(selected) color else color.copy(alpha=0.1f),
        border = BorderStroke(1.dp, color),
        modifier = Modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                serial,
                style = MaterialTheme.typography.labelSmall,
                color = textInverse,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CoinCounterRow(denomination: Int, totalAvailable: Int, currentSelected: Int, color: Color, textPrimary: Color, textSecondary: Color, surfaceColor: Color, borderColor: Color, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(surfaceColor, RoundedCornerShape(12.dp)).border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.Black.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "₹$denomination",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(0.8f),
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("₹$denomination Coins", style = MaterialTheme.typography.labelLarge, color = textPrimary)
                Text("$totalAvailable available", style = MaterialTheme.typography.labelSmall, color = textSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, enabled = currentSelected > 0) {
                Icon(Icons.Rounded.Remove, null, tint = if (currentSelected > 0) Color.Red else Color.Gray)
            }
            Text(
                text = "$currentSelected",
                style = MaterialTheme.typography.titleMedium,
                color = textPrimary,
                modifier = Modifier.widthIn(min = 24.dp).wrapContentWidth(Alignment.CenterHorizontally)
            )
            IconButton(onClick = onIncrement, enabled = currentSelected < totalAvailable) {
                Icon(Icons.Rounded.Add, null, tint = if (currentSelected < totalAvailable) textPrimary else Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LaunchpadGrid(items: List<Int>, isCoin: Boolean, colorProvider: (Int) -> Color, onClick: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { denom ->
            val color = colorProvider(denom)
            val shape = if(isCoin) CircleShape else RoundedCornerShape(4.dp)

            Surface(
                onClick = { onClick(denom) },
                color = color.copy(alpha = 0.15f),
                shape = shape,
                border = BorderStroke(1.dp, color),
                modifier = Modifier.width(if(isCoin) 60.dp else 80.dp).height(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "₹$denom",
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable fun TabButton(text: String, selected: Boolean, color: Color, selectedBg: Color, unselectedColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) { Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(if (selected) selectedBg else Color.Transparent).clickable(onClick = onClick)) { Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) color else unselectedColor) } }

@Composable
fun DraftNoteCard(note: DraftNote, color: Color, onClick: () -> Unit, onRemove: () -> Unit) {
    // 🧠 VISUAL DESIGN: "DIGITAL ASSET CARTRIDGE"
    // Distinct Zones for Value (Top) and Metadata (Bottom)

    Surface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .width(110.dp) // Wider for SN legibility
            .height(75.dp) // Taller for zoning
    ) {
        Box {
            // 1. DELETE ACTION (Top Right - Discrete)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.1f))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close, null,
                    tint = Color.White.copy(0.9f),
                    modifier = Modifier.size(12.dp)
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // ZONE 1: VALUE (Main Identity)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "₹${note.denomination}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if(note.isCoin) Color.Black.copy(0.8f) else Color.White
                    )
                }

                // ZONE 2: METADATA STRIP (SN)
                if (!note.isCoin) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(Color.Black.copy(alpha = 0.3f)), // Dark Data Band
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (note.serial.isEmpty()) "INPUT S/N" else note.serial,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontFamily = FontFamily.Monospace, // Tech/Code feel
                            fontWeight = FontWeight.Bold,
                            color = if(note.serial.isEmpty()) Color.White.copy(0.7f) else Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable fun NoteSerialEntry(denom: Int, brandColor: Color, textColor: Color, onSave: (String) -> Unit) { var serial by remember { mutableStateOf("") }; val focusRequester = remember { FocusRequester() }; LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus() }; Column(modifier = Modifier.padding(24.dp).navigationBarsPadding().imePadding()) { Text("Note Serial", style = MaterialTheme.typography.headlineSmall, color = textColor); Text("For ₹$denom Note", color = brandColor); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = serial, onValueChange = { serial = it.uppercase() }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), label = { Text("Serial Number") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (serial.isNotBlank()) onSave(serial) })); Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { if (serial.isNotBlank()) onSave(serial) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = brandColor)) { Text("Save") } } }
@Composable fun CoinQuantitySheet(denom: Int, currentCount: Int, brandColor: Color, textColor: Color, onSave: (Int) -> Unit) { var countText by remember { mutableStateOf(if (currentCount > 0) currentCount.toString() else "") }; val focusRequester = remember { FocusRequester() }; LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus() }; Column(modifier = Modifier.padding(24.dp).navigationBarsPadding().imePadding()) { Text("Coin Quantity", style = MaterialTheme.typography.headlineSmall, color = textColor); Text("For ₹$denom Coins", color = brandColor); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = countText, onValueChange = { if (it.all { c -> c.isDigit() }) countText = it }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), label = { Text("Count") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onSave(countText.toIntOrNull() ?: 0) })); Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { onSave(countText.toIntOrNull() ?: 0) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = brandColor)) { Text("Set Quantity") } } }

// --- HELPERS ---
@Composable private fun CleanInput(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, themeColor: Color, suggestions: List<String>, colors: AccountexColors) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = themeColor, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textSecondary) }; SmartInput(value = value, onValueChange = onValueChange, suggestions = suggestions, modifier = Modifier.fillMaxWidth(), placeholder = "Enter ${label.lowercase()}...") } }
@Composable private fun CleanDetailRow(icon: ImageVector, label: String, value: String, themeColor: Color, colors: AccountexColors, onClick: () -> Unit) { Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(themeColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = themeColor, modifier = Modifier.size(20.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary); Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary) }; Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary) } }
private fun createImageFile(context: Context): Pair<File, Uri> { val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()); val storageDir = File(context.getExternalFilesDir(null), "Accountex_Captures"); if (!storageDir.exists()) storageDir.mkdirs(); val file = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir); val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file); return Pair(file, uri) }