package com.novachat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.novachat.ui.blocking.BlockingScreen
import com.novachat.ui.blocking.SpamFolderScreen
import com.novachat.ui.chat.ChatScreen
import com.novachat.ui.compose.ComposeMessageScreen
import com.novachat.ui.conversations.ConversationsScreen
import com.novachat.ui.license.LicenseScreen
import com.novachat.ui.notifications.NotificationSettingsScreen
import com.novachat.ui.search.SearchScreen
import com.novachat.ui.settings.SettingsScreen
import com.novachat.ui.swipe.SwipeActionsScreen
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
        modifier = modifier
    ) {
        composable<ConversationsRoute> {
            ConversationsScreen(
                onConversationClick = { threadId, address, name ->
                    navController.navigate(ChatRoute(threadId, address, name))
                },
                onComposeClick = { navController.navigate(ComposeMessageRoute) },
                onSearchClick = { navController.navigate(SearchRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) }
            )
        }

        composable<ChatRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChatRoute>()
            ChatScreen(
                threadId = route.threadId,
                address = route.address,
                contactName = route.contactName,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ComposeMessageRoute> {
            ComposeMessageScreen(
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
                onBlockingClick = { navController.navigate(BlockingRoute) },
                onThemesClick = { navController.navigate(ThemesRoute) },
                onSwipeActionsClick = { navController.navigate(SwipeActionsRoute) },
                onNotificationsClick = { navController.navigate(NotificationSettingsRoute) },
                onPremiumClick = { navController.navigate(LicenseRoute) },
                onSpamFolderClick = { navController.navigate(SpamFolderRoute) }
            )
        }

        composable<BlockingRoute> {
            BlockingScreen(onBack = { navController.popBackStack() })
        }

        composable<SpamFolderRoute> {
            SpamFolderScreen(onBack = { navController.popBackStack() })
        }

        composable<ThemesRoute> {
            ThemesScreen(
                onBack = { navController.popBackStack() },
                onEditTheme = { navController.navigate(ThemeEditorRoute) }
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
    }
}
