package com.novachat.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
}
