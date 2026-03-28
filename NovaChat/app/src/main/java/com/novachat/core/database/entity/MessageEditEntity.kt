package com.novachat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_edits")
data class MessageEditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val previousBody: String,
    val newBody: String,
    val timestamp: Long
)
