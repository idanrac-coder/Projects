package com.novachat.domain.model

enum class MessageCategory {
    ALL,
    CONTACTS,
    UNREAD,
    FAVORITES;

    val displayName: String
        get() = when (this) {
            ALL -> "All"
            CONTACTS -> "Contacts"
            UNREAD -> "Unread"
            FAVORITES -> "Favorites"
        }
}
