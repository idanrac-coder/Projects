package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_reactions")
data class MessageReactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)
