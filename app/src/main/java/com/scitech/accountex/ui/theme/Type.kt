package com.scitech.accountex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Ideally, we would import "Outfit" or "Manrope" here from res/font.
// For now, we tune the System Font to look premium.

val Typography = Typography(
    // 1. HUGE BALANCES (Dashboard Header)
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-1.5).sp // Tight tracking for modern look
    ),

    // 2. SECTION HEADERS ("Recent Transactions")
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.5.sp
    ),

    // 3. CARD TITLES ("Net Savings")
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),

    // 4. BODY TEXT (Descriptions)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    // 5. BUTTONS ("Save Transaction")
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp // Uppercase look
    ),

    // 6. FINANCIAL DATA (The Monospace Fix)
    // Use this style for ALL currency displays
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace, // Ensures numbers align vertically
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    )
)