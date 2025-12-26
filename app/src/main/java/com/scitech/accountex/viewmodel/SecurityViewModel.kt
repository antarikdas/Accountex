package com.scitech.accountex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.scitech.accountex.utils.BiometricAuth
import com.scitech.accountex.utils.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SecurityPreferences(application)
    private val context = application.applicationContext

    // STATES
    private val _screenState = MutableStateFlow(SecurityState.LOADING)
    val screenState: StateFlow<SecurityState> = _screenState.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _headerMessage = MutableStateFlow("")
    val headerMessage: StateFlow<String> = _headerMessage.asStateFlow()

    private var tempPin: String? = null // For "Confirm PIN" step

    init {
        determineInitialState()
    }

    private fun determineInitialState() {
        if (prefs.isPinSet()) {
            _screenState.value = SecurityState.LOGIN
            _headerMessage.value = "Enter PIN"
        } else {
            _screenState.value = SecurityState.SETUP_ENTER
            _headerMessage.value = "Create New PIN"
        }
    }

    // --- KEYPAD INPUT ---
    fun onDigitClick(digit: String) {
        if (_pinInput.value.length < 4) {
            val current = _pinInput.value + digit
            _pinInput.value = current

            if (current.length == 4) {
                processPin(current)
            }
        }
    }

    fun onBackspaceClick() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
        }
    }

    // --- LOGIC ENGINE ---
    private fun processPin(pin: String) {
        when (_screenState.value) {
            SecurityState.LOGIN -> {
                if (pin == prefs.getPin()) {
                    _screenState.value = SecurityState.SUCCESS
                } else {
                    _headerMessage.value = "Wrong PIN. Try again."
                    _pinInput.value = ""
                    // Visual shake animation could be triggered here via a side-effect
                }
            }
            SecurityState.SETUP_ENTER -> {
                tempPin = pin
                _pinInput.value = ""
                _headerMessage.value = "Confirm PIN"
                _screenState.value = SecurityState.SETUP_CONFIRM
            }
            SecurityState.SETUP_CONFIRM -> {
                if (pin == tempPin) {
                    prefs.setPin(pin)
                    // If hardware supports biometric, ask to enable it. Else finish.
                    if (BiometricAuth.canAuthenticate(context)) {
                        _screenState.value = SecurityState.OFFER_BIOMETRIC
                    } else {
                        _screenState.value = SecurityState.SUCCESS
                    }
                } else {
                    _headerMessage.value = "Mismatch! Create PIN again."
                    _pinInput.value = ""
                    tempPin = null
                    _screenState.value = SecurityState.SETUP_ENTER
                }
            }
            else -> {}
        }
    }

    // --- BIOMETRIC ACTIONS ---
    fun onBiometricChoice(enabled: Boolean) {
        prefs.setBiometricEnabled(enabled)
        _screenState.value = SecurityState.SUCCESS
    }

    fun isBiometricEnabled() = prefs.isBiometricEnabled()
    fun onBiometricSuccess() { _screenState.value = SecurityState.SUCCESS }
}

enum class SecurityState { LOADING, LOGIN, SETUP_ENTER, SETUP_CONFIRM, OFFER_BIOMETRIC, SUCCESS }