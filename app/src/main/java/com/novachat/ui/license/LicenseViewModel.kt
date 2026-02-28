package com.novachat.ui.license

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.novachat.core.billing.LicenseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseManager: LicenseManager
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = licenseManager.isPremium
    val purchaseInProgress: StateFlow<Boolean> = licenseManager.purchaseInProgress
    val error: StateFlow<String?> = licenseManager.error

    val formattedPrice: String
        get() {
            val details = licenseManager.productDetails.value
            return details?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$9.99"
        }

    fun purchase(activity: Activity) {
        licenseManager.launchPurchaseFlow(activity)
    }

    fun restorePurchase() {
        licenseManager.restorePurchases()
    }

    fun clearError() {
        licenseManager.clearError()
    }
}
