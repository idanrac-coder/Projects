package com.novachat.ui.license

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novachat.core.billing.LicenseManager
import com.novachat.core.billing.TrialState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseManager: LicenseManager
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = licenseManager.isPremium
    val hasPremiumAccess: StateFlow<Boolean> = licenseManager.hasPremiumAccess
    val trialState: StateFlow<TrialState> = licenseManager.trialState
    val trialDaysRemaining: StateFlow<Int> = licenseManager.trialDaysRemaining
    val purchaseInProgress: StateFlow<Boolean> = licenseManager.purchaseInProgress
    val error: StateFlow<String?> = licenseManager.error

    val formattedPrice: String
        get() {
            val details = licenseManager.productDetails.value
            return details?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$4.99"
        }

    fun startTrial() {
        viewModelScope.launch {
            licenseManager.startTrial()
        }
    }

    fun purchase(activity: Activity) {
        licenseManager.launchPurchaseFlow(activity)
    }

    fun restorePurchase() {
        licenseManager.restorePurchases()
    }

    fun resetLicenseForTesting() {
        licenseManager.consumePurchaseForTesting()
    }

    fun clearError() {
        licenseManager.clearError()
    }
}
