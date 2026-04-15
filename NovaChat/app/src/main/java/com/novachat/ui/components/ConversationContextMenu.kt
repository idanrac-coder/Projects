package com.novachat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.R
import com.novachat.domain.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationContextMenu(
    conversation: Conversation,
    onMute: (muteUntil: Long) -> Unit,
    onUnmute: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onArchive: () -> Unit,
    onMarkAsRead: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showMutePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Text(
                text = conversation.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                maxLines = 1
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Mute / Unmute
            if (conversation.isCurrentlyMuted) {
                ContextMenuItem(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.context_menu_unmute),
                    onClick = {
                        onUnmute()
                        onDismiss()
                    }
                )
            } else {
                ContextMenuItem(
                    icon = Icons.Default.NotificationsOff,
                    label = stringResource(R.string.context_menu_mute),
                    onClick = { showMutePicker = true }
                )
            }

            // Pin / Unpin
            ContextMenuItem(
                icon = Icons.Default.PushPin,
                label = if (conversation.isPinned) stringResource(R.string.context_menu_unpin) else stringResource(R.string.context_menu_pin),
                onClick = {
                    if (conversation.isPinned) onUnpin() else onPin()
                    onDismiss()
                }
            )

            // Favorite / Unfavorite
            ContextMenuItem(
                icon = if (conversation.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                label = if (conversation.isFavorite) stringResource(R.string.context_menu_remove_favorite) else stringResource(R.string.context_menu_add_favorite),
                onClick = {
                    if (conversation.isFavorite) onUnfavorite() else onFavorite()
                    onDismiss()
                }
            )

            // Archive
            ContextMenuItem(
                icon = Icons.Default.Archive,
                label = stringResource(R.string.archive),
                onClick = {
                    onArchive()
                    onDismiss()
                }
            )

            // Mark as read
            if (conversation.unreadCount > 0) {
                ContextMenuItem(
                    icon = Icons.Default.DoneAll,
                    label = stringResource(R.string.context_menu_mark_as_read),
                    onClick = {
                        onMarkAsRead()
                        onDismiss()
                    }
                )
            }

            // Select
            ContextMenuItem(
                icon = Icons.Outlined.CheckBox,
                label = stringResource(R.string.context_menu_select),
                onClick = {
                    onSelect()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Delete
            ContextMenuItem(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )
        }
    }

    if (showMutePicker) {
        MuteDurationPicker(
            onConfirm = { muteUntil ->
                onMute(muteUntil)
                showMutePicker = false
                onDismiss()
            },
            onDismiss = { showMutePicker = false }
        )
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
        }
    }
}
