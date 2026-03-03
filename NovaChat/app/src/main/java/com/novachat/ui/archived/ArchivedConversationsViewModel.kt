package com.novachat.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.Conversation
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchivedUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ArchivedConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivedUiState())
    val uiState: StateFlow<ArchivedUiState> = _uiState.asStateFlow()

    init {
        loadArchived()
    }

    fun loadArchived() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val archived = conversationRepository.getArchivedConversations()
                _uiState.value = ArchivedUiState(conversations = archived, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = ArchivedUiState(isLoading = false)
            }
        }
    }

    fun unarchive(threadId: Long) {
        viewModelScope.launch {
            conversationRepository.unarchiveConversation(threadId)
            loadArchived()
        }
    }

    fun deleteConversation(threadId: Long) {
        viewModelScope.launch {
            conversationRepository.deleteThread(threadId)
            loadArchived()
        }
    }
}
