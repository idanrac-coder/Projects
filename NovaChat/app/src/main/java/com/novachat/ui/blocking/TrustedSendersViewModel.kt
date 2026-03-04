package com.novachat.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.entity.SenderAllowlistEntity
import com.novachat.core.sms.ScamDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrustedSendersViewModel @Inject constructor(
    private val spamLearningDao: SpamLearningDao,
    private val scamDetector: ScamDetector
) : ViewModel() {

    val allowlistedSenders: StateFlow<List<SenderAllowlistEntity>> =
        spamLearningDao.observeAllowlist()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun dismissAddDialog() {
        _showAddDialog.value = false
    }

    fun addSender(address: String) {
        if (address.isBlank()) return
        viewModelScope.launch {
            scamDetector.addToAllowlist(address.trim())
            _showAddDialog.value = false
        }
    }

    fun removeSender(address: String) {
        viewModelScope.launch {
            scamDetector.removeFromAllowlist(address)
        }
    }
}
