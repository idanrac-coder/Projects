package com.novachat.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.domain.model.AlertInfo
import com.novachat.domain.repository.FinancialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<AlertInfo> = emptyList(),
    val activeCount: Int = 0
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: FinancialRepository
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = combine(
        repository.getActiveAlerts(),
        repository.getAlertCount()
    ) { alerts, count ->
        AlertsUiState(alerts = alerts, activeCount = count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertsUiState())

    fun dismissAlert(id: Long) {
        viewModelScope.launch { repository.dismissAlert(id) }
    }
}
