package com.novachat.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.SwipeAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isPremium: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_PREMIUM] ?: false
    }

    val activeThemeId: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_THEME_ID] ?: 1L
    }

    val swipeLeftAction: Flow<SwipeAction> = dataStore.data.map { prefs ->
        val value = prefs[PreferencesKeys.SWIPE_LEFT_ACTION] ?: SwipeAction.ARCHIVE.name
        SwipeAction.valueOf(value)
    }

    val swipeRightAction: Flow<SwipeAction> = dataStore.data.map { prefs ->
        val value = prefs[PreferencesKeys.SWIPE_RIGHT_ACTION] ?: SwipeAction.DELETE.name
        SwipeAction.valueOf(value)
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FIRST_LAUNCH] ?: true
    }

    suspend fun setPremium(premium: Boolean) {
        dataStore.edit { it[PreferencesKeys.IS_PREMIUM] = premium }
    }

    suspend fun setActiveThemeId(themeId: Long) {
        dataStore.edit { it[PreferencesKeys.ACTIVE_THEME_ID] = themeId }
    }

    suspend fun setSwipeLeftAction(action: SwipeAction) {
        dataStore.edit { it[PreferencesKeys.SWIPE_LEFT_ACTION] = action.name }
    }

    suspend fun setSwipeRightAction(action: SwipeAction) {
        dataStore.edit { it[PreferencesKeys.SWIPE_RIGHT_ACTION] = action.name }
    }

    suspend fun setFirstLaunchComplete() {
        dataStore.edit { it[PreferencesKeys.FIRST_LAUNCH] = false }
    }

    val autoBackupEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false
    }

    val backupFrequency: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BACKUP_FREQUENCY] ?: "daily"
    }

    val lastBackupTime: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LAST_BACKUP_TIME] ?: 0L
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupFrequency(frequency: String) {
        dataStore.edit { it[PreferencesKeys.BACKUP_FREQUENCY] = frequency }
    }

    suspend fun setLastBackupTime(time: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_BACKUP_TIME] = time }
    }

    val activeBubbleShape: Flow<BubbleShape?> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_BUBBLE_SHAPE]?.let {
            try { BubbleShape.valueOf(it) } catch (_: Exception) { null }
        }
    }

    suspend fun setBubbleShape(shape: BubbleShape?) {
        dataStore.edit {
            if (shape != null) {
                it[PreferencesKeys.ACTIVE_BUBBLE_SHAPE] = shape.name
            } else {
                it.remove(PreferencesKeys.ACTIVE_BUBBLE_SHAPE)
            }
        }
    }

    val scamDetectionEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SCAM_DETECTION_ENABLED] ?: true
    }

    suspend fun setScamDetectionEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SCAM_DETECTION_ENABLED] = enabled }
    }

    val conversationBackgroundId: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CONVERSATION_BACKGROUND_ID] ?: "default"
    }

    suspend fun setConversationBackgroundId(backgroundId: String) {
        dataStore.edit { it[PreferencesKeys.CONVERSATION_BACKGROUND_ID] = backgroundId }
    }

    val quickReplyEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.QUICK_REPLY_ENABLED] ?: false
    }

    suspend fun setQuickReplyEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.QUICK_REPLY_ENABLED] = enabled }
    }

    val filterInternationalSenders: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FILTER_INTERNATIONAL_SENDERS] ?: false
    }

    suspend fun setFilterInternationalSenders(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.FILTER_INTERNATIONAL_SENDERS] = enabled }
    }

    val undoSendEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.UNDO_SEND_ENABLED] ?: true
    }

    suspend fun setUndoSendEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.UNDO_SEND_ENABLED] = enabled }
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    val installTime: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.INSTALL_TIME] ?: 0L
    }

    suspend fun setInstallTimeIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.INSTALL_TIME] == null) {
                prefs[PreferencesKeys.INSTALL_TIME] = System.currentTimeMillis()
            }
        }
    }

    val reviewShown: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.REVIEW_SHOWN] ?: false
    }

    suspend fun setReviewShown() {
        dataStore.edit { it[PreferencesKeys.REVIEW_SHOWN] = true }
    }

    val smartLinksCalendarEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SMART_LINKS_CALENDAR_ENABLED] ?: true
    }

    suspend fun setSmartLinksCalendarEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SMART_LINKS_CALENDAR_ENABLED] = enabled }
    }

    val smartLinksMapsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SMART_LINKS_MAPS_ENABLED] ?: true
    }

    suspend fun setSmartLinksMapsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SMART_LINKS_MAPS_ENABLED] = enabled }
    }

    val financialIntelligenceEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FINANCIAL_INTELLIGENCE_ENABLED] ?: false
    }

    suspend fun setFinancialIntelligenceEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.FINANCIAL_INTELLIGENCE_ENABLED] = enabled }
    }

    val financialLastParsedSmsId: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FINANCIAL_LAST_PARSED_SMS_ID] ?: 0L
    }

    suspend fun setFinancialLastParsedSmsId(id: Long) {
        dataStore.edit { it[PreferencesKeys.FINANCIAL_LAST_PARSED_SMS_ID] = id }
    }

    val financialPrimaryCurrency: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FINANCIAL_PRIMARY_CURRENCY] ?: "ILS"
    }

    suspend fun setFinancialPrimaryCurrency(currency: String) {
        dataStore.edit { it[PreferencesKeys.FINANCIAL_PRIMARY_CURRENCY] = currency }
    }

    val financialOnboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FINANCIAL_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setFinancialOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[PreferencesKeys.FINANCIAL_ONBOARDING_COMPLETE] = complete }
    }

    val trialStartTime: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TRIAL_START_TIME] ?: 0L
    }

    suspend fun startTrial() {
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.TRIAL_START_TIME] == null || prefs[PreferencesKeys.TRIAL_START_TIME] == 0L) {
                prefs[PreferencesKeys.TRIAL_START_TIME] = System.currentTimeMillis()
            }
        }
    }

    // "" means follow system default; "en" = English; "he" = Hebrew
    val appLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.APP_LANGUAGE] ?: ""
    }

    suspend fun setAppLanguage(languageTag: String) {
        dataStore.edit { it[PreferencesKeys.APP_LANGUAGE] = languageTag }
    }
}
