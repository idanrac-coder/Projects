package com.novachat.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.SpamMessageDao
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
    val filterInternationalSenders: Boolean = false,
    val agentStats: SpamAgentStats? = null
)

@HiltViewModel
class SmartSecureViewModel @Inject constructor(
    private val spamMessageDao: SpamMessageDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val scamDetector: ScamDetector
) : ViewModel() {

    private val _agentStats = MutableStateFlow<SpamAgentStats?>(null)

    val uiState: StateFlow<SmartSecureUiState> = combine(
        spamMessageDao.getSpamCount(),
        spamMessageDao.getAllSpamMessages(),
        preferencesRepository.scamDetectionEnabled,
        preferencesRepository.filterInternationalSenders,
        _agentStats
    ) { spamCount, spamMessages, scamEnabled, filterIntl, stats ->
        SmartSecureUiState(
            spamCount = spamCount,
            latestSpam = spamMessages.firstOrNull(),
            scamDetectionEnabled = scamEnabled,
            filterInternationalSenders = filterIntl,
            agentStats = stats
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

    fun setFilterInternationalSenders(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setFilterInternationalSenders(enabled)
        }
    }
}
