package com.scitech.accountex.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.scitech.accountex.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    // Animation states: Start scaled down (0.3f) and transparent (0f)
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // 1. Start Animations in parallel
        launch {
            // Scale up with a slight "overshoot" bounce effect for a premium feel
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = { OvershootInterpolator(1.5f).getInterpolation(it) }
                )
            )
        }
        launch {
            // Smooth fade in
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }

        // 2. Wait for animation + a small pause so it doesn't feel rushed
        delay(1800)

        // 3. Navigate away
        onAnimationFinished()
    }

    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Use the theme background color (e.g., your dark slate blue)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            // Ensure 'app_logo.png' is in res/drawable
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Accountex Logo",
            modifier = Modifier
                .size(250.dp) // Adjust size as needed based on your logo shape
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}