package com.novachat.domain.repository

import com.novachat.domain.model.Conversation
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageEdit
import com.novachat.domain.model.Reaction
import com.novachat.domain.model.ScheduledMessage
import com.novachat.domain.model.MessageReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ConversationRepository {
    val refreshTrigger: SharedFlow<Long>
    suspend fun getConversations(): List<Conversation>
    fun getCachedConversations(): List<Conversation>?
    fun getCachedMessagesForThread(threadId: Long): List<Message>?
    fun invalidateConversationsCache()
    fun invalidateMessagesCache(threadId: Long)
    fun invalidateAllCaches()
    fun notifyNewMessage(threadId: Long)
    suspend fun getArchivedConversations(): List<Conversation>
    suspend fun unarchiveConversation(threadId: Long)
    suspend fun getMessagesForThread(threadId: Long): List<Message>
    suspend fun searchMessages(query: String): List<Message>
    suspend fun markThreadAsRead(threadId: Long)
    fun isDefaultSmsApp(): Boolean
    suspend fun deleteThread(threadId: Long): Boolean
    suspend fun deleteMessage(messageId: Long)
    suspend fun pinConversation(threadId: Long, pinned: Boolean)
    suspend fun archiveConversation(threadId: Long, archived: Boolean)
    suspend fun muteConversation(threadId: Long, muted: Boolean)
    suspend fun sendSms(address: String, body: String): Result<Unit>
    suspend fun getThreadIdForAddress(address: String): Long

    suspend fun addReaction(messageId: Long, emoji: String)
    suspend fun removeReaction(messageId: Long, emoji: String)
    suspend fun getReactionsForMessages(messageIds: List<Long>): Map<Long, List<Reaction>>

    suspend fun scheduleMessage(address: String, body: String, scheduledTime: Long, threadId: Long = -1, contactName: String? = null, sendViaWhatsApp: Boolean = false): Long
    suspend fun cancelScheduledMessage(id: Long)
    fun getScheduledMessages(): Flow<List<ScheduledMessage>>

    suspend fun pinMessage(messageId: Long, threadId: Long)
    suspend fun unpinMessage(messageId: Long)
    suspend fun getPinnedMessageIds(threadId: Long): List<Long>

    suspend fun setReminderForMessage(messageId: Long, threadId: Long, address: String, body: String, contactName: String?, reminderTime: Long): Long
    suspend fun cancelReminder(id: Long)
    fun getPendingReminders(): Flow<List<MessageReminder>>

    suspend fun setCustomTheme(threadId: Long, themeId: Long?)
    suspend fun setAutoDelete(threadId: Long, durationMs: Long?)

    suspend fun setConversationCategory(threadId: Long, category: String?)
    fun getCustomCategories(): Flow<List<String>>
    suspend fun addCustomCategory(name: String): Long
    suspend fun renameCustomCategory(id: Long, newName: String)
    suspend fun deleteCustomCategory(id: Long)
    suspend fun getCustomCategoriesOnce(): List<Pair<Long, String>>

    suspend fun saveMessageEdit(messageId: Long, previousBody: String, newBody: String)
    suspend fun getEditHistory(messageId: Long): List<MessageEdit>
    suspend fun getEditedMessageIds(messageIds: List<Long>): Set<Long>
    suspend fun updateMessageBody(messageId: Long, newBody: String): Boolean
}
