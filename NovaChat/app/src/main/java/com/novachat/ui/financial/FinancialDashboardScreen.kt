package com.novachat.ui.financial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.R
import com.novachat.ui.financial.components.CategoryBreakdownBar
import com.novachat.ui.financial.components.CategoryBreakdownSkeleton
import com.novachat.ui.financial.components.GenericCardSkeleton
import com.novachat.ui.financial.components.MonthlySummaryCard
import com.novachat.ui.financial.components.MonthlySummaryCardSkeleton
import com.novachat.ui.financial.components.SpendingChart
import com.novachat.ui.financial.components.SpendingChartSkeleton
import com.novachat.ui.financial.components.SpendingVelocityCard
import com.novachat.ui.financial.components.TopMerchantsCard
import com.novachat.ui.financial.components.TransactionItem
import com.novachat.ui.financial.components.TransactionItemSkeleton
import java.text.DateFormatSymbols

// Dark theme color palette matching the HTML mockup
internal val FinancialBg = Color(0xFF121212)
internal val FinancialSurface = Color(0xFF1A1A1A)
internal val FinancialCard = Color(0xFF1E1E1E)
internal val FinancialAccent = Color(0xFF6C5CE7)
internal val FinancialAccentLight = Color(0xFFA29BFE)
internal val FinancialGreen = Color(0xFF00B894)
internal val FinancialRed = Color(0xFFFF6B6B)
internal val FinancialAmber = Color(0xFFFDCB6E)
internal val FinancialTextPrimary = Color(0xFFE0E0E0)
internal val FinancialTextSecondary = Color(0xFF888888)
internal val FinancialDivider = Color(0xFF252525)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToConversation: (Long, String, String?) -> Unit = { _, _, _ -> },
    viewModel: FinancialDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var transactionsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FinancialBg,
        topBar = {
            Column {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.financial_intelligence),
                        color = FinancialTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E)),
                actions = {
                    if (state.alerts.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = FinancialAccent) {
                                    Text("${state.alerts.size}", color = Color.White)
                                }
                            }
                        ) {
                            IconButton(onClick = onNavigateToAlerts) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = FinancialTextSecondary
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = FinancialTextSecondary
                        )
                    }
                }
            )
            HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)
            } // end Column topBar
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FinancialBg),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Month navigation row (OUTSIDE the hero card, like the mockup) ──
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
                            contentDescription = stringResource(R.string.previous_month),
                            tint = FinancialAccentLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    AnimatedContent(
                        targetState = "$monthName ${state.currentYear}",
                        transitionSpec = {
                            (slideInHorizontally(tween(260)) { it / 2 } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200)))
                        },
                        label = "monthTicker"
                    ) { label ->
                        Text(
                            text = label,
                            color = FinancialTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_month),
                            tint = FinancialAccentLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // ── Card filter chips (before hero, like the mockup) ──
            if (!state.isLoading && state.cards.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedCardLast4 == null,
                                onClick = { viewModel.selectCard(null) },
                                label = {
                                    Text(
                                        stringResource(R.string.all_cards),
                                        color = if (state.selectedCardLast4 == null) Color.White else FinancialTextSecondary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FinancialAccent,
                                    containerColor = Color(0xFF2A2A2A)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = state.selectedCardLast4 == null,
                                    borderColor = Color(0xFF3A3A3A),
                                    selectedBorderColor = FinancialAccent
                                )
                            )
                        }
                        items(state.cards.filter { !it.isHidden }) { card ->
                            val isSelected = state.selectedCardLast4 == card.last4
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectCard(card.last4) },
                                label = {
                                    Text(
                                        card.nickname ?: stringResource(R.string.card_number_fmt, card.last4),
                                        color = if (isSelected) Color.White else FinancialTextSecondary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FinancialAccent,
                                    containerColor = Color(0xFF2A2A2A)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF3A3A3A),
                                    selectedBorderColor = FinancialAccent
                                )
                            )
                        }
                    }
                }
            }

            // ── Hero card (summary + comparison stats) ──
            item {
                AnimatedContent(
                    targetState = state.currentMonth to state.currentYear,
                    transitionSpec = {
                        val goingForward = targetState.second > initialState.second ||
                            (targetState.second == initialState.second && targetState.first > initialState.first) ||
                            (targetState.first == 1 && initialState.first == 12)
                        val dir = if (goingForward) 1 else -1
                        (slideInHorizontally(tween(320)) { it * dir } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it * dir } + fadeOut(tween(200)))
                    },
                    label = "heroContent"
                ) { _ ->
                    if (state.isLoading) {
                        MonthlySummaryCardSkeleton(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    } else {
                        MonthlySummaryCard(
                            summary = state.monthlySummary,
                            onPreviousMonth = viewModel::previousMonth,
                            onNextMonth = viewModel::nextMonth,
                            monthComparison = state.monthComparison,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Category breakdown ──
            item {
                if (state.isLoading) {
                    CategoryBreakdownSkeleton(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                } else {
                    AnimatedVisibility(
                        visible = state.categoryBreakdown.isNotEmpty(),
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
                    ) {
                        CategoryBreakdownBar(
                            breakdown = state.categoryBreakdown,
                            onSubscriptionClick = onNavigateToSubscriptions,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Spending velocity ──
            item {
                if (state.isLoading) {
                    GenericCardSkeleton(
                        lineCount = 2,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                } else {
                    AnimatedVisibility(
                        visible = state.spendingVelocity != null,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
                    ) {
                        state.spendingVelocity?.let { velocity ->
                            SpendingVelocityCard(
                                velocity = velocity,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Top merchants ──
            item {
                if (state.isLoading) {
                    GenericCardSkeleton(
                        lineCount = 3,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                } else {
                    AnimatedVisibility(
                        visible = state.topMerchants.isNotEmpty(),
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
                    ) {
                        TopMerchantsCard(
                            merchants = state.topMerchants,
                            currency = state.monthlySummary.currency,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Spending chart ──
            item {
                if (state.isLoading) {
                    SpendingChartSkeleton(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                } else {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(400))
                    ) {
                        SpendingChart(
                            dailySpending = state.dailySpending,
                            month = state.currentMonth,
                            year = state.currentYear,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Recent transactions ──
            if (state.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FinancialCard)
                    ) {
                        repeat(5) { TransactionItemSkeleton() }
                    }
                }
            } else if (state.recentTransactions.isNotEmpty()) {
                item {
                    val visibleTransactions = if (transactionsExpanded)
                        state.recentTransactions
                    else
                        state.recentTransactions.take(5)

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FinancialCard)
                    ) {
                        Text(
                            text = stringResource(R.string.recent_transactions),
                            color = FinancialTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 10.dp)
                        )
                        visibleTransactions.forEachIndexed { idx, transaction ->
                            HorizontalDivider(color = FinancialDivider, thickness = 1.dp)
                            TransactionItem(
                                transaction = transaction,
                                onCategoryChange = { merchantName, category ->
                                    viewModel.updateTransactionCategory(merchantName, category.name)
                                },
                                onViewInConversation = { address ->
                                    viewModel.resolveAndNavigateToConversation(address) { threadId ->
                                        onNavigateToConversation(threadId, address, null)
                                    }
                                }
                            )
                        }
                        if (state.recentTransactions.size > 5) {
                            HorizontalDivider(color = FinancialDivider, thickness = 1.dp)
                            TextButton(
                                onClick = { transactionsExpanded = !transactionsExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (transactionsExpanded) stringResource(R.string.show_less)
                                    else stringResource(R.string.show_more_fmt, state.recentTransactions.size - 5),
                                    color = FinancialAccentLight
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
