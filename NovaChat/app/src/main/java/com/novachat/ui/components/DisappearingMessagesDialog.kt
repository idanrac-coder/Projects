package com.novachat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.R

data class AutoDeleteOption(
    val label: String,
    val durationMs: Long?
)

@Composable
fun DisappearingMessagesDialog(
    currentDurationMs: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    val options = listOf(
        AutoDeleteOption(stringResource(R.string.disappearing_off), null),
        AutoDeleteOption(stringResource(R.string.disappearing_5_seconds), 5_000L),
        AutoDeleteOption(stringResource(R.string.disappearing_1_minute), 60_000L),
        AutoDeleteOption(stringResource(R.string.disappearing_1_hour), 3_600_000L),
        AutoDeleteOption(stringResource(R.string.disappearing_24_hours), 86_400_000L),
        AutoDeleteOption(stringResource(R.string.disappearing_7_days), 604_800_000L),
        AutoDeleteOption(stringResource(R.string.disappearing_30_days), 2_592_000_000L),
        AutoDeleteOption(stringResource(R.string.disappearing_90_days), 7_776_000_000L),
    )

    var selected by remember { mutableStateOf(currentDurationMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (selected == null) Icons.Default.TimerOff else Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                stringResource(R.string.disappearing_messages),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.disappearing_messages_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected == option.durationMs)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface,
                        onClick = { selected = option.durationMs }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected == option.durationMs,
                                onClick = { selected = option.durationMs },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected == option.durationMs) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
