package com.novachat.domain.model

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val isStarred: Boolean = false
)
