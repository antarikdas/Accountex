package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.data.TransactionTemplate
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.TemplateViewModel

// Premium Palette for Templates
private val TmplBg = Color(0xFFF8FAFC)
private val SlateText = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: TemplateViewModel,
    onNavigateBack: () -> Unit,
    onTemplateSelect: (TransactionTemplate) -> Unit
) {
    val templates by viewModel.templates.collectAsState()

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = TmplBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quick Actions", fontWeight = FontWeight.Bold, color = SlateText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SlateText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = TmplBg)
            )
        }
    ) { paddingValues ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PostAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("No Quick Actions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SlateText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Save frequent transactions as templates from the 'Add Transaction' screen to see them here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Text(
                    "TAP TO APPLY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(templates) { template ->
                        TemplateGridCard(
                            template = template,
                            onSelect = onTemplateSelect,
                            onDelete = { viewModel.deleteTemplate(template) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateGridCard(
    template: TransactionTemplate,
    onSelect: (TransactionTemplate) -> Unit,
    onDelete: () -> Unit
) {
    val (icon, color) = getCategoryStyle(template.category)

    Surface(
        onClick = { onSelect(template) },
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.aspectRatio(0.85f) // Slightly tall square
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Delete Icon (Top Right)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Icon(Icons.Rounded.Close, "Delete", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big Colorful Icon
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text Content
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    template.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Price Tag
                Surface(
                    color = color.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        formatCurrency(template.defaultAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = color,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// --- HELPER: Smart Category Styles ---
// Returns an Icon and Color based on the category text
fun getCategoryStyle(category: String): Pair<ImageVector, Color> {
    val cat = category.trim().lowercase()
    return when {
        "food" in cat || "dining" in cat || "restaurant" in cat -> Pair(Icons.Rounded.Restaurant, Color(0xFFFF9800)) // Orange
        "transport" in cat || "travel" in cat || "fuel" in cat -> Pair(Icons.Rounded.DirectionsCar, Color(0xFF2196F3)) // Blue
        "bill" in cat || "recharge" in cat || "rent" in cat -> Pair(Icons.Rounded.Receipt, Color(0xFFE91E63)) // Pink
        "shopping" in cat || "cloth" in cat -> Pair(Icons.Rounded.ShoppingBag, Color(0xFF9C27B0)) // Purple
        "health" in cat || "medical" in cat -> Pair(Icons.Rounded.MedicalServices, Color(0xFFF44336)) // Red
        "salary" in cat || "income" in cat -> Pair(Icons.Rounded.AttachMoney, Color(0xFF4CAF50)) // Green
        "entertainment" in cat || "movie" in cat -> Pair(Icons.Rounded.Movie, Color(0xFF3F51B5)) // Indigo
        else -> Pair(Icons.Rounded.Star, Color(0xFF607D8B)) // Default Slate
    }
}