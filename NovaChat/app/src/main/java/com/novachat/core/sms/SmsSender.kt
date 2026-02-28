package com.novachat.core.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun sendSms(address: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val sentIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent("SMS_SENT"),
                PendingIntent.FLAG_IMMUTABLE
            )
            val deliveredIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent("SMS_DELIVERED"),
                PendingIntent.FLAG_IMMUTABLE
            )

            val parts = smsManager.divideMessage(body)
            if (parts.size == 1) {
                smsManager.sendTextMessage(address, null, body, sentIntent, deliveredIntent)
            } else {
                val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                    repeat(parts.size) { add(sentIntent) }
                }
                val deliveredIntents = ArrayList<PendingIntent>(parts.size).apply {
                    repeat(parts.size) { add(deliveredIntent) }
                }
                smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, deliveredIntents)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
