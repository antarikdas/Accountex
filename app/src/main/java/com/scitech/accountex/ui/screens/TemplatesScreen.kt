package com.scitech.accountex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.data.TransactionTemplate
import com.scitech.accountex.ui.theme.AccountexColors
import com.scitech.accountex.ui.theme.AppTheme
import com.scitech.accountex.utils.formatCurrency
import com.scitech.accountex.viewmodel.TemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: TemplateViewModel,
    onNavigateBack: () -> Unit,
    onTemplateSelect: (TransactionTemplate) -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // 🧠 SYSTEM THEME ACCESS
    val colors = AppTheme.colors

    BackHandler { onNavigateBack() }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (templates.isEmpty()) {
                EmptyStateMessage(modifier = Modifier.fillMaxSize(), colors = colors)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(templates) { template ->
                        val accountName =
                            accounts.find { it.id == template.accountId }?.name ?: "Unknown A/c"

                        TemplateGridCard(
                            template = template,
                            accountName = accountName,
                            colors = colors,
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
    accountName: String,
    colors: AccountexColors,
    onSelect: (TransactionTemplate) -> Unit,
    onDelete: () -> Unit
) {
    // 🧠 SMART INFERENCE (Uses System Colors)
    val (accentColor, icon) = getSmartStyle(template.category, colors)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceCard)
            .border(1.dp, colors.divider, RoundedCornerShape(20.dp))
            .clickable { onSelect(template) }
    ) {
        // Delete Action
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Icon(
                Icons.Rounded.Close,
                "Delete",
                tint = colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Glow Container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Column {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    template.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Amount
                Text(
                    formatCurrency(template.defaultAmount),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = accentColor, // Matches category psychology
                    fontWeight = FontWeight.SemiBold
                )

                // Account Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surfaceHighlight)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        accountName.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        fontSize = 9.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────── */
/* 🧠 SMART COLOR INFERENCE — PSYCHOLOGICAL MAPPING               */
/* ────────────────────────────────────────────────────────────── */

@Composable
fun getSmartStyle(category: String, colors: AccountexColors): Pair<Color, ImageVector> {
    val cat = category.trim().lowercase()
    return when {
        // Income = Green/Growth
        "salary" in cat || "income" in cat || "interest" in cat ->
            Pair(colors.income, Icons.Rounded.ArrowDownward)

        // Transfers = Primary/Teal
        "transfer" in cat ->
            Pair(colors.brandPrimary, Icons.Rounded.SyncAlt)

        // Expenses = Red/Loss
        "food" in cat ->
            Pair(colors.expense, Icons.Rounded.Restaurant)
        "transport" in cat ->
            Pair(colors.expense, Icons.Rounded.DirectionsCar)
        "shopping" in cat ->
            Pair(colors.expense, Icons.Rounded.ShoppingBag)
        "health" in cat ->
            Pair(colors.expense, Icons.Rounded.MedicalServices)

        // Default = Secondary/Purple
        else -> Pair(colors.brandSecondary, Icons.Rounded.Bolt)
    }
}

@Composable
fun EmptyStateMessage(modifier: Modifier = Modifier, colors: AccountexColors) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHighlight)
                    .border(
                        1.dp,
                        colors.divider,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PostAdd,
                    null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "No Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Save frequent transactions as templates.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}