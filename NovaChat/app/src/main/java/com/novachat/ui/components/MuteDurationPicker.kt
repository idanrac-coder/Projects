package com.novachat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.R

@Composable
fun MuteDurationPicker(
    onConfirm: (muteUntil: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }

    val muteOptions = listOf(
        stringResource(R.string.mute_1_hour) to (60L * 60 * 1000),
        stringResource(R.string.mute_8_hours) to (8L * 60 * 60 * 1000),
        stringResource(R.string.mute_1_week) to (7L * 24 * 60 * 60 * 1000),
        stringResource(R.string.mute_forever) to Long.MAX_VALUE
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.mute_notifications), fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.mute_for_how_long),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                muteOptions.forEachIndexed { index, (label, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedIndex == index,
                            onClick = null
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val durationMs = muteOptions[selectedIndex].second
                    val muteUntil = if (durationMs == Long.MAX_VALUE) Long.MAX_VALUE
                    else System.currentTimeMillis() + durationMs
                    onConfirm(muteUntil)
                }
            ) {
                Text(stringResource(R.string.mute), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
