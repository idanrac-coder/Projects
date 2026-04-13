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
                title = { Text("Financial Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            item { SectionHeader("General") }
            item {
                SettingsRow(
                    title = "Financial Intelligence",
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
                    title = "Primary Currency",
                    subtitle = "${state.primaryCurrency} (${currencySymbolFromCode(state.primaryCurrency)})",
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                )
            }

            // Card Management Section
            item { SectionHeader("Card Management") }
            item {
                SettingsRow(
                    title = "Manage Cards",
                    subtitle = "${state.cardCount} cards detected",
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
                        text = "Sender Management",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showAddSenderDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Add Sender")
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
            item { SectionHeader("Privacy & Security") }
            item {
                SettingsRow(
                    title = "Database Encryption",
                    subtitle = "Financial data is encrypted with SQLCipher",
                    leading = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) },
                    trailing = { Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) }
                )
            }
            item {
                SettingsRow(
                    title = "On-Device Only",
                    subtitle = "No data leaves your device",
                    leading = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) },
                    trailing = { Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) }
                )
            }
            item {
                SettingsRow(
                    title = "Clear Financial Data",
                    subtitle = "Delete all tracked transactions and subscriptions",
                    leading = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true }
                )
            }

            // Categories Section
            item { SectionHeader("Categories") }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        listOf(
                            FinancialCategory.BILL to "Bills",
                            FinancialCategory.SUBSCRIPTION to "Subscriptions",
                            FinancialCategory.PAYMENT to "Payments",
                            FinancialCategory.EXPENSE to "Expenses"
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
            item { SectionHeader("Setup") }
            item {
                SettingsRow(
                    title = "Setup Guide",
                    subtitle = "Re-open provider SMS activation links",
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    onClick = onNavigateToSetupGuide
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Financial Data") },
            text = { Text("This will permanently delete all tracked transactions, subscriptions, merchant data, and alerts. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearData()
                    showClearDialog = false
                }) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    senderToRemove?.let { sender ->
        AlertDialog(
            onDismissRequest = { senderToRemove = null },
            title = { Text("Remove Sender") },
            text = { Text("Remove \"${sender.displayName ?: sender.address}\"? All ${sender.transactionCount} transactions from this sender will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSender(sender.id)
                    senderToRemove = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { senderToRemove = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddSenderDialog) {
        var address by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSenderDialog = false },
            title = { Text("Add Sender") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Sender Address") },
                        placeholder = { Text("e.g., MAX, Leumi") }
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name (optional)") }
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
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSenderDialog = false }) { Text("Cancel") }
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
                Text("Monitor", style = MaterialTheme.typography.labelSmall)
                Switch(checked = monitorEnabled, onCheckedChange = onMonitorToggle)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(if (monitorEnabled) 1f else 0.4f)
            ) {
                Text("Alerts", style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = alertsEnabled,
                    onCheckedChange = onAlertsToggle,
                    enabled = monitorEnabled
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove sender",
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
