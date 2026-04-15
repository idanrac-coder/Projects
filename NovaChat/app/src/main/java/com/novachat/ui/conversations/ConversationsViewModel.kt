package com.novachat.ui.conversations

import android.util.Log
import com.novachat.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.model.Conversation
import com.novachat.domain.model.MessageCategory
import com.novachat.domain.model.SwipeAction
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.BlockRuleLimitException
import com.novachat.domain.repository.ConversationRepository
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SpamMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val filteredConversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedCategory: MessageCategory = MessageCategory.ALL,
    val selectedCustomCategory: String? = null,
    val customCategories: List<Pair<Long, String>> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedThreadIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val showBlockDialog: Boolean = false,
    val blockTarget: BlockTarget? = null,
    val showCategoryDialog: Boolean = false,
    val showManageCategoriesDialog: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val editingCategory: Pair<Long, String>? = null,
    val showDeleteConfirmation: Boolean = false,
    val pendingDeleteThreadIds: Set<Long> = emptySet(),
    val showDefaultSmsPrompt: Boolean = false,
    val showDefaultSmsCheck: Boolean = false,
    val showBlockLimitDialog: Boolean = false
)

data class BlockTarget(
    val address: String,
    val displayName: String
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val blockRepository: BlockRepository,
    private val spamMessageDao: SpamMessageDao,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    val swipeLeftAction: StateFlow<SwipeAction> = preferencesRepository.swipeLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeAction.ARCHIVE)

    val swipeRightAction: StateFlow<SwipeAction> = preferencesRepository.swipeRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeAction.DELETE)

    init {
        checkDefaultSmsApp()
        loadConversations()
        loadCustomCategories()
        observeIncomingMessages()
    }

    fun checkDefaultSmsApp() {
        if (!conversationRepository.isDefaultSmsApp()) {
            _uiState.value = _uiState.value.copy(showDefaultSmsCheck = true)
        } else {
            _uiState.value = _uiState.value.copy(showDefaultSmsCheck = false)
        }
    }

    fun dismissDefaultSmsCheck() {
        _uiState.value = _uiState.value.copy(showDefaultSmsCheck = false)
    }

    fun onDefaultSmsResult(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(showDefaultSmsCheck = false)
        if (accepted) {
            loadConversations()
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "@@@ ConvVM: observeIncomingMessages() STARTED collecting")
            conversationRepository.refreshTrigger.collect { threadId ->
                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "@@@ ConvVM: refreshTrigger RECEIVED threadId=$threadId -> loadConversations")
                loadConversations()
            }
        }
    }

    fun forceRefresh() {
        conversationRepository.invalidateAllCaches()
        loadConversations()
    }

    fun pullToRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            conversationRepository.invalidateConversationsCache()
            try {
                val conversations = conversationRepository.getConversations()
                    .filter { !it.isArchived }
                val latest = _uiState.value
                val filtered = filterConversations(
                    conversations, latest.selectedCategory,
                    latest.selectedCustomCategory, latest.searchQuery
                )
                _uiState.value = latest.copy(
                    conversations = conversations,
                    filteredConversations = filtered,
                    isRefreshing = false,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun loadConversations() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "@@@ ConvVM: loadConversations() START")
            val current = _uiState.value
            val hasCachedData = current.conversations.isNotEmpty()

            if (!hasCachedData) {
                val cached = conversationRepository.getCachedConversations()
                    ?.filter { !it.isArchived }
                if (cached != null && cached.isNotEmpty()) {
                    val latest = _uiState.value
                    val filtered = filterConversations(
                        cached, latest.selectedCategory,
                        latest.selectedCustomCategory, latest.searchQuery
                    )
                    _uiState.value = latest.copy(
                        conversations = cached,
                        filteredConversations = filtered,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = current.copy(isLoading = true, error = null)
                }
            }

            try {
                val conversations = conversationRepository.getConversations()
                    .filter { !it.isArchived }
                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "@@@ ConvVM: loadConversations() got ${conversations.size} non-archived conversations")
                val latest = _uiState.value
                val filtered = filterConversations(
                    conversations, latest.selectedCategory,
                    latest.selectedCustomCategory, latest.searchQuery
                )
                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "@@@ ConvVM: filtered=${filtered.size} category=${latest.selectedCategory} customCat=${latest.selectedCustomCategory}")
                _uiState.value = latest.copy(
                    conversations = conversations,
                    filteredConversations = filtered,
                    isLoading = false,
                    error = null
                )
                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "@@@ ConvVM: UI STATE UPDATED with ${conversations.size} conversations, ${filtered.size} filtered")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e("NC_DEBUG", "@@@ ConvVM: loadConversations() EXCEPTION", e)
                val stillEmpty = _uiState.value.conversations.isEmpty()
                if (stillEmpty) {
                    kotlinx.coroutines.delay(600)
                    if (_uiState.value.conversations.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load conversations"
                        )
                    }
                }
            }
        }
    }

    private fun loadCustomCategories() {
        viewModelScope.launch {
            conversationRepository.getCustomCategories().collect { names ->
                val withIds = conversationRepository.getCustomCategoriesOnce()
                _uiState.value = _uiState.value.copy(customCategories = withIds)
            }
        }
    }

    fun setCategory(category: MessageCategory) {
        val current = _uiState.value
        val filtered = filterConversations(current.conversations, category, null, current.searchQuery)
        _uiState.value = current.copy(
            selectedCategory = category,
            selectedCustomCategory = null,
            filteredConversations = filtered
        )
    }

    fun setCustomCategoryFilter(categoryName: String) {
        val current = _uiState.value
        val alreadySelected = current.selectedCustomCategory == categoryName
        if (alreadySelected) {
            setCategory(MessageCategory.ALL)
            return
        }
        val filtered = filterConversations(
            current.conversations, MessageCategory.ALL, categoryName, current.searchQuery
        )
        _uiState.value = current.copy(
            selectedCategory = MessageCategory.ALL,
            selectedCustomCategory = categoryName,
            filteredConversations = filtered
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            val current = _uiState.value
            val filtered = filterConversations(
                current.conversations, current.selectedCategory,
                current.selectedCustomCategory, current.searchQuery
            )
            _uiState.value = current.copy(filteredConversations = filtered)
        }
    }

    private fun filterConversations(
        conversations: List<Conversation>,
        category: MessageCategory,
        customCategory: String?,
        query: String
    ): List<Conversation> {
        var result = conversations
        if (customCategory != null) {
            result = result.filter { it.customCategory == customCategory }
        } else {
            result = when (category) {
                MessageCategory.ALL -> result
                MessageCategory.CONTACTS -> result.filter { it.category == MessageCategory.CONTACTS }
                MessageCategory.UNREAD -> result.filter { it.unreadCount > 0 }
                MessageCategory.FAVORITES -> result.filter { it.isFavorite }
            }
        }
        if (query.isNotBlank()) {
            result = result.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                    it.snippet.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
            }
        }
        return result
    }

    fun toggleSelectionMode() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isSelectionMode = !current.isSelectionMode,
            selectedThreadIds = emptySet()
        )
    }

    fun toggleSelection(threadId: Long) {
        val current = _uiState.value
        val newSelection = if (threadId in current.selectedThreadIds) {
            current.selectedThreadIds - threadId
        } else {
            current.selectedThreadIds + threadId
        }
        _uiState.value = current.copy(
            selectedThreadIds = newSelection,
            isSelectionMode = newSelection.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedThreadIds = emptySet()
        )
    }

    fun archiveSelected() {
        viewModelScope.launch {
            _uiState.value.selectedThreadIds.forEach { threadId ->
                conversationRepository.archiveConversation(threadId, true)
            }
            clearSelection()
            loadConversations()
        }
    }

    fun deleteSelected() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            pendingDeleteThreadIds = _uiState.value.selectedThreadIds
        )
    }

    fun pinSelected() {
        viewModelScope.launch {
            _uiState.value.selectedThreadIds.forEach { threadId ->
                conversationRepository.pinConversation(threadId, true)
            }
            clearSelection()
            loadConversations()
        }
    }

    fun markSelectedAsRead() {
        viewModelScope.launch {
            _uiState.value.selectedThreadIds.forEach { threadId ->
                conversationRepository.markThreadAsRead(threadId)
            }
            clearSelection()
            loadConversations()
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
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            pendingDeleteThreadIds = setOf(threadId)
        )
    }

    fun confirmDelete() {
        val current = _uiState.value
        val threadIds = current.pendingDeleteThreadIds
        if (threadIds.isEmpty()) return

        if (!conversationRepository.isDefaultSmsApp()) {
            _uiState.value = current.copy(
                showDeleteConfirmation = false,
                showDefaultSmsPrompt = true
            )
            return
        }

        val remainingConversations = current.conversations.filter { it.threadId !in threadIds }
        val remainingFiltered = current.filteredConversations.filter { it.threadId !in threadIds }
        _uiState.value = current.copy(
            showDeleteConfirmation = false,
            pendingDeleteThreadIds = emptySet(),
            conversations = remainingConversations,
            filteredConversations = remainingFiltered,
            isSelectionMode = false,
            selectedThreadIds = emptySet()
        )

        viewModelScope.launch {
            try {
                threadIds.forEach { threadId ->
                    conversationRepository.deleteThread(threadId)
                }
            } catch (_: Exception) { }
            loadConversations()
        }
    }

    fun dismissDefaultSmsPrompt() {
        _uiState.value = _uiState.value.copy(
            showDefaultSmsPrompt = false,
            pendingDeleteThreadIds = emptySet()
        )
    }

    fun retryDeleteAfterDefaultSet() {
        _uiState.value = _uiState.value.copy(showDefaultSmsPrompt = false)
        confirmDelete()
    }

    fun forceDeleteAfterDefaultSet() {
        val current = _uiState.value
        val threadIds = current.pendingDeleteThreadIds
        if (threadIds.isEmpty()) return

        val remainingConversations = current.conversations.filter { it.threadId !in threadIds }
        val remainingFiltered = current.filteredConversations.filter { it.threadId !in threadIds }
        _uiState.value = current.copy(
            showDefaultSmsPrompt = false,
            showDeleteConfirmation = false,
            pendingDeleteThreadIds = emptySet(),
            conversations = remainingConversations,
            filteredConversations = remainingFiltered,
            isSelectionMode = false,
            selectedThreadIds = emptySet()
        )

        viewModelScope.launch {
            try {
                threadIds.forEach { threadId ->
                    conversationRepository.deleteThread(threadId)
                }
            } catch (_: Exception) { }
            loadConversations()
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            pendingDeleteThreadIds = emptySet()
        )
    }

    fun muteConversation(threadId: Long, muted: Boolean) {
        viewModelScope.launch {
            conversationRepository.muteConversation(threadId, muted)
            loadConversations()
        }
    }

    fun muteConversationUntil(threadId: Long, muteUntil: Long) {
        viewModelScope.launch {
            conversationRepository.muteConversationUntil(threadId, muteUntil)
            loadConversations()
        }
    }

    fun unmuteConversation(threadId: Long) {
        viewModelScope.launch {
            conversationRepository.muteConversationUntil(threadId, null)
            loadConversations()
        }
    }

    fun setConversationFavorite(threadId: Long, favorite: Boolean) {
        viewModelScope.launch {
            conversationRepository.setConversationFavorite(threadId, favorite)
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
            SwipeAction.BLOCK -> showBlockDialog(conversation)
            SwipeAction.OFF -> { }
        }
    }

    fun showBlockDialog(conversation: Conversation) {
        _uiState.value = _uiState.value.copy(
            showBlockDialog = true,
            blockTarget = BlockTarget(
                address = conversation.address,
                displayName = conversation.displayName
            )
        )
    }

    fun showBlockDialogForSelected() {
        val current = _uiState.value
        val selected = current.conversations.filter { it.threadId in current.selectedThreadIds }
        if (selected.isEmpty()) return
        val first = selected.first()
        _uiState.value = current.copy(
            showBlockDialog = true,
            blockTarget = if (selected.size == 1) {
                BlockTarget(address = first.address, displayName = first.displayName)
            } else {
                BlockTarget(
                    address = selected.joinToString(", ") { it.address },
                    displayName = "${selected.size} contacts"
                )
            }
        )
    }

    fun dismissBlockDialog() {
        _uiState.value = _uiState.value.copy(
            showBlockDialog = false,
            blockTarget = null
        )
    }

    fun dismissBlockLimitDialog() {
        _uiState.value = _uiState.value.copy(showBlockLimitDialog = false)
    }

    fun confirmBlockNumber() {
        val current = _uiState.value
        val target = current.blockTarget ?: return
        viewModelScope.launch {
            try {
                val threadIdsToBlock = if (current.selectedThreadIds.isNotEmpty()) {
                    current.selectedThreadIds.toList()
                } else {
                    listOfNotNull(current.conversations.find { it.address == target.address }?.threadId)
                }

                val addresses = if (current.selectedThreadIds.isNotEmpty()) {
                    current.conversations
                        .filter { it.threadId in current.selectedThreadIds }
                        .map { it.address }
                } else {
                    target.address.split(", ")
                }
                addresses.forEachIndexed { index, address ->
                    val ruleId = blockRepository.addRule(
                        BlockRule(type = BlockType.NUMBER, value = address)
                    )
                    val threadId = threadIdsToBlock.getOrNull(index)
                    if (threadId != null) {
                        moveThreadToSpam(threadId, ruleId, BlockType.NUMBER.name)
                    }
                }
                dismissBlockDialog()
                clearSelection()
                loadConversations()
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    // Category management
    fun showCategoryAssignDialog() {
        _uiState.value = _uiState.value.copy(showCategoryDialog = true)
    }

    fun dismissCategoryDialog() {
        _uiState.value = _uiState.value.copy(showCategoryDialog = false)
    }

    fun assignCategoryToSelected(categoryName: String?) {
        viewModelScope.launch {
            _uiState.value.selectedThreadIds.forEach { threadId ->
                conversationRepository.setConversationCategory(threadId, categoryName)
            }
            dismissCategoryDialog()
            clearSelection()
            loadConversations()
        }
    }

    fun showManageCategoriesDialog() {
        _uiState.value = _uiState.value.copy(showManageCategoriesDialog = true)
    }

    fun dismissManageCategoriesDialog() {
        _uiState.value = _uiState.value.copy(
            showManageCategoriesDialog = false,
            showAddCategoryDialog = false,
            editingCategory = null
        )
    }

    fun showAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(showAddCategoryDialog = true)
    }

    fun dismissAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(showAddCategoryDialog = false)
    }

    fun addCustomCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            conversationRepository.addCustomCategory(name.trim())
            val updated = conversationRepository.getCustomCategoriesOnce()
            _uiState.value = _uiState.value.copy(
                customCategories = updated,
                showAddCategoryDialog = false
            )
        }
    }

    fun startEditCategory(id: Long, name: String) {
        _uiState.value = _uiState.value.copy(editingCategory = id to name)
    }

    fun renameCustomCategory(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            conversationRepository.renameCustomCategory(id, newName.trim())
            val updated = conversationRepository.getCustomCategoriesOnce()
            _uiState.value = _uiState.value.copy(
                customCategories = updated,
                editingCategory = null
            )
        }
    }

    fun deleteCustomCategory(id: Long) {
        viewModelScope.launch {
            conversationRepository.deleteCustomCategory(id)
            val updated = conversationRepository.getCustomCategoriesOnce()
            _uiState.value = _uiState.value.copy(
                customCategories = updated,
                editingCategory = null
            )
            if (_uiState.value.selectedCustomCategory != null) {
                val stillExists = updated.any { it.second == _uiState.value.selectedCustomCategory }
                if (!stillExists) setCategory(MessageCategory.ALL)
            }
        }
    }

    fun confirmBlockName(customName: String? = null) {
        val current = _uiState.value
        val target = current.blockTarget ?: return
        if (customName != null && customName.isBlank()) return
        viewModelScope.launch {
            try {
                val threadIdsToBlock = if (current.selectedThreadIds.isNotEmpty()) {
                    current.selectedThreadIds.toList()
                } else {
                    listOfNotNull(current.conversations.find { it.address == target.address }?.threadId)
                }

                val names = if (customName != null) {
                    List(threadIdsToBlock.size) { customName }
                } else if (current.selectedThreadIds.isNotEmpty()) {
                    current.conversations
                        .filter { it.threadId in current.selectedThreadIds }
                        .mapNotNull { it.contactName }
                } else {
                    listOfNotNull(
                        current.conversations.find { it.address == target.address }?.contactName
                    )
                }
                names.forEachIndexed { index, name ->
                    val ruleId = blockRepository.addRule(
                        BlockRule(type = BlockType.SENDER_NAME, value = name)
                    )
                    val threadId = threadIdsToBlock.getOrNull(index)
                    if (threadId != null) {
                        moveThreadToSpam(threadId, ruleId, BlockType.SENDER_NAME.name)
                    }
                }
                dismissBlockDialog()
                clearSelection()
                loadConversations()
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    fun confirmBlockWords(words: String) {
        val current = _uiState.value
        val target = current.blockTarget ?: return
        if (words.isBlank()) return
        viewModelScope.launch {
            try {
                val ruleId = blockRepository.addRule(
                    BlockRule(type = BlockType.KEYWORD, value = words)
                )
                val threadIdsToBlock = if (current.selectedThreadIds.isNotEmpty()) {
                    current.selectedThreadIds.toList()
                } else {
                    listOfNotNull(current.conversations.find { it.address == target.address }?.threadId)
                }
                threadIdsToBlock.forEach { threadId ->
                    moveThreadToSpam(threadId, ruleId, BlockType.KEYWORD.name)
                }
                dismissBlockDialog()
                clearSelection()
                loadConversations()
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    fun confirmBlockLanguage(language: String) {
        val current = _uiState.value
        val target = current.blockTarget ?: return
        if (language.isBlank()) return
        viewModelScope.launch {
            try {
                val ruleId = blockRepository.addRule(
                    BlockRule(type = BlockType.LANGUAGE, value = language.trim().lowercase())
                )
                val threadIdsToBlock = if (current.selectedThreadIds.isNotEmpty()) {
                    current.selectedThreadIds.toList()
                } else {
                    listOfNotNull(current.conversations.find { it.address == target.address }?.threadId)
                }
                threadIdsToBlock.forEach { threadId ->
                    moveThreadToSpam(threadId, ruleId, BlockType.LANGUAGE.name)
                }
                dismissBlockDialog()
                clearSelection()
                loadConversations()
            } catch (e: BlockRuleLimitException) {
                _uiState.value = _uiState.value.copy(showBlockLimitDialog = true, showBlockDialog = false)
            }
        }
    }

    private suspend fun moveThreadToSpam(threadId: Long, ruleId: Long, ruleType: String) {
        val messages = conversationRepository.getMessagesForThread(threadId)
        spamMessageDao.insertSpamMessages(
            messages.map { msg ->
                SpamMessageEntity(
                    smsId = msg.id,
                    address = msg.address,
                    body = msg.body,
                    timestamp = msg.timestamp,
                    matchedRuleId = ruleId,
                    matchedRuleType = ruleType
                )
            }
        )
        try { conversationRepository.deleteThread(threadId) } catch (e: Exception) {}
    }
}
