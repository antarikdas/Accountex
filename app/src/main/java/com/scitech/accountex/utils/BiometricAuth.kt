package com.scitech.accountex.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Accountex Locked",
        subtitle: String = "Verify your identity",
        negativeButtonText: String = "Use PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUsePin: () -> Unit // New callback for fallback
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricManager = BiometricManager.from(activity)

        // Hardware Check
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> { /* Proceed */ }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // If hardware is missing or no fingerprint enrolled, fallback to PIN immediately
                onUsePin()
                return
            }
            else -> {
                onError("Biometric authentication unavailable.")
                return
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user clicked "Use PIN" (Negative Button)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        onUsePin()
                    } else {
                        onError(errString.toString())
                    }
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    // Check if hardware exists to decide whether to offer enrollment
    fun canAuthenticate(context: android.content.Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
}