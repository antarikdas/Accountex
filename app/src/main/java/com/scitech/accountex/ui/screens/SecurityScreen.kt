package com.scitech.accountex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scitech.accountex.utils.BiometricAuth
import com.scitech.accountex.viewmodel.SecurityState
import com.scitech.accountex.viewmodel.SecurityViewModel

@Composable
fun SecurityScreen(onLoginSuccess: () -> Unit) {
    val viewModel: SecurityViewModel = viewModel()
    val state by viewModel.screenState.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val headerMessage by viewModel.headerMessage.collectAsState()

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // 1. Handle Navigation Success
    LaunchedEffect(state) {
        if (state == SecurityState.SUCCESS) onLoginSuccess()
    }

    // 2. Auto-Trigger Biometric (Only on LOGIN state)
    LaunchedEffect(Unit) {
        if (state == SecurityState.LOGIN && viewModel.isBiometricEnabled() && activity != null) {
            BiometricAuth.authenticate(
                activity = activity,
                onSuccess = { viewModel.onBiometricSuccess() },
                onError = { /* Ignore errors, let user use PIN */ },
                onUsePin = { /* Just stay on PIN screen */ }
            )
        }
    }

    // 3. Setup Dialog (Offer Biometric)
    if (state == SecurityState.OFFER_BIOMETRIC) {
        AlertDialog(
            onDismissRequest = { viewModel.onBiometricChoice(false) },
            icon = { Icon(Icons.Default.Fingerprint, null) },
            title = { Text("Enable Fingerprint?") },
            text = { Text("Login faster next time using your fingerprint.") },
            confirmButton = { Button(onClick = { viewModel.onBiometricChoice(true) }) { Text("Yes, Enable") } },
            dismissButton = { TextButton(onClick = { viewModel.onBiometricChoice(false) }) { Text("Skip") } }
        )
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.5f))

        // Lock Icon
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))

        // Header Text
        Text(headerMessage, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        // PIN Dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                val filled = i < pinInput.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, if(filled) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Numeric Keypad
        Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("BIO", "0", "DEL"))

            for (row in rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (key in row) {
                        KeypadButton(key, viewModel, state == SecurityState.LOGIN && viewModel.isBiometricEnabled(), activity)
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun KeypadButton(key: String, viewModel: SecurityViewModel, showBiometric: Boolean, activity: FragmentActivity?) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(enabled = key != "") {
                when (key) {
                    "DEL" -> viewModel.onBackspaceClick()
                    "BIO" -> {
                        if (showBiometric && activity != null) {
                            BiometricAuth.authenticate(
                                activity,
                                onSuccess = { viewModel.onBiometricSuccess() },
                                onError = {},
                                onUsePin = {}
                            )
                        }
                    }
                    else -> viewModel.onDigitClick(key)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (key == "DEL") {
            Icon(Icons.Default.Backspace, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (key == "BIO") {
            if (showBiometric) {
                Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        } else {
            Text(key, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        }
    }
}