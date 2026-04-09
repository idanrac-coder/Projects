package com.novachat.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.AlertInfo
import com.novachat.domain.model.CardInfo
import com.novachat.domain.model.CategoryBreakdown
import com.novachat.domain.model.DailySpending
import com.novachat.domain.model.MonthlySummary
import com.novachat.domain.model.TransactionInfo
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
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancialDashboardViewModel @Inject constructor(
    private val repository: FinancialRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedCard = MutableStateFlow<String?>(null)
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val uiState: StateFlow<DashboardUiState> = combine(
        _currentMonth, _currentYear, _selectedCard
    ) { month, year, card -> Triple(month, year, card) }
        .flatMapLatest { (month, year, card) ->
            combine(
                repository.getMonthlySummary(year, month, card),
                repository.getCategoryBreakdown(year, month, card),
                repository.getDailySpending(year, month, card),
                repository.getRecentTransactions(20, card),
                repository.getActiveAlerts()
            ) { summary, breakdown, daily, transactions, alerts ->
                DashboardUiState(
                    monthlySummary = summary,
                    categoryBreakdown = breakdown,
                    dailySpending = daily,
                    recentTransactions = transactions,
                    alerts = alerts,
                    selectedCardLast4 = card,
                    currentMonth = month,
                    currentYear = year
                )
            }.combine(repository.getAllCards()) { state, cards ->
                state.copy(cards = cards)
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
}
