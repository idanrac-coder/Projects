package com.novachat.ui.notifications

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.core.database.entity.NotificationProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationProfilesScreen(
    onBack: () -> Unit,
    viewModel: NotificationProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Notification Profiles", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create profile")
            }
        }
    ) { padding ->
        if (uiState.profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "No notification profiles",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create profiles like Work, Sleep, or\nPersonal with custom notification settings",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        onClick = { viewModel.deactivateAll() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.profiles.none { it.isActive })
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Default", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Standard notifications",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (uiState.profiles.none { it.isActive }) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(uiState.profiles, key = { it.id }) { profile ->
                    NotificationProfileCard(
                        profile = profile,
                        onActivate = { viewModel.activateProfile(profile.id) },
                        onEdit = { viewModel.showEditDialog(profile) },
                        onDelete = { viewModel.deleteProfile(profile.id) }
                    )
                }
            }
        }

        if (uiState.showCreateDialog) {
            CreateEditProfileDialog(
                existing = uiState.editingProfile,
                onDismiss = { viewModel.dismissDialog() },
                onCreate = { name, vib, priority, popup, sH, sM, eH, eM ->
                    viewModel.createProfile(name, vib, priority, popup, sH, sM, eH, eM)
                },
                onUpdate = { id, name, vib, priority, popup, sH, sM, eH, eM ->
                    viewModel.updateProfile(id, name, vib, priority, popup, sH, sM, eH, eM)
                }
            )
        }
    }
}

@Composable
private fun NotificationProfileCard(
    profile: NotificationProfileEntity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onActivate,
        shape = RoundedCornerShape(16.dp),
        color = if (profile.isActive)
            MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (profile.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (profile.vibrationEnabled) Icons.Default.Vibration
                    else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append(if (profile.vibrationEnabled) "Vibration" else "Silent")
                            append(" · ")
                            append(profile.priority)
                            if (profile.scheduleStartHour != null) {
                                append(" · ")
                                append("%02d:%02d-%02d:%02d".format(
                                    profile.scheduleStartHour,
                                    profile.scheduleStartMinute ?: 0,
                                    profile.scheduleEndHour ?: 0,
                                    profile.scheduleEndMinute ?: 0
                                ))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (profile.isActive) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CreateEditProfileDialog(
    existing: NotificationProfileEntity?,
    onDismiss: () -> Unit,
    onCreate: (String, Boolean, String, Boolean, Int?, Int?, Int?, Int?) -> Unit,
    onUpdate: (Long, String, Boolean, String, Boolean, Int?, Int?, Int?, Int?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var vibration by remember { mutableStateOf(existing?.vibrationEnabled ?: true) }
    var popup by remember { mutableStateOf(existing?.popupEnabled ?: true) }
    var priority by remember { mutableStateOf(existing?.priority ?: "HIGH") }
    var hasSchedule by remember { mutableStateOf(existing?.scheduleStartHour != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Profile" else "New Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    placeholder = { Text("e.g., Work, Sleep, Personal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibration", fontWeight = FontWeight.Medium)
                        Text(
                            "Vibrate on notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vibration,
                        onCheckedChange = { vibration = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Popup notifications", fontWeight = FontWeight.Medium)
                        Text(
                            "Show heads-up notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = popup,
                        onCheckedChange = { popup = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Text("Priority", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "DEFAULT", "HIGH").forEach { p ->
                        Surface(
                            onClick = { priority = p },
                            shape = RoundedCornerShape(10.dp),
                            color = if (priority == p) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = p,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (priority == p) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Schedule", fontWeight = FontWeight.Medium)
                        Text(
                            "Auto-activate at specific times",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hasSchedule,
                        onCheckedChange = { hasSchedule = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (existing != null) {
                        onUpdate(
                            existing.id, name, vibration, priority, popup,
                            if (hasSchedule) 22 else null,
                            if (hasSchedule) 0 else null,
                            if (hasSchedule) 7 else null,
                            if (hasSchedule) 0 else null
                        )
                    } else {
                        onCreate(
                            name, vibration, priority, popup,
                            if (hasSchedule) 22 else null,
                            if (hasSchedule) 0 else null,
                            if (hasSchedule) 7 else null,
                            if (hasSchedule) 0 else null
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (existing != null) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
