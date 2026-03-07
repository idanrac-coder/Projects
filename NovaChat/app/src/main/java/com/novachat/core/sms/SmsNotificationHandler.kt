package com.novachat.core.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.novachat.MainActivity
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SpamMessageEntity
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.repository.BlockRepository
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsNotificationHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactResolver: ContactResolver,
    private val blockRepository: BlockRepository,
    private val spamMessageDao: SpamMessageDao,
    private val smsProvider: SmsProvider,
    private val conversationRepository: ConversationRepository,
    private val scamDetector: ScamDetector,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    companion object {
        private const val TAG = "SmsNotificationHandler"
        const val CHANNEL_ID = "novachat_messages"
        const val CHANNEL_NAME = "Messages"
        const val NOTIFICATION_GROUP = "com.novachat.MESSAGES"
    }

    init {
        createNotificationChannel()
    }

    private fun isInternationalSender(address: String, userCountryIso: String): Boolean {
        val normalized = address.replace(Regex("[^+0-9]"), "")
        if (normalized.length < 8) return false
        val userDialCode = countryToDialCode(userCountryIso) ?: return false
        val senderDialCode = when {
            normalized.startsWith("+") -> {
                val digits = normalized.drop(1)
                when {
                    digits.startsWith("1") && digits.length >= 10 -> "1"
                    digits.startsWith("972") -> "972"
                    digits.startsWith("44") -> "44"
                    digits.startsWith("49") -> "49"
                    digits.startsWith("33") -> "33"
                    digits.startsWith("91") -> "91"
                    digits.startsWith("7") && digits.length >= 10 -> "7"
                    digits.startsWith("81") -> "81"
                    digits.startsWith("86") -> "86"
                    digits.startsWith("61") -> "61"
                    digits.startsWith("55") -> "55"
                    digits.startsWith("52") -> "52"
                    else -> digits.take(3)
                }
            }
            normalized.startsWith("0") -> "0"
            else -> return false
        }
        if (senderDialCode == "0") return true
        return senderDialCode != userDialCode
    }

    private fun countryToDialCode(iso: String): String? = when (iso.uppercase()) {
        "US", "CA" -> "1"
        "IL" -> "972"
        "GB", "UK" -> "44"
        "DE" -> "49"
        "FR" -> "33"
        "IN" -> "91"
        "RU" -> "7"
        "JP" -> "81"
        "CN" -> "86"
        "AU" -> "61"
        "BR" -> "55"
        "MX" -> "52"
        else -> null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New message notifications"
            enableLights(true)
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    suspend fun handleIncomingSms(
        address: String,
        body: String,
        timestamp: Long,
        isDefaultApp: Boolean = false
    ) {
        Log.d("NC_DEBUG", "=== handleIncomingSms START === address=$address isDefaultApp=$isDefaultApp body=${body.take(30)}")
        val contactName = contactResolver.getContactName(address)

        val isSystemBlocked = try {
            android.provider.BlockedNumberContract.isBlocked(context, address)
        } catch (e: Exception) {
            false
        }

        val blockedRule = blockRepository.isBlocked(address, contactName, body)

        if (isSystemBlocked || blockedRule != null) {
            handleBlockedMessage(address, body, timestamp, blockedRule)
            return
        }

        val isKnownContact = contactName != null

        val filterInternational = userPreferencesRepository.filterInternationalSenders.first()
        if (filterInternational && !isKnownContact && !scamDetector.isAllowlisted(address)) {
            val userCountry = try {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale?.country ?: Locale.getDefault().country
            } catch (_: Exception) { Locale.getDefault().country }
            if (userCountry != null && userCountry.isNotBlank() && isInternationalSender(address, userCountry)) {
                Log.d(TAG, "International sender filter: $address from non-$userCountry country, hiding")
                spamMessageDao.insertSpamMessage(
                    SpamMessageEntity(
                        smsId = System.currentTimeMillis(),
                        address = address,
                        body = body,
                        timestamp = timestamp,
                        matchedRuleId = -1,
                        matchedRuleType = "INTERNATIONAL_FILTER"
                    )
                )
                try {
                    val threadId = smsProvider.getThreadIdForAddress(address)
                    if (threadId != 0L) smsProvider.deleteThread(threadId)
                } catch (_: Exception) { }
                return
            }
        }
        val scamDetectionEnabled = userPreferencesRepository.scamDetectionEnabled.first()
        val isSenderAllowlisted = scamDetector.isAllowlisted(address)

        if (isKnownContact || !scamDetectionEnabled || isSenderAllowlisted) {
            when {
                isKnownContact -> Log.d(TAG, "Contact trust: $address is a known contact ($contactName), skipping spam analysis")
                isSenderAllowlisted -> Log.d(TAG, "Sender allowlist: $address was marked as not spam by user, skipping spam analysis")
                else -> Log.d(TAG, "Scam detection disabled by user, skipping spam analysis")
            }
        } else {
            // Run the learning spam agent — high-confidence spam is auto-blocked.
            val spamAnalysis = scamDetector.analyzeWithReputation(body, address, isKnownContact = false)
            val autoBlockThreshold = scamDetector.getAutoBlockThreshold(spamAnalysis.category)
            if (spamAnalysis.isScam && spamAnalysis.confidence >= autoBlockThreshold) {
                Log.d(TAG, "Spam agent auto-blocked: confidence=${spamAnalysis.confidence} category=${spamAnalysis.category}")
                spamMessageDao.insertSpamMessage(
                    SpamMessageEntity(
                        smsId = System.currentTimeMillis(),
                        address = address,
                        body = body,
                        timestamp = timestamp,
                        matchedRuleId = -1,
                        matchedRuleType = "SPAM_AGENT:${spamAnalysis.category?.name ?: "UNKNOWN"}"
                    )
                )
                scamDetector.reportSpam(address, body, spamAnalysis.category)

                // Repeat offender auto-block: permanently block after 2+ spam flags
                val reputation = scamDetector.getSenderReputation(address)
                if (reputation != null && reputation.spamCount >= 2) {
                    Log.d(TAG, "Repeat offender auto-block: $address flagged ${reputation.spamCount} times, adding permanent block rule")
                    try {
                        blockRepository.addRule(
                            com.novachat.domain.model.BlockRule(
                                type = com.novachat.domain.model.BlockType.NUMBER,
                                value = address
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to auto-create block rule for $address", e)
                    }
                }
                return
            }
        }

        var threadId = 0L
        if (isDefaultApp) {
            Log.d("NC_DEBUG", "=== Handler: inserting SMS (we are default app)")
            val result = smsProvider.insertIncomingSms(address, body, timestamp)
            threadId = result.threadId
            Log.d("NC_DEBUG", "=== Handler: insert result uri=${result.uri} threadId=$threadId")
            if (result.uri == null) {
                Log.d("NC_DEBUG", "=== Handler: insert returned null, retrying after 300ms")
                kotlinx.coroutines.delay(300)
                val retry = smsProvider.insertIncomingSms(address, body, timestamp)
                Log.d("NC_DEBUG", "=== Handler: retry result uri=${retry.uri}")
            }
        } else {
            Log.d("NC_DEBUG", "=== Handler: NOT default app, skipping insert (stock app will write)")
        }
        if (threadId == 0L) {
            threadId = smsProvider.getThreadIdForAddress(address)
            Log.d("NC_DEBUG", "=== Handler: fallback getThreadIdForAddress=$threadId")
        }

        val lowConfidenceSpam = if (!isKnownContact) {
            val analysis = scamDetector.analyzeWithReputation(body, address, isKnownContact = false)
            if (analysis.isScam) analysis else null
        } else null

        val notifTitle = if (lowConfidenceSpam != null) {
            "\u26A0\uFE0F ${contactName ?: address}"
        } else {
            contactName ?: address
        }
        val notifBody = if (lowConfidenceSpam != null) {
            "[Possible ${lowConfidenceSpam.category?.name?.replace('_', ' ')?.lowercase() ?: "spam"}] $body"
        } else {
            body
        }

        Log.d("NC_DEBUG", "=== Handler: showing notification threadId=$threadId address=$address")
        showNotification(
            title = notifTitle,
            body = notifBody,
            notificationId = if (threadId > 0) threadId.toInt() else address.hashCode(),
            threadId = threadId,
            address = address,
            contactName = contactName,
            lowConfidenceSpam = lowConfidenceSpam,
            spamBody = body
        )

        val effectiveThreadId = if (threadId != 0L) threadId else -1L
        Log.d("NC_DEBUG", "=== Handler: invalidateAllCaches + notifyNewMessage($effectiveThreadId)")
        conversationRepository.invalidateAllCaches()
        conversationRepository.notifyNewMessage(effectiveThreadId)
        Log.d("NC_DEBUG", "=== handleIncomingSms END ===")
    }

    private suspend fun handleBlockedMessage(
        address: String,
        body: String,
        timestamp: Long,
        blockedRule: com.novachat.domain.model.BlockRule?
    ) {
        if (blockedRule != null) {
            spamMessageDao.insertSpamMessage(
                SpamMessageEntity(
                    smsId = System.currentTimeMillis(),
                    address = address,
                    body = body,
                    timestamp = timestamp,
                    matchedRuleId = blockedRule.id,
                    matchedRuleType = blockedRule.type.name
                )
            )
        }
        try {
            val threadId = smsProvider.getThreadIdForAddress(address)
            if (threadId != 0L) {
                smsProvider.deleteThread(threadId)
            }
        } catch (_: Exception) { }
    }

    fun handleIncomingMms() {
        // Placeholder for MMS notification handling
    }

    private fun showNotification(
        title: String,
        body: String,
        notificationId: Int,
        threadId: Long = -1L,
        address: String? = null,
        contactName: String? = null,
        lowConfidenceSpam: ScamAnalysis? = null,
        spamBody: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (address != null) {
                putExtra("threadId", threadId)
                putExtra("address", address)
                putExtra("contactName", contactName)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPending = PendingIntent.getBroadcast(
            context, notificationId * 10 + 1, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DELETE
            putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val deletePending = PendingIntent.getBroadcast(
            context, notificationId * 10 + 2, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .addAction(0, "Mark as read", markReadPending)
            .addAction(0, "Delete", deletePending)

        if (lowConfidenceSpam != null && address != null && spamBody != null && threadId > 0) {
            val reportSpamIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_REPORT_SPAM
                putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
                putExtra(NotificationActionReceiver.EXTRA_BODY, spamBody)
                putExtra(NotificationActionReceiver.EXTRA_CATEGORY, lowConfidenceSpam.category?.name)
            }
            val reportSpamPending = PendingIntent.getBroadcast(
                context, notificationId * 10 + 3, reportSpamIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Report spam", reportSpamPending)
        }

        val notification = builder.build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
