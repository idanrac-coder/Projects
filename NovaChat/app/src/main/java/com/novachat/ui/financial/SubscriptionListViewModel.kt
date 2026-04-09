package com.novachat.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.CardInfo
import com.novachat.domain.model.SubscriptionInfo
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
import javax.inject.Inject

data class SubscriptionListUiState(
    val subscriptions: List<SubscriptionInfo> = emptyList(),
    val total: Double = 0.0,
    val cards: List<CardInfo> = emptyList(),
    val selectedCardLast4: String? = null,
    val activeCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    private val repository: FinancialRepository
) : ViewModel() {

    private val _selectedCard = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SubscriptionListUiState> = _selectedCard
        .flatMapLatest { card ->
            combine(
                repository.getActiveSubscriptions(card),
                repository.getSubscriptionTotal(card),
                repository.getAllCards()
            ) { subs, total, cards ->
                SubscriptionListUiState(
                    subscriptions = subs,
                    total = total,
                    cards = cards,
                    selectedCardLast4 = card,
                    activeCount = subs.size
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionListUiState())

    fun selectCard(last4: String?) {
        _selectedCard.value = last4
    }

    fun markInactive(id: Long) {
        viewModelScope.launch { repository.markSubscriptionInactive(id) }
    }
}
