package com.novachat.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.novachat.ui.financial.AlertsScreen
import com.novachat.ui.financial.CardManagerScreen
import com.novachat.ui.financial.FinancialDashboardScreen
import com.novachat.ui.financial.FinancialOnboardingScreen
import com.novachat.ui.financial.FinancialSettingsScreen
import com.novachat.ui.financial.SubscriptionListScreen
import com.novachat.ui.license.LicenseScreen
import com.novachat.ui.media.MediaGalleryScreen
import com.novachat.ui.pinned.PinnedMessagesScreen
import com.novachat.ui.notifications.NotificationProfilesScreen
import com.novachat.ui.notifications.NotificationSettingsScreen
import com.novachat.ui.qr.QrContactScreen
import com.novachat.ui.scheduled.ScheduledMessagesScreen
import com.novachat.ui.search.SearchScreen
import com.novachat.ui.settings.MessagingSettingsScreen
import com.novachat.ui.settings.SettingsScreen
import com.novachat.ui.swipe.SwipeActionsScreen
import com.novachat.ui.themes.BackgroundsScreen
import com.novachat.ui.themes.ThemeEditorScreen
import com.novachat.ui.themes.ThemesScreen

@Composable
fun NovaChatNavHost(
    navController: NavHostController,
    isPremium: Boolean = false,
    financialOnboardingComplete: Boolean = false,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        ConversationsRoute::class.qualifiedName,
        FinancialDashboardRoute::class.qualifiedName,
        SettingsRoute::class.qualifiedName
    )
    val isTopLevelRoute = currentRoute in topLevelRoutes

    // Capture bar height once measured; used to clamp the scroll offset
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }

    // Proportional offset: 0f = fully visible, bottomBarHeightPx = fully hidden below screen
    var bottomBarOffsetPx by remember { mutableFloatStateOf(0f) }

    // Reset to fully visible whenever we arrive on a top-level screen
    LaunchedEffect(currentRoute) {
        if (isTopLevelRoute) bottomBarOffsetPx = 0f
    }

    // Proportional real-time tracking — bar slides with the finger, no threshold snap
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = -available.y // positive when scrolling down
                bottomBarOffsetPx = (bottomBarOffsetPx + delta).coerceIn(0f, bottomBarHeightPx)
                return Offset.Zero // observe only — never consume
            }
        }
    }

    val showBottomBar = isTopLevelRoute // scroll hiding handled by graphicsLayer, not visibility

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = EnterTransition.None,  // route transitions cover this visually
                exit = ExitTransition.None
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .onGloballyPositioned { bottomBarHeightPx = it.size.height.toFloat() }
                        .graphicsLayer { translationY = bottomBarOffsetPx }
                ) {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route::class.qualifiedName
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (dest == TopLevelDestination.FINANCIAL) {
                                    val target = when {
                                        !isPremium -> LicenseRoute
                                        !financialOnboardingComplete -> FinancialOnboardingRoute
                                        else -> FinancialDashboardRoute
                                    }
                                    navController.navigate(target) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ConversationsRoute,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable<ConversationsRoute> {
                ConversationsScreen(
                    onConversationClick = { threadId, address, name ->
                        navController.navigate(ChatRoute(threadId, address, name))
                    },
                    onComposeClick = { navController.navigate(ComposeMessageRoute()) },
                    onSearchClick = { navController.navigate(SearchRoute) },
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
                    onSmartSecureClick = { navController.navigate(SmartSecureRoute) },
                    onMessagingSettingsClick = { navController.navigate(MessagingSettingsRoute) },
                    onFinancialIntelligenceClick = {
                        val target = when {
                            !isPremium -> LicenseRoute
                            !financialOnboardingComplete -> FinancialOnboardingRoute
                            else -> FinancialSettingsRoute
                        }
                        navController.navigate(target)
                    }
                )
            }

            composable<MessagingSettingsRoute> {
                MessagingSettingsScreen(onBack = { navController.popBackStack() })
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
                NotificationProfilesScreen(onBack = { navController.popBackStack() })
            }

            // Financial Intelligence screens
            composable<FinancialDashboardRoute> {
                FinancialDashboardScreen(
                    onNavigateToSettings = { navController.navigate(FinancialSettingsRoute) },
                    onNavigateToSubscriptions = { navController.navigate(SubscriptionListRoute) },
                    onNavigateToAlerts = { navController.navigate(AlertsRoute) }
                )
            }

            composable<SubscriptionListRoute> {
                SubscriptionListScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<AlertsRoute> {
                AlertsScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<FinancialSettingsRoute> {
                FinancialSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCardManager = { navController.navigate(CardManagerRoute) },
                    onNavigateToSetupGuide = { navController.navigate(FinancialOnboardingRoute) }
                )
            }

            composable<FinancialOnboardingRoute> {
                FinancialOnboardingScreen(
                    onComplete = {
                        navController.popBackStack()
                        navController.navigate(FinancialDashboardRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<CardManagerRoute> {
                CardManagerScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
