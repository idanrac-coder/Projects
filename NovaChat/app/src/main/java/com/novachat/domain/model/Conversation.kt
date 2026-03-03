package com.novachat.domain.model

data class Conversation(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val snippet: String,
    val timestamp: Long,
    val messageCount: Int,
    val unreadCount: Int,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val avatarUri: String? = null,
    val category: MessageCategory = MessageCategory.ALL,
    val customCategory: String? = null,
    val customThemeId: Long? = null
) {
    val displayName: String
        get() = contactName ?: address
}
