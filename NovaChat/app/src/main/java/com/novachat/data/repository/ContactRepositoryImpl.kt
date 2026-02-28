package com.novachat.data.repository

import com.novachat.core.sms.ContactResolver
import com.novachat.domain.model.Contact
import com.novachat.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactResolver: ContactResolver
) : ContactRepository {

    override suspend fun getAllContacts(): List<Contact> {
        return contactResolver.getAllContacts()
    }

    override fun getContactName(phoneNumber: String): String? {
        return contactResolver.getContactName(phoneNumber)
    }
}
