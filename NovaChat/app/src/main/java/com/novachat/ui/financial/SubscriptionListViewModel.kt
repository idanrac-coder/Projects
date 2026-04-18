package com.novachat.ui.financial

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.CardInfo
import com.novachat.domain.model.InsightType
import com.novachat.domain.model.SubscriptionSummary
import com.novachat.domain.repository.FinancialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val INSIGHTS_KEY = stringSetPreferencesKey("subscription_insights")
private val DEFAULT_INSIGHTS = setOf(InsightType.ANNUAL_COST.name, InsightType.PRICE_CHANGES.name)

data class SubscriptionListUiState(
    val subscriptions: List<SubscriptionSummary> = emptyList(),
    val total: Double = 0.0,
    val annualProjection: Double = 0.0,
    val priceChangeCount: Int = 0,
    val upcomingRenewal: SubscriptionSummary? = null,
    val staleCount: Int = 0,
    val cards: List<CardInfo> = emptyList(),
    val selectedCardLast4: String? = null,
    val enabledInsights: Set<InsightType> = setOf(InsightType.ANNUAL_COST, InsightType.PRICE_CHANGES)
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    private val repository: FinancialRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _selectedCard = MutableStateFlow<String?>(null)

    private val savedInsights = dataStore.data.map { prefs ->
        (prefs[INSIGHTS_KEY] ?: DEFAULT_INSIGHTS)
            .mapNotNull { runCatching { InsightType.valueOf(it) }.getOrNull() }
            .toSet()
    }

    val uiState: StateFlow<SubscriptionListUiState> = _selectedCard
        .flatMapLatest { card ->
            combine(
                repository.getSubscriptionsFromCategory(card),
                repository.getSubscriptionTotalFromCategory(card),
                repository.getAllCards(),
                savedInsights
            ) { subs, total, cards, insights ->
                val staleCutoff = System.currentTimeMillis() - 45L * 24 * 60 * 60 * 1000
                SubscriptionListUiState(
                    subscriptions = subs,
                    total = total,
                    annualProjection = total * 12,
                    priceChangeCount = subs.count {
                        it.previousAmount != null && it.amount != it.previousAmount
                    },
                    upcomingRenewal = subs
                        .filter { it.nextChargeEstimate != null }
                        .minByOrNull { it.nextChargeEstimate!! },
                    staleCount = subs.count { it.lastCharged < staleCutoff },
                    cards = cards,
                    selectedCardLast4 = card,
                    enabledInsights = insights
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionListUiState())

    fun selectCard(last4: String?) {
        _selectedCard.value = last4
    }

    fun toggleInsight(type: InsightType) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                val current = prefs[INSIGHTS_KEY] ?: DEFAULT_INSIGHTS
                prefs[INSIGHTS_KEY] = if (type.name in current) current - type.name else current + type.name
            }
        }
    }
}
