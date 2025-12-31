package com.scitech.accountex.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. GLOBAL ACCESSOR
object AppTheme {
    val colors: AccountexColors
        @Composable
        get() = LocalAccountexColors.current
}

private val LocalAccountexColors = staticCompositionLocalOf<AccountexColors> {
    error("No AccountexColors provided")
}

// 2. THEME ENUM (UPDATED)
enum class ThemeType {
    Nebula,     // Default
    Cyberpunk,  // Neon
    Nature,     // Calm
    Crimson,    // Power (New)
    Royal,      // Luxury (New)
    Arctic      // Clean (New)
}

// 3. GLOBAL STATE
var CurrentTheme by mutableStateOf(ThemeType.Nebula)

// 4. THEME ENGINE
@Composable
fun AccountexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeType: ThemeType = CurrentTheme,
    content: @Composable () -> Unit
) {
    // Select the Palette based on Mode & Type
    val colors = when (themeType) {
        ThemeType.Nebula -> if (darkTheme) NebulaDarkPalette else NebulaLightPalette
        ThemeType.Cyberpunk -> CyberpunkPalette
        ThemeType.Nature -> NatureLightPalette
        ThemeType.Crimson -> CrimsonPalette
        ThemeType.Royal -> RoyalPalette
        ThemeType.Arctic -> ArcticPalette
    }

    // Map to Material3
    val materialColorScheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.brandPrimary,
            onPrimary = colors.textInverse,
            secondary = colors.actionDominant,
            background = colors.background,
            surface = colors.surfaceCard,
            error = colors.expense
        )
    } else {
        lightColorScheme(
            primary = colors.brandPrimary,
            onPrimary = colors.textInverse,
            secondary = colors.actionDominant,
            background = colors.background,
            surface = colors.surfaceCard,
            error = colors.expense
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !colors.isDark
        }
    }

    CompositionLocalProvider(
        LocalAccountexColors provides colors
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}