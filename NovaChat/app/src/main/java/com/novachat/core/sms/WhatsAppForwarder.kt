package com.novachat.core.sms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppForwarder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Sends a message to a WhatsApp contact by opening WhatsApp directly
     * in the conversation (no share sheet). Uses the wa.me deep link which
     * navigates straight to the contact's chat with the message pre-filled.
     */
    fun sendMessage(phoneNumber: String, messageBody: String): Boolean {
        return try {
            val formattedNumber = formatToE164(phoneNumber) ?: phoneNumber.replace(Regex("[^\\d+]"), "")
            val numberWithoutPlus = formattedNumber.removePrefix("+")
            val encoded = Uri.encode(messageBody)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$numberWithoutPlus&text=$encoded")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w(TAG, "WhatsApp is not installed")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WhatsApp message", e)
            false
        }
    }

    private fun formatToE164(phoneNumber: String): String? {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            var countryIso = tm.networkCountryIso
            if (countryIso.isNullOrEmpty()) {
                countryIso = tm.simCountryIso
            }
            if (countryIso.isNullOrEmpty()) {
                countryIso = java.util.Locale.getDefault().country
            }
            if (countryIso.isNotEmpty()) {
                val e164 = android.telephony.PhoneNumberUtils.formatNumberToE164(phoneNumber, countryIso.uppercase())
                if (e164 != null) {
                    return e164
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting phone number", e)
        }
        return null
    }

    fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "WhatsAppForwarder"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
    }
}
