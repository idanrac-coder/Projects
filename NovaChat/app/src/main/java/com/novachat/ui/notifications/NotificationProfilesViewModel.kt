package com.novachat.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.NotificationProfileDao
import com.novachat.core.database.entity.NotificationProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationProfilesUiState(
    val profiles: List<NotificationProfileEntity> = emptyList(),
    val showCreateDialog: Boolean = false,
    val editingProfile: NotificationProfileEntity? = null
)

@HiltViewModel
class NotificationProfilesViewModel @Inject constructor(
    private val dao: NotificationProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationProfilesUiState())
    val uiState: StateFlow<NotificationProfilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllProfiles().collect { profiles ->
                _uiState.value = _uiState.value.copy(profiles = profiles)
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, editingProfile = null)
    }

    fun showEditDialog(profile: NotificationProfileEntity) {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, editingProfile = profile)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, editingProfile = null)
    }

    fun createProfile(name: String, vibration: Boolean, priority: String, popup: Boolean, startH: Int?, startM: Int?, endH: Int?, endM: Int?) {
        viewModelScope.launch {
            dao.insert(
                NotificationProfileEntity(
                    name = name,
                    vibrationEnabled = vibration,
                    priority = priority,
                    popupEnabled = popup,
                    scheduleStartHour = startH,
                    scheduleStartMinute = startM,
                    scheduleEndHour = endH,
                    scheduleEndMinute = endM
                )
            )
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }

    fun updateProfile(id: Long, name: String, vibration: Boolean, priority: String, popup: Boolean, startH: Int?, startM: Int?, endH: Int?, endM: Int?) {
        viewModelScope.launch {
            dao.update(id, name, vibration, priority, popup, startH, startM, endH, endM)
            _uiState.value = _uiState.value.copy(showCreateDialog = false, editingProfile = null)
        }
    }

    fun activateProfile(id: Long) {
        viewModelScope.launch {
            dao.deactivateAll()
            dao.activate(id)
        }
    }

    fun deactivateAll() {
        viewModelScope.launch {
            dao.deactivateAll()
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            dao.delete(id)
        }
    }
}
