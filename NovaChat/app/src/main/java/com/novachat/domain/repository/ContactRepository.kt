package com.novachat.domain.repository

import com.novachat.domain.model.Contact

interface ContactRepository {
    suspend fun getAllContacts(): List<Contact>
    fun getContactName(phoneNumber: String): String?
}
