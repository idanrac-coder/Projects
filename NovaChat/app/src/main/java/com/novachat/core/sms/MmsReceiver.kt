package com.novachat.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsNotificationHandler: SmsNotificationHandler

    override fun onReceive(context: Context, intent: Intent) {
        // MMS handling is more complex; for now we trigger a refresh
        smsNotificationHandler.handleIncomingMms()
    }
}
