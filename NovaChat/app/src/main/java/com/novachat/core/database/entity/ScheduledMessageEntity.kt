package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val body: String,
    val scheduledTime: Long,
    val threadId: Long = -1,
    val contactName: String? = null,
    val isSent: Boolean = false,
    val sendViaWhatsApp: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
