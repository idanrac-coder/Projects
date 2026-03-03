package com.novachat.ui.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.ScheduledMessage
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduledMessagesUiState(
    val messages: List<ScheduledMessage> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ScheduledMessagesViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduledMessagesUiState())
    val uiState: StateFlow<ScheduledMessagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            conversationRepository.getScheduledMessages().collect { messages ->
                _uiState.value = ScheduledMessagesUiState(
                    messages = messages,
                    isLoading = false
                )
            }
        }
    }

    fun cancelMessage(id: Long) {
        viewModelScope.launch {
            conversationRepository.cancelScheduledMessage(id)
        }
    }
}
