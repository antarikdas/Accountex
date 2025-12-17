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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    val errorMsg by viewModel.errorEvent.collectAsState()
    val shouldNavigateBack by viewModel.navigationEvent.collectAsState()

    // Suggestions for Editing
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()

    // Editing State
    var isEditing by remember { mutableStateOf(false) }
    var editAmount by remember { mutableStateOf("") }
    var editDate by remember { mutableLongStateOf(0L) }
    var editCategory by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    // Image Management State
    var selectedImageUriToManage by remember { mutableStateOf<String?>(null) }
    var selectedImageUriToZoom by remember { mutableStateOf<String?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // --- Image Launchers ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.addImageUri(tempPhotoUri.toString())
            selectedImageUriToManage = null
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (selectedImageUriToManage == "ADD") {
                viewModel.addImageUri(uri.toString())
            } else if (selectedImageUriToManage != null) {
                viewModel.replaceImageUri(selectedImageUriToManage!!, uri.toString())
            }
            selectedImageUriToManage = null
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
            Toast.makeText(context, "Camera permission required.", Toast.LENGTH_SHORT).show()
            selectedImageUriToManage = null
        }
    }

    LaunchedEffect(transactionId) { viewModel.loadTransaction(transactionId) }
    LaunchedEffect(shouldNavigateBack) { if (shouldNavigateBack) onNavigateBack() }
    LaunchedEffect(errorMsg) { if (errorMsg != null) { Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show(); viewModel.clearError() } }

    LaunchedEffect(transaction) {
        transaction?.let {
            editAmount = it.amount.toString()
            editDate = it.date
            editCategory = it.category
            editDescription = it.description
        }
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaction" else "Details", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { if(isEditing) isEditing = false else onNavigateBack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
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
                Button(
                    onClick = {
                        val newAmount = editAmount.toDoubleOrNull()
                        if (newAmount != null && newAmount > 0) {
                            viewModel.updateTransaction(transactionId, newAmount, editDate, editCategory, editDescription)
                            isEditing = false
                            Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, "Invalid Amount", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (transaction == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val tx = transaction!!

                // --- DYNAMIC THEME COLOR ---
                val mainColor = when (tx.type) {
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFFFFA000) // Amber for Third Party
                }

                // --- TYPE LABEL TEXT ---
                val typeLabel = when(tx.type) {
                    TransactionType.INCOME -> "Income"
                    TransactionType.EXPENSE -> "Expense"
                    TransactionType.THIRD_PARTY_IN -> "Held for Others"
                    TransactionType.THIRD_PARTY_OUT -> "Handed Over"
                    else -> "Transfer"
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Amount
                    Text("Amount", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isEditing) {
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            textStyle = MaterialTheme.typography.displayMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            formatCurrency(tx.amount),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = mainColor // Applied Dynamic Color
                        )
                        // Type Badge
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = mainColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                typeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = mainColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                            // 1. Related Party (Only if Third Party)
                            if (!tx.thirdPartyName.isNullOrEmpty()) {
                                DetailRow(Icons.Rounded.Person, "Related Party", tx.thirdPartyName, mainColor)
                            } else if (tx.type == TransactionType.THIRD_PARTY_IN || tx.type == TransactionType.THIRD_PARTY_OUT) {
                                DetailRow(Icons.Rounded.Info, "Status", "No Name Record", mainColor)
                            }

                            // 2. Category (Editable)
                            if (isEditing) {
                                EditInputWrapper(label = "Category", icon = Icons.Default.Category, color = mainColor) {
                                    SmartInput(
                                        value = editCategory,
                                        onValueChange = { editCategory = it },
                                        suggestions = categorySuggestions,
                                        placeholder = "Enter category"
                                    )
                                }
                            } else {
                                DetailRow(Icons.Default.Category, "Category", tx.category, mainColor)
                            }

                            // 3. Date (Editable)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = isEditing) {
                                    val c = Calendar.getInstance().apply { timeInMillis = editDate }
                                    DatePickerDialog(context, { _, y, m, d -> TimePickerDialog(context, { _, h, min -> c.set(y, m, d, h, min); editDate = c.timeInMillis }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(48.dp).background(mainColor.copy(0.1f), CircleShape), Alignment.Center) { Icon(Icons.Default.CalendarToday, null, tint = mainColor) }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Date & Time", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(if(isEditing) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(editDate)) else formatDate(tx.date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if(isEditing) mainColor else MaterialTheme.colorScheme.onSurface)
                                }
                                if(isEditing) { Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.Edit, null, tint = mainColor, modifier = Modifier.size(16.dp)) }
                            }

                            // 4. Description (Editable)
                            if (isEditing) {
                                EditInputWrapper(label = "Description", icon = Icons.Default.Description, color = mainColor) {
                                    SmartInput(
                                        value = editDescription,
                                        onValueChange = { editDescription = it },
                                        suggestions = descriptionSuggestions,
                                        placeholder = "Add description"
                                    )
                                }
                            } else if (tx.description.isNotEmpty()) {
                                DetailRow(Icons.Default.Description, "Description", tx.description, mainColor)
                            }
                        }
                    }

                    // --- IMAGE GALLERY ---
                    Spacer(modifier = Modifier.height(24.dp))
                    ImageGallerySection(
                        uris = tx.imageUris,
                        onAddClick = { selectedImageUriToManage = "ADD" },
                        onImageClick = { uriStr -> selectedImageUriToManage = uriStr },
                        onZoomClick = { uriStr -> selectedImageUriToZoom = uriStr }
                    )
                }
            }
        }
    }

    // --- Image Options Bottom Sheet/Dialog ---
    if (selectedImageUriToManage != null) {
        val isAddMode = selectedImageUriToManage == "ADD"
        AlertDialog(
            onDismissRequest = { selectedImageUriToManage = null },
            title = { Text(if (isAddMode) "Add New Attachment" else "Manage Attachment") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(if (isAddMode) "Take New Photo" else "Replace with Camera Photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val (file, uri) = createImageFile(context)
                                tempPhotoUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Default.Collections, null) },
                        modifier = Modifier.clickable {
                            galleryLauncher.launch("image/*")
                        }
                    )
                    if (!isAddMode) {
                        ListItem(
                            headlineContent = { Text("Delete Photo") },
                            leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                viewModel.removeImageUri(selectedImageUriToManage!!)
                                selectedImageUriToManage = null
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedImageUriToManage = null }) { Text("Cancel") }
            }
        )
    }

    if (selectedImageUriToZoom != null) {
        Dialog(onDismissRequest = { selectedImageUriToZoom = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { selectedImageUriToZoom = null }) {
                AsyncImage(
                    model = Uri.parse(selectedImageUriToZoom),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// ===========================================
// COMPONENTS
// ===========================================

@Composable
private fun ImageGallerySection(
    uris: List<String>,
    onAddClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onZoomClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Attachments (${uris.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uris.isEmpty()) {
            Text("No images attached yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(uris) { index, uriStr ->
                    ImageThumbnail(uriStr, index, onImageClick, onZoomClick)
                }
            }
        }
    }
}

@Composable
private fun ImageThumbnail(uriStr: String, index: Int, onImageClick: (String) -> Unit, onZoomClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onZoomClick(uriStr) }
    ) {
        AsyncImage(
            model = Uri.parse(uriStr),
            contentDescription = "Attachment ${index + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Manage",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onImageClick(uriStr) }
        )
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

@Composable
fun EditInputWrapper(label: String, icon: ImageVector, color: Color, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(color.copy(0.1f), CircleShape), Alignment.Center) {
                Icon(icon, null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        content()
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(tint.copy(0.1f), CircleShape), Alignment.Center) { Icon(icon, null, tint = tint) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}