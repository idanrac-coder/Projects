package com.novachat.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.novachat.core.theme.GradientAvatar
import com.novachat.domain.model.Conversation
import com.novachat.domain.model.MessageCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val sanitizeRegex = Regex("[\\uFFFC\\uFFFD\\uFFFE\\uFFFF\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F]")
private fun sanitize(text: String) = text.replace(sanitizeRegex, "").trim()

private val categoryPalette = listOf(
    Color(0xFF6C5CE7) to Color(0xFFEDE9FF),  // violet
    Color(0xFF00B894) to Color(0xFFE0F8F1),  // green
    Color(0xFFE17055) to Color(0xFFFDE8E2),  // coral
    Color(0xFF0984E3) to Color(0xFFDEEFFC),  // blue
    Color(0xFFFD79A8) to Color(0xFFFFE6F0),  // pink
    Color(0xFFE84393) to Color(0xFFFFDCEE),  // magenta
    Color(0xFF00CEC9) to Color(0xFFDBFAF9),  // teal
    Color(0xFFF39C12) to Color(0xFFFFF3DA),  // amber
    Color(0xFF6D5BBA) to Color(0xFFEDE8FF),  // purple
    Color(0xFF1ABC9C) to Color(0xFFD5F5EE),  // emerald
)

private fun categoryColors(name: String): Pair<Color, Color> {
    val index = abs(name.hashCode()) % categoryPalette.size
    return categoryPalette[index]
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false
) {
    val hasUnread = conversation.unreadCount > 0

    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "selection_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else Color.Transparent,
        label = "selection_bg"
    )

    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(selectedScale)
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with gradient or selection checkmark
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable {
                    if (!isSelectionMode) {
                        val uri = Uri.fromParts("tel", conversation.address, null)
                        val intent = Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, uri).apply {
                            putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, false)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    } else {
                        onClick()
                    }
                }
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                GradientAvatar(
                    address = conversation.address,
                    displayName = conversation.displayName,
                    size = 52
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = sanitize(conversation.displayName),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (conversation.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                    if (conversation.isMuted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatTimestamp(conversation.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sanitize(conversation.snippet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categoryLabel = conversation.customCategory
                        ?: if (conversation.category == MessageCategory.CONTACTS) "Contacts" else null
                    if (categoryLabel != null) {
                        CategoryBadge(name = categoryLabel)
                    }
                    if (conversation.isArchived) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Archived",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (hasUnread) {
                        UnreadCountBadge(unreadCount = conversation.unreadCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(name: String) {
    val (textColor, bgColor) = remember(name) { categoryColors(name) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun UnreadCountBadge(unreadCount: Int) {
    val minBadgeWidth: Dp = 20.dp
    Box(
        modifier = Modifier
            .heightIn(min = 20.dp)
            .widthIn(min = minBadgeWidth)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.DATE) == msgTime.get(Calendar.DATE) &&
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) -> {
            val diff = System.currentTimeMillis() - timestamp
            if (diff < 60_000) "Now"
            else SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        now.get(Calendar.DATE) - msgTime.get(Calendar.DATE) == 1 &&
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) -> "Yesterday"
        now.get(Calendar.WEEK_OF_YEAR) == msgTime.get(Calendar.WEEK_OF_YEAR) &&
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        }
        now.get(Calendar.YEAR) != msgTime.get(Calendar.YEAR) ->
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
