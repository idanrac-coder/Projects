package com.novachat

import android.app.Application
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.novachat.BuildConfig
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.novachat.core.sms.SmsNotificationHandler
import com.novachat.core.sms.SmsProvider
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NovaChatApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var smsNotificationHandler: dagger.Lazy<SmsNotificationHandler>

    @Inject
    lateinit var smsProvider: dagger.Lazy<SmsProvider>

    private var smsObserver: ContentObserver? = null
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e("NovaChatApp", "Uncaught coroutine error", throwable)
        }
    )

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerSmsContentObserver()
    }

    private fun registerSmsContentObserver() {
        val handler = Handler(Looper.getMainLooper())
        var lastNotifyTime = 0L

        smsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                val now = System.currentTimeMillis()
                if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "~~~ ContentObserver.onChange uri=$uri timeSinceLast=${now - lastNotifyTime}ms")

                appScope.launch {
                    try {
                        processProviderInsertedMessages(uri)
                    } catch (e: Exception) {
                        Log.e("NovaChatApp", "processProviderInsertedMessages failed", e)
                    }
                }

                if (now - lastNotifyTime > 200) {
                    lastNotifyTime = now
                    if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "~~~ ContentObserver: FIRING refreshAfterChange")
                    conversationRepository.refreshAfterChange()
                } else {
                    if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "~~~ ContentObserver: THROTTLED (too soon)")
                }
            }
        }

        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver!!
        )
        contentResolver.registerContentObserver(
            Uri.parse("content://mms-sms/"),
            true,
            smsObserver!!
        )
    }

    private suspend fun processProviderInsertedMessages(uri: Uri?) {
        val candidateIds = when {
            uri == null -> smsProvider.get().getRecentInboxMessageIds(5000L)
            uri == Telephony.Sms.CONTENT_URI || uri.toString() == "content://mms-sms/" ->
                smsProvider.get().getRecentInboxMessageIds(5000L)
            else -> {
                try {
                    val id = ContentUris.parseId(uri)
                    listOf(id)
                } catch (_: Exception) {
                    smsProvider.get().getRecentInboxMessageIds(5000L)
                }
            }
        }

        for (messageId in candidateIds) {
            if (smsProvider.get().wasInsertedByUs(messageId)) continue
            val info = smsProvider.get().getInboxMessageById(messageId) ?: continue
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", "~~~ processProviderInsertedMessages: processing messageId=$messageId address=${info.address}")
            smsNotificationHandler.get().handleProviderInsertedIncomingMessage(
                address = info.address,
                body = info.body,
                timestamp = info.timestamp,
                messageId = messageId
            )
        }
    }
}
