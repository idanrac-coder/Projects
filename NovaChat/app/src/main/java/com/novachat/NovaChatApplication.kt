package com.novachat

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NovaChatApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var conversationRepository: ConversationRepository

    private var smsObserver: ContentObserver? = null

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
                Log.d("NC_DEBUG", "~~~ ContentObserver.onChange uri=$uri timeSinceLast=${now - lastNotifyTime}ms")
                if (now - lastNotifyTime > 200) {
                    lastNotifyTime = now
                    Log.d("NC_DEBUG", "~~~ ContentObserver: FIRING invalidateAllCaches + notifyNewMessage(-1)")
                    conversationRepository.invalidateAllCaches()
                    conversationRepository.notifyNewMessage(-1L)
                } else {
                    Log.d("NC_DEBUG", "~~~ ContentObserver: THROTTLED (too soon)")
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
}
