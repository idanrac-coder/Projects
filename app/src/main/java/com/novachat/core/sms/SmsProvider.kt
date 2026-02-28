package com.novachat.core.sms

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.novachat.domain.model.Conversation
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsProvider @Inject constructor(
    private val contentResolver: ContentResolver,
    private val contactResolver: ContactResolver
) {

    suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val conversations = mutableListOf<Conversation>()
        val uri = Telephony.Sms.Conversations.CONTENT_URI

        val cursor = contentResolver.query(
            uri,
            arrayOf(
                Telephony.Sms.Conversations.THREAD_ID,
                Telephony.Sms.Conversations.SNIPPET,
                Telephony.Sms.Conversations.MESSAGE_COUNT
            ),
            null, null,
            "date DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID))
                val snippet = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET)) ?: ""
                val messageCount = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.Conversations.MESSAGE_COUNT))

                val threadInfo = getThreadInfo(threadId)
                if (threadInfo != null) {
                    val contactName = contactResolver.getContactName(threadInfo.address)
                    conversations.add(
                        Conversation(
                            threadId = threadId,
                            address = threadInfo.address,
                            contactName = contactName,
                            snippet = snippet,
                            timestamp = threadInfo.timestamp,
                            messageCount = messageCount,
                            unreadCount = getUnreadCount(threadId)
                        )
                    )
                }
            }
        }
        conversations
    }

    private fun getThreadInfo(threadId: Long): ThreadInfo? {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1"
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                ThreadInfo(
                    address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "",
                    timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                )
            } else null
        }
    }

    private fun getUnreadCount(threadId: Long): Int {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf("COUNT(*)"),
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString()),
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        } ?: 0
    }

    suspend fun getMessagesForThread(threadId: Long): List<Message> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Message>()
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            ),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                messages.add(cursorToMessage(it))
            }
        }
        messages
    }

    suspend fun searchMessages(query: String): List<Message> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Message>()
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            ),
            "${Telephony.Sms.BODY} LIKE ?",
            arrayOf("%$query%"),
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                messages.add(cursorToMessage(it))
            }
        }
        messages
    }

    suspend fun markThreadAsRead(threadId: Long) = withContext(Dispatchers.IO) {
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 1)
        }
        contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString())
        )
    }

    suspend fun deleteThread(threadId: Long) = withContext(Dispatchers.IO) {
        contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString())
        )
    }

    private fun cursorToMessage(cursor: Cursor): Message {
        val typeInt = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))
        return Message(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)),
            threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)),
            address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "",
            body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: "",
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)),
            type = mapSmsType(typeInt),
            isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
        )
    }

    private fun mapSmsType(type: Int): MessageType = when (type) {
        Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageType.RECEIVED
        Telephony.Sms.MESSAGE_TYPE_SENT -> MessageType.SENT
        Telephony.Sms.MESSAGE_TYPE_DRAFT -> MessageType.DRAFT
        Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageType.FAILED
        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> MessageType.OUTBOX
        Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageType.QUEUED
        else -> MessageType.RECEIVED
    }

    private data class ThreadInfo(val address: String, val timestamp: Long)
}
