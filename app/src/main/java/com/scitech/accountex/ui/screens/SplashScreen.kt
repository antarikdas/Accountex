package com.scitech.accountex.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scitech.accountex.R
import com.scitech.accountex.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    // 🧠 SYSTEM THEME ACCESS
    val colors = AppTheme.colors

    // Animation states
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // 1. Logo Physics (Pop in)
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = { OvershootInterpolator(1.2f).getInterpolation(it) }
                )
            )
        }
        // 2. Logo Fade
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }

        // 3. Text Staggered Fade In (Wait 400ms then fade in)
        delay(400)
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }

        // 4. Hold & Transition
        delay(1600)
        onAnimationFinished()
    }

    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 🧠 IMMERSION: Use the core gradient logic for the background
            .background(colors.headerGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LOGO
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Accountex Logo",
                modifier = Modifier
                    .size(180.dp) // Slightly smaller to make room for text
                    .scale(scale.value)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // BRAND NAME
            Text(
                text = "ACCOUNTEX",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White, // Always White on the gradient
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp, // Premium tracking
                modifier = Modifier.alpha(textAlpha.value)
            )

            Text(
                text = "Financial Command Center",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}