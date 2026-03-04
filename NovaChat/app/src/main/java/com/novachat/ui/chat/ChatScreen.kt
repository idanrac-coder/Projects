package com.novachat.ui.chat

import com.novachat.ui.blocking.BlockRuleLimitDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.core.theme.AuroraColors
import com.novachat.core.theme.GradientAvatar
import com.novachat.core.theme.LocalChatWallpaper
import com.novachat.domain.model.Message
import com.novachat.domain.model.MessageType
import com.novachat.domain.model.ReactionEmojis
import com.novachat.domain.model.WallpaperType
import com.novachat.ui.components.DisappearingMessagesDialog
import com.novachat.ui.components.EmojiPicker
import com.novachat.ui.components.MessageBubble
import com.novachat.ui.components.VoiceRecorderOverlay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.Toast

private sealed interface ChatListItem {
    @Immutable
    data class DateHeader(val label: String) : ChatListItem

    @Immutable
    data class MessageRow(val message: Message) : ChatListItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: Long,
    address: String,
    contactName: String?,
    onBack: () -> Unit,
    onNavigateToCompose: (String) -> Unit = {},
    onNavigateToMediaGallery: () -> Unit = {},
    onNavigateToPinnedMessages: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messageText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showOverflowMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var previousItemCount by rememberSaveable(threadId) { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var voiceDurationMs by remember { mutableStateOf(0L) }
    var phoneNumberDialogTarget by remember { mutableStateOf<String?>(null) }

    val chatItems = remember(uiState.messages) {
        buildList {
            var lastDate = ""
            for (message in uiState.messages) {
                val date = formatDate(message.timestamp)
                if (date != lastDate) {
                    lastDate = date
                    add(ChatListItem.DateHeader(date))
                }
                add(ChatListItem.MessageRow(message))
            }
        }
    }

    LaunchedEffect(threadId) {
        NotificationManagerCompat.from(context).cancel(address.hashCode())
        NotificationManagerCompat.from(context).cancel(threadId.toInt())
        viewModel.loadMessages(threadId)
    }

    LaunchedEffect(uiState.spamReportedEvent) {
        if (uiState.spamReportedEvent) {
            Toast.makeText(context, "Reported as spam", Toast.LENGTH_SHORT).show()
            viewModel.consumeSpamReportedEvent()
        }
    }

    LaunchedEffect(uiState.blockSuccessNavigateBack) {
        if (uiState.blockSuccessNavigateBack) {
            viewModel.clearBlockSuccessEvent()
            onBack()
        }
    }

    LaunchedEffect(chatItems.size) {
        val totalCount = chatItems.size
        if (uiState.messages.isEmpty()) {
            previousItemCount = 0
            return@LaunchedEffect
        }
        if (previousItemCount == 0) {
            listState.scrollToItem(totalCount - 1)
        } else {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val wasNearBottom = lastVisibleIndex >= previousItemCount - 3
            if (totalCount > previousItemCount && wasNearBottom) {
                listState.animateScrollToItem(totalCount - 1)
            }
        }
        previousItemCount = totalCount
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    val context = LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val uri = Uri.fromParts("tel", address, null)
                            val intent = Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, uri).apply {
                                putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, false)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore if no activity can handle
                            }
                        }
                    ) {
                        GradientAvatar(
                            address = address,
                            displayName = contactName ?: address,
                            size = 36
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = contactName ?: address,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$address"))
                        try { context.startActivity(callIntent) } catch (_: Exception) {}
                    }) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Search") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleSearch()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pinned messages") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToPinnedMessages()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Media gallery") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToMediaGallery()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Disappearing messages") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.showDisappearingDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Block contact",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.showBlockDialog()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val searchFocusRequester = remember { FocusRequester() }

        LaunchedEffect(uiState.isSearchActive) {
            if (uiState.isSearchActive) searchFocusRequester.requestFocus()
        }

        LaunchedEffect(uiState.activeMatchMessageId) {
            val targetId = uiState.activeMatchMessageId ?: return@LaunchedEffect
            val index = chatItems.indexOfFirst {
                it is ChatListItem.MessageRow && it.message.id == targetId
            }
            if (index >= 0) listState.animateScrollToItem(index)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Search bar
            AnimatedVisibility(
                visible = uiState.isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester),
                            placeholder = {
                                Text(
                                    "Search in conversation...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = if (uiState.searchQuery.isNotBlank()) {
                                {
                                    Text(
                                        text = if (uiState.matchCount > 0)
                                            "${uiState.activeMatchIndex + 1}/${uiState.matchCount}"
                                        else "0/0",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else null
                        )
                        IconButton(
                            onClick = { viewModel.navigateMatchUp() },
                            enabled = uiState.matchCount > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                        }
                        IconButton(
                            onClick = { viewModel.navigateMatchDown() },
                            enabled = uiState.matchCount > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                        }
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            }

            // Pinned messages banner
            if (uiState.pinnedMessages.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onNavigateToPinnedMessages() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.pinnedMessages.size} pinned message${if (uiState.pinnedMessages.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Unknown sender action banner
            val showSenderBanner = contactName == null
                && !uiState.isSenderAllowlisted
                && !uiState.senderBannerDismissed
            AnimatedVisibility(
                visible = showSenderBanner,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AuroraColors.TealSpark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unknown sender",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.trustSender(address) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "Trust",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AuroraColors.TealSpark
                            )
                        }
                        Text(
                            "\u00B7",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        TextButton(
                            onClick = { viewModel.showBlockDialog() },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "Block",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissSenderBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Messages with wallpaper background
            val chatWallpaper = LocalChatWallpaper.current
            val wallpaperModifier = when (chatWallpaper.type) {
                WallpaperType.GRADIENT -> Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(chatWallpaper.primaryColor.copy(alpha = 0.15f), chatWallpaper.secondaryColor.copy(alpha = 0.15f))
                    )
                )
                WallpaperType.PATTERN_BOTANICAL,
                WallpaperType.PATTERN_GEOMETRIC,
                WallpaperType.PATTERN_WAVES,
                WallpaperType.PATTERN_DOTS,
                WallpaperType.PATTERN_LEAVES -> Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(chatWallpaper.primaryColor.copy(alpha = 0.08f), chatWallpaper.secondaryColor.copy(alpha = 0.04f))
                    )
                )
                else -> Modifier
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.pullToRefresh() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(wallpaperModifier)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            count = chatItems.size,
                            key = { index ->
                                when (val item = chatItems[index]) {
                                    is ChatListItem.DateHeader -> "date_${item.label}"
                                    is ChatListItem.MessageRow -> item.message.id
                                }
                            },
                            contentType = { index ->
                                when (chatItems[index]) {
                                    is ChatListItem.DateHeader -> 0
                                    is ChatListItem.MessageRow -> 1
                                }
                            }
                        ) { index ->
                            when (val item = chatItems[index]) {
                                is ChatListItem.DateHeader -> {
                                    DateSeparator(label = item.label)
                                }
                                is ChatListItem.MessageRow -> {
                                    SwipeableMessageItem(
                                        message = item.message,
                                        uiState = uiState,
                                        hapticFeedback = hapticFeedback,
                                        clipboardManager = clipboardManager,
                                        context = context,
                                        viewModel = viewModel,
                                        onPhoneNumberClick = { phoneNumberDialogTarget = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reaction picker
            AnimatedVisibility(
                visible = uiState.showReactionPicker != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(150))
            ) {
                if (uiState.showReactionPicker != null) {
                    val messageId = uiState.showReactionPicker!!
                    val message = uiState.messages.find { it.id == messageId }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(ReactionEmojis.allReactions) { emoji ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.addReaction(messageId, emoji)
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = emoji, fontSize = 22.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(onClick = {
                                    viewModel.hideReactionPicker()
                                    if (message != null) viewModel.setReplyTo(message)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reply")
                                }
                                TextButton(onClick = {
                                    viewModel.hideReactionPicker()
                                    if (message != null) {
                                        clipboardManager.setText(AnnotatedString(message.body))
                                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy")
                                }
                                TextButton(onClick = {
                                    viewModel.hideReactionPicker()
                                    if (message != null) {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message.body)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Forward message"))
                                    }
                                }) {
                                    Icon(Icons.Default.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Forward")
                                }
                                TextButton(onClick = {
                                    viewModel.hideReactionPicker()
                                    if (message != null) viewModel.togglePinMessage(message)
                                }) {
                                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (message?.isPinned == true) "Unpin" else "Pin")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(onClick = {
                                    viewModel.hideReactionPicker()
                                    viewModel.showReminderDialog(messageId)
                                }) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remind")
                                }
                                TextButton(onClick = {
                                    viewModel.hideReactionPicker()
                                    if (message != null) {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message.body)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.hideReactionPicker()
                                        viewModel.deleteMessage(messageId)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                                TextButton(onClick = { viewModel.hideReactionPicker() }) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            // Smart replies
            AnimatedVisibility(
                visible = uiState.smartReplies.isNotEmpty() && uiState.replyToMessage == null && uiState.showReactionPicker == null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.smartReplies) { reply ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            onClick = { viewModel.sendMessage(address, reply) }
                        ) {
                            Text(
                                text = reply,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Reply-to preview
            AnimatedVisibility(
                visible = uiState.replyToMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                uiState.replyToMessage?.let { replyMsg ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = replyMsg.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.setReplyTo(null) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // WhatsApp mode indicator
            AnimatedVisibility(
                visible = uiState.sendViaWhatsApp,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = Color(0xFF25D366).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "W",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Sending via WhatsApp",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF25D366),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.toggleSendViaWhatsApp() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Switch to SMS",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF25D366)
                            )
                        }
                    }
                }
            }

            // Voice recording overlay
            VoiceRecorderOverlay(
                isRecording = isVoiceRecording,
                durationMs = voiceDurationMs,
                onCancel = {
                    isVoiceRecording = false
                    voiceDurationMs = 0
                },
                onSend = {
                    isVoiceRecording = false
                    voiceDurationMs = 0
                    Toast.makeText(context, "Voice messages will be available with MMS support", Toast.LENGTH_SHORT).show()
                }
            )

            // Undo send bar
            AnimatedVisibility(
                visible = uiState.pendingSendMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sending...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = { viewModel.cancelPendingSend() },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "UNDO",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Input area
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    if (uiState.sendViaWhatsApp) "WhatsApp message" else "Message",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = if (uiState.isWhatsAppAvailable) {
                                {
                                    IconButton(
                                        onClick = { viewModel.toggleSendViaWhatsApp() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        if (uiState.sendViaWhatsApp) {
                                            Text(
                                                text = "W",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF25D366)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Sms,
                                                contentDescription = "Switch to WhatsApp",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else null,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Create,
                                            contentDescription = "AI Compose",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showEmojiPicker = !showEmojiPicker },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.EmojiEmotions,
                                            contentDescription = "Emoji",
                                            tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.showScheduleDialog() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddCircleOutline,
                                            contentDescription = "More",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    // Send / Mic button with animated transition
                    val sendButtonColor = if (messageText.isNotBlank()) {
                        if (uiState.sendViaWhatsApp) Color(0xFF25D366)
                        else MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.surfaceVariant

                    Surface(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(address, messageText)
                                messageText = ""
                                showEmojiPicker = false
                            } else {
                                isVoiceRecording = !isVoiceRecording
                                if (isVoiceRecording) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    voiceDurationMs = 0
                                }
                            }
                        },
                        shape = CircleShape,
                        color = sendButtonColor,
                        modifier = Modifier.size(48.dp),
                        enabled = !uiState.isSending
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = when {
                                    uiState.isSending -> "loading"
                                    messageText.isNotBlank() -> "send"
                                    else -> "mic"
                                },
                                transitionSpec = {
                                    scaleIn(animationSpec = tween(200)) togetherWith
                                        scaleOut(animationSpec = tween(200)) using
                                        SizeTransform(clip = false)
                                },
                                label = "send_button_anim"
                            ) { state ->
                                when (state) {
                                    "loading" -> CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    "send" -> Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = if (uiState.sendViaWhatsApp) "Send via WhatsApp" else "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    else -> Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Voice",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Emoji picker
            EmojiPicker(
                visible = showEmojiPicker,
                onEmojiSelected = { emoji ->
                    messageText += emoji
                }
            )
        }

        // Dialogs
        if (uiState.showScheduleDialog && messageText.isNotBlank()) {
            ScheduleMessageDialog(
                sendViaWhatsApp = uiState.sendViaWhatsApp,
                onDismiss = { viewModel.hideScheduleDialog() },
                onSchedule = { time ->
                    viewModel.scheduleMessage(address, messageText, time, contactName)
                    messageText = ""
                }
            )
        }

        if (uiState.showReminderDialog != null) {
            val message = uiState.messages.find { it.id == uiState.showReminderDialog }
            if (message != null) {
                ReminderDialog(
                    onDismiss = { viewModel.hideReminderDialog() },
                    onSetReminder = { time ->
                        viewModel.setReminder(message, time, contactName)
                    }
                )
            }
        }

        if (uiState.showDisappearingDialog) {
            DisappearingMessagesDialog(
                currentDurationMs = uiState.disappearingDurationMs,
                onDismiss = { viewModel.hideDisappearingDialog() },
                onSelect = { durationMs -> viewModel.setDisappearingDuration(durationMs) }
            )
        }

        if (uiState.showBlockDialog) {
            val displayName = contactName ?: address
            val hasContactName = contactName != null && contactName != address

            ChatBlockDialog(
                displayName = displayName,
                address = address,
                hasContactName = hasContactName,
                onBlockNumber = { viewModel.blockNumber(address) },
                onBlockName = {
                    if (contactName != null) {
                        viewModel.blockName(contactName)
                    }
                },
                onBlockWords = { words -> viewModel.blockWords(words) },
                onBlockLanguage = { lang -> viewModel.blockLanguage(lang) },
                onDismiss = { viewModel.dismissBlockDialog() }
            )
        }

        if (uiState.showBlockLimitDialog) {
            BlockRuleLimitDialog(
                onUpgrade = {
                    viewModel.dismissBlockLimitDialog()
                    onNavigateToPremium()
                },
                onDismiss = { viewModel.dismissBlockLimitDialog() }
            )
        }

        if (phoneNumberDialogTarget != null) {
            PhoneNumberActionDialog(
                phoneNumber = phoneNumberDialogTarget!!,
                onSendMessage = { number ->
                    phoneNumberDialogTarget = null
                    onNavigateToCompose(number)
                },
                onAddContact = { number ->
                    phoneNumberDialogTarget = null
                    val uri = Uri.fromParts("tel", number, null)
                    val intent = Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, uri).apply {
                        putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, false)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                },
                onCall = { number ->
                    phoneNumberDialogTarget = null
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    try { context.startActivity(intent) } catch (_: Exception) {}
                },
                onCopy = { number ->
                    phoneNumberDialogTarget = null
                    clipboardManager.setText(AnnotatedString(number))
                    Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { phoneNumberDialogTarget = null }
            )
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SwipeableMessageItem(
    message: Message,
    uiState: ChatUiState,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context,
    viewModel: ChatViewModel,
    onPhoneNumberClick: (String) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var didHapticAtThreshold by remember { mutableStateOf(false) }

    val isMatch = uiState.searchQuery.isNotBlank() &&
        message.id in uiState.matchingMessageIds

    val swipeAlpha by animateFloatAsState(
        targetValue = if (offsetX > 30f) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "swipe_icon_alpha"
    )

    val swipeIconScale by animateFloatAsState(
        targetValue = if (offsetX > 80f) 1.3f else if (offsetX > 30f) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipe_icon_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = offsetX
            }
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 80f) {
                            viewModel.setReplyTo(message)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        offsetX = 0f
                        didHapticAtThreshold = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val prev = offsetX
                        offsetX = (offsetX + dragAmount).coerceIn(0f, 120f)
                        if (offsetX >= 80f && prev < 80f && !didHapticAtThreshold) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            didHapticAtThreshold = true
                        }
                        if (offsetX < 80f) didHapticAtThreshold = false
                    }
                )
            }
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = "Reply",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size((20 * swipeIconScale).dp),
            tint = if (offsetX > 80f) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.primary.copy(alpha = swipeAlpha)
        )

        val scamWarning = remember(message.id, uiState.scamWarnings, uiState.dismissedScamWarnings) {
            uiState.scamWarnings[message.id]
                ?.takeIf { message.id !in uiState.dismissedScamWarnings }
        }

        MessageBubble(
            message = message,
            modifier = Modifier.padding(vertical = 2.dp),
            highlightText = if (isMatch) uiState.searchQuery else null,
            isActiveMatch = message.id == uiState.activeMatchMessageId,
            scamAnalysis = scamWarning,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.showReactionPicker(message.id)
            },
            onReactionClick = { emoji ->
                viewModel.removeReaction(message.id, emoji)
            },
            onCodeCopy = { code ->
                clipboardManager.setText(AnnotatedString(code))
                Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onPhoneNumberClick = onPhoneNumberClick,
            onShortCodeCopy = { code ->
                clipboardManager.setText(AnnotatedString(code))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                Toast.makeText(context, "Code \"$code\" copied", Toast.LENGTH_SHORT).show()
            },
            onDismissScamWarning = { viewModel.dismissScamWarning(message.id) },
            onConfirmSpam = { viewModel.confirmSpam(message.id) }
        )
    }
}

@Composable
private fun PhoneNumberActionDialog(
    phoneNumber: String,
    onSendMessage: (String) -> Unit,
    onAddContact: (String) -> Unit,
    onCall: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(phoneNumber, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { onSendMessage(phoneNumber) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Send message", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Surface(
                    onClick = { onAddContact(phoneNumber) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to contacts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Surface(
                    onClick = { onCall(phoneNumber) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Call", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Surface(
                    onClick = { onCopy(phoneNumber) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy number", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ChatBlockDialog(
    displayName: String,
    address: String,
    hasContactName: Boolean,
    onBlockNumber: () -> Unit,
    onBlockName: (String) -> Unit,
    onBlockWords: (String) -> Unit,
    onBlockLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = { Text("Block $displayName") },
        text = {
            if (selectedOption == null) {
                Column {
                    Text(
                        text = "Choose how you want to block this contact. Blocked messages will be moved to the spam folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        onClick = onBlockNumber,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhoneDisabled,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Block by number",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = { onBlockName(displayName) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Block by sender",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = { selectedOption = "WORDS" },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Block by words",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = { selectedOption = "LANGUAGE" },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Block by language",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                Column {
                    Text(
                        text = when (selectedOption) {
                            "WORDS" -> "Enter words to block (comma separated)"
                            "LANGUAGE" -> "Enter language code (e.g., en, es, fr)"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (selectedOption != null) {
                TextButton(
                    onClick = {
                        when (selectedOption) {
                            "WORDS" -> onBlockWords(inputText)
                            "LANGUAGE" -> onBlockLanguage(inputText)
                        }
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Text("Block")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedOption != null) {
                        selectedOption = null
                        inputText = ""
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (selectedOption != null) "Back" else "Cancel")
            }
        }
    )
}

@Composable
private fun ScheduleMessageDialog(
    sendViaWhatsApp: Boolean = false,
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit
) {
    val options = listOf(
        "In 1 hour" to 60 * 60 * 1000L,
        "In 3 hours" to 3 * 60 * 60 * 1000L,
        "Tomorrow morning (9 AM)" to calculateTomorrow9Am(),
        "Tomorrow evening (6 PM)" to calculateTomorrow6Pm(),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Message") },
        text = {
            Column {
                if (sendViaWhatsApp) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "W",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF25D366)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Will be sent via WhatsApp",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF25D366),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    "Choose when to send this message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { (label, offset) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { onSchedule(System.currentTimeMillis() + offset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (sendViaWhatsApp) Color(0xFF25D366)
                                       else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReminderDialog(
    onDismiss: () -> Unit,
    onSetReminder: (Long) -> Unit
) {
    val options = listOf(
        "In 30 minutes" to 30 * 60 * 1000L,
        "In 1 hour" to 60 * 60 * 1000L,
        "In 3 hours" to 3 * 60 * 60 * 1000L,
        "Tomorrow morning (9 AM)" to calculateTomorrow9Am(),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remind Me") },
        text = {
            Column {
                Text(
                    "When should we remind you?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { (label, offset) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { onSetReminder(System.currentTimeMillis() + offset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun calculateTomorrow9Am(): Long {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DATE, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    return cal.timeInMillis - System.currentTimeMillis()
}

private fun calculateTomorrow6Pm(): Long {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DATE, 1)
        set(Calendar.HOUR_OF_DAY, 18)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    return cal.timeInMillis - System.currentTimeMillis()
}

private fun formatDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return when {
        cal.get(Calendar.DATE) == today.get(Calendar.DATE) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> "Today"
        today.get(Calendar.DATE) - cal.get(Calendar.DATE) == 1 &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> "Yesterday"
        else -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
