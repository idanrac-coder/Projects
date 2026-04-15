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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novachat.BuildConfig
import com.novachat.R
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
                        stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_messages))
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
                            text = if (isPremium) stringResource(R.string.premium_account) else stringResource(R.string.upgrade_to_premium),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isPremium) stringResource(R.string.all_features_unlocked) else stringResource(R.string.unlock_all_features),
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
            SettingsSectionHeader(title = stringResource(R.string.section_appearance))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.themes),
                        subtitle = stringResource(R.string.themes_subtitle),
                        onClick = onThemesClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.SwipeRight,
                        title = stringResource(R.string.swipe_actions),
                        subtitle = stringResource(R.string.swipe_actions_subtitle),
                        onClick = onSwipeActionsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Translate,
                        title = stringResource(R.string.app_language),
                        subtitle = when (appLanguage) {
                            "he" -> stringResource(R.string.language_hebrew)
                            "en" -> stringResource(R.string.language_english)
                            else -> stringResource(R.string.language_system_default)
                        },
                        onClick = { showLanguagePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Protection section
            SettingsSectionHeader(title = stringResource(R.string.section_protection))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.smart_and_secure),
                    subtitle = stringResource(R.string.smart_and_secure_subtitle),
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
                    title = stringResource(R.string.financial_intelligence),
                    subtitle = if (isPremium) stringResource(R.string.financial_intelligence_subtitle) else stringResource(R.string.premium_feature),
                    onClick = onFinancialIntelligenceClick,
                    trailing = if (!isPremium) {
                        { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.premium_feature), modifier = Modifier.size(16.dp), tint = AuroraColors.Warning) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Messages section
            SettingsSectionHeader(title = stringResource(R.string.section_messages))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = stringResource(R.string.messaging_settings),
                        subtitle = stringResource(R.string.messaging_settings_subtitle),
                        onClick = onMessagingSettingsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.notifications),
                        subtitle = stringResource(R.string.notifications_subtitle),
                        onClick = onNotificationsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.notification_profiles),
                        subtitle = stringResource(R.string.notification_profiles_subtitle),
                        onClick = onNotificationProfilesClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.scheduled_messages),
                        subtitle = stringResource(R.string.scheduled_messages_subtitle),
                        onClick = onScheduledClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Archive,
                        title = stringResource(R.string.archived),
                        subtitle = stringResource(R.string.archived_subtitle),
                        onClick = onArchivedClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data & Privacy section
            SettingsSectionHeader(title = stringResource(R.string.section_data_privacy))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = stringResource(R.string.backup_and_restore),
                        subtitle = stringResource(R.string.backup_subtitle),
                        onClick = onBackupClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.QrCode,
                        title = stringResource(R.string.qr_contact_sharing),
                        subtitle = stringResource(R.string.qr_contact_sharing_subtitle),
                        onClick = onQrClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Policy,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.privacy_policy_subtitle),
                        onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionHeader(title = stringResource(R.string.section_support))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.send_feedback),
                        subtitle = stringResource(R.string.send_feedback_subtitle),
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
                        title = stringResource(R.string.rate_us),
                        subtitle = stringResource(R.string.rate_us_subtitle),
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

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTag by remember { mutableStateOf(currentLanguage) }

    val languageOptions = listOf(
        "" to stringResource(R.string.language_system_default),
        "en" to stringResource(R.string.language_english),
        "he" to stringResource(R.string.language_hebrew_native)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_language), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                languageOptions.forEach { (tag, label) ->
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
                Text(stringResource(R.string.apply), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
