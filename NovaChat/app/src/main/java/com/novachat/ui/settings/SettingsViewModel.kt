package com.novachat.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.WhatsAppForwardSettings
import com.novachat.core.sms.WhatsAppForwarder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _whatsAppForwardEnabled = MutableStateFlow(whatsAppForwardSettings.isEnabled)
    val whatsAppForwardEnabled: StateFlow<Boolean> = _whatsAppForwardEnabled.asStateFlow()

    val isPremium: StateFlow<Boolean> = preferencesRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val undoSendEnabled: StateFlow<Boolean> = preferencesRepository.undoSendEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val appLanguage: StateFlow<String> = preferencesRepository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isWhatsAppInstalled: Boolean
        get() = whatsAppForwarder.isWhatsAppInstalled()

    fun setWhatsAppForwardEnabled(enabled: Boolean) {
        whatsAppForwardSettings.isEnabled = enabled
        _whatsAppForwardEnabled.value = enabled
    }

    fun setUndoSendEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUndoSendEnabled(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setAppLanguage(languageTag: String) {
        viewModelScope.launch {
            preferencesRepository.setAppLanguage(languageTag)
            val localeList = if (languageTag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(languageTag)
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
}
