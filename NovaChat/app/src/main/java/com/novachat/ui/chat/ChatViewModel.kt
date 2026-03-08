package com.novachat.ui.chat

import android.util.Log
import com.novachat.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.sms.ScamAnalysis
import com.novachat.core.sms.ScamDetector
import com.novachat.core.sms.WhatsAppForwarder
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageType
import com.novachat.domain.model.Reaction
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.BlockRuleLimitException
import com.novachat.domain.repository.ConversationRepository
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SpamMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val matchingMessageIds: List<Long> = emptyList(),
    val activeMatchIndex: Int = -1,
    val smartReplies: List<String> = emptyList(),
    val replyToMessage: Message? = null,
    val showReactionPicker: Long? = null,
    val pinnedMessages: List<Message> = emptyList(),
    val showScheduleDialog: Boolean = false,
    val showReminderDialog: Long? = null,
    val showBlockDialog: Boolean = false,
    val showDisappearingDialog: Boolean = false,
    val disappearingDurationMs: Long? = null,
    val sendViaWhatsApp: Boolean = false,
    val isWhatsAppAvailable: Boolean = false,
    val scamWarnings: Map<Long, ScamAnalysis> = emptyMap(),
    val dismissedScamWarnings: Set<Long> = emptySet(),
    val pendingSendMessage: PendingSend? = null,
    val spamReportedEvent: Boolean = false,
    val showBlockLimitDialog: Boolean = false,
    val blockSuccessNavigateBack: Boolean = false,
    val isSenderAllowlisted: Boolean = false,
    val senderBannerDismissed: Boolean = false
) {
    val matchCount: Int get() = matchingMessageIds.size
    val activeMatchMessageId: Long? get() =
        if (activeMatchIndex in matchingMessageIds.indices) matchingMessageIds[activeMatchIndex] else null
}

data class PendingSend(
    val address: String,
    val body: String,
    val sendAtMillis: Long,
    val isWhatsApp: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val blockRepository: BlockRepository,
    private val spamMessageDao: SpamMessageDao,
    private val whatsAppForwarder: WhatsAppForwarder,
    private val scamDetector: ScamDetector,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentThreadId: Long = -1
    private var loadJob: Job? = null
    private var undoJob: Job? = null

    companion object {
        const val UNDO_SEND_DELAY_MS = 5000L
    }

    init {
        _uiState.value = _uiState.value.copy(
            isWhatsAppAvailable = whatsAppForwarder.isWhatsAppInstalled()
        )
        observeIncomingMessages()
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: observeIncomingMessages() STARTED collecting")
            conversationRepository.refreshTrigger.collect { threadId ->
                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: refreshTrigger RECEIVED threadId=$threadId currentThreadId=$currentThreadId")
                if (currentThreadId != -1L && (threadId == currentThreadId || threadId == -1L)) {
                    if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: MATCH -> invalidateMessagesCache + refreshMessages")
                    conversationRepository.invalidateMessagesCache(currentThreadId)
                    refreshMessages()
                } else {
                    if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: NO MATCH (currentThreadId=$currentThreadId, received=$threadId)")
                }
            }
        }
    }

    fun loadMessages(threadId: Long) {
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: loadMessages($threadId) START")
        currentThreadId = threadId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = _uiState.value
            val hasCachedData = current.messages.isNotEmpty()

            if (!hasCachedData) {
                val cached = conversationRepository.getCachedMessagesForThread(threadId)
                if (cached != null && cached.isNotEmpty()) {
                    val pinnedMsgs = cached.filter { it.isPinned }
                    val quickReplyOn = userPreferencesRepository.quickReplyEnabled.first()
                    val smartReplies = if (quickReplyOn) generateSmartReplies(cached) else emptyList()
                    _uiState.value = current.copy(
                        messages = cached,
                        isLoading = false,
                        smartReplies = smartReplies,
                        pinnedMessages = pinnedMsgs,
                        error = null
                    )
                } else {
                    _uiState.value = current.copy(isLoading = true)
                }
            }

            try {
                conversationRepository.invalidateMessagesCache(threadId)
                var messages = conversationRepository.getMessagesForThread(threadId)
                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: loadMessages($threadId) initial query returned ${messages.size} messages")

                if (messages.isEmpty()) {
                    if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: messages empty, starting retry loop")
                    for (attempt in 1..3) {
                        delay(500L * attempt)
                        conversationRepository.invalidateMessagesCache(threadId)
                        messages = conversationRepository.getMessagesForThread(threadId)
                        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: retry #$attempt returned ${messages.size} messages")
                        if (messages.isNotEmpty()) break
                    }
                }

                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "$$$ ChatVM: loadMessages($threadId) FINAL count=${messages.size}")
                if (messages.isNotEmpty()) {
                    conversationRepository.markThreadAsRead(threadId)
                }
                val pinnedMsgs = messages.filter { it.isPinned }
                val quickReplyOn = userPreferencesRepository.quickReplyEnabled.first()
                val smartReplies = if (quickReplyOn) generateSmartReplies(messages) else emptyList()
                val scamWarnings = analyzeForScams(messages)
                val senderAddress = messages.firstOrNull { it.type == MessageType.RECEIVED }?.address
                val allowlisted = if (senderAddress != null) scamDetector.isAllowlisted(senderAddress) else false
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false,
                    smartReplies = smartReplies,
                    pinnedMessages = pinnedMsgs,
                    scamWarnings = scamWarnings,
                    isSenderAllowlisted = allowlisted,
                    error = null
                )
            } catch (e: Exception) {
                val stillEmpty = _uiState.value.messages.isEmpty()
                if (stillEmpty) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
            }
        }
    }

    private suspend fun analyzeForScams(messages: List<Message>): Map<Long, ScamAnalysis> {
        val result = mutableMapOf<Long, ScamAnalysis>()
        val reportedIds = spamMessageDao.getReportedSmsIds().toSet()
        val recent = messages.filter { it.type == MessageType.RECEIVED }.takeLast(20)
        for (msg in recent) {
            if (msg.id in reportedIds) continue
            val analysis = scamDetector.analyzeWithReputation(msg.body, msg.address)
            if (analysis.isScam) {
                result[msg.id] = analysis
            }
        }
        return result
    }

    fun dismissScamWarning(messageId: Long) {
        val message = _uiState.value.messages.find { it.id == messageId }
        if (message != null) {
            val senderAddress = message.address
            val allWarningIdsForSender = _uiState.value.scamWarnings.keys.filter { id ->
                _uiState.value.messages.find { it.id == id }?.address == senderAddress
            }.toSet()
            _uiState.value = _uiState.value.copy(
                dismissedScamWarnings = _uiState.value.dismissedScamWarnings + allWarningIdsForSender,
                scamWarnings = _uiState.value.scamWarnings.filterKeys { it !in allWarningIdsForSender }
            )
            viewModelScope.launch {
                scamDetector.addToAllowlist(senderAddress)
                scamDetector.reportNotSpam(senderAddress, message.body)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                dismissedScamWarnings = _uiState.value.dismissedScamWarnings + messageId
            )
        }
    }

    fun trustSender(address: String) {
        val allWarningIdsForSender = _uiState.value.scamWarnings.keys.filter { id ->
            _uiState.value.messages.find { it.id == id }?.address == address
        }.toSet()
        _uiState.value = _uiState.value.copy(
            isSenderAllowlisted = true,
            senderBannerDismissed = true,
            dismissedScamWarnings = _uiState.value.dismissedScamWarnings + allWarningIdsForSender,
            scamWarnings = _uiState.value.scamWarnings.filterKeys { it !in allWarningIdsForSender }
        )
        viewModelScope.launch {
            scamDetector.addToAllowlist(address)
        }
    }

    fun dismissSenderBanner() {
        _uiState.value = _uiState.value.copy(senderBannerDismissed = true)
    }

    fun confirmSpam(messageId: Long) {
        val message = _uiState.value.messages.find { it.id == messageId } ?: return
        val analysis = _uiState.value.scamWarnings[messageId]
        _uiState.value = _uiState.value.copy(
            dismissedScamWarnings = _uiState.value.dismissedScamWarnings + messageId,
            spamReportedEvent = true
        )
        viewModelScope.launch {
            scamDetector.reportSpam(message.address, message.body, analysis?.category)

            try {
                val ruleId = blockRepository.addRule(
                    BlockRule(type = BlockType.NUMBER, value = message.address)
                )
                moveThreadToSpamAndClose(ruleId, "SCAM_REPORT")
            } catch (_: BlockRuleLimitException) {
                moveThreadToSpamAndClose(-1, "SCAM_REPORT")
            }
        }
    }

    fun consumeSpamReportedEvent() {
        _uiState.value = _uiState.value.copy(spamReportedEvent = false)
    }

    private fun generateSmartReplies(messages: List<Message>): List<String> {
        if (messages.isEmpty()) return emptyList()
        val lastMsg = messages.lastOrNull { it.type == com.novachat.domain.model.MessageType.RECEIVED }
            ?: return emptyList()
        val body = lastMsg.body.lowercase()

        return when {
            body.contains("how are you") || body.contains("how's it going") ->
                listOf("I'm good, thanks!", "Doing great!", "Not bad, you?")
            body.contains("thank") ->
                listOf("You're welcome!", "No problem!", "Anytime!")
            body.contains("congrat") || body.contains("great job") || body.contains("well done") ->
                listOf("Thank you!", "Appreciate it!", "\uD83D\uDE4F")
            body.contains("?") ->
                listOf("Yes", "No", "Maybe later")
            body.contains("good morning") || body.contains("good evening") || body.contains("good night") ->
                listOf("Good morning!", "Have a great day!", "\uD83D\uDE0A")
            body.contains("miss you") || body.contains("love you") ->
                listOf("Miss you too!", "\u2764\uFE0F", "See you soon!")
            body.contains("bye") || body.contains("see you") || body.contains("talk to you") ->
                listOf("Bye!", "Talk soon!", "See you!")
            body.contains("lol") || body.contains("haha") || body.contains("\uD83D\uDE02") ->
                listOf("\uD83D\uDE02", "Haha!", "So funny!")
            body.contains("ok") || body.contains("sure") || body.contains("alright") ->
                listOf("\uD83D\uDC4D", "Great!", "Sounds good!")
            else ->
                listOf("Ok", "Got it", "\uD83D\uDC4D")
        }
    }

    fun sendMessage(address: String, body: String) {
        if (body.isBlank()) return
        if (_uiState.value.sendViaWhatsApp) {
            sendViaWhatsAppDelayed(address, body)
            return
        }
        undoJob?.cancel()
        val pending = PendingSend(
            address = address,
            body = body,
            sendAtMillis = System.currentTimeMillis() + UNDO_SEND_DELAY_MS
        )
        _uiState.value = _uiState.value.copy(
            pendingSendMessage = pending,
            replyToMessage = null
        )
        undoJob = viewModelScope.launch {
            delay(UNDO_SEND_DELAY_MS)
            executeSend(address, body)
        }
    }

    fun cancelPendingSend() {
        undoJob?.cancel()
        undoJob = null
        _uiState.value = _uiState.value.copy(pendingSendMessage = null)
    }

    private fun executeSend(address: String, body: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, pendingSendMessage = null)
            val result = conversationRepository.sendSms(address, body)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSending = false)
                loadMessages(currentThreadId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "Failed to send"
                )
            }
        }
    }

    private fun sendViaWhatsAppDelayed(address: String, body: String) {
        undoJob?.cancel()
        val pending = PendingSend(
            address = address,
            body = body,
            sendAtMillis = System.currentTimeMillis() + UNDO_SEND_DELAY_MS,
            isWhatsApp = true
        )
        _uiState.value = _uiState.value.copy(
            pendingSendMessage = pending,
            replyToMessage = null
        )
        undoJob = viewModelScope.launch {
            delay(UNDO_SEND_DELAY_MS)
            _uiState.value = _uiState.value.copy(pendingSendMessage = null, isSending = true)
            val success = whatsAppForwarder.sendMessage(address, body)
            _uiState.value = _uiState.value.copy(
                isSending = false,
                error = if (!success) "WhatsApp is not available" else null
            )
        }
    }

    fun toggleSendViaWhatsApp() {
        _uiState.value = _uiState.value.copy(
            sendViaWhatsApp = !_uiState.value.sendViaWhatsApp
        )
    }

    fun scheduleMessage(address: String, body: String, scheduledTime: Long, contactName: String?) {
        viewModelScope.launch {
            conversationRepository.scheduleMessage(
                address = address,
                body = body,
                scheduledTime = scheduledTime,
                threadId = currentThreadId,
                contactName = contactName,
                sendViaWhatsApp = _uiState.value.sendViaWhatsApp
            )
            _uiState.value = _uiState.value.copy(showScheduleDialog = false)
        }
    }

    fun showScheduleDialog() {
        _uiState.value = _uiState.value.copy(showScheduleDialog = true)
    }

    fun hideScheduleDialog() {
        _uiState.value = _uiState.value.copy(showScheduleDialog = false)
    }

    fun refreshMessages() {
        if (currentThreadId != -1L) {
            loadMessages(currentThreadId)
        }
    }

    fun pullToRefresh() {
        if (currentThreadId == -1L) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            conversationRepository.invalidateMessagesCache(currentThreadId)
            try {
                val messages = conversationRepository.getMessagesForThread(currentThreadId)
                conversationRepository.markThreadAsRead(currentThreadId)
                val pinnedMsgs = messages.filter { it.isPinned }
                val quickReplyOn = userPreferencesRepository.quickReplyEnabled.first()
                val smartReplies = if (quickReplyOn) generateSmartReplies(messages) else emptyList()
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isRefreshing = false,
                    isLoading = false,
                    smartReplies = smartReplies,
                    pinnedMessages = pinnedMsgs,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun toggleSearch() {
        val current = _uiState.value
        if (current.isSearchActive) {
            _uiState.value = current.copy(
                isSearchActive = false,
                searchQuery = "",
                matchingMessageIds = emptyList(),
                activeMatchIndex = -1
            )
        } else {
            _uiState.value = current.copy(isSearchActive = true)
        }
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value
        if (query.isBlank()) {
            _uiState.value = current.copy(
                searchQuery = query,
                matchingMessageIds = emptyList(),
                activeMatchIndex = -1
            )
            return
        }
        val matches = current.messages
            .filter { it.body.contains(query, ignoreCase = true) }
            .map { it.id }
        _uiState.value = current.copy(
            searchQuery = query,
            matchingMessageIds = matches,
            activeMatchIndex = if (matches.isNotEmpty()) matches.size - 1 else -1
        )
    }

    fun navigateMatchUp() {
        val current = _uiState.value
        if (current.matchCount == 0) return
        val newIndex = if (current.activeMatchIndex > 0)
            current.activeMatchIndex - 1
        else
            current.matchCount - 1
        _uiState.value = current.copy(activeMatchIndex = newIndex)
    }

    fun navigateMatchDown() {
        val current = _uiState.value
        if (current.matchCount == 0) return
        val newIndex = if (current.activeMatchIndex < current.matchCount - 1)
            current.activeMatchIndex + 1
        else
            0
        _uiState.value = current.copy(activeMatchIndex = newIndex)
    }

    fun setReplyTo(message: Message?) {
        _uiState.value = _uiState.value.copy(replyToMessage = message)
    }

    fun showReactionPicker(messageId: Long) {
        _uiState.value = _uiState.value.copy(showReactionPicker = messageId)
    }

    fun hideReactionPicker() {
        _uiState.value = _uiState.value.copy(showReactionPicker = null)
    }

    fun addReaction(messageId: Long, emoji: String) {
        viewModelScope.launch {
            conversationRepository.addReaction(messageId, emoji)
            _uiState.value = _uiState.value.copy(showReactionPicker = null)
            loadMessages(currentThreadId)
        }
    }

    fun removeReaction(messageId: Long, emoji: String) {
        viewModelScope.launch {
            conversationRepository.removeReaction(messageId, emoji)
            loadMessages(currentThreadId)
        }
    }

    fun togglePinMessage(message: Message) {
        viewModelScope.launch {
            if (message.isPinned) {
                conversationRepository.unpinMessage(message.id)
            } else {
                conversationRepository.pinMessage(message.id, message.threadId)
            }
            loadMessages(currentThreadId)
        }
    }

    fun showReminderDialog(messageId: Long) {
        _uiState.value = _uiState.value.copy(showReminderDialog = messageId)
    }

    fun hideReminderDialog() {
        _uiState.value = _uiState.value.copy(showReminderDialog = null)
    }

    fun setReminder(message: Message, reminderTime: Long, contactName: String?) {
        viewModelScope.launch {
            conversationRepository.setReminderForMessage(
                messageId = message.id,
                threadId = message.threadId,
                address = message.address,
                body = message.body,
                contactName = contactName,
                reminderTime = reminderTime
            )
            _uiState.value = _uiState.value.copy(showReminderDialog = null)
        }
    }

    fun showBlockDialog() {
        _uiState.value = _uiState.value.copy(showBlockDialog = true)
    }

    fun dismissBlockDialog() {
        _uiState.value = _uiState.value.copy(showBlockDialog = false)
    }

    fun blockNumber(address: String) {
        viewModelScope.launch {
            try {
                val ruleId = blockRepository.addRule(
                    BlockRule(type = BlockType.NUMBER, value = address)
                )
                moveThreadToSpamAndClose(ruleId, BlockType.NUMBER.name)
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    fun blockName(name: String) {
        viewModelScope.launch {
            try {
                val ruleId = blockRepository.addRule(
                    BlockRule(type = BlockType.SENDER_NAME, value = name)
                )
                moveThreadToSpamAndClose(ruleId, BlockType.SENDER_NAME.name)
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    fun blockWords(words: String) {
        if (words.isBlank()) return
        viewModelScope.launch {
            try {
                val ruleId = blockRepository.addRule(
                    BlockRule(type = BlockType.KEYWORD, value = words)
                )
                moveThreadToSpamAndClose(ruleId, BlockType.KEYWORD.name)
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    fun blockLanguage(language: String) {
        if (language.isBlank()) return
        viewModelScope.launch {
            try {
                val ruleId = blockRepository.addRule(
                    BlockRule(type = BlockType.LANGUAGE, value = language.trim().lowercase())
                )
                moveThreadToSpamAndClose(ruleId, BlockType.LANGUAGE.name)
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    fun dismissBlockLimitDialog() {
        _uiState.value = _uiState.value.copy(showBlockLimitDialog = false)
    }

    private suspend fun moveThreadToSpamAndClose(ruleId: Long, ruleType: String) {
        if (currentThreadId != -1L) {
            val messages = conversationRepository.getMessagesForThread(currentThreadId)
            messages.forEach { msg ->
                spamMessageDao.insertSpamMessage(
                    SpamMessageEntity(
                        smsId = msg.id,
                        address = msg.address,
                        body = msg.body,
                        timestamp = msg.timestamp,
                        matchedRuleId = ruleId,
                        matchedRuleType = ruleType
                    )
                )
            }
            try { conversationRepository.deleteThread(currentThreadId) } catch (e: Exception) {}
        }
        _uiState.value = _uiState.value.copy(showBlockDialog = false, blockSuccessNavigateBack = true)
    }

    fun clearBlockSuccessEvent() {
        _uiState.value = _uiState.value.copy(blockSuccessNavigateBack = false)
    }

    fun getMessageById(messageId: Long): Message? {
        return _uiState.value.messages.find { it.id == messageId }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                conversationRepository.invalidateMessagesCache(currentThreadId)
                loadMessages(currentThreadId)
            } catch (_: Exception) { }
        }
    }

    fun showDisappearingDialog() {
        _uiState.value = _uiState.value.copy(showDisappearingDialog = true)
    }

    fun hideDisappearingDialog() {
        _uiState.value = _uiState.value.copy(showDisappearingDialog = false)
    }

    fun setDisappearingDuration(durationMs: Long?) {
        _uiState.value = _uiState.value.copy(
            disappearingDurationMs = durationMs,
            showDisappearingDialog = false
        )
    }

}
