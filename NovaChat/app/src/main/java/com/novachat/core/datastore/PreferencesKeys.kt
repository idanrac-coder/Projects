package com.novachat.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val IS_PREMIUM = booleanPreferencesKey("is_premium")
    val ACTIVE_THEME_ID = longPreferencesKey("active_theme_id")
    val SWIPE_LEFT_ACTION = stringPreferencesKey("swipe_left_action")
    val SWIPE_RIGHT_ACTION = stringPreferencesKey("swipe_right_action")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val DEFAULT_NOTIFICATION_SOUND = stringPreferencesKey("default_notification_sound")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    val LED_COLOR = stringPreferencesKey("led_color")
    val POPUP_STYLE = stringPreferencesKey("popup_style")
    val GROUPING_MODE = stringPreferencesKey("grouping_mode")
    val QUICK_REPLY_ENABLED = booleanPreferencesKey("quick_reply_enabled")
    val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
    val DND_START_HOUR = stringPreferencesKey("dnd_start_hour")
    val DND_END_HOUR = stringPreferencesKey("dnd_end_hour")
    val IS_DEFAULT_SMS_APP = booleanPreferencesKey("is_default_sms_app")
    val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
}
