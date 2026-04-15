package com.novachat.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.novachat.R

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelRes: Int,
    val route: Any
) {
    MESSAGES(
        selectedIcon = Icons.Default.Chat,
        unselectedIcon = Icons.Outlined.Chat,
        labelRes = R.string.nav_messages,
        route = ConversationsRoute
    ),
    FINANCIAL(
        selectedIcon = Icons.Default.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        labelRes = R.string.nav_financial,
        route = FinancialDashboardRoute
    ),
    SETTINGS(
        selectedIcon = Icons.Default.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelRes = R.string.nav_settings,
        route = SettingsRoute
    )
}
