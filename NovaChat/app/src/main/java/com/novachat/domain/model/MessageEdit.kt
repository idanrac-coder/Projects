package com.novachat.domain.model

data class MessageEdit(
    val id: Long,
    val messageId: Long,
    val previousBody: String,
    val newBody: String,
    val timestamp: Long
)
