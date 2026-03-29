package com.novachat.core.sms

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val VALID_PHONE_PATTERN = Regex("^[+\\d][\\d\\s\\-()]*$")
    }

    suspend fun sendSms(address: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (address.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Recipient address is empty"))
            }
            if (!VALID_PHONE_PATTERN.matches(address.trim())) {
                return@withContext Result.failure(IllegalArgumentException("Invalid phone number format"))
            }

            val normalizedAddress = normalizePhoneNumber(address)
            val smsManager = context.getSystemService(SmsManager::class.java)

            val parts = smsManager.divideMessage(body)
            if (parts.size == 1) {
                smsManager.sendTextMessage(normalizedAddress, null, body, null, null)
            } else {
                smsManager.sendMultipartTextMessage(normalizedAddress, null, parts, null, null)
            }

            writeSentMessageToProvider(normalizedAddress, body)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizePhoneNumber(address: String): String {
        val trimmed = address.trim()
        try {
            val tm = context.getSystemService(TelephonyManager::class.java)
            val countryIso = (tm.networkCountryIso.takeUnless { it.isNullOrEmpty() }
                ?: tm.simCountryIso.takeUnless { it.isNullOrEmpty() }
                ?: java.util.Locale.getDefault().country)
            if (countryIso.isNotEmpty()) {
                val e164 = PhoneNumberUtils.formatNumberToE164(trimmed, countryIso.uppercase())
                if (e164 != null) return e164
            }
        } catch (_: Exception) { }
        return trimmed
    }

    private fun writeSentMessageToProvider(address: String, body: String) {
        try {
            val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.THREAD_ID, threadId)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        } catch (_: Exception) {
            // Non-fatal: message was sent but couldn't be persisted locally
        }
    }
}
