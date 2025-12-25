package com.scitech.accountex.ui.theme

import androidx.compose.ui.graphics.Color

// --- BRAND IDENTITY (Neo-FinTech) ---
val EmeraldPrimary      = Color(0xFF006C5B) // Stability, Wealth
val EmeraldContainer    = Color(0xFF66FBF0)
val EmeraldDark         = Color(0xFF80F2DD) // For Dark Mode

val MidnightSurface     = Color(0xFF0F172A) // Deep OLED Background
val SlateSurface        = Color(0xFFF8FAFC) // Clean Light Background
val SlateText           = Color(0xFF1E293B) // High contrast text

// --- SEMANTIC COLORS (The Financial Logic) ---
// We use Gold for Income (Value) instead of generic Green
val IncomeGold          = Color(0xFFD4AF37)
val IncomeContainer     = Color(0xFFFFF3C9)

// We use Coral for Expense (Urgent but calm) instead of aggressive Red
val ExpenseCoral        = Color(0xFFFF6B6B)
val ExpenseContainer    = Color(0xFFFFDAD6)

val InfoBlue            = Color(0xFF3B82F6)
val NeutralGray         = Color(0xFF64748B)

// --- MATERIAL 3 MAPPING ---

// Light Scheme
val LightPrimary        = EmeraldPrimary
val LightOnPrimary      = Color.White
val LightBackground     = SlateSurface
val LightSurface        = Color.White
val LightError          = ExpenseCoral

// Dark Scheme
val DarkPrimary         = EmeraldDark
val DarkOnPrimary       = Color(0xFF003730)
val DarkBackground      = MidnightSurface
val DarkSurface         = Color(0xFF1E293B) // Slightly lighter than background
val DarkError           = Color(0xFFFFB4AB)