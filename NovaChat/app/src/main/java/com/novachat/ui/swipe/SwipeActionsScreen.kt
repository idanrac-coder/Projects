package com.novachat.ui.swipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.domain.model.SwipeAction
import com.novachat.ui.components.swipeActionColor
import com.novachat.ui.components.swipeActionIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionsScreen(
    onBack: () -> Unit,
    viewModel: SwipeActionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swipe Actions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SwipePreview(
                leftAction = uiState.leftAction,
                rightAction = uiState.rightAction
            )

            // Left swipe configuration
            SwipeActionSelector(
                title = "Swipe Right",
                subtitle = "Swipe from left to right",
                icon = Icons.Default.SwipeRight,
                currentAction = uiState.leftAction,
                onActionSelected = { viewModel.setLeftAction(it) }
            )

            // Right swipe configuration
            SwipeActionSelector(
                title = "Swipe Left",
                subtitle = "Swipe from right to left",
                icon = Icons.Default.SwipeLeft,
                currentAction = uiState.rightAction,
                onActionSelected = { viewModel.setRightAction(it) }
            )

            // Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Swipe on any conversation in the main list to quickly perform the configured action.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SwipePreview(
    leftAction: SwipeAction,
    rightAction: SwipeAction
) {
    val leftColor by animateColorAsState(swipeActionColor(leftAction), label = "left")
    val rightColor by animateColorAsState(swipeActionColor(rightAction), label = "right")

    Card(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .background(leftColor),
                contentAlignment = Alignment.Center
            ) {
                if (leftAction != SwipeAction.OFF) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            swipeActionIcon(leftAction),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = formatActionName(leftAction),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(2f)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text("Sample Conversation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Last message preview...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .background(rightColor),
                contentAlignment = Alignment.Center
            ) {
                if (rightAction != SwipeAction.OFF) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            swipeActionIcon(rightAction),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = formatActionName(rightAction),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeActionSelector(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentAction: SwipeAction,
    onActionSelected: (SwipeAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box {
                Row(
                    modifier = Modifier
                        .background(swipeActionColor(currentAction).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        swipeActionIcon(currentAction),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = swipeActionColor(currentAction)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatActionName(currentAction),
                        style = MaterialTheme.typography.labelMedium,
                        color = swipeActionColor(currentAction)
                    )
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Change", modifier = Modifier.size(16.dp))
                    }
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SwipeAction.entries.forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        swipeActionIcon(action),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = swipeActionColor(action)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(formatActionName(action))
                                }
                            },
                            onClick = {
                                onActionSelected(action)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatActionName(action: SwipeAction): String = when (action) {
    SwipeAction.ARCHIVE -> "Archive"
    SwipeAction.DELETE -> "Delete"
    SwipeAction.PIN -> "Pin"
    SwipeAction.MARK_READ_UNREAD -> "Read/Unread"
    SwipeAction.MUTE -> "Mute"
    SwipeAction.BLOCK -> "Block"
    SwipeAction.OFF -> "Off"
}
