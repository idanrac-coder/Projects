package com.novachat.ui.settings

import androidx.lifecycle.ViewModel
import com.novachat.core.sms.WhatsAppForwardSettings
import com.novachat.core.sms.WhatsAppForwarder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val whatsAppForwardSettings: WhatsAppForwardSettings,
    private val whatsAppForwarder: WhatsAppForwarder
) : ViewModel() {

    private val _whatsAppForwardEnabled = MutableStateFlow(whatsAppForwardSettings.isEnabled)
    val whatsAppForwardEnabled: StateFlow<Boolean> = _whatsAppForwardEnabled.asStateFlow()

    val isWhatsAppInstalled: Boolean
        get() = whatsAppForwarder.isWhatsAppInstalled()

    fun setWhatsAppForwardEnabled(enabled: Boolean) {
        whatsAppForwardSettings.isEnabled = enabled
        _whatsAppForwardEnabled.value = enabled
    }
}
