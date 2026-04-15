package com.novachat.ui.blocking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.billing.LicenseManager
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.SmsProvider
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.BlockRuleLimitException
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockingUiState(
    val numberRules: List<BlockRule> = emptyList(),
    val keywordRules: List<BlockRule> = emptyList(),
    val senderRules: List<BlockRule> = emptyList(),
    val languageRules: List<BlockRule> = emptyList(),
    val ruleCount: Int = 0,
    val isPremium: Boolean = false,
    val showAddDialog: Boolean = false,
    val addDialogType: BlockType = BlockType.NUMBER,
    val error: String? = null
) {
    val canAddMoreRules: Boolean
        get() = isPremium || ruleCount < BlockRepository.FREE_RULE_LIMIT
}

@HiltViewModel
class BlockingViewModel @Inject constructor(
    private val blockRepository: BlockRepository,
    private val spamMessageDao: SpamMessageDao,
    private val smsProvider: SmsProvider,
    private val conversationRepository: ConversationRepository,
    preferencesRepository: UserPreferencesRepository,
    licenseManager: LicenseManager
) : ViewModel() {

    private val _showDialog = MutableStateFlow(false)
    private val _dialogType = MutableStateFlow(BlockType.NUMBER)
    private val _error = MutableStateFlow<String?>(null)

    private val _showBlockLimitDialog = MutableStateFlow(false)
    val showBlockLimitDialog: StateFlow<Boolean> = _showBlockLimitDialog.asStateFlow()

    val uiState: StateFlow<BlockingUiState> = combine(
        blockRepository.getRulesByType(BlockType.NUMBER),
        blockRepository.getRulesByType(BlockType.KEYWORD),
        blockRepository.getRulesByType(BlockType.SENDER_NAME),
        blockRepository.getRulesByType(BlockType.LANGUAGE),
        blockRepository.getRuleCount(),
        licenseManager.hasPremiumAccess,
        _showDialog,
        _dialogType,
        _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        BlockingUiState(
            numberRules = values[0] as List<BlockRule>,
            keywordRules = values[1] as List<BlockRule>,
            senderRules = values[2] as List<BlockRule>,
            languageRules = values[3] as List<BlockRule>,
            ruleCount = values[4] as Int,
            isPremium = values[5] as Boolean,
            showAddDialog = values[6] as Boolean,
            addDialogType = values[7] as BlockType,
            error = values[8] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlockingUiState())

    fun showAddDialog(type: BlockType) {
        _dialogType.value = type
        _showDialog.value = true
    }

    fun dismissDialog() {
        _showDialog.value = false
    }

    fun addRule(value: String, isRegex: Boolean = false) {
        val type = _dialogType.value
        viewModelScope.launch {
            try {
                blockRepository.addRule(
                    BlockRule(
                        type = type,
                        value = value.trim(),
                        isRegex = isRegex
                    )
                )
                _showDialog.value = false
            } catch (e: BlockRuleLimitException) {
                _showDialog.value = false
                _showBlockLimitDialog.value = true
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun dismissBlockLimitDialog() {
        _showBlockLimitDialog.value = false
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            val spamMessages = spamMessageDao.getSpamMessagesByRuleId(id)
            for (spam in spamMessages) {
                try {
                    smsProvider.insertIncomingSms(spam.address, spam.body, spam.timestamp)
                } catch (e: Exception) {
                    Log.w("BlockingVM", "Failed to restore message to inbox", e)
                }
            }
            if (spamMessages.isNotEmpty()) {
                spamMessageDao.deleteByRuleId(id)
                conversationRepository.invalidateAllCaches()
            }
            blockRepository.deleteRule(id)
        }
    }
}
