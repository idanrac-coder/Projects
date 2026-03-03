package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_messages")
data class PinnedMessageEntity(
    @PrimaryKey
    val messageId: Long,
    val threadId: Long,
    val pinnedAt: Long = System.currentTimeMillis()
)
