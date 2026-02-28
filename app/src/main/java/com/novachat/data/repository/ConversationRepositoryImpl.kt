package com.novachat.data.repository

import com.novachat.core.database.dao.ConversationMetaDao
import com.novachat.core.database.entity.ConversationMetaEntity
import com.novachat.core.sms.SmsSender
import com.novachat.core.sms.SmsProvider
import com.novachat.domain.model.Conversation
import com.novachat.domain.model.Message
import com.novachat.domain.repository.ConversationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val smsProvider: SmsProvider,
    private val smsSender: SmsSender,
    private val conversationMetaDao: ConversationMetaDao
) : ConversationRepository {

    override suspend fun getConversations(): List<Conversation> {
        val conversations = smsProvider.getConversations()
        return conversations.map { conversation ->
            val meta = conversationMetaDao.getMetaForThread(conversation.threadId)
            if (meta != null) {
                conversation.copy(
                    isPinned = meta.isPinned,
                    isMuted = meta.isMuted,
                    isArchived = meta.isArchived
                )
            } else {
                conversation
            }
        }.sortedWith(
            compareByDescending<Conversation> { it.isPinned }
                .thenByDescending { it.timestamp }
        )
    }

    override suspend fun getMessagesForThread(threadId: Long): List<Message> {
        return smsProvider.getMessagesForThread(threadId)
    }

    override suspend fun searchMessages(query: String): List<Message> {
        return smsProvider.searchMessages(query)
    }

    override suspend fun markThreadAsRead(threadId: Long) {
        smsProvider.markThreadAsRead(threadId)
    }

    override suspend fun deleteThread(threadId: Long) {
        smsProvider.deleteThread(threadId)
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
        return smsSender.sendSms(address, body)
    }

    private suspend fun ensureMetaExists(threadId: Long) {
        if (conversationMetaDao.getMetaForThread(threadId) == null) {
            conversationMetaDao.upsertMeta(ConversationMetaEntity(threadId = threadId))
        }
    }
}
