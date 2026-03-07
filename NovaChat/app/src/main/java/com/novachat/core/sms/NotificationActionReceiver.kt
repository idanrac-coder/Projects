package com.novachat.core.sms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.sms.ScamCategory
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.novachat.ACTION_MARK_READ"
        const val ACTION_DELETE = "com.novachat.ACTION_DELETE"
        const val ACTION_REPORT_SPAM = "com.novachat.ACTION_REPORT_SPAM"
        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_CATEGORY = "extra_category"
        private const val TAG = "NotifActionReceiver"
    }

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var smsProvider: SmsProvider

    @Inject
    lateinit var scamDetector: ScamDetector

    @Inject
    lateinit var blockRepository: BlockRepository

    @Inject
    lateinit var spamMessageDao: SpamMessageDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (threadId <= 0) {
            Log.w(TAG, "Invalid threadId: $threadId")
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            ACTION_MARK_READ -> {
                Log.d(TAG, "Mark as read: threadId=$threadId")
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        conversationRepository.markThreadAsRead(threadId)
                        conversationRepository.invalidateAllCaches()
                        conversationRepository.notifyNewMessage(-1L)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to mark as read", e)
                    } finally {
                        if (notificationId >= 0) {
                            notificationManager.cancel(notificationId)
                        }
                        pendingResult.finish()
                    }
                }
            }

            ACTION_DELETE -> {
                Log.d(TAG, "Delete thread: threadId=$threadId")
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        smsProvider.deleteThread(threadId)
                        conversationRepository.invalidateAllCaches()
                        conversationRepository.notifyNewMessage(-1L)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete thread", e)
                    } finally {
                        if (notificationId >= 0) {
                            notificationManager.cancel(notificationId)
                        }
                        pendingResult.finish()
                    }
                }
            }

            ACTION_REPORT_SPAM -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
                val body = intent.getStringExtra(EXTRA_BODY) ?: return
                val categoryStr = intent.getStringExtra(EXTRA_CATEGORY)
                val category = categoryStr?.let { str ->
                    try { ScamCategory.valueOf(str) } catch (_: Exception) { null }
                }
                Log.d(TAG, "Report spam: address=$address threadId=$threadId")
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        scamDetector.reportSpam(address, body, category)
                        val ruleId = try {
                            blockRepository.addRule(BlockRule(type = BlockType.NUMBER, value = address))
                        } catch (_: Exception) { -1L }
                        val messages = conversationRepository.getMessagesForThread(threadId)
                        val ruleType = "SCAM_REPORT"
                        messages.forEach { msg ->
                            spamMessageDao.insertSpamMessage(
                                SpamMessageEntity(
                                    smsId = msg.id,
                                    address = msg.address,
                                    body = msg.body,
                                    timestamp = msg.timestamp,
                                    matchedRuleId = ruleId,
                                    matchedRuleType = ruleType
                                )
                            )
                        }
                        smsProvider.deleteThread(threadId)
                        conversationRepository.invalidateAllCaches()
                        conversationRepository.notifyNewMessage(-1L)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to report spam", e)
                    } finally {
                        if (notificationId >= 0) {
                            notificationManager.cancel(notificationId)
                        }
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
