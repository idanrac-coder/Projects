package com.novachat.core.sms

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.novachat.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsNotificationHandler: SmsNotificationHandler

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("NC_DIAG", ">>> SmsReceiver.onReceive action=${intent.action}")

        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.e("NC_DIAG", ">>> SmsReceiver: IGNORED action=${intent.action}")
            return
        }

        val isDefaultApp = context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_SMS) == true
        val defaultPkg = Telephony.Sms.getDefaultSmsPackage(context)
        val noDefaultApp = defaultPkg == null
        val shouldWriteSms = isDefaultApp || noDefaultApp
        Log.e("NC_DIAG", ">>> SmsReceiver: isDefault=$isDefaultApp noDefault=$noDefaultApp shouldWrite=$shouldWriteSms action=${intent.action}")

        if (isDefaultApp && intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", ">>> SmsReceiver: SKIP (default app got SMS_RECEIVED, waiting for SMS_DELIVER)")
            return
        }
        if (!isDefaultApp && !noDefaultApp && intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", ">>> SmsReceiver: SKIP (not default app got SMS_DELIVER, another app is default)")
            return
        }
        if (noDefaultApp && intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", ">>> SmsReceiver: SKIP (no default app, but already handled via SMS_DELIVER)")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            if (BuildConfig.DEBUG) Log.d("NC_DEBUG", ">>> SmsReceiver: NO MESSAGES in intent")
            return
        }

        val senderAddress = messages[0].displayOriginatingAddress ?: return
        val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
        val timestamp = messages[0].timestampMillis
        Log.e("NC_DIAG", ">>> SmsReceiver: PROCESSING sender=$senderAddress bodyLen=${fullBody.length} shouldWrite=$shouldWriteSms")

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                smsNotificationHandler.handleIncomingSms(
                    address = senderAddress,
                    body = fullBody,
                    timestamp = timestamp,
                    isDefaultApp = shouldWriteSms
                )
                Log.e("NC_DIAG", ">>> SmsReceiver: handleIncomingSms completed normally")
            } catch (e: Exception) {
                Log.e("NC_DIAG", ">>> SmsReceiver: EXCEPTION in handleIncomingSms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
