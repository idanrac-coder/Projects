package com.novachat.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.Conversation
import com.novachat.domain.model.SwipeAction
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    val swipeLeftAction: StateFlow<SwipeAction> = preferencesRepository.swipeLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeAction.ARCHIVE)

    val swipeRightAction: StateFlow<SwipeAction> = preferencesRepository.swipeRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeAction.DELETE)

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val conversations = conversationRepository.getConversations()
                    .filter { !it.isArchived }
                _uiState.value = ConversationsUiState(
                    conversations = conversations,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = ConversationsUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load conversations"
                )
            }
        }
    }

    fun pinConversation(threadId: Long, pinned: Boolean) {
        viewModelScope.launch {
            conversationRepository.pinConversation(threadId, pinned)
            loadConversations()
        }
    }

    fun archiveConversation(threadId: Long) {
        viewModelScope.launch {
            conversationRepository.archiveConversation(threadId, true)
            loadConversations()
        }
    }

    fun deleteConversation(threadId: Long) {
        viewModelScope.launch {
            conversationRepository.deleteThread(threadId)
            loadConversations()
        }
    }

    fun muteConversation(threadId: Long, muted: Boolean) {
        viewModelScope.launch {
            conversationRepository.muteConversation(threadId, muted)
            loadConversations()
        }
    }

    fun markAsRead(threadId: Long) {
        viewModelScope.launch {
            conversationRepository.markThreadAsRead(threadId)
            loadConversations()
        }
    }

    fun executeSwipeAction(action: SwipeAction, conversation: Conversation) {
        when (action) {
            SwipeAction.ARCHIVE -> archiveConversation(conversation.threadId)
            SwipeAction.DELETE -> deleteConversation(conversation.threadId)
            SwipeAction.PIN -> pinConversation(conversation.threadId, !conversation.isPinned)
            SwipeAction.MARK_READ_UNREAD -> markAsRead(conversation.threadId)
            SwipeAction.MUTE -> muteConversation(conversation.threadId, !conversation.isMuted)
            SwipeAction.BLOCK -> { /* handled in blocking screen */ }
            SwipeAction.OFF -> { }
        }
    }
}
