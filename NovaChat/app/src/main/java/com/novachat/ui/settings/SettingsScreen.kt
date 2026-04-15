package com.novachat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novachat.BuildConfig
import com.novachat.core.theme.AuroraColors

private const val PRIVACY_POLICY_URL = "https://idanrac-coder.github.io/Projects/privacy-policy.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemesClick: () -> Unit,
    onSwipeActionsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onBackupClick: () -> Unit = {},
    onQrClick: () -> Unit = {},
    onScheduledClick: () -> Unit = {},
    onArchivedClick: () -> Unit = {},
    onNotificationProfilesClick: () -> Unit = {},
    onSmartSecureClick: () -> Unit = {},
    onMessagingSettingsClick: () -> Unit = {},
    onFinancialIntelligenceClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var showLanguagePicker by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Premium CTA card
            Surface(
                onClick = onPremiumClick,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.Verified else Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPremium) "Premium Account" else "Upgrade to Premium",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isPremium) "All features unlocked" else "Unlock all themes, features & more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance section
            SettingsSectionHeader(title = "Appearance")
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Themes",
                        subtitle = "Colors, bubbles & wallpapers",
                        onClick = onThemesClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.SwipeRight,
                        title = "Swipe Actions",
                        subtitle = "Configure swipe gestures",
                        onClick = onSwipeActionsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Translate,
                        title = "Language",
                        subtitle = when (appLanguage) {
                            "he" -> "Hebrew"
                            "en" -> "English"
                            else -> "System default"
                        },
                        onClick = { showLanguagePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Smart & Secure section
            SettingsSectionHeader(title = "Protection")
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Smart & Secure",
                    subtitle = "AI spam detection, protection stats",
                    onClick = onSmartSecureClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsItem(
                    icon = Icons.Default.BarChart,
                    title = "Financial Intelligence",
                    subtitle = if (isPremium) "Track spending & subscriptions" else "Premium feature",
                    onClick = onFinancialIntelligenceClick,
                    trailing = if (!isPremium) {
                        { Icon(Icons.Default.Lock, contentDescription = "Premium", modifier = Modifier.size(16.dp), tint = AuroraColors.Warning) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Messages section
            SettingsSectionHeader(title = "Messages")
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "Messaging Settings",
                        subtitle = "Undo send, smart links & forwarding",
                        onClick = onMessagingSettingsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Sounds, vibration & DND",
                        onClick = onNotificationsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notification Profiles",
                        subtitle = "Work, Sleep, Personal modes",
                        onClick = onNotificationProfilesClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Schedule,
                        title = "Scheduled Messages",
                        subtitle = "Manage pending messages",
                        onClick = onScheduledClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Archive,
                        title = "Archived",
                        subtitle = "View archived chats",
                        onClick = onArchivedClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data & Privacy section
            SettingsSectionHeader(title = "Data & Privacy")
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restore",
                        subtitle = "Back up to Google Drive",
                        onClick = onBackupClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.QrCode,
                        title = "QR Contact Sharing",
                        subtitle = "Share your contact via QR",
                        onClick = onQrClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        subtitle = "How your data is handled",
                        onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionHeader(title = "Support")
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = "Send Feedback",
                        subtitle = "Report bugs or suggest features",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:iracsoftware@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Aura Feedback v${BuildConfig.VERSION_NAME}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:iracsoftware@gmail.com"))
                                try { context.startActivity(fallback) } catch (_: Exception) { }
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.RateReview,
                        title = "Rate Us",
                        subtitle = "Leave a review on Google Play",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                uriHandler.openUri("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (showLanguagePicker) {
                LanguagePickerDialog(
                    currentLanguage = appLanguage,
                    onConfirm = { tag ->
                        viewModel.setAppLanguage(tag)
                        showLanguagePicker = false
                    },
                    onDismiss = { showLanguagePicker = false }
                )
            }

            Text(
                text = "Aura v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            trailing()
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

private val LANGUAGE_OPTIONS = listOf(
    "" to "System default",
    "en" to "English",
    "he" to "עברית (Hebrew)"
)

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTag by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Language", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                LANGUAGE_OPTIONS.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTag == tag,
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
            TextButton(onClick = { onConfirm(selectedTag) }) {
                Text("Apply", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
