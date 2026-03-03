package com.novachat.core.sms

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppForwardSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    companion object {
        private const val PREFS_NAME = "whatsapp_forward_prefs"
        private const val KEY_ENABLED = "forward_to_whatsapp_enabled"
    }
}
