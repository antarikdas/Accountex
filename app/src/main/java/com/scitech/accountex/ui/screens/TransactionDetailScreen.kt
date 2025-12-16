package com.scitech.accountex.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.data.Transaction
import com.scitech.accountex.data.TransactionType
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

    // Editing State
    var isEditing by remember { mutableStateOf(false) }
    var editAmount by remember { mutableStateOf("") }
    var editDate by remember { mutableLongStateOf(0L) }

    // Initial Load
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    // Handle Delete Success / Error
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) onNavigateBack()
    }
    LaunchedEffect(errorMsg) {
        if (errorMsg != null) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Sync edit state when transaction loads
    LaunchedEffect(transaction) {
        transaction?.let {
            editAmount = it.amount.toString()
            editDate = it.date
        }
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaction" else "Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if(isEditing) isEditing = false else onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isEditing && transaction != null) {
                        // DELETE BUTTON
                        IconButton(onClick = { viewModel.deleteTransaction() }) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                        // EDIT BUTTON
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Outlined.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (isEditing) {
                Button(
                    onClick = {
                        val newAmount = editAmount.toDoubleOrNull()
                        if (newAmount != null && newAmount > 0) {
                            viewModel.updateTransaction(transactionId, newAmount, editDate)
                            isEditing = false
                            Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid Amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (transaction == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val tx = transaction!!

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- AMOUNT DISPLAY / EDIT ---
                    Text("Amount", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            textStyle = MaterialTheme.typography.displayMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
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

                    // --- DETAILS CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                            // Category
                            DetailRow(
                                icon = Icons.Default.Category,
                                label = "Category",
                                value = tx.category
                            )

                            // Date (Editable)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = isEditing) {
                                    val calendar = Calendar.getInstance()
                                    calendar.timeInMillis = editDate
                                    DatePickerDialog(context, { _, y, m, d ->
                                        TimePickerDialog(context, { _, h, min ->
                                            calendar.set(y, m, d, h, min)
                                            editDate = calendar.timeInMillis
                                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Date & Time", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(
                                        if(isEditing) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(editDate)) else formatDate(tx.date),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if(isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if(isEditing) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }

                            // Description
                            if (tx.description.isNotEmpty() || isEditing) {
                                DetailRow(
                                    icon = Icons.Default.Description,
                                    label = "Description",
                                    value = tx.description.ifEmpty { "No Description" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}