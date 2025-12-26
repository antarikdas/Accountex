package com.scitech.accountex.utils

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("accountex_secure_prefs", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = prefs.contains("user_pin")

    fun getPin(): String = prefs.getString("user_pin", "") ?: ""

    fun setPin(pin: String) {
        prefs.edit().putString("user_pin", pin).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }
}