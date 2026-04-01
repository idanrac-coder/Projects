package com.novachat.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.novachat.ui.archived.ArchivedConversationsScreen
import com.novachat.ui.backup.BackupRestoreScreen
import com.novachat.ui.blocking.BlockingScreen
import com.novachat.ui.blocking.InboxSpamScanScreen
import com.novachat.ui.blocking.SmartSecureScreen
import com.novachat.ui.blocking.SpamFolderScreen
import com.novachat.ui.blocking.TrustedSendersScreen
import com.novachat.ui.chat.ChatScreen
import com.novachat.ui.compose.ComposeMessageScreen
import com.novachat.ui.conversations.ConversationsScreen
import com.novachat.ui.license.LicenseScreen
import com.novachat.ui.media.MediaGalleryScreen
import com.novachat.ui.pinned.PinnedMessagesScreen
import com.novachat.ui.notifications.NotificationProfilesScreen
import com.novachat.ui.notifications.NotificationSettingsScreen
import com.novachat.ui.qr.QrContactScreen
import com.novachat.ui.scheduled.ScheduledMessagesScreen
import com.novachat.ui.search.SearchScreen
import com.novachat.ui.settings.SettingsScreen
import com.novachat.ui.swipe.SwipeActionsScreen
import com.novachat.ui.themes.BackgroundsScreen
import com.novachat.ui.themes.ThemeEditorScreen
import com.novachat.ui.themes.ThemesScreen

@Composable
fun NovaChatNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ConversationsRoute,
        modifier = modifier.fillMaxSize()
    ) {
        composable<ConversationsRoute> {
            ConversationsScreen(
                onConversationClick = { threadId, address, name ->
                    navController.navigate(ChatRoute(threadId, address, name))
                },
                onComposeClick = { navController.navigate(ComposeMessageRoute()) },
                onSearchClick = { navController.navigate(SearchRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
                onNavigateToPremium = { navController.navigate(LicenseRoute) }
            )
        }

        composable<ChatRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChatRoute>()
            ChatScreen(
                threadId = route.threadId,
                address = route.address,
                contactName = route.contactName,
                onBack = { navController.popBackStack() },
                onNavigateToCompose = { number ->
                    navController.navigate(ComposeMessageRoute(number))
                },
                onNavigateToMediaGallery = {
                    navController.navigate(MediaGalleryRoute(route.threadId, route.contactName))
                },
                onNavigateToPinnedMessages = {
                    navController.navigate(PinnedMessagesRoute(route.threadId, route.contactName))
                },
                onNavigateToPremium = { navController.navigate(LicenseRoute) }
            )
        }

        composable<ComposeMessageRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ComposeMessageRoute>()
            ComposeMessageScreen(
                initialRecipient = route.recipient,
                onBack = { navController.popBackStack() },
                onConversationStarted = { threadId, address, name ->
                    navController.popBackStack()
                    navController.navigate(ChatRoute(threadId, address, name))
                }
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onMessageClick = { threadId, address, name ->
                    navController.navigate(ChatRoute(threadId, address, name))
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onThemesClick = { navController.navigate(ThemesRoute) },
                onSwipeActionsClick = { navController.navigate(SwipeActionsRoute) },
                onNotificationsClick = { navController.navigate(NotificationSettingsRoute) },
                onPremiumClick = { navController.navigate(LicenseRoute) },
                onBackupClick = { navController.navigate(BackupRestoreRoute) },
                onQrClick = { navController.navigate(QrContactRoute("", null)) },
                onScheduledClick = { navController.navigate(ScheduledMessagesRoute) },
                onArchivedClick = { navController.navigate(ArchivedConversationsRoute) },
                onNotificationProfilesClick = { navController.navigate(NotificationProfilesRoute) },
                onSmartSecureClick = { navController.navigate(SmartSecureRoute) }
            )
        }

        composable<SmartSecureRoute> {
            SmartSecureScreen(
                onBack = { navController.popBackStack() },
                onBlockingClick = { navController.navigate(BlockingRoute) },
                onSpamFolderClick = { navController.navigate(SpamFolderRoute) },
                onTrustedSendersClick = { navController.navigate(TrustedSendersRoute) },
                onScanInboxClick = { navController.navigate(InboxSpamScanRoute) }
            )
        }

        composable<InboxSpamScanRoute> {
            InboxSpamScanScreen(
                onBack = { navController.popBackStack() },
                onSpamFolderClick = {
                    navController.popBackStack()
                    navController.navigate(SpamFolderRoute)
                }
            )
        }

        composable<TrustedSendersRoute> {
            TrustedSendersScreen(onBack = { navController.popBackStack() })
        }

        composable<BlockingRoute> {
            BlockingScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(LicenseRoute) }
            )
        }

        composable<SpamFolderRoute> {
            SpamFolderScreen(onBack = { navController.popBackStack() })
        }

        composable<ThemesRoute> {
            ThemesScreen(
                onBack = { navController.popBackStack() },
                onBackgroundsClick = { navController.navigate(BackgroundsRoute) },
                onEditTheme = { navController.navigate(ThemeEditorRoute) },
                onNavigateToPremium = { navController.navigate(LicenseRoute) }
            )
        }

        composable<BackgroundsRoute> {
            BackgroundsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(LicenseRoute) }
            )
        }

        composable<ThemeEditorRoute> {
            ThemeEditorScreen(onBack = { navController.popBackStack() })
        }

        composable<SwipeActionsRoute> {
            SwipeActionsScreen(onBack = { navController.popBackStack() })
        }

        composable<NotificationSettingsRoute> {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<LicenseRoute> {
            LicenseScreen(onBack = { navController.popBackStack() })
        }

        composable<MediaGalleryRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MediaGalleryRoute>()
            MediaGalleryScreen(
                threadId = route.threadId,
                contactName = route.contactName,
                onBack = { navController.popBackStack() }
            )
        }

        composable<PinnedMessagesRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PinnedMessagesRoute>()
            PinnedMessagesScreen(
                threadId = route.threadId,
                contactName = route.contactName,
                onBack = { navController.popBackStack() }
            )
        }

        composable<QrContactRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<QrContactRoute>()
            QrContactScreen(
                phoneNumber = route.phoneNumber,
                contactName = route.contactName,
                onBack = { navController.popBackStack() }
            )
        }

        composable<BackupRestoreRoute> {
            BackupRestoreScreen(onBack = { navController.popBackStack() })
        }

        composable<ScheduledMessagesRoute> {
            ScheduledMessagesScreen(onBack = { navController.popBackStack() })
        }

        composable<ArchivedConversationsRoute> {
            ArchivedConversationsScreen(
                onBack = { navController.popBackStack() },
                onConversationClick = { threadId, address, name ->
                    navController.navigate(ChatRoute(threadId, address, name))
                }
            )
        }

        composable<NotificationProfilesRoute> {
            NotificationProfilesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
