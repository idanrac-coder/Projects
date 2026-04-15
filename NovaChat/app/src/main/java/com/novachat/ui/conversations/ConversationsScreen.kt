package com.novachat.ui.conversations

import com.novachat.ui.blocking.BlockRuleLimitDialog
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.core.theme.AuroraColors
import com.novachat.core.theme.ShimmerConversationItem
import com.novachat.domain.model.MessageCategory
import com.novachat.domain.model.SwipeAction
import androidx.compose.material3.HorizontalDivider
import com.novachat.domain.model.Conversation
import com.novachat.ui.components.ConversationContextMenu
import com.novachat.ui.components.ConversationItem
import com.novachat.ui.components.SwipeableRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onConversationClick: (threadId: Long, address: String, contactName: String?) -> Unit,
    onComposeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNavigateToPremium: () -> Unit = {},
    onNavigateToArchived: () -> Unit = {},
    viewModel: ConversationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val swipeLeft by viewModel.swipeLeftAction.collectAsStateWithLifecycle()
    val swipeRight by viewModel.swipeRightAction.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var contextMenuConversation by remember { mutableStateOf<Conversation?>(null) }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.forceDeleteAfterDefaultSet()
        } else {
            viewModel.dismissDefaultSmsPrompt()
        }
    }

    val defaultSmsCheckLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onDefaultSmsResult(result.resultCode == Activity.RESULT_OK)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var firstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) {
                    firstResume = false
                    viewModel.checkDefaultSmsApp()
                    return@LifecycleEventObserver
                }
                viewModel.checkDefaultSmsApp()
                viewModel.forceRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                    Text(
                        text = "${uiState.selectedThreadIds.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.showCategoryAssignDialog() }) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Category", tint = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                    IconButton(onClick = { viewModel.showBlockDialogForSelected() }) {
                        Icon(Icons.Default.Block, contentDescription = "Block", tint = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                    IconButton(onClick = { viewModel.archiveSelected() }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                    IconButton(onClick = { viewModel.deleteSelected() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AuroraColors.Error)
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !uiState.isSelectionMode,
                enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onComposeClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New message")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // App title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aura Messaging",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 0.dp
            ) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search conversations...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (uiState.searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MessageCategory.entries, key = { it.name }) { category ->
                    val isSelected = uiState.selectedCategory == category && uiState.selectedCustomCategory == null
                    Surface(
                        onClick = { viewModel.setCategory(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = category.displayName,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }

                items(uiState.customCategories, key = { it.first }) { (_, name) ->
                    val isSelected = uiState.selectedCustomCategory == name
                    Surface(
                        onClick = { viewModel.setCustomCategoryFilter(name) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }

                // Archived chip — navigates to archived screen
                item {
                    Surface(
                        onClick = onNavigateToArchived,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = "Archived",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Archived",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { viewModel.showManageCategoriesDialog() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Manage categories",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conversation list
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.pullToRefresh() },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(8) {
                                ShimmerConversationItem()
                            }
                        }
                    }

                    !uiState.isLoading && uiState.filteredConversations.isEmpty() && uiState.error == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 48.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "No messages yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Start a conversation by tapping\nthe compose button",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (!uiState.isLoading && uiState.filteredConversations.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        val conversations = uiState.filteredConversations
                        items(
                            count = conversations.size,
                            key = { conversations[it].threadId }
                        ) { index ->
                            val conversation = conversations[index]
                            if (uiState.isSelectionMode) {
                                ConversationItem(
                                    conversation = conversation,
                                    isSelected = conversation.threadId in uiState.selectedThreadIds,
                                    isSelectionMode = true,
                                    onClick = { viewModel.toggleSelection(conversation.threadId) },
                                    onLongClick = { }
                                )
                            } else {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                viewModel.executeSwipeAction(swipeLeft, conversation)
                                                swipeLeft == SwipeAction.ARCHIVE
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                viewModel.executeSwipeAction(swipeRight, conversation)
                                                swipeRight == SwipeAction.ARCHIVE
                                            }
                                            else -> false
                                        }
                                    }
                                )

                                SwipeableRow(
                                    state = dismissState,
                                    leftAction = swipeLeft,
                                    rightAction = swipeRight
                                ) {
                                    ConversationItem(
                                        conversation = conversation,
                                        onClick = {
                                            onConversationClick(
                                                conversation.threadId,
                                                conversation.address,
                                                conversation.contactName
                                            )
                                        },
                                        onLongClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            contextMenuConversation = conversation
                                        }
                                    )
                                }
                            }
                            if (index < conversations.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 86.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Dialogs
        if (uiState.showCategoryDialog) {
            CategoryAssignDialog(
                customCategories = uiState.customCategories.map { it.second },
                onAssign = { viewModel.assignCategoryToSelected(it) },
                onRemove = { viewModel.assignCategoryToSelected(null) },
                onDismiss = { viewModel.dismissCategoryDialog() }
            )
        }

        if (uiState.showManageCategoriesDialog) {
            ManageCategoriesDialog(
                categories = uiState.customCategories,
                showAddDialog = uiState.showAddCategoryDialog,
                editingCategory = uiState.editingCategory,
                onAddClick = { viewModel.showAddCategoryDialog() },
                onAddConfirm = { viewModel.addCustomCategory(it) },
                onAddDismiss = { viewModel.dismissAddCategoryDialog() },
                onEditClick = { id, name -> viewModel.startEditCategory(id, name) },
                onRenameConfirm = { id, name -> viewModel.renameCustomCategory(id, name) },
                onDeleteClick = { viewModel.deleteCustomCategory(it) },
                onEditDismiss = { viewModel.dismissAddCategoryDialog() },
                onDismiss = { viewModel.dismissManageCategoriesDialog() }
            )
        }

        if (uiState.showBlockDialog && uiState.blockTarget != null) {
            BlockConfirmationDialog(
                displayName = uiState.blockTarget!!.displayName,
                address = uiState.blockTarget!!.address,
                hasNumber = uiState.blockTarget!!.address.isNotBlank(),
                hasContactName = uiState.blockTarget!!.displayName != uiState.blockTarget!!.address,
                onBlockNumber = { viewModel.confirmBlockNumber() },
                onBlockName = { name -> viewModel.confirmBlockName(name) },
                onBlockWords = { words -> viewModel.confirmBlockWords(words) },
                onBlockLanguage = { lang -> viewModel.confirmBlockLanguage(lang) },
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

        if (uiState.showDeleteConfirmation) {
            val count = uiState.pendingDeleteThreadIds.size
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirmation() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        if (count == 1) "Delete conversation?"
                        else "Delete $count conversations?"
                    )
                },
                text = {
                    Text(
                        if (count == 1) "This conversation will be permanently deleted. This action cannot be undone."
                        else "These $count conversations will be permanently deleted. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (uiState.showDefaultSmsPrompt) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDefaultSmsPrompt() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = { Text("Default messaging app required") },
                text = {
                    Text(
                        "To delete conversations, Aura needs to be set as your " +
                            "default messaging app. This allows Aura to manage " +
                            "your messages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                } else {
                                    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                    }
                                }
                                defaultSmsLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Unable to open default SMS settings. Please set Aura as default in your device Settings > Apps > Default apps.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set as Default")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDefaultSmsPrompt() }) {
                        Text("Not now")
                    }
                }
            )
        }

        contextMenuConversation?.let { conv ->
            ConversationContextMenu(
                conversation = conv,
                onMute = { muteUntil -> viewModel.muteConversationUntil(conv.threadId, muteUntil) },
                onUnmute = { viewModel.unmuteConversation(conv.threadId) },
                onPin = { viewModel.pinConversation(conv.threadId, true) },
                onUnpin = { viewModel.pinConversation(conv.threadId, false) },
                onArchive = { viewModel.archiveConversation(conv.threadId) },
                onMarkAsRead = { viewModel.markAsRead(conv.threadId) },
                onFavorite = { viewModel.setConversationFavorite(conv.threadId, true) },
                onUnfavorite = { viewModel.setConversationFavorite(conv.threadId, false) },
                onSelect = { viewModel.toggleSelection(conv.threadId) },
                onDelete = { viewModel.deleteConversation(conv.threadId) },
                onDismiss = { contextMenuConversation = null }
            )
        }

        if (uiState.showDefaultSmsCheck && !uiState.showDefaultSmsPrompt) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDefaultSmsCheck() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = { Text("Set as default messaging app") },
                text = {
                    Text(
                        "For the best experience, set Aura as your default messaging " +
                            "app. This enables full message management including marking " +
                            "messages as read, deleting conversations, and sending messages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                } else {
                                    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                    }
                                }
                                defaultSmsCheckLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Unable to open default SMS settings. Please set Aura as default in your device Settings > Apps > Default apps.",
                                    Toast.LENGTH_LONG
                                ).show()
                                viewModel.dismissDefaultSmsCheck()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set as Default")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDefaultSmsCheck() }) {
                        Text("Not now")
                    }
                }
            )
        }
    }
}

@Composable
private fun BlockConfirmationDialog(
    displayName: String,
    address: String,
    hasNumber: Boolean,
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

                    if (hasNumber) {
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
                    }
                    if (hasContactName) {
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
                    }
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
private fun CategoryAssignDialog(
    customCategories: List<String>,
    onAssign: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = { Text("Assign Category") },
        text = {
            Column {
                if (customCategories.isEmpty()) {
                    Text(
                        text = "No custom categories yet. Create one from the Edit button on the category bar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Choose a category for the selected conversations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    customCategories.forEach { name ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { onAssign(name) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        onClick = onRemove
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RemoveCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Remove from category",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
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
private fun ManageCategoriesDialog(
    categories: List<Pair<Long, String>>,
    showAddDialog: Boolean,
    editingCategory: Pair<Long, String>?,
    onAddClick: () -> Unit,
    onAddConfirm: (String) -> Unit,
    onAddDismiss: () -> Unit,
    onEditClick: (Long, String) -> Unit,
    onRenameConfirm: (Long, String) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onEditDismiss: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories") },
        text = {
            Column {
                if (categories.isEmpty()) {
                    Text(
                        text = "No custom categories yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    categories.forEach { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onEditClick(id, name) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteClick(id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Add new category",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onAddDismiss,
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onAddConfirm(newName) },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = onAddDismiss) { Text("Cancel") }
            }
        )
    }

    if (editingCategory != null) {
        var editName by remember(editingCategory.first) { mutableStateOf(editingCategory.second) }
        AlertDialog(
            onDismissRequest = onEditDismiss,
            title = { Text("Rename Category") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onRenameConfirm(editingCategory.first, editName) },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = onEditDismiss) { Text("Cancel") }
            }
        )
    }
}
