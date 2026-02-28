package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_meta")
data class ConversationMetaEntity(
    @PrimaryKey
    val threadId: Long,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val customNotificationSound: String? = null,
    val customVibration: Boolean? = null,
    val customLedColor: Int? = null,
    val priorityLevel: String? = null,
    val bubbleEnabled: Boolean? = null
)
