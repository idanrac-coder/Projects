package com.novachat.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.Contact
import com.novachat.domain.repository.ContactRepository
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComposeUiState(
    val contacts: List<Contact> = emptyList(),
    val filteredContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ComposeMessageViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            try {
                val contacts = contactRepository.getAllContacts()
                _uiState.value = _uiState.value.copy(
                    contacts = contacts,
                    filteredContacts = contacts
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load contacts"
                )
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(filteredContacts = _uiState.value.contacts)
        } else {
            val filtered = _uiState.value.contacts.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.phoneNumber.contains(query)
            }
            _uiState.value = _uiState.value.copy(filteredContacts = filtered)
        }
    }

    fun sendMessage(address: String, body: String, onSuccess: (threadId: Long) -> Unit) {
        if (body.isBlank() || address.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val result = conversationRepository.sendSms(address, body)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSending = false)
                val threadId = conversationRepository.getThreadIdForAddress(address)
                onSuccess(threadId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "Failed to send"
                )
            }
        }
    }
}
