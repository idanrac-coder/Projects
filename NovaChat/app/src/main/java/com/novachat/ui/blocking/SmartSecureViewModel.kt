package com.novachat.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SenderAllowlistEntity
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.ScamDetector
import com.novachat.core.sms.SpamAgentStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartSecureUiState(
    val spamCount: Int = 0,
    val latestSpam: SpamMessageEntity? = null,
    val scamDetectionEnabled: Boolean = true,
    val agentStats: SpamAgentStats? = null,
    val allowlistedSenders: List<SenderAllowlistEntity> = emptyList()
)

@HiltViewModel
class SmartSecureViewModel @Inject constructor(
    private val spamMessageDao: SpamMessageDao,
    private val spamLearningDao: SpamLearningDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val scamDetector: ScamDetector
) : ViewModel() {

    private val _agentStats = MutableStateFlow<SpamAgentStats?>(null)

    val uiState: StateFlow<SmartSecureUiState> = combine(
        spamMessageDao.getSpamCount(),
        spamMessageDao.getAllSpamMessages(),
        preferencesRepository.scamDetectionEnabled,
        _agentStats,
        spamLearningDao.observeAllowlist()
    ) { values ->
        val spamCount = values[0] as Int
        val spamMessages = @Suppress("UNCHECKED_CAST") (values[1] as List<SpamMessageEntity>)
        val scamEnabled = values[2] as Boolean
        val stats = values[3] as SpamAgentStats?
        val allowlist = @Suppress("UNCHECKED_CAST") (values[4] as List<SenderAllowlistEntity>)
        SmartSecureUiState(
            spamCount = spamCount,
            latestSpam = spamMessages.firstOrNull(),
            scamDetectionEnabled = scamEnabled,
            agentStats = stats,
            allowlistedSenders = allowlist
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartSecureUiState())

    init {
        loadAgentStats()
    }

    private fun loadAgentStats() {
        viewModelScope.launch {
            _agentStats.value = scamDetector.getLearningStats()
        }
    }

    fun setScamDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setScamDetectionEnabled(enabled)
        }
    }

    fun removeFromAllowlist(address: String) {
        viewModelScope.launch {
            scamDetector.removeFromAllowlist(address)
        }
    }
}
