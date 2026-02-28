package com.novachat.domain.model

data class Message(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val type: MessageType,
    val isRead: Boolean,
    val isMms: Boolean = false,
    val attachmentUri: String? = null
)

enum class MessageType {
    RECEIVED,
    SENT,
    DRAFT,
    FAILED,
    OUTBOX,
    QUEUED
}
