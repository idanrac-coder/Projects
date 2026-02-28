package com.novachat.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Quickreply
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.domain.model.GroupingMode
import com.novachat.domain.model.PopupStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Global toggle
            SwitchRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Enable or disable all notifications",
                checked = uiState.isEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )

            HorizontalDivider()

            // Vibration
            SwitchRow(
                icon = Icons.Default.Vibration,
                title = "Vibration",
                subtitle = "Vibrate on new messages",
                checked = uiState.vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) }
            )

            // Quick Reply
            SwitchRow(
                icon = Icons.Outlined.Quickreply,
                title = "Quick Reply",
                subtitle = "Enable inline reply from notifications",
                checked = uiState.quickReplyEnabled,
                onCheckedChange = { viewModel.setQuickReplyEnabled(it) }
            )

            HorizontalDivider()

            // Popup Style
            Text(
                text = "Popup Style",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PopupStyle.entries.forEach { style ->
                    FilterChip(
                        selected = uiState.popupStyle == style,
                        onClick = { viewModel.setPopupStyle(style) },
                        label = { Text(formatPopupStyle(style)) }
                    )
                }
            }

            // Grouping Mode
            Text(
                text = "Notification Grouping",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.groupingMode == mode,
                        onClick = { viewModel.setGroupingMode(mode) },
                        label = { Text(formatGroupingMode(mode)) }
                    )
                }
            }

            HorizontalDivider()

            // Scheduled DND
            SwitchRow(
                icon = Icons.Default.DoNotDisturb,
                title = "Scheduled Do Not Disturb",
                subtitle = "Silence notifications during quiet hours",
                checked = uiState.dndEnabled,
                onCheckedChange = { viewModel.setDndEnabled(it) }
            )

            if (uiState.dndEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.dndStartHour,
                                onValueChange = { viewModel.setDndStartHour(it) },
                                label = { Text("Start") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.dndEndHour,
                                onValueChange = { viewModel.setDndEndHour(it) },
                                label = { Text("End") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Use 24h format (e.g., 22:00 - 07:00)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Per-conversation info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "To set per-conversation notification preferences, long-press on a conversation in the main list.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatPopupStyle(style: PopupStyle): String = when (style) {
    PopupStyle.HEADS_UP -> "Heads-up"
    PopupStyle.BANNER -> "Banner"
    PopupStyle.SILENT -> "Silent"
}

private fun formatGroupingMode(mode: GroupingMode): String = when (mode) {
    GroupingMode.BY_CONTACT -> "By Contact"
    GroupingMode.BUNDLE_ALL -> "Bundle All"
    GroupingMode.INDIVIDUAL -> "Individual"
}
