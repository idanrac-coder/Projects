package com.novachat.ui.pinned

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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageType
import com.novachat.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PinnedMessagesUiState(
    val pinnedMessages: List<Message> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PinnedMessagesViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinnedMessagesUiState())
    val uiState: StateFlow<PinnedMessagesUiState> = _uiState.asStateFlow()

    private var currentThreadId: Long = -1

    fun loadPinnedMessages(threadId: Long) {
        currentThreadId = threadId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val messages = conversationRepository.getMessagesForThread(threadId)
                val pinned = messages.filter { it.isPinned }
                _uiState.value = PinnedMessagesUiState(
                    pinnedMessages = pinned,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = PinnedMessagesUiState(isLoading = false)
            }
        }
    }

    fun unpinMessage(message: Message) {
        viewModelScope.launch {
            conversationRepository.unpinMessage(message.id)
            loadPinnedMessages(currentThreadId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedMessagesScreen(
    threadId: Long,
    contactName: String?,
    onBack: () -> Unit,
    viewModel: PinnedMessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(threadId) {
        viewModel.loadPinnedMessages(threadId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pinned Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (uiState.pinnedMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No pinned messages",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Long-press a message in the chat to pin it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(uiState.pinnedMessages, key = { it.id }) { message ->
                    PinnedMessageCard(
                        message = message,
                        contactName = contactName,
                        onUnpin = { viewModel.unpinMessage(message) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedMessageCard(
    message: Message,
    contactName: String?,
    onUnpin: () -> Unit
) {
    val isSent = message.type == MessageType.SENT
    val dateFormat = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSent) "You" else (contactName ?: message.address),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = onUnpin) {
                    Text("Unpin", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
