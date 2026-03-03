package com.novachat.ui.blocking

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.domain.model.BlockRule
import com.novachat.domain.model.BlockType
import com.novachat.domain.repository.BlockRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockingScreen(
    onBack: () -> Unit,
    viewModel: BlockingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("By Number", "By Words", "By Sender", "By Language")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.canAddMoreRules) {
                FloatingActionButton(
                    onClick = {
                        val type = when (selectedTab) {
                            0 -> BlockType.NUMBER
                            1 -> BlockType.KEYWORD
                            2 -> BlockType.SENDER_NAME
                            else -> BlockType.LANGUAGE
                        }
                        viewModel.showAddDialog(type)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add rule")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!uiState.isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Free plan: ${uiState.ruleCount}/${BlockRepository.FREE_RULE_LIMIT} block rules used. Upgrade to Premium for unlimited rules.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val rules = when (selectedTab) {
                0 -> uiState.numberRules
                1 -> uiState.keywordRules
                2 -> uiState.senderRules
                else -> uiState.languageRules
            }

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ${tabs[selectedTab].lowercase()} rules yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add a rule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        BlockRuleCard(
                            rule = rule,
                            onDelete = { viewModel.deleteRule(rule.id) }
                        )
                    }
                }
            }
        }

        if (uiState.showAddDialog) {
            AddBlockRuleDialog(
                type = uiState.addDialogType,
                onConfirm = { value, isRegex -> viewModel.addRule(value, isRegex) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
    }
}

@Composable
private fun BlockRuleCard(
    rule: BlockRule,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.value,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (rule.isRegex) {
                    Text(
                        text = "Regex pattern",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddBlockRuleDialog(
    type: BlockType,
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }

    val title = when (type) {
        BlockType.NUMBER -> "Block Number"
        BlockType.KEYWORD -> "Block Keyword"
        BlockType.SENDER_NAME -> "Block Sender"
        BlockType.LANGUAGE -> "Block Language"
    }
    val placeholder = when (type) {
        BlockType.NUMBER -> "Phone number (e.g., +1555*)"
        BlockType.KEYWORD -> "Keyword or phrase"
        BlockType.SENDER_NAME -> "Sender name"
        BlockType.LANGUAGE -> "Language code (e.g. en, es, fr)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRegex,
                        onCheckedChange = { isRegex = it }
                    )
                    Text(
                        text = "Use regex pattern",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (type == BlockType.NUMBER) {
                    Text(
                        text = "Use * as wildcard (e.g., +1555* blocks all numbers starting with +1555)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value, isRegex) },
                enabled = value.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
