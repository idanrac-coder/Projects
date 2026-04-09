package com.novachat.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.novachat.MainActivity
import com.novachat.R
import com.novachat.core.database.financial.entity.FinancialAlertEntity

object FinancialNotificationHelper {
    const val CHANNEL_ID = "novachat_financial_alerts"
    private const val CHANNEL_NAME = "Financial Alerts"
    private const val NOTIFICATION_GROUP = "financial_alerts_group"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Unusual charges, subscription price changes, and duplicate transactions"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showAlert(context: Context, alert: FinancialAlertEntity) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alert.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (alert.type) {
            "UNUSUAL_AMOUNT" -> "Unusual Charge Detected"
            "NEW_MERCHANT_HIGH" -> "New Merchant - High Amount"
            "DUPLICATE_CHARGE" -> "Duplicate Charge"
            "SUBSCRIPTION_PRICE_INCREASE" -> "Subscription Price Increase"
            else -> "Financial Alert"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(alert.id.toInt(), notification)
    }
}
