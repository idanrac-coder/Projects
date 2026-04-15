package com.novachat.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_meta",
    indices = [
        Index(value = ["isPinned"]),
        Index(value = ["isArchived"]),
        Index(value = ["isMuted"]),
        Index(value = ["muteUntil"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isLocked"])
    ]
)
data class ConversationMetaEntity(
    @PrimaryKey
    val threadId: Long,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,  // legacy — kept for schema compat; use muteUntil instead
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val customNotificationSound: String? = null,
    val customVibration: Boolean? = null,
    val customLedColor: Int? = null,
    val priorityLevel: String? = null,
    val bubbleEnabled: Boolean? = null,
    val customThemeId: Long? = null,
    val autoDeleteAfterMs: Long? = null,
    val customCategory: String? = null,
    val lastReadTimestamp: Long? = null,
    val lastReadMessageCount: Int? = null,
    val muteUntil: Long? = null,       // null = not muted, Long.MAX_VALUE = forever, else expiry timestamp
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isLocked: Boolean = false
)
