package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_reminders")
data class MessageReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val threadId: Long,
    val address: String,
    val messageBody: String,
    val contactName: String?,
    val reminderTime: Long,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
