package com.novachat.domain.repository

import com.novachat.domain.model.Conversation
import com.novachat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun getConversations(): List<Conversation>
    suspend fun getMessagesForThread(threadId: Long): List<Message>
    suspend fun searchMessages(query: String): List<Message>
    suspend fun markThreadAsRead(threadId: Long)
    suspend fun deleteThread(threadId: Long)
    suspend fun pinConversation(threadId: Long, pinned: Boolean)
    suspend fun archiveConversation(threadId: Long, archived: Boolean)
    suspend fun muteConversation(threadId: Long, muted: Boolean)
    suspend fun sendSms(address: String, body: String): Result<Unit>
}
