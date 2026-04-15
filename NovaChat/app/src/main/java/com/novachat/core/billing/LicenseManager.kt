package com.novachat.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.worker.TrialExpiryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class TrialState { NOT_STARTED, ACTIVE, EXPIRED }

@Singleton
class LicenseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "LicenseManager"
        const val PRODUCT_ID = "novachat_lifetime_premium"
        private const val MAX_RETRY_ATTEMPTS = 3
        const val TRIAL_DURATION_MS = 21L * 24 * 60 * 60 * 1000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var retryCount = 0

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _trialState = MutableStateFlow(TrialState.NOT_STARTED)
    val trialState: StateFlow<TrialState> = _trialState.asStateFlow()

    private val _trialDaysRemaining = MutableStateFlow(0)
    val trialDaysRemaining: StateFlow<Int> = _trialDaysRemaining.asStateFlow()

    private val _hasPremiumAccess = MutableStateFlow(false)
    val hasPremiumAccess: StateFlow<Boolean> = _hasPremiumAccess.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _purchaseInProgress = MutableStateFlow(false)
    val purchaseInProgress: StateFlow<Boolean> = _purchaseInProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        scope.launch {
            combine(
                preferencesRepository.isPremium,
                preferencesRepository.trialStartTime
            ) { premium, trialStart ->
                Pair(premium, trialStart)
            }.collect { (premium, trialStart) ->
                _isPremium.value = premium
                val now = System.currentTimeMillis()
                val state = when {
                    trialStart == 0L -> TrialState.NOT_STARTED
                    now < trialStart + TRIAL_DURATION_MS -> TrialState.ACTIVE
                    else -> TrialState.EXPIRED
                }
                _trialState.value = state
                if (state == TrialState.ACTIVE) {
                    val remainingMs = (trialStart + TRIAL_DURATION_MS) - now
                    _trialDaysRemaining.value = (remainingMs / (24 * 60 * 60 * 1000)).toInt() + 1
                } else {
                    _trialDaysRemaining.value = 0
                }
                _hasPremiumAccess.value = premium || state == TrialState.ACTIVE
            }
        }
        connectToGooglePlay()
    }

    suspend fun startTrial() {
        preferencesRepository.startTrial()
        val trialStart = System.currentTimeMillis()
        TrialExpiryWorker.schedule(context, trialStart, TRIAL_DURATION_MS)
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryCount = 0
                    queryProductDetails()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    retryCount++
                    val delayMs = (1000L * (1 shl (retryCount - 1)))
                    Log.w(TAG, "Billing disconnected, retrying in ${delayMs}ms (attempt $retryCount)")
                    scope.launch {
                        delay(delayMs)
                        connectToGooglePlay()
                    }
                } else {
                    Log.e(TAG, "Billing disconnected after $MAX_RETRY_ATTEMPTS retries")
                }
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList.firstOrNull()
            }
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any {
                    it.products.contains(PRODUCT_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                scope.launch {
                    preferencesRepository.setPremium(hasPremium)
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = _productDetails.value ?: return

        _purchaseInProgress.value = true

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun restorePurchases() {
        queryExistingPurchases()
    }

    fun recheckLicense() {
        if (billingClient.isReady) {
            queryExistingPurchases()
        } else {
            connectToGooglePlay()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _purchaseInProgress.value = false

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchase(purchase)
                        scope.launch {
                            preferencesRepository.setPremium(true)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _error.value = null
            }
            else -> {
                _error.value = "Purchase failed. Please try again."
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* logged */ }
        }
    }

    fun consumePurchaseForTesting() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val premiumPurchase = purchases.firstOrNull {
                    it.products.contains(PRODUCT_ID)
                }
                premiumPurchase?.let { purchase ->
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Purchase consumed for testing")
                            scope.launch { preferencesRepository.setPremium(false) }
                        } else {
                            Log.e(TAG, "Failed to consume purchase")
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
