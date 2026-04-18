package com.novachat.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.CardInfo
import com.novachat.domain.repository.FinancialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardManagerUiState(
    val cards: List<CardInfo> = emptyList()
)

@HiltViewModel
class CardManagerViewModel @Inject constructor(
    private val repository: FinancialRepository
) : ViewModel() {

    val uiState: StateFlow<CardManagerUiState> = repository.getAllCards()
        .map { CardManagerUiState(cards = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardManagerUiState())

    fun updateNickname(last4: String, nickname: String) {
        viewModelScope.launch { repository.updateCardNickname(last4, nickname) }
    }

    fun setHidden(last4: String, hidden: Boolean) {
        viewModelScope.launch { repository.setCardHidden(last4, hidden) }
    }

    fun addCard(last4: String, nickname: String?) {
        viewModelScope.launch { repository.addCard(last4, nickname) }
    }

    fun deleteCard(last4: String) {
        viewModelScope.launch { repository.deleteCard(last4) }
    }

    fun moveUp(last4: String) {
        val cards = uiState.value.cards
        val idx = cards.indexOfFirst { it.last4 == last4 }
        if (idx <= 0) return
        val newOrder = cards.toMutableList().also {
            val tmp = it[idx]; it[idx] = it[idx - 1]; it[idx - 1] = tmp
        }.map { it.last4 }
        viewModelScope.launch { repository.reorderCards(newOrder) }
    }

    fun moveDown(last4: String) {
        val cards = uiState.value.cards
        val idx = cards.indexOfFirst { it.last4 == last4 }
        if (idx < 0 || idx >= cards.size - 1) return
        val newOrder = cards.toMutableList().also {
            val tmp = it[idx]; it[idx] = it[idx + 1]; it[idx + 1] = tmp
        }.map { it.last4 }
        viewModelScope.launch { repository.reorderCards(newOrder) }
    }
}
