package com.novachat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.WhatsAppForwardSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagingSettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val whatsAppForwardSettings: WhatsAppForwardSettings
) : ViewModel() {

    val undoSendEnabled: StateFlow<Boolean> = preferencesRepository.undoSendEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _whatsAppForwardEnabled = MutableStateFlow(whatsAppForwardSettings.isEnabled)
    val whatsAppForwardEnabled: StateFlow<Boolean> = _whatsAppForwardEnabled.asStateFlow()

    val calendarLinksEnabled: StateFlow<Boolean> = preferencesRepository.smartLinksCalendarEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val mapsLinksEnabled: StateFlow<Boolean> = preferencesRepository.smartLinksMapsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setUndoSendEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUndoSendEnabled(enabled)
        }
    }

    fun setWhatsAppForwardEnabled(enabled: Boolean) {
        whatsAppForwardSettings.isEnabled = enabled
        _whatsAppForwardEnabled.value = enabled
    }

    fun setCalendarLinksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSmartLinksCalendarEnabled(enabled)
        }
    }

    fun setMapsLinksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSmartLinksMapsEnabled(enabled)
        }
    }
}
