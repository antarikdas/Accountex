package com.scitech.accountex.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.R
import com.scitech.accountex.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var isBiometricEnabled by remember { mutableStateOf(viewModel.isBiometricEnabled()) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- SECTION 1: SECURITY ---
                item {
                    SectionHeader("Security")
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Biometric Login", fontWeight = FontWeight.Bold)
                                Text("Use Fingerprint/FaceID", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = {
                                    isBiometricEnabled = it
                                    viewModel.setBiometricEnabled(it)
                                }
                            )
                        }
                        Divider()
                        SettingsButton(
                            icon = Icons.Default.LockReset,
                            text = "Reset PIN",
                            onClick = {
                                viewModel.clearPin()
                                Toast.makeText(context, "PIN Cleared. Restart app to set new PIN.", Toast.LENGTH_LONG).show()
                            },
                            textColor = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // --- SECTION 2: DATA ---
                item {
                    SectionHeader("Data Management")
                    SettingsCard {
                        SettingsButton(
                            icon = Icons.Default.FileDownload,
                            text = "Export to Excel",
                            onClick = { viewModel.exportToExcel() }
                        )
                        Divider()
                        SettingsButton(
                            icon = Icons.Default.DeleteForever,
                            text = "Factory Reset",
                            onClick = { showResetDialog = true },
                            textColor = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // --- SECTION 3: APP GUIDE (WITH IMAGES) ---
                item {
                    SectionHeader("App Guide")
                    Text(
                        "Swipe horizontally to view guides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val guides = listOf(
                        GuideItem("Dashboard", "Your financial overview.", R.drawable.guide_dashboard),
                        GuideItem("Add Transaction", "Record income & expenses.", R.drawable.guide_add_tx),
                        GuideItem("Ledger", "View full history.", R.drawable.guide_ledger)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(guides) { guide ->
                            GuideCard(guide)
                        }
                    }
                }

                // --- SECTION 4: ABOUT COMPONENT ---
                item {
                    SectionHeader("About Components")
                    SettingsCard {
                        ComponentDescription("Dashboard", "The command center. Shows your current total balance, today's summary, and provides quick access to key actions.")
                        Divider()
                        ComponentDescription("Ledger", "A forensic history of every transaction. Supports filtering, searching, and editing past records.")
                        Divider()
                        ComponentDescription("Analytics", "Visual charts breaking down your spending habits by category and type.")
                        Divider()
                        ComponentDescription("Note Inventory", "A unique feature to track specific currency note serial numbers for high-precision auditing.")
                        Divider()
                        ComponentDescription("Manage Accounts", "Create and edit multiple payment sources like Bank Accounts, Cash Wallets, or Reserve Funds.")
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Accountex v1.0.0 (Pro)",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Factory Reset?") },
            text = { Text("This will DELETE ALL DATA permanently. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.factoryReset()
                        showResetDialog = false
                        Toast.makeText(context, "App Reset. Please Restart.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE EVERYTHING") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(0.dp), content = content)
    }
}

@Composable
fun SettingsButton(icon: ImageVector, text: String, onClick: () -> Unit, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor)
        Spacer(Modifier.width(16.dp))
        Text(text, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ComponentDescription(title: String, desc: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(desc, style = MaterialTheme.typography.bodyMedium)
    }
}

data class GuideItem(val title: String, val desc: String, val imageRes: Int)

@Composable
fun GuideCard(guide: GuideItem) {
    Card(
        modifier = Modifier.size(width = 200.dp, height = 220.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            ) {
                Image(
                    painter = painterResource(id = guide.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(guide.title, fontWeight = FontWeight.Bold)
                Text(guide.desc, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}