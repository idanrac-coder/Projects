package com.novachat.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novachat.core.database.dao.ScheduledMessageDao
import com.novachat.core.sms.SmsSender
import com.novachat.core.sms.WhatsAppForwarder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduledMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduledMessageDao: ScheduledMessageDao,
    private val smsSender: SmsSender,
    private val whatsAppForwarder: WhatsAppForwarder
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dueMessages = scheduledMessageDao.getDueMessages(System.currentTimeMillis())
        dueMessages.forEach { scheduled ->
            val success = if (scheduled.sendViaWhatsApp) {
                whatsAppForwarder.sendMessage(scheduled.address, scheduled.body)
            } else {
                smsSender.sendSms(scheduled.address, scheduled.body).isSuccess
            }
            if (success) {
                scheduledMessageDao.markAsSent(scheduled.id)
            } else {
                scheduledMessageDao.incrementRetryCount(scheduled.id)
                if (scheduled.retryCount + 1 >= MAX_RETRIES) {
                    scheduledMessageDao.markAsFailed(scheduled.id)
                }
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "scheduled_messages_check"
        private const val MAX_RETRIES = 3

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduledMessageWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
