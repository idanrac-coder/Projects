package com.novachat.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object ConversationsRoute

@Serializable
data class ChatRoute(val threadId: Long, val address: String, val contactName: String?)

@Serializable
data class ComposeMessageRoute(val recipient: String? = null)

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
object BackgroundsRoute

@Serializable
object ThemeEditorRoute

@Serializable
object SwipeActionsRoute

@Serializable
object NotificationSettingsRoute

@Serializable
object LicenseRoute

@Serializable
data class MediaGalleryRoute(val threadId: Long, val contactName: String?)

@Serializable
data class PinnedMessagesRoute(val threadId: Long, val contactName: String?)

@Serializable
data class QrContactRoute(val phoneNumber: String, val contactName: String?)

@Serializable
object BackupRestoreRoute

@Serializable
object ScheduledMessagesRoute

@Serializable
object ArchivedConversationsRoute

@Serializable
object NotificationProfilesRoute

@Serializable
object SmartSecureRoute

@Serializable
object TrustedSendersRoute

@Serializable
object InboxSpamScanRoute

@Serializable
object MessagingSettingsRoute
