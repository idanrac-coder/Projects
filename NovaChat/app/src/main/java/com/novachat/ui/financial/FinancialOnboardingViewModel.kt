package com.novachat.ui.financial

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.worker.FinancialParsingWorker
import com.novachat.domain.repository.FinancialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FinancialOnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val financialRepository: FinancialRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep

    private val _selectedProviders = MutableStateFlow<Set<String>>(emptySet())
    val selectedProviders: StateFlow<Set<String>> = _selectedProviders.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < 2) _currentStep.value++
    }

    fun previousStep() {
        if (_currentStep.value > 0) _currentStep.value--
    }

    fun toggleProvider(smsAddress: String) {
        _selectedProviders.value = _selectedProviders.value.toMutableSet().apply {
            if (!add(smsAddress)) remove(smsAddress)
        }
    }

    fun completeOnboarding(providers: List<FinancialProvider>, scanInbox: Boolean = false) {
        viewModelScope.launch {
            // Write navigation-gating flags first, non-cancellably, before any slow DB work.
            // popBackStack() clears this ViewModel's scope; withContext(NonCancellable) ensures
            // the DataStore writes complete even if cancellation arrives mid-flight.
            withContext(NonCancellable) {
                userPreferencesRepository.setFinancialOnboardingComplete(true)
                userPreferencesRepository.setFinancialIntelligenceEnabled(true)
            }
            _selectedProviders.value.forEach { addr ->
                val provider = providers.firstOrNull { it.smsAddress == addr }
                financialRepository.addSender(addr, provider?.name)
            }
            FinancialParsingWorker.enqueue(context)
            if (scanInbox) {
                FinancialParsingWorker.enqueueScan(context)
            }
        }
    }
}
