package com.novachat.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SpamMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpamFolderViewModel @Inject constructor(
    private val spamMessageDao: SpamMessageDao
) : ViewModel() {

    val spamMessages: StateFlow<List<SpamMessageEntity>> = spamMessageDao.getAllSpamMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
