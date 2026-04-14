package com.novachat.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.AlertInfo
import com.novachat.domain.model.CardInfo
import com.novachat.domain.model.CategoryBreakdown
import com.novachat.domain.model.DailySpending
import com.novachat.domain.model.MonthComparison
import com.novachat.domain.model.MonthlySummary
import com.novachat.domain.model.SpendingVelocity
import com.novachat.domain.model.TopMerchant
import com.novachat.domain.model.TransactionInfo
import com.novachat.domain.repository.ConversationRepository
import com.novachat.domain.repository.FinancialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val monthlySummary: MonthlySummary = MonthlySummary(0.0, 0, 0.0, "ILS", Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR)),
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val dailySpending: List<DailySpending> = emptyList(),
    val recentTransactions: List<TransactionInfo> = emptyList(),
    val alerts: List<AlertInfo> = emptyList(),
    val cards: List<CardInfo> = emptyList(),
    val selectedCardLast4: String? = null,
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val topMerchants: List<TopMerchant> = emptyList(),
    val monthComparison: MonthComparison? = null,
    val spendingVelocity: SpendingVelocity? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancialDashboardViewModel @Inject constructor(
    private val repository: FinancialRepository,
    private val conversationRepository: ConversationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedCard = MutableStateFlow<String?>(null)
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val uiState: StateFlow<DashboardUiState> = combine(
        _currentMonth, _currentYear, _selectedCard
    ) { month, year, card -> Triple(month, year, card) }
        .flatMapLatest { (month, year, card) ->
            val prevMonth = if (month == 1) 12 else month - 1
            val prevYear = if (month == 1) year - 1 else year

            val coreFlow = combine(
                repository.getMonthlySummary(year, month, card),
                repository.getCategoryBreakdown(year, month, card),
                repository.getDailySpending(year, month, card),
                repository.getRecentTransactionsForMonth(year, month, 10, card),
                repository.getActiveAlerts()
            ) { summary, breakdown, daily, transactions, alerts ->
                Quintuple(summary, breakdown, daily, transactions, alerts)
            }

            val extraFlow = combine(
                repository.getTopMerchants(year, month, card),
                repository.getMonthlySummary(prevYear, prevMonth, card)
            ) { topMerchants, prevSummary -> Pair(topMerchants, prevSummary) }

            combine(coreFlow, extraFlow, repository.getAllCards()) { core, extra, cards ->
                val (summary, breakdown, daily, transactions, alerts) = core
                val (topMerchants, prevSummary) = extra

                val today = Calendar.getInstance()
                val isCurrentMonth = month == today.get(Calendar.MONTH) + 1 && year == today.get(Calendar.YEAR)
                val daysInMonth = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                }.getActualMaximum(Calendar.DAY_OF_MONTH)
                val daysElapsed = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else daysInMonth
                val dailyRate = if (daysElapsed > 0) summary.total / daysElapsed else 0.0
                val velocity = if (isCurrentMonth && summary.count > 0) {
                    SpendingVelocity(
                        dailyRate = dailyRate,
                        projectedMonthTotal = dailyRate * daysInMonth,
                        daysElapsed = daysElapsed,
                        daysInMonth = daysInMonth,
                        currency = summary.currency
                    )
                } else null

                val comparison = if (summary.count > 0 || prevSummary.count > 0) {
                    val pct = if (prevSummary.total > 0)
                        ((summary.total - prevSummary.total) / prevSummary.total) * 100
                    else if (summary.total > 0) 100.0 else 0.0
                    MonthComparison(
                        currentTotal = summary.total,
                        previousTotal = prevSummary.total,
                        percentageChange = pct,
                        currency = summary.currency,
                        currentMonth = month,
                        currentYear = year
                    )
                } else null

                DashboardUiState(
                    monthlySummary = summary,
                    categoryBreakdown = breakdown,
                    dailySpending = daily,
                    recentTransactions = transactions,
                    alerts = alerts,
                    cards = cards,
                    selectedCardLast4 = card,
                    currentMonth = month,
                    currentYear = year,
                    topMerchants = topMerchants,
                    monthComparison = comparison,
                    spendingVelocity = velocity
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun selectCard(last4: String?) {
        _selectedCard.value = last4
    }

    fun previousMonth() {
        if (_currentMonth.value == 1) {
            _currentMonth.value = 12
            _currentYear.value -= 1
        } else {
            _currentMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_currentMonth.value == 12) {
            _currentMonth.value = 1
            _currentYear.value += 1
        } else {
            _currentMonth.value += 1
        }
    }

    fun dismissAlert(id: Long) {
        viewModelScope.launch { repository.dismissAlert(id) }
    }

    fun updateTransactionCategory(merchantName: String, newCategory: String) {
        viewModelScope.launch { repository.updateMerchantCategory(merchantName, newCategory) }
    }

    fun resolveAndNavigateToConversation(address: String, onThreadResolved: (Long) -> Unit) {
        viewModelScope.launch {
            val threadId = conversationRepository.getThreadIdForAddress(address)
            if (threadId > 0) onThreadResolved(threadId)
        }
    }
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
