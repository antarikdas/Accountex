package com.scitech.accountex.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    surface = LightSurface,
    error = LightError,
    // Semantic mappings for custom components
    tertiary = IncomeGold,
    tertiaryContainer = IncomeContainer,
    errorContainer = ExpenseContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    error = DarkError,
    // Semantic mappings
    tertiary = IncomeGold,
    tertiaryContainer = Color(0xFF422D00), // Darker gold container
    errorContainer = Color(0xFF93000A)
)

@Composable
fun AccountexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // EDGE-TO-EDGE: Transparent Status Bar
            window.statusBarColor = Color.Transparent.toArgb()

            // Icon Colors: Dark icons in Light Mode, Light icons in Dark Mode
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}