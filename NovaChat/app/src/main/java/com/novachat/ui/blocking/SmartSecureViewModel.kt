package com.novachat.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.ScamDetector
import com.novachat.core.sms.SpamAgentStats
import com.novachat.core.sms.ml.PersonalSpamAdapter
import com.novachat.core.sms.ml.SpamMlClassifier
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
    val agentStats: SpamAgentStats? = null,
    val mlModelAvailable: Boolean = false,
    val mlModelVersion: Int = 0,
    val personalModelReady: Boolean = false
)

@HiltViewModel
class SmartSecureViewModel @Inject constructor(
    private val spamMessageDao: SpamMessageDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val scamDetector: ScamDetector,
    private val mlClassifier: SpamMlClassifier,
    private val personalAdapter: PersonalSpamAdapter
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
            agentStats = stats,
            mlModelAvailable = mlClassifier.isModelAvailable,
            mlModelVersion = mlClassifier.modelVersion,
            personalModelReady = personalAdapter.isReady
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartSecureUiState())

    init {
        loadAgentStats()
        loadMlStatus()
    }

    private fun loadAgentStats() {
        viewModelScope.launch {
            _agentStats.value = scamDetector.getLearningStats()
        }
    }

    private fun loadMlStatus() {
        viewModelScope.launch {
            mlClassifier.ensureLoaded()
            personalAdapter.refresh()
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
