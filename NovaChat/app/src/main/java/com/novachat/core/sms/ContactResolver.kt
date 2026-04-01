package com.novachat.core.sms

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import com.novachat.domain.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactResolver @Inject constructor(
    private val contentResolver: ContentResolver
) {
    private val nameCache = java.util.concurrent.ConcurrentHashMap<String, String?>()
    @Volatile
    private var contactsPreloaded = false
    private val miniMatchMap = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val lock = Any()

    companion object {
        private val NON_DIGIT_REGEX = Regex("\\D")
        private val WHITESPACE_REGEX = Regex("\\s")
    }

    fun preloadContacts() {
        if (contactsPreloaded) return
        synchronized(lock) {
            if (contactsPreloaded) return

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { cursor ->
                val colName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val colNumber = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(colName) ?: continue
                    val number = cursor.getString(colNumber) ?: continue
                    val key = miniMatch(number)
                    if (key.isNotEmpty()) {
                        miniMatchMap.putIfAbsent(key, name)
                    }
                }
            }
            contactsPreloaded = true
        }
    }

    fun getContactName(phoneNumber: String): String? {
        nameCache[phoneNumber]?.let { return it }
        if (nameCache.containsKey(phoneNumber)) return null

        if (contactsPreloaded) {
            val key = miniMatch(phoneNumber)
            val name = if (key.isNotEmpty()) miniMatchMap[key] else null
            nameCache[phoneNumber] = name
            return name
        }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )
        val name = cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            } else null
        }
        nameCache[phoneNumber] = name
        return name
    }

    private fun miniMatch(number: String): String {
        val digits = number.replace(NON_DIGIT_REGEX, "")
        return if (digits.length >= 7) digits.takeLast(7) else digits
    }

    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val seenNumbers = mutableSetOf<String>()
            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    ?.replace(WHITESPACE_REGEX, "") ?: continue
                if (seenNumbers.add(number)) {
                    contacts.add(
                        Contact(
                            id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)),
                            name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: "",
                            phoneNumber = number,
                            photoUri = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)),
                            isStarred = it.getInt(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)) == 1
                        )
                    )
                }
            }
        }
        contacts
    }

    fun clearCache() {
        nameCache.clear()
        miniMatchMap.clear()
        contactsPreloaded = false
    }
}
