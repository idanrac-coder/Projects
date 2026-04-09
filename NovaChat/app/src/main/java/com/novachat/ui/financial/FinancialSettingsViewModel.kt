package com.novachat.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.domain.model.SenderInfo
import com.novachat.domain.repository.FinancialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinancialSettingsUiState(
    val isEnabled: Boolean = false,
    val primaryCurrency: String = "ILS",
    val cardCount: Int = 0,
    val senders: List<SenderInfo> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap()
)

@HiltViewModel
class FinancialSettingsViewModel @Inject constructor(
    private val repository: FinancialRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<FinancialSettingsUiState> = combine(
        userPreferencesRepository.financialIntelligenceEnabled,
        userPreferencesRepository.financialPrimaryCurrency,
        repository.getCardCount(),
        repository.getAllSenders(),
        repository.getCategoryCounts()
    ) { enabled, currency, cardCount, senders, categoryCounts ->
        FinancialSettingsUiState(
            isEnabled = enabled,
            primaryCurrency = currency,
            cardCount = cardCount,
            senders = senders,
            categoryCounts = categoryCounts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSettingsUiState())

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setFinancialIntelligenceEnabled(enabled) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { userPreferencesRepository.setFinancialPrimaryCurrency(currency) }
    }

    fun setSenderEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setSenderEnabled(id, enabled) }
    }

    fun setSenderAlertsEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setSenderAlertsEnabled(id, enabled) }
    }

    fun addSender(address: String, displayName: String?) {
        viewModelScope.launch { repository.addSender(address, displayName) }
    }

    fun clearData() {
        viewModelScope.launch { repository.clearAllFinancialData() }
    }
}
