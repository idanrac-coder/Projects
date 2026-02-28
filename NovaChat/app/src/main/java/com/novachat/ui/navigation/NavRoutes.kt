package com.novachat.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object ConversationsRoute

@Serializable
data class ChatRoute(val threadId: Long, val address: String, val contactName: String?)

@Serializable
object ComposeMessageRoute

@Serializable
object SearchRoute

@Serializable
object SettingsRoute

@Serializable
object BlockingRoute

@Serializable
object SpamFolderRoute

@Serializable
object ThemesRoute

@Serializable
object ThemeEditorRoute

@Serializable
object SwipeActionsRoute

@Serializable
object NotificationSettingsRoute

@Serializable
object LicenseRoute
