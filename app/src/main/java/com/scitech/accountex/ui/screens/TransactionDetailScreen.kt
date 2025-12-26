package com.scitech.accountex.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.scitech.accountex.data.CurrencyNote
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.components.SmartInput
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.utils.formatDate
import com.scitech.accountex.viewmodel.TransactionDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Int,
    viewModel: TransactionDetailViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val transaction by viewModel.transaction.collectAsState()
    val relatedNotes by viewModel.relatedNotes.collectAsState()
    val accounts by viewModel.accounts.collectAsState() // NEW: Needed for Account Switcher

    val shouldNavigateBack by viewModel.navigationEvent.collectAsState()

    // Suggestions
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()

    // --- STATE MANAGEMENT ---
    var isEditing by rememberSaveable { mutableStateOf(false) }

    // Form States
    var editAmount by rememberSaveable { mutableStateOf("") }
    var editDate by rememberSaveable { mutableLongStateOf(0L) }
    var editCategory by rememberSaveable { mutableStateOf("") }
    var editDescription by rememberSaveable { mutableStateOf("") }
    var editAccountId by rememberSaveable { mutableIntStateOf(0) } // NEW

    // Image States
    var selectedImageUriToManage by remember { mutableStateOf<String?>(null) }
    var selectedImageUriToZoom by remember { mutableStateOf<String?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // --- INITIALIZATION ---
    LaunchedEffect(transactionId) { viewModel.loadTransaction(transactionId) }

    // Populate form data when transaction is loaded or mode changes
    LaunchedEffect(transaction, isEditing) {
        if (!isEditing && transaction != null) {
            editAmount = transaction!!.amount.toString()
            editDate = transaction!!.date
            editCategory = transaction!!.category
            editDescription = transaction!!.description
            editAccountId = transaction!!.accountId
        }
    }

    LaunchedEffect(shouldNavigateBack) { if (shouldNavigateBack) onNavigateBack() }

    BackHandler {
        if (isEditing) isEditing = false else onNavigateBack()
    }

    // --- LAUNCHERS (Camera/Gallery) ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.addImageUri(tempPhotoUri.toString())
            selectedImageUriToManage = null
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.addImageUri(uri.toString())
            selectedImageUriToManage = null
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val (file, uri) = createImageFile(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission required.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaction" else "Details", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { if(isEditing) isEditing = false else onNavigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (!isEditing && transaction != null) {
                        IconButton(onClick = { viewModel.deleteTransaction() }) { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                        IconButton(onClick = { isEditing = true }) { Icon(Icons.Outlined.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            )
        },
        bottomBar = {
            if (isEditing) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = {
                            val newAmount = editAmount.toDoubleOrNull()
                            if (newAmount != null && newAmount > 0) {
                                // CALLS THE NEW ROBUST VIEWMODEL FUNCTION
                                viewModel.updateFullTransaction(
                                    newAmount = newAmount,
                                    newDate = editDate,
                                    newCategory = editCategory,
                                    newDescription = editDescription,
                                    newAccountId = editAccountId
                                )
                                isEditing = false
                                Toast.makeText(context, "Update Successful", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid Amount", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (transaction == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val tx = transaction!!
                val accountName = accounts.find { it.id == tx.accountId }?.name ?: "Unknown Account"

                // Theme Color Logic
                val mainColor = when (tx.type) {
                    TransactionType.EXPENSE -> Color(0xFFD32F2F)
                    TransactionType.INCOME -> Color(0xFF2E7D32)
                    TransactionType.TRANSFER -> Color(0xFF3B82F6)
                    else -> Color(0xFFF57C00)
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- 1. AMOUNT HEADER ---
                    Text("Amount", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) editAmount = it },
                            textStyle = MaterialTheme.typography.displayMedium.copy(textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = mainColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    } else {
                        Text(formatCurrency(tx.amount), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = mainColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        // TYPE BADGE
                        Surface(color = mainColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text(tx.type.name.replace("_", " "), style = MaterialTheme.typography.labelMedium, color = mainColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- 2. DETAILS CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                            // A. ACCOUNT (Editable)
                            if (isEditing) {
                                AccountSelectorRow(
                                    currentId = editAccountId,
                                    accounts = accounts, // Pass the list
                                    onSelect = { editAccountId = it },
                                    color = mainColor
                                )
                            } else {
                                DetailRow(Icons.Rounded.AccountBalanceWallet, "Account", accountName, mainColor)
                            }

                            // B. PARTY (Read Only)
                            if (!tx.thirdPartyName.isNullOrEmpty()) {
                                DetailRow(Icons.Rounded.Person, "Related Party", tx.thirdPartyName, mainColor)
                            }

                            // C. CATEGORY (Editable)
                            if (isEditing) {
                                EditInputWrapper("Category", Icons.Default.Category, mainColor) {
                                    SmartInput(value = editCategory, onValueChange = { editCategory = it }, suggestions = categorySuggestions, placeholder = "Category")
                                }
                            } else {
                                DetailRow(Icons.Default.Category, "Category", tx.category, mainColor)
                            }

                            // D. DATE (Editable)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = isEditing) {
                                    val c = Calendar.getInstance().apply { timeInMillis = editDate }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        TimePickerDialog(context, { _, h, min ->
                                            c.set(y, m, d, h, min)
                                            editDate = c.timeInMillis
                                        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(48.dp).background(mainColor.copy(0.1f), CircleShape), Alignment.Center) { Icon(Icons.Default.CalendarToday, null, tint = mainColor) }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Date & Time", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(if(isEditing) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(editDate)) else formatDate(tx.date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                if(isEditing) Icon(Icons.Outlined.Edit, null, tint = mainColor, modifier = Modifier.size(20.dp))
                            }

                            // E. DESCRIPTION (Editable)
                            if (isEditing) {
                                EditInputWrapper("Description", Icons.Default.Description, mainColor) {
                                    OutlinedTextField(
                                        value = editDescription,
                                        onValueChange = { editDescription = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Add notes...") }
                                    )
                                }
                            } else if (tx.description.isNotEmpty()) {
                                DetailRow(Icons.Default.Description, "Description", tx.description, mainColor)
                            }
                        }
                    }

                    // --- 3. CASH TRAIL (Read Only context) ---
                    val spentNotes = relatedNotes.filter { it.spentTransactionId == tx.id }
                    val receivedNotes = relatedNotes.filter { it.receivedTransactionId == tx.id }

                    if (!isEditing && (spentNotes.isNotEmpty() || receivedNotes.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("PHYSICAL CASH RECORD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                if(spentNotes.isNotEmpty()) NoteGroupRow("Paid using", spentNotes, Color(0xFFD32F2F))
                                if(receivedNotes.isNotEmpty()) NoteGroupRow("Received / Change", receivedNotes, Color(0xFF2E7D32))
                            }
                        }
                    }

                    // --- 4. ATTACHMENTS ---
                    Spacer(modifier = Modifier.height(24.dp))
                    ImageGallerySection(
                        uris = tx.imageUris,
                        onAddClick = { selectedImageUriToManage = "ADD" },
                        onImageClick = { uri -> selectedImageUriToManage = uri },
                        onZoomClick = { uri -> selectedImageUriToZoom = uri }
                    )

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // --- DIALOGS (Image handling) ---
    if (selectedImageUriToManage != null) {
        val isAdd = selectedImageUriToManage == "ADD"
        AlertDialog(
            onDismissRequest = { selectedImageUriToManage = null },
            title = { Text(if (isAdd) "Add Photo" else "Manage Photo") },
            text = { Column {
                ListItem(headlineContent = { Text("Take New Photo") }, leadingContent = { Icon(Icons.Default.CameraAlt, null) }, modifier = Modifier.clickable { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val (f, u) = createImageFile(context); tempPhotoUri = u; cameraLauncher.launch(u) } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) } })
                ListItem(headlineContent = { Text("Select from Gallery") }, leadingContent = { Icon(Icons.Default.Collections, null) }, modifier = Modifier.clickable { galleryLauncher.launch("image/*") })
                if (!isAdd) {
                    ListItem(headlineContent = { Text("Delete This Photo") }, leadingContent = { Icon(Icons.Outlined.Delete, null, tint = Color.Red) }, modifier = Modifier.clickable { viewModel.removeImageUri(selectedImageUriToManage!!); selectedImageUriToManage = null })
                }
            }},
            confirmButton = { TextButton(onClick = { selectedImageUriToManage = null }) { Text("Cancel") } }
        )
    }

    if (selectedImageUriToZoom != null) {
        Dialog(onDismissRequest = { selectedImageUriToZoom = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { selectedImageUriToZoom = null }) {
                AsyncImage(model = Uri.parse(selectedImageUriToZoom), contentDescription = null, modifier = Modifier.fillMaxWidth().align(Alignment.Center), contentScale = ContentScale.Fit)
            }
        }
    }
}

// ==========================================
// NEW SUB-COMPONENTS
// ==========================================

@Composable
fun AccountSelectorRow(currentId: Int, accounts: List<com.scitech.accountex.data.Account>, onSelect: (Int) -> Unit, color: Color) {
    var expanded by remember { mutableStateOf(false) }
    val currentName = accounts.find { it.id == currentId }?.name ?: "Select Account"

    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).background(color.copy(0.1f), CircleShape), Alignment.Center) {
                Icon(Icons.Rounded.AccountBalanceWallet, null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Account", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(currentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text(acc.name) },
                    onClick = { onSelect(acc.id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun NoteGroupRow(label: String, notes: List<CurrencyNote>, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 6.dp).size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            notes.groupBy { it.denomination }.toSortedMap(reverseOrder()).forEach { (denom, list) ->
                val type = if(denom <= 20) "Coins" else "Notes"
                Text("${list.size} x ₹$denom $type", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ImageGallerySection(uris: List<String>, onAddClick: () -> Unit, onImageClick: (String) -> Unit, onZoomClick: (String) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Attachments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Add, "Add", modifier = Modifier.clickable { onAddClick() })
        }
        Spacer(Modifier.height(8.dp))
        if (uris.isEmpty()) Text("No attachments", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(uris) { idx, uri ->
                AsyncImage(
                    model = Uri.parse(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)).clickable { onZoomClick(uri) }
                )
            }
        }
    }
}

private fun createImageFile(context: Context): Pair<File, Uri> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.getExternalFilesDir(null), "Accountex_Captures"); if (!storageDir.exists()) storageDir.mkdirs()
    val file = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    return Pair(file, uri)
}

@Composable
fun EditInputWrapper(label: String, icon: ImageVector, color: Color, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(color.copy(0.1f), CircleShape), Alignment.Center) { Icon(icon, null, tint = color) }; Spacer(Modifier.width(16.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
        content()
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(tint.copy(0.1f), CircleShape), Alignment.Center) { Icon(icon, null, tint = tint) }
        Spacer(Modifier.width(16.dp))
        Column { Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
    }
}