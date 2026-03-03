package com.novachat.ui.scheduled

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.domain.model.ScheduledMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesScreen(
    onBack: () -> Unit,
    viewModel: ScheduledMessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var cancelTarget by remember { mutableStateOf<ScheduledMessage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "No scheduled messages",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Schedule a message from any chat\nusing the schedule button",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ScheduledMessageCard(
                                message = message,
                                onCancel = { cancelTarget = message }
                            )
                        }
                    }
                }
            }
        }

        cancelTarget?.let { msg ->
            AlertDialog(
                onDismissRequest = { cancelTarget = null },
                title = { Text("Cancel Scheduled Message") },
                text = {
                    Text("This message will not be sent. Are you sure?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.cancelMessage(msg.id)
                        cancelTarget = null
                    }) {
                        Text("Cancel Message", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cancelTarget = null }) {
                        Text("Keep")
                    }
                }
            )
        }
    }
}

@Composable
private fun ScheduledMessageCard(
    message: ScheduledMessage,
    onCancel: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val displayName = message.contactName ?: message.address

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(message.scheduledTime)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "To: $displayName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (message.sendViaWhatsApp) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "WhatsApp",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF25D366),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
