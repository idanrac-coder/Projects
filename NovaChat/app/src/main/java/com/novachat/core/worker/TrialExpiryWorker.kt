package com.novachat.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novachat.MainActivity
import com.novachat.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class TrialExpiryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "trial_expiry_notification"
        private const val CHANNEL_ID = "novachat_trial"
        private const val NOTIFICATION_ID = 9001

        fun schedule(context: Context, trialStartTimeMs: Long, trialDurationMs: Long) {
            val expiryMs = trialStartTimeMs + trialDurationMs
            // Notify 1 day before expiry
            val notifyAtMs = expiryMs - TimeUnit.DAYS.toMillis(1)
            val delayMs = notifyAtMs - System.currentTimeMillis()
            if (delayMs <= 0) return

            val request = OneTimeWorkRequestBuilder<TrialExpiryWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        createChannel()
        showNotification()
        return Result.success()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Premium Trial",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders about your Premium trial status"
        }
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun showNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "license")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_logo)
            .setContentTitle("Your Premium trial ends tomorrow")
            .setContentText("Purchase a license to keep all premium features unlocked.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Purchase a license to keep all premium features unlocked."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }
}
