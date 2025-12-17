package com.scitech.accountex.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.components.SmartInput
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.utils.formatDate
import com.scitech.accountex.viewmodel.TransactionDetailViewModel
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

    // Image Zoom State
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

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
                            color = if (tx.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                            // Category (Editable)
                            if (isEditing) {
                                EditInputWrapper(label = "Category", icon = Icons.Default.Category) {
                                    SmartInput(
                                        value = editCategory,
                                        onValueChange = { editCategory = it },
                                        suggestions = categorySuggestions,
                                        placeholder = "Enter category"
                                    )
                                }
                            } else {
                                DetailRow(Icons.Default.Category, "Category", tx.category)
                            }

                            // Date (Editable)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = isEditing) {
                                    val c = Calendar.getInstance().apply { timeInMillis = editDate }
                                    DatePickerDialog(context, { _, y, m, d -> TimePickerDialog(context, { _, h, min -> c.set(y, m, d, h, min); editDate = c.timeInMillis }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), Alignment.Center) { Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary) }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Date & Time", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(if(isEditing) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(editDate)) else formatDate(tx.date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if(isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                }
                                if(isEditing) { Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                            }

                            // Description (Editable)
                            if (isEditing) {
                                EditInputWrapper(label = "Description", icon = Icons.Default.Description) {
                                    SmartInput(
                                        value = editDescription,
                                        onValueChange = { editDescription = it },
                                        suggestions = descriptionSuggestions,
                                        placeholder = "Add description"
                                    )
                                }
                            } else if (tx.description.isNotEmpty()) {
                                DetailRow(Icons.Default.Description, "Description", tx.description)
                            }
                        }
                    }

                    // --- IMAGE GALLERY ---
                    if (tx.imageUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Attachments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.align(Alignment.Start)) {
                            items(tx.imageUris) { uriStr ->
                                AsyncImage(
                                    model = Uri.parse(uriStr),
                                    contentDescription = "Attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray)
                                        .clickable { selectedImageUri = uriStr }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Image Dialog
    if (selectedImageUri != null) {
        Dialog(onDismissRequest = { selectedImageUri = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { selectedImageUri = null }) {
                AsyncImage(
                    model = Uri.parse(selectedImageUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// Helper wrapper to show label/icon in Edit Mode (since SmartInput no longer does it)
@Composable
fun EditInputWrapper(label: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        content()
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}