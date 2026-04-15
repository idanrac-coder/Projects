package com.novachat.ui.blocking

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.novachat.R
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
        title = { Text(stringResource(R.string.block_rule_limit_title)) },
        text = {
            Text(stringResource(R.string.block_rule_limit_message, BlockRepository.FREE_RULE_LIMIT))
        },
        confirmButton = {
            TextButton(onClick = onUpgrade) {
                Text(stringResource(R.string.upgrade_to_premium))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
