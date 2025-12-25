package com.scitech.accountex.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.data.TransactionType

// --- 1. THE BIG CALCULATOR DISPLAY ---
@Composable
fun CalculatorDisplay(amount: String, mainColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ENTER AMOUNT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "₹",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Medium),
                color = mainColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            // The Main Number
            Text(
                text = if (amount.isEmpty()) "0" else amount,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                ),
                color = mainColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- 2. THE NUMERIC KEYPAD ---
@Composable
fun CalculatorKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "⌫")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { key ->
                    KeypadButton(
                        symbol = key,
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (key == "⌫") onBackspaceClick() else onDigitClick(key)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(72.dp) // Large touch targets
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) // Subtle glass feel
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (symbol == "⌫") {
            Icon(Icons.Default.Backspace, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// --- 3. PREMIUM TYPE SWITCHER ---
@Composable
fun NeoTypeSwitcher(currentType: TransactionType, mainColor: Color, onTypeSelected: (TransactionType) -> Unit) {
    val isThirdParty = currentType == TransactionType.THIRD_PARTY_IN || currentType == TransactionType.THIRD_PARTY_OUT
    val isExchange = currentType == TransactionType.EXCHANGE
    val isTransfer = currentType == TransactionType.TRANSFER
    val isPersonal = !isThirdParty && !isExchange && !isTransfer

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Top Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            SwitcherTab("Personal", isPersonal || isTransfer || isExchange, Modifier.weight(1f)) { onTypeSelected(TransactionType.EXPENSE) }
            SwitcherTab("Third Party", isThirdParty, Modifier.weight(1f)) { onTypeSelected(TransactionType.THIRD_PARTY_IN) }
        }

        // Icon Row (Only for Personal Modes)
        if (isPersonal || isExchange || isTransfer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TypeIconTab(Icons.Rounded.ArrowUpward, "Expense", currentType == TransactionType.EXPENSE, MaterialTheme.colorScheme.error) { onTypeSelected(TransactionType.EXPENSE) }
                TypeIconTab(Icons.Rounded.ArrowDownward, "Income", currentType == TransactionType.INCOME, MaterialTheme.colorScheme.tertiary) { onTypeSelected(TransactionType.INCOME) }
                TypeIconTab(Icons.Rounded.SwapHoriz, "Transfer", currentType == TransactionType.TRANSFER, MaterialTheme.colorScheme.primary) { onTypeSelected(TransactionType.TRANSFER) }
                TypeIconTab(Icons.Rounded.CurrencyExchange, "Change", currentType == TransactionType.EXCHANGE, Color(0xFF9C27B0)) { onTypeSelected(TransactionType.EXCHANGE) }
            }
        } else {
            // Third Party Toggle
            Row(modifier = Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer)) {
                SwitcherTab("Receive Money", currentType == TransactionType.THIRD_PARTY_IN, Modifier.weight(1f)) { onTypeSelected(TransactionType.THIRD_PARTY_IN) }
                SwitcherTab("Hand Over", currentType == TransactionType.THIRD_PARTY_OUT, Modifier.weight(1f)) { onTypeSelected(TransactionType.THIRD_PARTY_OUT) }
            }
        }
    }
}

@Composable
private fun SwitcherTab(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val shadow = if (isSelected) 2.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = textColor, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RowScope.TypeIconTab(icon: ImageVector, label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) color else Color.Transparent, label = "bg")
    val contentColor by animateColorAsState(if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, label = "content")
    val weight by animateFloatAsState(if (isSelected) 1.5f else 1f, label = "weight")

    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp))
            AnimatedVisibility(visible = isSelected) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}