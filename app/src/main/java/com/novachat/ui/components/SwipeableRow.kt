package com.novachat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.novachat.domain.model.SwipeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableRow(
    state: SwipeToDismissBoxState,
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    content: @Composable () -> Unit
) {
    if (leftAction == SwipeAction.OFF && rightAction == SwipeAction.OFF) {
        content()
        return
    }

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val direction = state.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val action = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> leftAction
                SwipeToDismissBoxValue.EndToStart -> rightAction
                else -> SwipeAction.OFF
            }
            val backgroundColor by animateColorAsState(
                targetValue = swipeActionColor(action),
                label = "swipe_bg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = swipeActionIcon(action),
                    contentDescription = action.name,
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = leftAction != SwipeAction.OFF,
        enableDismissFromEndToStart = rightAction != SwipeAction.OFF,
        content = { content() }
    )
}

fun swipeActionColor(action: SwipeAction): Color = when (action) {
    SwipeAction.ARCHIVE -> Color(0xFF1976D2)
    SwipeAction.DELETE -> Color(0xFFD32F2F)
    SwipeAction.PIN -> Color(0xFF388E3C)
    SwipeAction.MARK_READ_UNREAD -> Color(0xFF7B1FA2)
    SwipeAction.MUTE -> Color(0xFFF57C00)
    SwipeAction.BLOCK -> Color(0xFF455A64)
    SwipeAction.OFF -> Color.Transparent
}

fun swipeActionIcon(action: SwipeAction): ImageVector = when (action) {
    SwipeAction.ARCHIVE -> Icons.Default.Archive
    SwipeAction.DELETE -> Icons.Default.Delete
    SwipeAction.PIN -> Icons.Default.PushPin
    SwipeAction.MARK_READ_UNREAD -> Icons.Default.MarkEmailRead
    SwipeAction.MUTE -> Icons.Default.VolumeOff
    SwipeAction.BLOCK -> Icons.Default.Block
    SwipeAction.OFF -> Icons.Default.Archive
}
