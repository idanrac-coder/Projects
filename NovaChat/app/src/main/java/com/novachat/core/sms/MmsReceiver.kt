package com.novachat.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsNotificationHandler: SmsNotificationHandler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                smsNotificationHandler.handleIncomingMms()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
