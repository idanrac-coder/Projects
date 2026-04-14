package com.novachat.ui.financial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.ui.financial.components.AlertsBanner
import com.novachat.ui.financial.components.CategoryBreakdownBar
import com.novachat.ui.financial.components.MonthlySummaryCard
import com.novachat.ui.financial.components.SpendingChart
import com.novachat.ui.financial.components.TransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    viewModel: FinancialDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var transactionsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Financial Intelligence") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Card filter chips
            if (state.cards.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedCardLast4 == null,
                                onClick = { viewModel.selectCard(null) },
                                label = { Text("All Cards") }
                            )
                        }
                        items(state.cards.filter { !it.isHidden }) { card ->
                            FilterChip(
                                selected = state.selectedCardLast4 == card.last4,
                                onClick = { viewModel.selectCard(card.last4) },
                                label = { Text(card.nickname ?: "Card *${card.last4}") }
                            )
                        }
                    }
                }
            }

            // Monthly Summary
            item {
                MonthlySummaryCard(
                    summary = state.monthlySummary,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Alerts Banner
            if (state.alerts.isNotEmpty()) {
                item {
                    AlertsBanner(
                        alerts = state.alerts,
                        onDismiss = viewModel::dismissAlert,
                        onViewAll = onNavigateToAlerts
                    )
                }
            }

            // Category Breakdown
            if (state.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBreakdownBar(
                        breakdown = state.categoryBreakdown,
                        onSubscriptionClick = onNavigateToSubscriptions,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Spending Chart
            item {
                SpendingChart(
                    dailySpending = state.dailySpending,
                    month = state.currentMonth,
                    year = state.currentYear,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Recent Transactions
            if (state.recentTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                val visibleTransactions = if (transactionsExpanded)
                    state.recentTransactions
                else
                    state.recentTransactions.take(5)
                items(visibleTransactions, key = { it.id }) { transaction ->
                    TransactionItem(transaction = transaction)
                }
                if (state.recentTransactions.size > 5) {
                    item {
                        TextButton(
                            onClick = { transactionsExpanded = !transactionsExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (transactionsExpanded) "Show less" else "Show ${state.recentTransactions.size - 5} more")
                        }
                    }
                }
            }
        }
    }
}
