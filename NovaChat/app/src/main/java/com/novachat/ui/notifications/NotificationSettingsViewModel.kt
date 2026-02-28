package com.novachat.ui.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.PreferencesKeys
import com.novachat.domain.model.GroupingMode
import com.novachat.domain.model.PopupStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val isEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val popupStyle: PopupStyle = PopupStyle.HEADS_UP,
    val groupingMode: GroupingMode = GroupingMode.BY_CONTACT,
    val quickReplyEnabled: Boolean = true,
    val dndEnabled: Boolean = false,
    val dndStartHour: String = "22:00",
    val dndEndHour: String = "07:00"
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _uiState.value = NotificationSettingsUiState(
                    isEnabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                    vibrationEnabled = prefs[PreferencesKeys.VIBRATION_ENABLED] ?: true,
                    popupStyle = PopupStyle.valueOf(
                        prefs[PreferencesKeys.POPUP_STYLE] ?: PopupStyle.HEADS_UP.name
                    ),
                    groupingMode = GroupingMode.valueOf(
                        prefs[PreferencesKeys.GROUPING_MODE] ?: GroupingMode.BY_CONTACT.name
                    ),
                    quickReplyEnabled = prefs[PreferencesKeys.QUICK_REPLY_ENABLED] ?: true,
                    dndEnabled = prefs[PreferencesKeys.DND_ENABLED] ?: false,
                    dndStartHour = prefs[PreferencesKeys.DND_START_HOUR] ?: "22:00",
                    dndEndHour = prefs[PreferencesKeys.DND_END_HOUR] ?: "07:00"
                )
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = enabled }
        }
    }

    fun setPopupStyle(style: PopupStyle) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.POPUP_STYLE] = style.name }
        }
    }

    fun setGroupingMode(mode: GroupingMode) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.GROUPING_MODE] = mode.name }
        }
    }

    fun setQuickReplyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.QUICK_REPLY_ENABLED] = enabled }
        }
    }

    fun setDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.DND_ENABLED] = enabled }
        }
    }

    fun setDndStartHour(hour: String) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.DND_START_HOUR] = hour }
        }
    }

    fun setDndEndHour(hour: String) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.DND_END_HOUR] = hour }
        }
    }
}
