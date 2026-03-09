package com.novachat.data.repository

import android.util.Log
import com.novachat.BuildConfig
import com.novachat.core.database.dao.ConversationMetaDao
import com.novachat.core.database.dao.CustomCategoryDao
import com.novachat.core.database.dao.MessageReactionDao
import com.novachat.core.database.dao.MessageReminderDao
import com.novachat.core.database.dao.PinnedMessageDao
import com.novachat.core.database.dao.ScheduledMessageDao
import com.novachat.core.database.entity.ConversationMetaEntity
import com.novachat.core.database.entity.CustomCategoryEntity
import com.novachat.core.database.entity.MessageReactionEntity
import com.novachat.core.database.entity.MessageReminderEntity
import com.novachat.core.database.entity.PinnedMessageEntity
import com.novachat.core.database.entity.ScheduledMessageEntity
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.sms.SmsSender
import com.novachat.core.sms.SmsProvider
import com.novachat.domain.model.Conversation
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageCategory
import com.novachat.domain.model.MessageReminder
import com.novachat.domain.model.Reaction
import com.novachat.domain.model.ScheduledMessage
import com.novachat.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val smsProvider: SmsProvider,
    private val spamMessageDao: SpamMessageDao,
    private val smsSender: SmsSender,
    private val conversationMetaDao: ConversationMetaDao,
    private val customCategoryDao: CustomCategoryDao,
    private val messageReactionDao: MessageReactionDao,
    private val scheduledMessageDao: ScheduledMessageDao,
    private val messageReminderDao: MessageReminderDao,
    private val pinnedMessageDao: PinnedMessageDao
) : ConversationRepository {

    private val _refreshTrigger = MutableSharedFlow<Long>(extraBufferCapacity = 5, replay = 0)
    override val refreshTrigger: SharedFlow<Long> = _refreshTrigger.asSharedFlow()

    @Volatile
    private var cachedConversations: List<Conversation>? = null
    private val cachedMessages = ConcurrentHashMap<Long, List<Message>>()

    private companion object {
        const val MAX_CACHED_THREADS = 8
    }

    override fun getCachedConversations(): List<Conversation>? = cachedConversations

    override fun getCachedMessagesForThread(threadId: Long): List<Message>? =
        cachedMessages[threadId]

    override fun invalidateConversationsCache() {
        cachedConversations = null
    }

    override fun invalidateMessagesCache(threadId: Long) {
        cachedMessages.remove(threadId)
    }

    override fun invalidateAllCaches() {
        cachedConversations = null
        cachedMessages.clear()
    }

    override fun notifyNewMessage(threadId: Long) {
        val emitted = _refreshTrigger.tryEmit(threadId)
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "### Repo.notifyNewMessage($threadId) emitted=$emitted subscriberCount=${_refreshTrigger.subscriptionCount.value}")
    }

    override suspend fun getConversations(): List<Conversation> {
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "### Repo.getConversations() START")
        val spamAddresses = spamMessageDao.getSpamAddresses().toSet()
        val normalizeAddress: (String) -> String = { a ->
            a.replace(Regex("[^0-9]"), "").let { d ->
                if (d.startsWith("972") && d.length >= 12) "0" + d.drop(3) else d
            }
        }
        val spamNormalized = spamAddresses.map { normalizeAddress(it) }.toSet()
        val rawConversations = smsProvider.getConversations()
        val conversations = rawConversations.filter { conv ->
            val norm = normalizeAddress(conv.address)
            norm !in spamNormalized && !spamAddresses.contains(conv.address)
        }
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "### Repo.getConversations() raw=${rawConversations.size} filtered=${conversations.size} spamAddrs=${spamAddresses.size}")
        val allMetas = conversationMetaDao.getAllMetas().associateBy { it.threadId }
        val result = conversations
            .map { conversation ->
                val meta = allMetas[conversation.threadId]
                val category = categorize(conversation)
                val effectiveUnread = computeEffectiveUnread(conversation, meta)
                if (meta != null) {
                    conversation.copy(
                        isPinned = meta.isPinned,
                        isMuted = meta.isMuted,
                        isArchived = meta.isArchived,
                        category = category,
                        customCategory = meta.customCategory,
                        customThemeId = meta.customThemeId,
                        unreadCount = effectiveUnread
                    )
                } else {
                    conversation.copy(category = category)
                }
            }.sortedWith(
                compareByDescending<Conversation> { it.isPinned }
                    .thenByDescending { it.timestamp }
            )
        cachedConversations = result
        return result
    }

    private fun computeEffectiveUnread(
        conversation: Conversation,
        meta: ConversationMetaEntity?
    ): Int {
        if (conversation.unreadCount > 0) {
            val lastReadCount = meta?.lastReadMessageCount
            if (lastReadCount != null && conversation.messageCount <= lastReadCount) {
                return 0
            }
            return conversation.unreadCount
        }
        return 0
    }

    private fun categorize(conversation: Conversation): MessageCategory {
        if (conversation.contactName != null) return MessageCategory.CONTACTS
        return MessageCategory.ALL
    }

    override suspend fun getArchivedConversations(): List<Conversation> {
        val archivedMetas = conversationMetaDao.getArchivedConversationsOnce()
        val archivedThreadIds = archivedMetas.map { it.threadId }.toSet()
        val allConversations = smsProvider.getConversations()
        return allConversations
            .filter { it.threadId in archivedThreadIds }
            .map { conversation ->
                val meta = archivedMetas.find { it.threadId == conversation.threadId }
                conversation.copy(
                    isPinned = meta?.isPinned ?: false,
                    isMuted = meta?.isMuted ?: false,
                    isArchived = true,
                    category = categorize(conversation),
                    customCategory = meta?.customCategory,
                    customThemeId = meta?.customThemeId
                )
            }
            .sortedByDescending { it.timestamp }
    }

    override suspend fun unarchiveConversation(threadId: Long) {
        ensureMetaExists(threadId)
        conversationMetaDao.setArchived(threadId, false)
    }

    override suspend fun getMessagesForThread(threadId: Long): List<Message> {
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "### Repo.getMessagesForThread($threadId) START")
        val messages = smsProvider.getMessagesForThread(threadId)
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "### Repo.getMessagesForThread($threadId) returned ${messages.size} messages")
        val messageIds = messages.map { it.id }
        val reactions = getReactionsForMessages(messageIds)
        val pinnedIds = pinnedMessageDao.getPinnedMessageIds(threadId).toSet()
        val result = messages.map { msg ->
            msg.copy(
                reactions = reactions[msg.id] ?: emptyList(),
                isPinned = msg.id in pinnedIds
            )
        }
        if (cachedMessages.size >= MAX_CACHED_THREADS && !cachedMessages.containsKey(threadId)) {
            cachedMessages.keys.firstOrNull()?.let { cachedMessages.remove(it) }
        }
        cachedMessages[threadId] = result
        return result
    }

    override suspend fun searchMessages(query: String): List<Message> {
        return smsProvider.searchMessages(query)
    }

    override suspend fun markThreadAsRead(threadId: Long) {
        smsProvider.markThreadAsRead(threadId)
        ensureMetaExists(threadId)
        val messageCount = smsProvider.getMessageCountForThread(threadId)
        conversationMetaDao.setLastReadTimestamp(threadId, System.currentTimeMillis())
        conversationMetaDao.setLastReadMessageCount(threadId, messageCount)
        cachedConversations = null
    }

    override fun isDefaultSmsApp(): Boolean = smsProvider.isDefaultSmsApp()

    override suspend fun deleteThread(threadId: Long): Boolean {
        val deleted = smsProvider.deleteThread(threadId) > 0
        if (deleted) {
            cachedConversations = null
            cachedMessages.remove(threadId)
        }
        return deleted
    }

    override suspend fun pinConversation(threadId: Long, pinned: Boolean) {
        ensureMetaExists(threadId)
        conversationMetaDao.setPinned(threadId, pinned)
    }

    override suspend fun archiveConversation(threadId: Long, archived: Boolean) {
        ensureMetaExists(threadId)
        conversationMetaDao.setArchived(threadId, archived)
    }

    override suspend fun muteConversation(threadId: Long, muted: Boolean) {
        ensureMetaExists(threadId)
        conversationMetaDao.setMuted(threadId, muted)
    }

    override suspend fun sendSms(address: String, body: String): Result<Unit> {
        val result = smsSender.sendSms(address, body)
        if (result.isSuccess) {
            cachedConversations = null
        }
        return result
    }

    override suspend fun getThreadIdForAddress(address: String): Long {
        return smsProvider.getThreadIdForAddress(address)
    }

    override suspend fun addReaction(messageId: Long, emoji: String) {
        messageReactionDao.insert(
            MessageReactionEntity(messageId = messageId, emoji = emoji)
        )
    }

    override suspend fun removeReaction(messageId: Long, emoji: String) {
        messageReactionDao.removeReaction(messageId, emoji)
    }

    override suspend fun getReactionsForMessages(messageIds: List<Long>): Map<Long, List<Reaction>> {
        if (messageIds.isEmpty()) return emptyMap()
        return messageReactionDao.getReactionsForMessages(messageIds)
            .groupBy { it.messageId }
            .mapValues { (_, entities) ->
                entities.map { Reaction(it.id, it.messageId, it.emoji, it.timestamp) }
            }
    }

    override suspend fun scheduleMessage(
        address: String, body: String, scheduledTime: Long, threadId: Long, contactName: String?,
        sendViaWhatsApp: Boolean
    ): Long {
        return scheduledMessageDao.insert(
            ScheduledMessageEntity(
                address = address,
                body = body,
                scheduledTime = scheduledTime,
                threadId = threadId,
                contactName = contactName,
                sendViaWhatsApp = sendViaWhatsApp
            )
        )
    }

    override suspend fun cancelScheduledMessage(id: Long) {
        scheduledMessageDao.deleteById(id)
    }

    override fun getScheduledMessages(): Flow<List<ScheduledMessage>> {
        return scheduledMessageDao.getPendingMessages().map { list ->
            list.map { entity ->
                ScheduledMessage(
                    id = entity.id,
                    address = entity.address,
                    body = entity.body,
                    scheduledTime = entity.scheduledTime,
                    threadId = entity.threadId,
                    contactName = entity.contactName,
                    isSent = entity.isSent,
                    sendViaWhatsApp = entity.sendViaWhatsApp,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override suspend fun pinMessage(messageId: Long, threadId: Long) {
        pinnedMessageDao.pin(PinnedMessageEntity(messageId = messageId, threadId = threadId))
    }

    override suspend fun unpinMessage(messageId: Long) {
        pinnedMessageDao.unpin(messageId)
    }

    override suspend fun getPinnedMessageIds(threadId: Long): List<Long> {
        return pinnedMessageDao.getPinnedMessageIds(threadId)
    }

    override suspend fun setReminderForMessage(
        messageId: Long, threadId: Long, address: String, body: String,
        contactName: String?, reminderTime: Long
    ): Long {
        return messageReminderDao.insert(
            MessageReminderEntity(
                messageId = messageId,
                threadId = threadId,
                address = address,
                messageBody = body,
                contactName = contactName,
                reminderTime = reminderTime
            )
        )
    }

    override suspend fun cancelReminder(id: Long) {
        messageReminderDao.deleteById(id)
    }

    override fun getPendingReminders(): Flow<List<MessageReminder>> {
        return messageReminderDao.getPendingReminders().map { list ->
            list.map { entity ->
                MessageReminder(
                    id = entity.id,
                    messageId = entity.messageId,
                    threadId = entity.threadId,
                    address = entity.address,
                    messageBody = entity.messageBody,
                    contactName = entity.contactName,
                    reminderTime = entity.reminderTime,
                    isTriggered = entity.isTriggered,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override suspend fun setCustomTheme(threadId: Long, themeId: Long?) {
        ensureMetaExists(threadId)
        conversationMetaDao.setCustomTheme(threadId, themeId)
    }

    override suspend fun setAutoDelete(threadId: Long, durationMs: Long?) {
        ensureMetaExists(threadId)
        conversationMetaDao.setAutoDelete(threadId, durationMs)
    }

    override suspend fun setConversationCategory(threadId: Long, category: String?) {
        ensureMetaExists(threadId)
        conversationMetaDao.setCustomCategory(threadId, category)
    }

    override fun getCustomCategories(): Flow<List<String>> {
        return customCategoryDao.getAll().map { list -> list.map { it.name } }
    }

    override suspend fun addCustomCategory(name: String): Long {
        return customCategoryDao.insert(CustomCategoryEntity(name = name))
    }

    override suspend fun renameCustomCategory(id: Long, newName: String) {
        customCategoryDao.rename(id, newName)
    }

    override suspend fun deleteCustomCategory(id: Long) {
        customCategoryDao.deleteById(id)
    }

    override suspend fun getCustomCategoriesOnce(): List<Pair<Long, String>> {
        return customCategoryDao.getAllOnce().map { it.id to it.name }
    }

    private suspend fun ensureMetaExists(threadId: Long) {
        if (conversationMetaDao.getMetaForThread(threadId) == null) {
            conversationMetaDao.upsertMeta(ConversationMetaEntity(threadId = threadId))
        }
    }
}
