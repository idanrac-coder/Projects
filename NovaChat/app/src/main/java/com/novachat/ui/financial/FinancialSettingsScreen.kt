package com.novachat.ui.financial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.core.sms.financial.FinancialCategory
import com.novachat.domain.model.SenderInfo
import androidx.compose.ui.res.stringResource
import com.novachat.R
import com.novachat.ui.financial.components.CATEGORY_COLORS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCardManager: () -> Unit,
    onNavigateToSetupGuide: () -> Unit,
    viewModel: FinancialSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showAddSenderDialog by remember { mutableStateOf(false) }
    var senderToRemove by remember { mutableStateOf<SenderInfo?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.financial_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // General Section
            item { SectionHeader(stringResource(R.string.general)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.financial_intelligence),
                    trailing = {
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = { viewModel.setEnabled(it) }
                        )
                    }
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.primary_currency),
                    subtitle = "${state.primaryCurrency} (${currencySymbolFromCode(state.primaryCurrency)})",
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.scan_inbox),
                    subtitle = stringResource(R.string.scan_inbox_financial_subtitle),
                    leading = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                    onClick = { viewModel.scanInbox() }
                )
            }

            // Card Management Section
            item { SectionHeader(stringResource(R.string.card_management)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.manage_cards),
                    subtitle = stringResource(R.string.cards_detected_fmt, state.cardCount),
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    onClick = onNavigateToCardManager
                )
            }

            // Sender Management Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sender_management),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showAddSenderDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.add_sender))
                    }
                }
            }
            items(state.senders) { sender ->
                SenderRow(
                    name = sender.displayName ?: sender.address,
                    subtitle = "${sender.source} · ${sender.transactionCount} transactions",
                    monitorEnabled = sender.isEnabled,
                    alertsEnabled = sender.alertsEnabled,
                    onMonitorToggle = { viewModel.setSenderEnabled(sender.id, it) },
                    onAlertsToggle = { viewModel.setSenderAlertsEnabled(sender.id, it) },
                    onRemove = { senderToRemove = sender }
                )
            }

            // Privacy & Security Section
            item { SectionHeader(stringResource(R.string.privacy_and_security)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.database_encryption),
                    subtitle = stringResource(R.string.database_encryption_subtitle),
                    leading = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) },
                    trailing = { Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) }
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.on_device_only),
                    subtitle = stringResource(R.string.on_device_only_subtitle),
                    leading = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) },
                    trailing = { Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) }
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.clear_financial_data),
                    subtitle = stringResource(R.string.clear_financial_data_subtitle),
                    leading = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true }
                )
            }

            // Categories Section
            item { SectionHeader(stringResource(R.string.categories)) }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        listOf(
                            FinancialCategory.BILL to stringResource(R.string.category_bills),
                            FinancialCategory.SUBSCRIPTION to stringResource(R.string.category_subscriptions),
                            FinancialCategory.PAYMENT to stringResource(R.string.category_payments),
                            FinancialCategory.EXPENSE to stringResource(R.string.category_expenses)
                        ).forEach { (cat, label) ->
                            val color = CATEGORY_COLORS[cat] ?: Color.Gray
                            val count = state.categoryCounts[cat.name] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .padding(0.dp)
                                            .then(
                                                Modifier
                                                    .size(10.dp)
                                                    .background(color, CircleShape)
                                            )
                                    )
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Setup Section
            item { SectionHeader(stringResource(R.string.setup)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.setup_guide),
                    subtitle = stringResource(R.string.setup_guide_subtitle),
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    onClick = onNavigateToSetupGuide
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_financial_data)) },
            text = { Text(stringResource(R.string.clear_financial_data_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearData()
                    showClearDialog = false
                }) { Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    senderToRemove?.let { sender ->
        AlertDialog(
            onDismissRequest = { senderToRemove = null },
            title = { Text(stringResource(R.string.remove_sender)) },
            text = { Text(stringResource(R.string.remove_sender_confirm, sender.displayName ?: sender.address, sender.transactionCount)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSender(sender.id)
                    senderToRemove = null
                }) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { senderToRemove = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAddSenderDialog) {
        var address by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSenderDialog = false },
            title = { Text(stringResource(R.string.add_sender)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(stringResource(R.string.sender_address)) },
                        placeholder = { Text(stringResource(R.string.sender_address_hint)) }
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(stringResource(R.string.display_name_optional)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (address.isNotBlank()) {
                            viewModel.addSender(address, displayName.ifBlank { null })
                            showAddSenderDialog = false
                        }
                    }
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddSenderDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun SenderRow(
    name: String,
    subtitle: String,
    monitorEnabled: Boolean,
    alertsEnabled: Boolean,
    onMonitorToggle: (Boolean) -> Unit,
    onAlertsToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.monitor), style = MaterialTheme.typography.labelSmall)
                Switch(checked = monitorEnabled, onCheckedChange = onMonitorToggle)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(if (monitorEnabled) 1f else 0.4f)
            ) {
                Text(stringResource(R.string.alerts), style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = alertsEnabled,
                    onCheckedChange = onAlertsToggle,
                    enabled = monitorEnabled
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_sender),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun currencySymbolFromCode(code: String): String = when (code) {
    "ILS" -> "₪"
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    else -> code
}
