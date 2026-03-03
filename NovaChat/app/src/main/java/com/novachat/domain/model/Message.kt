package com.novachat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Message(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val type: MessageType,
    val isRead: Boolean,
    val isMms: Boolean = false,
    val attachmentUri: String? = null,
    val reactions: List<Reaction> = emptyList(),
    val isPinned: Boolean = false,
    val isVoiceMessage: Boolean = false,
    val voiceDurationMs: Long = 0,
    val replyToMessageId: Long? = null,
    val replyToBody: String? = null,
    val scheduledTime: Long? = null,
    val formattedBody: String? = null
)

enum class MessageType {
    RECEIVED,
    SENT,
    DRAFT,
    FAILED,
    OUTBOX,
    QUEUED
}
