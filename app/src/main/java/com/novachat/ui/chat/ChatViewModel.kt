package com.novachat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.Message
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentThreadId: Long = -1

    fun loadMessages(threadId: Long) {
        currentThreadId = threadId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val messages = conversationRepository.getMessagesForThread(threadId)
                conversationRepository.markThreadAsRead(threadId)
                _uiState.value = ChatUiState(messages = messages, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = ChatUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load messages"
                )
            }
        }
    }

    fun sendMessage(address: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val result = conversationRepository.sendSms(address, body)
            result.onSuccess {
                loadMessages(currentThreadId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "Failed to send"
                )
            }
        }
    }

    fun refreshMessages() {
        if (currentThreadId != -1L) {
            loadMessages(currentThreadId)
        }
    }
}
