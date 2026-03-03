package com.novachat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VoiceRecorderOverlay(
    isRecording: Boolean,
    durationMs: Long,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isRecording,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel recording",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                val transition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(pulseScale)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )

                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    onClick = onSend,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send voice",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
