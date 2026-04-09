package com.novachat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val route: Any
) {
    MESSAGES(
        selectedIcon = Icons.Default.Chat,
        unselectedIcon = Icons.Outlined.Chat,
        label = "Messages",
        route = ConversationsRoute
    ),
    FINANCIAL(
        selectedIcon = Icons.Default.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        label = "Financial",
        route = FinancialDashboardRoute
    ),
    SETTINGS(
        selectedIcon = Icons.Default.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        label = "Settings",
        route = SettingsRoute
    )
}
