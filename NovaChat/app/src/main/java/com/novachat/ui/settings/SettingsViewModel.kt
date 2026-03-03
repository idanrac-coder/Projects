package com.novachat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.WhatsAppForwardSettings
import com.novachat.core.sms.WhatsAppForwarder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val whatsAppForwardSettings: WhatsAppForwardSettings,
    private val whatsAppForwarder: WhatsAppForwarder,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _whatsAppForwardEnabled = MutableStateFlow(whatsAppForwardSettings.isEnabled)
    val whatsAppForwardEnabled: StateFlow<Boolean> = _whatsAppForwardEnabled.asStateFlow()

    val isPremium: StateFlow<Boolean> = preferencesRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isWhatsAppInstalled: Boolean
        get() = whatsAppForwarder.isWhatsAppInstalled()

    fun setWhatsAppForwardEnabled(enabled: Boolean) {
        whatsAppForwardSettings.isEnabled = enabled
        _whatsAppForwardEnabled.value = enabled
    }
}
