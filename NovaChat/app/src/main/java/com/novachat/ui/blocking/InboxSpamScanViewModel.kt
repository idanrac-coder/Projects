package com.novachat.ui.blocking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.ScanExcludedMessageEntity
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.core.sms.ContactResolver
import com.novachat.core.sms.ScamDetector
import com.novachat.core.sms.SmsProvider
import com.novachat.core.sms.SpamFilter
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

enum class ScanPhase { READY, SCANNING, REVIEW, PROCESSING, DONE }

enum class ScanAction { MOVE_TO_SPAM, DELETE_PERMANENTLY, MARK_NOT_SPAM }

data class ScannedSpamMessage(
    val smsId: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val score: Int,
    val matchedRuleType: String?,
    val selected: Boolean = true
)

data class InboxSpamScanUiState(
    val phase: ScanPhase = ScanPhase.READY,
    val totalMessages: Int = 0,
    val scannedCount: Int = 0,
    val spamResults: List<ScannedSpamMessage> = emptyList(),
    val processedCount: Int = 0,
    val lastAction: ScanAction? = null
) {
    val selectedCount: Int get() = spamResults.count { it.selected }
    val allSelected: Boolean get() = spamResults.isNotEmpty() && spamResults.all { it.selected }
}

@HiltViewModel
class InboxSpamScanViewModel @Inject constructor(
    private val smsProvider: SmsProvider,
    private val spamFilter: SpamFilter,
    private val spamMessageDao: SpamMessageDao,
    private val spamLearningDao: SpamLearningDao,
    private val contactResolver: ContactResolver,
    private val scamDetector: ScamDetector,
    private val conversationRepository: ConversationRepository,
    private val blockRepository: BlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxSpamScanUiState())
    val uiState: StateFlow<InboxSpamScanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        startScan()
    }

    fun startScan() {
        if (_uiState.value.phase == ScanPhase.SCANNING) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(phase = ScanPhase.SCANNING, scannedCount = 0, spamResults = emptyList()) }

                contactResolver.preloadContacts()
                val inboxMessages = smsProvider.getInboxMessages()
                val alreadyBlockedIds = spamMessageDao.getReportedSmsIds().toSet()
                val excludedSmsIds = spamLearningDao.getExcludedSmsIds().toSet()
                val excludedBodyHashes = spamLearningDao.getExcludedBodyHashes().toSet()

                _uiState.update { it.copy(totalMessages = inboxMessages.size) }

                val spamFound = mutableListOf<ScannedSpamMessage>()
                for ((index, msg) in inboxMessages.withIndex()) {
                    if (msg.smsId in alreadyBlockedIds ||
                        msg.smsId in excludedSmsIds ||
                        msg.body.hashCode() in excludedBodyHashes
                    ) {
                        if (index % 10 == 0) _uiState.update { it.copy(scannedCount = index + 1) }
                        continue
                    }

                    val isKnownContact = contactResolver.getContactName(msg.address) != null
                    if (isKnownContact || scamDetector.isAllowlisted(msg.address)) {
                        if (index % 10 == 0) _uiState.update { it.copy(scannedCount = index + 1) }
                        continue
                    }

                    val result = spamFilter.classify(msg.address, msg.body, isKnownContact)

                    if (result.classification == SpamFilter.SpamClassification.SPAM) {
                        spamFound.add(
                            ScannedSpamMessage(
                                smsId = msg.smsId,
                                threadId = msg.threadId,
                                address = msg.address,
                                body = msg.body,
                                timestamp = msg.timestamp,
                                score = result.score,
                                matchedRuleType = result.matchedRuleType
                            )
                        )
                    }

                    if (index % 10 == 0) {
                        _uiState.update { it.copy(scannedCount = index + 1, spamResults = spamFound.toList()) }
                    }
                    yield()
                }

                _uiState.update {
                    it.copy(
                        phase = ScanPhase.REVIEW,
                        scannedCount = inboxMessages.size,
                        spamResults = spamFound.toList()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InboxSpamScanVM", "Scan failed", e)
                _uiState.update { it.copy(phase = ScanPhase.REVIEW) }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.update { InboxSpamScanUiState() }
    }

    fun toggleMessage(smsId: Long) {
        _uiState.update { state ->
            state.copy(
                spamResults = state.spamResults.map {
                    if (it.smsId == smsId) it.copy(selected = !it.selected) else it
                }
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(spamResults = state.spamResults.map { it.copy(selected = true) })
        }
    }

    fun deselectAll() {
        _uiState.update { state ->
            state.copy(spamResults = state.spamResults.map { it.copy(selected = false) })
        }
    }

    fun toggleSelectAll() {
        if (_uiState.value.allSelected) deselectAll() else selectAll()
    }

    fun moveToSpamFolder() {
        val selected = _uiState.value.spamResults.filter { it.selected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(phase = ScanPhase.PROCESSING, processedCount = 0, lastAction = ScanAction.MOVE_TO_SPAM) }
            var count = 0
            val affectedThreads = mutableSetOf<Long>()
            for (msg in selected) {
                try {
                    spamMessageDao.insertSpamMessage(
                        SpamMessageEntity(
                            smsId = msg.smsId,
                            address = msg.address,
                            body = msg.body,
                            timestamp = msg.timestamp,
                            matchedRuleId = -1,
                            matchedRuleType = run {
                                val inner = msg.matchedRuleType ?: "SCORE_${msg.score}"
                                val payload = if (msg.matchedRuleType != null) {
                                    "$inner|SCORE_${msg.score}"
                                } else {
                                    inner
                                }
                                "INBOX_SCAN:$payload"
                            }
                        )
                    )
                    smsProvider.deleteMessage(msg.smsId)
                    spamFilter.reportSpam(msg.address, msg.body, null)
                    affectedThreads.add(msg.threadId)
                    count++
                    _uiState.update { it.copy(processedCount = count) }
                } catch (e: Exception) {
                    Log.e("InboxSpamScanVM", "Failed to move message ${msg.smsId} to spam", e)
                }
            }
            addBlockRulesForAddresses(selected)
            for (threadId in affectedThreads) {
                conversationRepository.refreshAfterChange(threadId)
            }
            _uiState.update { it.copy(phase = ScanPhase.DONE, processedCount = count) }
        }
    }

    fun deletePermanently() {
        val selected = _uiState.value.spamResults.filter { it.selected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(phase = ScanPhase.PROCESSING, processedCount = 0, lastAction = ScanAction.DELETE_PERMANENTLY) }
            var count = 0
            val affectedThreads = mutableSetOf<Long>()
            for (msg in selected) {
                try {
                    smsProvider.deleteMessage(msg.smsId)
                    spamFilter.reportSpam(msg.address, msg.body, null)
                    affectedThreads.add(msg.threadId)
                    count++
                    _uiState.update { it.copy(processedCount = count) }
                } catch (e: Exception) {
                    Log.e("InboxSpamScanVM", "Failed to delete message ${msg.smsId}", e)
                }
            }
            addBlockRulesForAddresses(selected)
            for (threadId in affectedThreads) {
                conversationRepository.refreshAfterChange(threadId)
            }
            _uiState.update { it.copy(phase = ScanPhase.DONE, processedCount = count) }
        }
    }

    fun markNotSpam() {
        val selected = _uiState.value.spamResults.filter { it.selected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            for (msg in selected) {
                try {
                    spamLearningDao.insertScanExclusion(
                        ScanExcludedMessageEntity(
                            smsId = msg.smsId,
                            address = msg.address,
                            bodyHash = msg.body.hashCode()
                        )
                    )
                    spamFilter.reportNotSpam(msg.address, msg.body)
                } catch (e: Exception) {
                    Log.e("InboxSpamScanVM", "Failed to mark message ${msg.smsId} as not spam", e)
                }
            }
            val selectedIds = selected.map { it.smsId }.toSet()
            _uiState.update { state ->
                state.copy(
                    spamResults = state.spamResults.filter { it.smsId !in selectedIds }
                )
            }
        }
    }

    private suspend fun addBlockRulesForAddresses(messages: List<ScannedSpamMessage>) {
        val distinctAddresses = messages.map { it.address }.distinct()
        for (address in distinctAddresses) {
            try {
                blockRepository.addRule(BlockRule(type = BlockType.NUMBER, value = address))
            } catch (e: Exception) {
                Log.w("InboxSpamScanVM", "Could not add block rule for $address", e)
            }
        }
    }
}
