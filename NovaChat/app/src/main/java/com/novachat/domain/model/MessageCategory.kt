package com.novachat.domain.model

enum class MessageCategory {
    ALL,
    CONTACTS;

    val displayName: String
        get() = when (this) {
            ALL -> "All"
            CONTACTS -> "Contacts"
        }
}
