package com.novachat.ui.blocking

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.novachat.domain.repository.BlockRepository

@Composable
fun BlockRuleLimitDialog(
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Block Rule Limit Reached") },
        text = {
            Text("You've used all ${BlockRepository.FREE_RULE_LIMIT} free block rules. Upgrade to Premium for unlimited block rules.")
        },
        confirmButton = {
            TextButton(onClick = onUpgrade) {
                Text("Upgrade to Premium")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
