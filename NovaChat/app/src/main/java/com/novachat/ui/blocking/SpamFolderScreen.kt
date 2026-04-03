package com.novachat.ui.blocking

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.novachat.core.database.entity.SpamMessageEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatMatchedRuleType(ruleType: String): String = when {
        ruleType.startsWith("SPAM_AGENT:") -> ruleType.removePrefix("SPAM_AGENT:").replace('_', ' ')
        ruleType == "SCAM_REPORT" -> "Reported"
        ruleType == "NUMBER" -> "Blocked by number"
        ruleType == "SENDER_NAME" -> "Blocked by sender"
        ruleType == "KEYWORD" -> "Blocked by keyword"
        ruleType == "LANGUAGE" -> "Blocked by language"
        ruleType == "INTERNATIONAL_FILTER" -> "International filter"
        ruleType.startsWith("SPAM_FILTER:") ->
            formatSpamFilterReason(ruleType.removePrefix("SPAM_FILTER:"))
        ruleType.startsWith("INBOX_SCAN:") ->
            formatSpamFilterReason(ruleType.removePrefix("INBOX_SCAN:"))
        else -> ruleType
    }

    private fun formatSpamFilterReason(inner: String): String {
        val cleaned = inner.replace(Regex("\\|SCORE_\\d+$"), "")
        return when {
        cleaned.startsWith("DET:") -> when (val det = cleaned.removePrefix("DET:")) {
            "SHORTENED_URL" -> "Shortened link"
            "SUSPICIOUS_TLD" -> "Suspicious website"
            "IP_URL" -> "Suspicious link"
            "URGENT_KEYWORDS" -> "Urgency tactics"
            "OTP_VERIFY_EN", "OTP_VERIFY_HE" -> "Possible OTP scam"
            else -> "Suspicious content"
        }
        cleaned.startsWith("HEUR:") -> {
            val heur = cleaned.removePrefix("HEUR:")
            when {
                heur.contains("contains_url") -> "Suspicious link"
                heur.contains("otp_verify") -> "Possible OTP scam"
                heur.contains("high_special_chars") && heur.contains("sender_unknown") -> "Suspicious content"
                heur.contains("sender_unknown") -> "Unknown sender"
                else -> "Spam detected"
            }
        }
        cleaned.startsWith("SCORE_") -> "Spam detected"
        else -> "Blocked as spam"
    }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamFolderScreen(
    onBack: () -> Unit,
    viewModel: SpamFolderViewModel = hiltViewModel()
) {
    val spamMessages by viewModel.spamMessages.collectAsStateWithLifecycle()
    val restoredEvent by viewModel.restoredEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var detailMessage by remember { mutableStateOf<SpamMessageEntity?>(null) }

    LaunchedEffect(restoredEvent) {
        restoredEvent?.let { address ->
            snackbarHostState.showSnackbar("Message from $address restored to inbox")
            viewModel.clearRestoredEvent()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Blocked / Spam") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (spamMessages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Clear all")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (spamMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No spam messages",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                items(spamMessages, key = { it.id }) { spam ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { detailMessage = spam },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spam.address,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (spam.matchedRuleType.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = formatMatchedRuleType(spam.matchedRuleType),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = spam.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val dateText = remember(spam.timestamp) {
                                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                        .format(Date(spam.timestamp))
                                }
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(onClick = { viewModel.restoreToInbox(spam.id) }) {
                                Icon(
                                    Icons.Default.MoveToInbox,
                                    contentDescription = "Restore to inbox",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteSpamMessage(spam.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        detailMessage?.let { spam ->
            SpamDetailSheet(
                address = spam.address,
                body = spam.body,
                score = parseSpamScoreFromMatchedRuleType(spam.matchedRuleType),
                matchedRuleType = spam.matchedRuleType,
                timestamp = spam.timestamp,
                onDismiss = { detailMessage = null }
            )
        }
    }
}
