package com.novachat.ui.financial

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.R
import com.novachat.domain.model.InsightType
import com.novachat.domain.model.SubscriptionSummary
import com.novachat.ui.financial.components.SubscriptionItem
import com.novachat.ui.financial.components.currencySymbol
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomize by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.subscriptions)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomize = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Customize insights")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.subscriptions.isEmpty() && state.cards.isNotEmpty()) {
            SubscriptionsEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Month navigation
                item {
                    val monthName = DateFormatSymbols().months[state.currentMonth - 1]
                        .replaceFirstChar { it.uppercase() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::previousMonth) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_month)
                            )
                        }
                        AnimatedContent(
                            targetState = "$monthName ${state.currentYear}",
                            transitionSpec = {
                                (slideInHorizontally(tween(260)) { it / 2 } + fadeIn(tween(260))) togetherWith
                                    (slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200)))
                            },
                            label = "subMonthTicker"
                        ) { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = viewModel::nextMonth) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_month)
                            )
                        }
                    }
                }

                // Header card
                item {
                    SubscriptionHeaderCard(
                        total = state.total,
                        annualProjection = state.annualProjection,
                        activeCount = state.subscriptions.size,
                        currency = state.subscriptions.firstOrNull()?.currency ?: "ILS",
                        multiCurrency = state.subscriptions.map { it.currency }.toSet().size > 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // Insights row
                if (state.enabledInsights.isNotEmpty()) {
                    item {
                        InsightsRow(
                            state = state,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                // Card filter chips
                if (state.cards.filter { !it.isHidden }.size > 1) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedCardLast4 == null,
                                    onClick = { viewModel.selectCard(null) },
                                    label = { Text(stringResource(R.string.all_cards)) }
                                )
                            }
                            items(state.cards.filter { !it.isHidden }) { card ->
                                FilterChip(
                                    selected = state.selectedCardLast4 == card.last4,
                                    onClick = { viewModel.selectCard(card.last4) },
                                    label = {
                                        Text(
                                            text = card.nickname?.let { "$it *${card.last4}" }
                                                ?: "*${card.last4}"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Section header
                if (state.subscriptions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Subscriptions (${state.subscriptions.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Subscription list — no swipe, one row per merchant
                items(state.subscriptions, key = { "${it.merchantName}|${it.cardLast4}" }) { sub ->
                    SubscriptionItem(subscription = sub)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }

                // Footer note
                item {
                    Text(
                        text = "To move an item here, change its category to \"Subscription\" in your transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }

    if (showCustomize) {
        ModalBottomSheet(
            onDismissRequest = { showCustomize = false },
            sheetState = sheetState
        ) {
            InsightCustomizeSheet(
                enabled = state.enabledInsights,
                onToggle = { viewModel.toggleInsight(it) },
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun SubscriptionHeaderCard(
    total: Double,
    annualProjection: Double,
    activeCount: Int,
    currency: String,
    multiCurrency: Boolean,
    modifier: Modifier = Modifier
) {
    val sym = currencySymbol(currency)
    val prefix = if (multiCurrency) "~" else ""
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF3F8CFF))))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.subscriptions_total_label),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = "$prefix$sym${"%.2f".format(total)}",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$prefix$sym${"%.0f".format(annualProjection)} / year",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$activeCount ${stringResource(R.string.subscriptions_active_suffix)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun InsightsRow(
    state: SubscriptionListUiState,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        if (InsightType.ANNUAL_COST in state.enabledInsights) {
            item {
                val sym = currencySymbol(state.subscriptions.firstOrNull()?.currency ?: "ILS")
                InsightChip(
                    icon = Icons.Default.TrendingUp,
                    iconTint = Color(0xFF3F8CFF),
                    label = "$sym${"%.0f".format(state.annualProjection)} / year"
                )
            }
        }
        if (InsightType.PRICE_CHANGES in state.enabledInsights && state.priceChangeCount > 0) {
            item {
                InsightChip(
                    icon = Icons.Default.AttachMoney,
                    iconTint = Color(0xFFF44336),
                    label = "${state.priceChangeCount} price change${if (state.priceChangeCount > 1) "s" else ""}"
                )
            }
        }
        if (InsightType.NEXT_RENEWAL in state.enabledInsights && state.upcomingRenewal != null) {
            item {
                val sub = state.upcomingRenewal
                val daysUntil = sub.nextChargeEstimate?.let {
                    ((it - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).toInt()
                }
                val label = when {
                    daysUntil == null -> "${sub.merchantName} renewal"
                    daysUntil <= 0 -> "${sub.merchantName} due now"
                    daysUntil == 1 -> "${sub.merchantName} tomorrow"
                    else -> "${sub.merchantName} in ${daysUntil}d"
                }
                InsightChip(
                    icon = Icons.Default.NotificationsNone,
                    iconTint = Color(0xFF6C63FF),
                    label = label
                )
            }
        }
        if (InsightType.STALE in state.enabledInsights && state.staleCount > 0) {
            item {
                InsightChip(
                    icon = Icons.Default.Warning,
                    iconTint = Color(0xFFFDCB6E),
                    label = "${state.staleCount} not charged recently"
                )
            }
        }
    }
}

@Composable
private fun InsightChip(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun InsightCustomizeSheet(
    enabled: Set<InsightType>,
    onToggle: (InsightType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Customize Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        InsightToggleRow(
            icon = Icons.Default.TrendingUp,
            iconTint = Color(0xFF3F8CFF),
            title = "Annual cost projection",
            subtitle = "Monthly total × 12",
            checked = InsightType.ANNUAL_COST in enabled,
            onCheckedChange = { onToggle(InsightType.ANNUAL_COST) }
        )
        InsightToggleRow(
            icon = Icons.Default.AttachMoney,
            iconTint = Color(0xFFF44336),
            title = "Price changes",
            subtitle = "Subscriptions that changed price",
            checked = InsightType.PRICE_CHANGES in enabled,
            onCheckedChange = { onToggle(InsightType.PRICE_CHANGES) }
        )
        InsightToggleRow(
            icon = Icons.Default.NotificationsNone,
            iconTint = Color(0xFF6C63FF),
            title = "Next renewal",
            subtitle = "Nearest upcoming charge",
            checked = InsightType.NEXT_RENEWAL in enabled,
            onCheckedChange = { onToggle(InsightType.NEXT_RENEWAL) }
        )
        InsightToggleRow(
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFFDCB6E),
            title = "Not charged recently",
            subtitle = "No charge in 45+ days — possibly cancelled",
            checked = InsightType.STALE in enabled,
            onCheckedChange = { onToggle(InsightType.STALE) }
        )
    }
}

@Composable
private fun InsightToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { onCheckedChange() })
    }
}

@Composable
private fun SubscriptionsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "No subscriptions yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Transactions categorized as \"Subscription\" will appear here, one row per merchant.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
