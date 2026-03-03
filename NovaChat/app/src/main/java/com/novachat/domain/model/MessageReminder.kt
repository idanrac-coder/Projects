package com.novachat.domain.model

data class MessageReminder(
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
