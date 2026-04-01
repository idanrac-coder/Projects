package com.novachat.core.sms

import android.app.role.RoleManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.novachat.BuildConfig
import com.novachat.domain.model.Conversation
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val RECENTLY_INSERTED_TTL_MS = 3000L

@Singleton
class SmsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val contactResolver: ContactResolver
) {
    private val recentlyInsertedIds = ConcurrentHashMap<Long, Long>()

    fun markAsInsertedByUs(messageId: Long) {
        recentlyInsertedIds[messageId] = System.currentTimeMillis()
    }

    fun wasInsertedByUs(messageId: Long): Boolean {
        pruneExpiredIds()
        return recentlyInsertedIds.remove(messageId) != null
    }

    private fun pruneExpiredIds() {
        val now = System.currentTimeMillis()
        recentlyInsertedIds.entries.removeIf { (_, ts) -> now - ts > RECENTLY_INSERTED_TTL_MS }
    }

    fun isDefaultSmsApp(): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    }

    suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "*** SmsProvider.getConversations() querying content://sms")
        data class ThreadAccum(
            var address: String = "",
            var snippet: String = "",
            var timestamp: Long = 0L,
            var messageCount: Int = 0,
            var unreadCount: Int = 0
        )

        val threadMap = LinkedHashMap<Long, ThreadAccum>()

        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            ),
            null, null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val colThread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val colAddress = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val colBody = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val colDate = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val colType = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val colRead = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(colThread)
                val address = cursor.getString(colAddress) ?: ""
                val body = cursor.getString(colBody) ?: ""
                val date = cursor.getLong(colDate)
                val type = cursor.getInt(colType)
                val read = cursor.getInt(colRead)

                val accum = threadMap.getOrPut(threadId) {
                    ThreadAccum(
                        address = address,
                        snippet = body,
                        timestamp = date
                    )
                }
                accum.messageCount++
                if (read == 0 && type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    accum.unreadCount++
                }
            }
        }

        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "*** SmsProvider.getConversations() found ${threadMap.size} threads")
        if (BuildConfig.DEBUG) threadMap.forEach { (tid, acc) ->
            Log.d("NC_DEBUG", "***   thread=$tid addr=${acc.address} msgs=${acc.messageCount} unread=${acc.unreadCount} snippet=${acc.snippet.take(20)}")
        }

        if (threadMap.isEmpty()) return@withContext emptyList()

        contactResolver.preloadContacts()

        threadMap.map { (threadId, accum) ->
            Conversation(
                threadId = threadId,
                address = accum.address,
                contactName = contactResolver.getContactName(accum.address),
                snippet = accum.snippet,
                timestamp = accum.timestamp,
                messageCount = accum.messageCount,
                unreadCount = accum.unreadCount
            )
        }
    }

    suspend fun getMessagesForThread(threadId: Long): List<Message> = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "*** SmsProvider.getMessagesForThread($threadId) querying")
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
            "${Telephony.Sms.DATE} ASC, ${Telephony.Sms._ID} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                messages.add(cursorToMessage(it))
            }
        }
        messages.sortWith(compareBy({ it.timestamp }, { it.id }))
        if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "*** SmsProvider.getMessagesForThread($threadId) found ${messages.size} messages")
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
            put(Telephony.Sms.SEEN, 1)
        }
        contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND (${Telephony.Sms.READ} = 0 OR ${Telephony.Sms.SEEN} = 0)",
            arrayOf(threadId.toString())
        )
    }

    data class InsertResult(val uri: Uri?, val threadId: Long)

    suspend fun insertIncomingSms(address: String, body: String, timestamp: Long): InsertResult = withContext(Dispatchers.IO) {
        val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, timestamp)
            put(Telephony.Sms.DATE_SENT, timestamp)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            put(Telephony.Sms.THREAD_ID, threadId)
        }
        try {
            val uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                ?: contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            Log.d("SmsProvider", "Insert result: uri=$uri threadId=$threadId address=$address")
            uri?.let { u ->
                try {
                    val id = ContentUris.parseId(u)
                    markAsInsertedByUs(id)
                } catch (_: Exception) { }
            }
            InsertResult(uri, threadId)
        } catch (e: Exception) {
            Log.e("SmsProvider", "Failed to insert incoming SMS", e)
            InsertResult(null, threadId)
        }
    }

    suspend fun getThreadIdForAddress(address: String): Long = withContext(Dispatchers.IO) {
        try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun getMessageCountForThread(threadId: Long): Int = withContext(Dispatchers.IO) {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            null
        )
        cursor?.use { it.count } ?: 0
    }

    suspend fun deleteThread(threadId: Long): Int = withContext(Dispatchers.IO) {
        contentResolver.delete(
            Uri.parse("content://mms-sms/conversations/$threadId"),
            null,
            null
        )
    }

    suspend fun deleteMessage(messageId: Long): Int = withContext(Dispatchers.IO) {
        contentResolver.delete(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId),
            null,
            null
        )
    }

    suspend fun updateMessageBody(messageId: Long, newBody: String): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(Telephony.Sms.BODY, newBody)
        }
        val rows = contentResolver.update(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId),
            values, null, null
        )
        rows > 0
    }

    data class InboxMessageInfo(
        val messageId: Long,
        val address: String,
        val body: String,
        val timestamp: Long
    )

    suspend fun getInboxMessageById(messageId: Long): InboxMessageInfo? = withContext(Dispatchers.IO) {
        contentResolver.query(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId),
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    InboxMessageInfo(
                        messageId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)),
                        address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "",
                        body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: "",
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    )
                } else null
                } else null
        }
    }

    suspend fun getRecentInboxMessageIds(sinceMsAgo: Long = 5000L): List<Long> = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - sinceMsAgo
        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
            arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), since.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val colId = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val ids = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(colId))
            }
            ids
        } ?: emptyList()
    }

    data class InboxScanMessage(
        val smsId: Long,
        val threadId: Long,
        val address: String,
        val body: String,
        val timestamp: Long
    )

    suspend fun getInboxMessages(): List<InboxScanMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<InboxScanMessage>()
        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            "${Telephony.Sms.TYPE} = ?",
            arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val colId = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val colThread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val colAddress = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val colBody = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val colDate = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                messages.add(
                    InboxScanMessage(
                        smsId = cursor.getLong(colId),
                        threadId = cursor.getLong(colThread),
                        address = cursor.getString(colAddress) ?: "",
                        body = cursor.getString(colBody) ?: "",
                        timestamp = cursor.getLong(colDate)
                    )
                )
            }
        }
        messages
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
