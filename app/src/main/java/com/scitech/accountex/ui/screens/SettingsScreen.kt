package com.scitech.accountex.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.R
import com.scitech.accountex.ui.theme.AccountexColors
import com.scitech.accountex.ui.theme.AppTheme
import com.scitech.accountex.ui.theme.CurrentTheme
import com.scitech.accountex.ui.theme.ThemeType
import com.scitech.accountex.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // 🧠 SYSTEM THEME ACCESS
    val colors = AppTheme.colors

    var isBiometricEnabled by remember { mutableStateOf(viewModel.isBiometricEnabled()) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Control Room", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.brandPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(colors.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- SECTION 0: APPEARANCE ---
                item {
                    SectionHeader("Visual Environment", colors)
                    SettingsCard(colors) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Active Identity", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    Text("Select your workspace atmosphere", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                                // Active Theme Indicator
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.headerGradient)
                                        .border(2.dp, colors.divider, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // THEME SELECTOR BUTTONS (GRID LAYOUT)
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Row 1
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ThemeOptionBtn("Nebula", Color(0xFF00897B), CurrentTheme == ThemeType.Nebula, colors) { viewModel.setTheme(ThemeType.Nebula) }
                                    ThemeOptionBtn("Cyber", Color(0xFFFF00FF), CurrentTheme == ThemeType.Cyberpunk, colors) { viewModel.setTheme(ThemeType.Cyberpunk) }
                                    ThemeOptionBtn("Nature", Color(0xFF2E7D32), CurrentTheme == ThemeType.Nature, colors) { viewModel.setTheme(ThemeType.Nature) }
                                }
                                // Row 2
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ThemeOptionBtn("Crimson", Color(0xFFD50000), CurrentTheme == ThemeType.Crimson, colors) { viewModel.setTheme(ThemeType.Crimson) }
                                    ThemeOptionBtn("Royal", Color(0xFF1A237E), CurrentTheme == ThemeType.Royal, colors) { viewModel.setTheme(ThemeType.Royal) }
                                    ThemeOptionBtn("Arctic", Color(0xFF00B0FF), CurrentTheme == ThemeType.Arctic, colors) { viewModel.setTheme(ThemeType.Arctic) }
                                }
                            }
                        }
                    }
                }

                // ... (Rest of existing Settings sections: Security, Data, Guide) ...
                // --- SECTION 1: SECURITY ---
                item {
                    SectionHeader("Security Protocol", colors)
                    SettingsCard(colors) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Biometric Access", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Text("Fingerprint / FaceID", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = {
                                    isBiometricEnabled = it
                                    viewModel.setBiometricEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.brandPrimary,
                                    checkedTrackColor = colors.brandPrimary.copy(alpha = 0.3f),
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceHighlight
                                )
                            )
                        }
                        HorizontalDivider(color = colors.divider)
                        SettingsButton(
                            icon = Icons.Default.LockReset,
                            text = "Reset PIN",
                            onClick = {
                                viewModel.clearPin()
                                Toast.makeText(context, "PIN Cleared. Restart app to set new PIN.", Toast.LENGTH_LONG).show()
                            },
                            textColor = colors.expense,
                            colors = colors
                        )
                    }
                }

                // --- SECTION 2: DATA ---
                item {
                    SectionHeader("Data Management", colors)
                    SettingsCard(colors) {
                        SettingsButton(
                            icon = Icons.Default.FileDownload,
                            text = "Export to Excel",
                            onClick = { viewModel.exportToExcel() },
                            textColor = colors.textPrimary,
                            colors = colors
                        )
                        HorizontalDivider(color = colors.divider)
                        SettingsButton(
                            icon = Icons.Default.DeleteForever,
                            text = "Factory Reset",
                            onClick = { showResetDialog = true },
                            textColor = colors.expense,
                            colors = colors
                        )
                    }
                }

                // --- SECTION 3: APP GUIDE ---
                item {
                    SectionHeader("Operational Manual", colors)
                    Text(
                        "Swipe horizontally to view guides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val guides = listOf(
                        GuideItem("Dashboard", "Your financial overview.", R.drawable.guide_dashboard),
                        GuideItem("Add Transaction", "Record income & expenses.", R.drawable.guide_add_tx),
                        GuideItem("Ledger", "View full history.", R.drawable.guide_ledger)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(guides) { guide ->
                            GuideCard(guide, colors)
                        }
                    }
                }

                // --- SECTION 4: ABOUT COMPONENT ---
                item {
                    SectionHeader("System Architecture", colors)
                    SettingsCard(colors) {
                        ComponentDescription("Dashboard", "The command center. Shows your current total balance, today's summary, and provides quick access to key actions.", colors)
                        HorizontalDivider(color = colors.divider)
                        ComponentDescription("Ledger", "A forensic history of every transaction. Supports filtering, searching, and editing past records.", colors)
                        HorizontalDivider(color = colors.divider)
                        ComponentDescription("Analytics", "Visual charts breaking down your spending habits by category and type.", colors)
                        HorizontalDivider(color = colors.divider)
                        ComponentDescription("Note Inventory", "A unique feature to track specific currency note serial numbers for high-precision auditing.", colors)
                        HorizontalDivider(color = colors.divider)
                        ComponentDescription("Manage Accounts", "Create and edit multiple payment sources like Bank Accounts, Cash Wallets, or Reserve Funds.", colors)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Accountex v1.0.0 (Nebula)",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            containerColor = colors.surfaceCard,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
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
                    colors = ButtonDefaults.buttonColors(containerColor = colors.expense)
                ) { Text("DELETE EVERYTHING", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = colors.textSecondary) }
            }
        )
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun ThemeOptionBtn(label: String, color: Color, isSelected: Boolean, colors: AccountexColors, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(if(isSelected) 3.dp else 0.dp, if(isSelected) color else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if(isSelected) colors.textPrimary else colors.textSecondary
        )
    }
}

@Composable
fun SectionHeader(title: String, colors: AccountexColors) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.brandPrimary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(colors: AccountexColors, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.divider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(0.dp), content = content)
    }
}

@Composable
fun SettingsButton(icon: ImageVector, text: String, onClick: () -> Unit, textColor: Color, colors: AccountexColors) {
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
fun ComponentDescription(title: String, desc: String, colors: AccountexColors) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}

data class GuideItem(val title: String, val desc: String, val imageRes: Int)

@Composable
fun GuideCard(guide: GuideItem, colors: AccountexColors) {
    Card(
        modifier = Modifier.size(width = 200.dp, height = 220.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.divider),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.surfaceHighlight)
            ) {
                // Placeholder if image fails, or real image
                Image(
                    painter = painterResource(id = guide.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(guide.title, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(guide.desc, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = colors.textSecondary)
            }
        }
    }
}