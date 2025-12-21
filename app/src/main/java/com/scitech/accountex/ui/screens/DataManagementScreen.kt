package com.scitech.accountex.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.scitech.accountex.viewmodel.DataManagementViewModel
import java.text.SimpleDateFormat
import java.util.*

// Theme Colors
private val DarkBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val GoldAccent = Color(0xFFFFAB40)
private val BlueAccent = Color(0xFF448AFF)
private val TextSlate = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: DataManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Launchers for ZIP files
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportData(it) } }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importData(it) } }

    // Side Effect for Toasts based on State
    LaunchedEffect(uiState) {
        if (uiState.isSuccess) {
            Toast.makeText(context, uiState.statusMessage, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        } else if (uiState.isError) {
            Toast.makeText(context, uiState.statusMessage, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Data Safety", fontWeight = FontWeight.Bold, color = TextSlate) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSlate)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // INFO CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFF00695C))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "This creates a ZIP archive with your data and all images.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF004D40)
                        )
                    }
                }

                Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextSlate)

                // BACKUP CARD
                ActionCard(
                    title = "Create Full Backup",
                    subtitle = "Save data & images to a ZIP file.",
                    icon = Icons.Default.Backup,
                    color = GoldAccent,
                    onClick = {
                        val fileName = "Accountex_FullBackup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.zip"
                        createBackupLauncher.launch(fileName)
                    }
                )

                // RESTORE CARD
                ActionCard(
                    title = "Restore Backup",
                    subtitle = "Import a ZIP file. Overwrites current data.",
                    icon = Icons.Default.Restore,
                    color = BlueAccent,
                    onClick = {
                        // Accept standard zip MIME types
                        restoreBackupLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                    }
                )
            }

            // LOADING OVERLAY
            if (uiState.isLoading) {
                Dialog(onDismissRequest = {}) {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BlueAccent)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(uiState.statusMessage, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextSlate)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}