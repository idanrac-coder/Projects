package com.novachat.core.sms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        private const val TAG = "NotifActionReceiver"
    }

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var smsProvider: SmsProvider

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
        }
    }
}
