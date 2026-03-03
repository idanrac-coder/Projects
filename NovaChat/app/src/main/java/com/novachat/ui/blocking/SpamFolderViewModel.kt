package com.novachat.ui.blocking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.core.sms.SmsProvider
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpamFolderViewModel @Inject constructor(
    private val spamMessageDao: SpamMessageDao,
    private val smsProvider: SmsProvider,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    val spamMessages: StateFlow<List<SpamMessageEntity>> = spamMessageDao.getAllSpamMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _restoredEvent = MutableStateFlow<String?>(null)
    val restoredEvent: StateFlow<String?> = _restoredEvent.asStateFlow()

    fun clearRestoredEvent() {
        _restoredEvent.value = null
    }

    fun restoreToInbox(id: Long) {
        viewModelScope.launch {
            val spam = spamMessageDao.getSpamMessageById(id) ?: return@launch
            try {
                val result = smsProvider.insertIncomingSms(spam.address, spam.body, spam.timestamp)
                if (result.uri != null) {
                    spamMessageDao.deleteSpamMessageById(id)
                    conversationRepository.invalidateAllCaches()
                    conversationRepository.notifyNewMessage(result.threadId)
                    _restoredEvent.value = spam.address
                } else {
                    Log.e("SpamFolderVM", "Failed to restore message to inbox: insert returned null")
                }
            } catch (e: Exception) {
                Log.e("SpamFolderVM", "Failed to restore message to inbox", e)
            }
        }
    }

    fun deleteSpamMessage(id: Long) {
        viewModelScope.launch {
            spamMessageDao.deleteSpamMessageById(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            spamMessageDao.clearAll()
        }
    }
}
