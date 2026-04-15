package com.novachat.ui.blocking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.R
import com.novachat.core.theme.AuroraColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSpamScanScreen(
    onBack: () -> Unit,
    onSpamFolderClick: () -> Unit = {},
    viewModel: InboxSpamScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.phase) {
        ScanPhase.READY, ScanPhase.SCANNING -> ScanningPhase(uiState = uiState, onBack = onBack)
        ScanPhase.REVIEW -> ReviewPhase(
            uiState = uiState,
            onBack = onBack,
            onToggleMessage = viewModel::toggleMessage,
            onToggleSelectAll = viewModel::toggleSelectAll,
            onMoveToSpam = viewModel::moveToSpamFolder,
            onDeletePermanently = viewModel::deletePermanently,
            onMarkNotSpam = viewModel::markNotSpam
        )
        ScanPhase.PROCESSING -> ProcessingPhase(uiState = uiState)
        ScanPhase.DONE -> DonePhase(
            uiState = uiState,
            onBack = onBack,
            onSpamFolderClick = onSpamFolderClick
        )
    }
}

@Composable
private fun ScanningPhase(uiState: InboxSpamScanUiState, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(96.dp),
                        strokeWidth = 4.dp,
                        color = AuroraColors.TealSpark
                    )
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = AuroraColors.TealSpark
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.scanning_inbox),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.totalMessages > 0) {
                    Text(
                        text = stringResource(R.string.scan_progress_fmt, uiState.scannedCount, uiState.totalMessages),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    LinearProgressIndicator(
                        progress = { uiState.scannedCount.toFloat() / uiState.totalMessages.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AuroraColors.TealSpark,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (uiState.spamResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.spam_found_so_far_fmt, uiState.spamResults.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.may_take_a_moment),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewPhase(
    uiState: InboxSpamScanUiState,
    onBack: () -> Unit,
    onToggleMessage: (Long) -> Unit,
    onToggleSelectAll: () -> Unit,
    onMoveToSpam: () -> Unit,
    onDeletePermanently: () -> Unit,
    onMarkNotSpam: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        val count = uiState.selectedCount
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.permanently_delete),
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    if (count == 1) stringResource(R.string.delete_confirm_singular)
                    else stringResource(R.string.delete_confirm_plural_fmt, count)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePermanently()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_results), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (uiState.spamResults.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onMarkNotSpam,
                            enabled = uiState.selectedCount > 0,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AuroraColors.Success
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.selectedCount > 0)
                                    AuroraColors.Success.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.not_spam_count_fmt, uiState.selectedCount))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onMoveToSpam,
                                enabled = uiState.selectedCount > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.spam_count_fmt, uiState.selectedCount))
                            }

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                enabled = uiState.selectedCount > 0,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.selectedCount > 0)
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.delete_count_fmt, uiState.selectedCount))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.spamResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = AuroraColors.Success
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.your_inbox_is_clean),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_spam_detected),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FilledTonalButton(onClick = onBack) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (uiState.spamResults.size == 1)
                                    stringResource(R.string.spam_found_singular)
                                else
                                    stringResource(R.string.spam_found_plural_fmt, uiState.spamResults.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSelectAll() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.allSelected,
                            onCheckedChange = { onToggleSelectAll() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.selected_count_fmt, uiState.selectedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(uiState.spamResults, key = { it.smsId }) { msg ->
                    SpamScanMessageCard(
                        message = msg,
                        onToggle = { onToggleMessage(msg.smsId) }
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SpamScanMessageCard(
    message: ScannedSpamMessage,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = message.selected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 4.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.address,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (message.matchedRuleType != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = formatScanMatchedRuleType(message.matchedRuleType),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatScanTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ProcessingPhase(uiState: InboxSpamScanUiState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = AuroraColors.TealSpark
                )
                Spacer(modifier = Modifier.height(24.dp))
                val actionText = when (uiState.lastAction) {
                    ScanAction.MOVE_TO_SPAM -> stringResource(R.string.moving_to_spam)
                    ScanAction.DELETE_PERMANENTLY -> stringResource(R.string.deleting_messages)
                    ScanAction.MARK_NOT_SPAM -> stringResource(R.string.marking_not_spam)
                    null -> stringResource(R.string.processing)
                }
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (uiState.processedCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.processed_count_fmt, uiState.processedCount, uiState.selectedCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DonePhase(
    uiState: InboxSpamScanUiState,
    onBack: () -> Unit,
    onSpamFolderClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AuroraColors.Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AuroraColors.Success
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val count = uiState.processedCount
                val message = when (uiState.lastAction) {
                    ScanAction.MOVE_TO_SPAM ->
                        if (count == 1) stringResource(R.string.result_moved_spam_singular)
                        else stringResource(R.string.result_moved_spam_plural_fmt, count)
                    ScanAction.DELETE_PERMANENTLY ->
                        if (count == 1) stringResource(R.string.result_deleted_singular)
                        else stringResource(R.string.result_deleted_plural_fmt, count)
                    ScanAction.MARK_NOT_SPAM ->
                        if (count == 1) stringResource(R.string.result_not_spam_singular)
                        else stringResource(R.string.result_not_spam_plural_fmt, count)
                    null -> stringResource(R.string.done)
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState.lastAction == ScanAction.MOVE_TO_SPAM) {
                    FilledTonalButton(onClick = onSpamFolderClick) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.view_spam_folder))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}

@Composable
private fun formatScanMatchedRuleType(ruleType: String): String = when {
    ruleType.startsWith("SCAM:") -> ruleType.removePrefix("SCAM:").replace('_', ' ').lowercase()
        .replaceFirstChar { it.uppercase() }
    ruleType.startsWith("HEBREW:") -> stringResource(R.string.spam_reason_hebrew)
    ruleType.startsWith("DET_RAW:") -> stringResource(R.string.spam_reason_suspicious_pattern)
    ruleType.startsWith("DET:") -> when (ruleType.removePrefix("DET:")) {
        "SHORTENED_URL" -> stringResource(R.string.spam_reason_shortened_link)
        "SUSPICIOUS_TLD" -> stringResource(R.string.spam_reason_suspicious_website)
        "IP_URL" -> stringResource(R.string.spam_reason_suspicious_link)
        "URGENT_KEYWORDS" -> stringResource(R.string.spam_reason_urgency)
        else -> stringResource(R.string.spam_reason_suspicious_content)
    }
    ruleType.startsWith("HEUR:") -> stringResource(R.string.spam_reason_detected)
    ruleType.startsWith("SCORE_") -> stringResource(R.string.spam_reason_detected)
    else -> stringResource(R.string.spam_reason_detected)
}

private fun formatScanTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
    ) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
