package com.scitech.accountex.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.scitech.accountex.data.CurrencyNote
import com.scitech.accountex.data.TransactionType
import com.scitech.accountex.ui.theme.AccountexColors
import com.scitech.accountex.ui.theme.AppTheme
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.utils.formatDate
import com.scitech.accountex.viewmodel.TransactionDetailViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Int,
    viewModel: TransactionDetailViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val transaction by viewModel.transaction.collectAsState()
    val relatedNotes by viewModel.relatedNotes.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // 🧠 SYSTEM THEME ACCESS
    val colors = AppTheme.colors

    // Listen for ViewModel signals
    val navigationAction by viewModel.navigationEvent.collectAsState()

    // State for viewing images
    var selectedImageUriToZoom by remember { mutableStateOf<String?>(null) }

    // --- INITIALIZATION ---
    LaunchedEffect(transactionId) { viewModel.loadTransaction(transactionId) }

    // --- NAVIGATION HANDLER ---
    LaunchedEffect(navigationAction) {
        when (navigationAction) {
            TransactionDetailViewModel.NavigationAction.NAVIGATE_BACK -> {
                viewModel.consumeNavigationEvent()
                onNavigateBack()
            }
            TransactionDetailViewModel.NavigationAction.NAVIGATE_TO_EDIT -> {
                viewModel.consumeNavigationEvent()
                onNavigateToEdit(transactionId)
            }
            null -> {}
        }
    }

    // Handle Hardware Back Press
    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Evidence Record", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary) } },
                actions = {
                    if (transaction != null) {
                        IconButton(onClick = { viewModel.deleteTransaction() }) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = colors.expense)
                        }
                        IconButton(onClick = { viewModel.onEditClicked() }) {
                            Icon(Icons.Outlined.Edit, "Edit", tint = colors.brandPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (transaction == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = colors.brandPrimary)
            } else {
                val tx = transaction!!
                val accountName = accounts.find { it.id == tx.accountId }?.name ?: "Unknown Account"

                // 🧠 PSYCHOLOGICAL COLOR MAPPING
                val mainColor = when (tx.type) {
                    TransactionType.EXPENSE -> colors.expense
                    TransactionType.INCOME -> colors.income
                    TransactionType.TRANSFER -> colors.brandPrimary
                    else -> colors.brandSecondary
                }

                Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

                    // --- 1. AMOUNT HEADER ---
                    Text("NET VALUE", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(formatCurrency(tx.amount), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = mainColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(color = mainColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(tx.type.name.replace("_", " "), style = MaterialTheme.typography.labelMedium, color = mainColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- 2. DETAILS CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, colors.divider)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                            DetailRow(Icons.Rounded.AccountBalanceWallet, "Account", accountName, mainColor, colors)

                            if (!tx.thirdPartyName.isNullOrEmpty()) DetailRow(Icons.Rounded.Person, "Related Party", tx.thirdPartyName, mainColor, colors)

                            DetailRow(Icons.Default.Category, "Category", tx.category, mainColor, colors)

                            // Date Display
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).background(mainColor.copy(0.1f), CircleShape), Alignment.Center) { Icon(Icons.Default.CalendarToday, null, tint = mainColor) }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Date & Time", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    Text(formatDate(tx.date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                }
                            }

                            if (tx.description.isNotEmpty()) {
                                DetailRow(Icons.Default.Description, "Description", tx.description, mainColor, colors)
                            }
                        }
                    }

                    // --- 3. FORENSIC CASH TRAIL ---
                    val spentNotes = relatedNotes.filter { it.spentTransactionId == tx.id }
                    val receivedNotes = relatedNotes.filter { it.receivedTransactionId == tx.id }

                    if (spentNotes.isNotEmpty() || receivedNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceHighlight), shape = RoundedCornerShape(24.dp)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("PHYSICAL CASH RECORD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                if(spentNotes.isNotEmpty()) NoteGroupRow("Paid using", spentNotes, colors.expense, colors)
                                if(receivedNotes.isNotEmpty()) NoteGroupRow("Received / Change", receivedNotes, colors.income, colors)
                            }
                        }
                    }

                    // --- 4. IMAGE GALLERY (VIEW ONLY) ---
                    if (tx.imageUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        ImageGallerySection(
                            uris = tx.imageUris,
                            colors = colors,
                            onZoomClick = { uri -> selectedImageUriToZoom = uri }
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // --- ZOOM DIALOG ---
    if (selectedImageUriToZoom != null) {
        Dialog(onDismissRequest = { selectedImageUriToZoom = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { selectedImageUriToZoom = null }) {
                AsyncImage(model = Uri.parse(selectedImageUriToZoom), contentDescription = null, modifier = Modifier.fillMaxWidth().align(Alignment.Center), contentScale = ContentScale.Fit)
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun NoteGroupRow(label: String, notes: List<CurrencyNote>, color: Color, colors: AccountexColors) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 6.dp).size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            // Sort Notes by value
            notes.groupBy { it.denomination }.toSortedMap(reverseOrder()).forEach { (denom, list) ->
                // 🧠 SEMANTIC NOTE COLORING
                val noteColor = if(denom < 100) colors.currencyCoin else colors.currencyTier1
                Text("${list.size} x ₹$denom", style = MaterialTheme.typography.bodySmall, color = noteColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ImageGallerySection(uris: List<String>, colors: AccountexColors, onZoomClick: (String) -> Unit) {
    Column {
        Text("Attachments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(uris) { _, uri ->
                AsyncImage(
                    model = Uri.parse(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                        .clickable { onZoomClick(uri) }
                )
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, tint: Color, colors: AccountexColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(tint.copy(0.1f), CircleShape), Alignment.Center) { Icon(icon, null, tint = tint) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        }
    }
}