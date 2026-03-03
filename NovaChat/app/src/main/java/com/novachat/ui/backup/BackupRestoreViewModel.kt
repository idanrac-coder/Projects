package com.novachat.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val autoBackupEnabled: StateFlow<Boolean> = preferencesRepository.autoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val backupFrequency: StateFlow<String> = preferencesRepository.backupFrequency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "daily")

    val lastBackupTime: StateFlow<Long> = preferencesRepository.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupEnabled(enabled)
        }
    }

    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            preferencesRepository.setBackupFrequency(frequency)
        }
    }

    fun setLastBackupTime(time: Long) {
        viewModelScope.launch {
            preferencesRepository.setLastBackupTime(time)
        }
    }
}
