package com.novachat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novachat.core.theme.LocalChatColors
import com.novachat.core.theme.LocalChatShapes
import com.novachat.domain.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    body: String,
    timestamp: Long,
    type: MessageType,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val chatShapes = LocalChatShapes.current
    val isSent = type == MessageType.SENT
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val bubbleColor = if (isSent) chatColors.sentBubble else chatColors.receivedBubble
    val textColor = if (isSent) chatColors.sentText else chatColors.receivedText
    val bubbleShape = if (isSent) chatShapes.sentBubbleShape else chatShapes.receivedBubbleShape

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = screenWidth * 0.78f)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = body,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatTime(timestamp),
                    color = textColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    textAlign = if (isSent) TextAlign.End else TextAlign.Start,
                    modifier = Modifier
                        .align(if (isSent) Alignment.End else Alignment.Start)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}
