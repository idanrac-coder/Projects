package com.novachat.core.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BubbleNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BUBBLE_CHANNEL_ID = "novachat_bubbles"
        private const val BUBBLE_CHANNEL_NAME = "Chat Bubbles"
    }

    fun createBubbleChannel() {
        val channel = NotificationChannel(
            BUBBLE_CHANNEL_ID,
            BUBBLE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Floating chat bubbles for quick replies"
            setAllowBubbles(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun createDynamicShortcut(
        threadId: Long,
        contactName: String,
        address: String
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val shortcutId = "chat_$threadId"

        val person = Person.Builder()
            .setName(contactName)
            .setKey(address)
            .build()

        val shortcut = ShortcutInfo.Builder(context, shortcutId)
            .setLongLived(true)
            .setShortLabel(contactName)
            .setLongLabel("Chat with $contactName")
            .setPerson(person)
            .setIntent(
                Intent(context, Class.forName("com.novachat.MainActivity")).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("threadId", threadId)
                    putExtra("address", address)
                }
            )
            .build()

        shortcutManager.pushDynamicShortcut(shortcut)
    }

    fun showBubbleNotification(
        threadId: Long,
        contactName: String,
        address: String,
        messageBody: String
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val shortcutId = "chat_$threadId"

        createDynamicShortcut(threadId, contactName, address)

        val person = Person.Builder()
            .setName(contactName)
            .setKey(address)
            .build()

        val intent = Intent(context, Class.forName("com.novachat.MainActivity")).apply {
            action = Intent.ACTION_VIEW
            putExtra("threadId", threadId)
            putExtra("address", address)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, threadId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val bubbleMetadata = Notification.BubbleMetadata.Builder(
            pendingIntent,
            Icon.createWithResource(context, android.R.drawable.sym_action_chat)
        )
            .setDesiredHeight(600)
            .setAutoExpandBubble(false)
            .setSuppressNotification(false)
            .build()

        val messagingStyle = Notification.MessagingStyle(person)
            .addMessage(messageBody, System.currentTimeMillis(), person)

        val notification = Notification.Builder(context, BUBBLE_CHANNEL_ID)
            .setContentTitle(contactName)
            .setContentText(messageBody)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setShortcutId(shortcutId)
            .setBubbleMetadata(bubbleMetadata)
            .setStyle(messagingStyle)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(threadId.toInt(), notification)
    }
}
