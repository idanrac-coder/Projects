package com.novachat.domain.model

data class ScheduledMessage(
    val id: Long = 0,
    val address: String,
    val body: String,
    val scheduledTime: Long,
    val threadId: Long = -1,
    val contactName: String? = null,
    val isSent: Boolean = false,
    val sendViaWhatsApp: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val isFailed: Boolean = false
)
