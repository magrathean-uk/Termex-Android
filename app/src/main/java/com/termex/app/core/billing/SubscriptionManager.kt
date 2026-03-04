package com.termex.app.core.billing

import android.app.Activity
import android.util.Log
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.min

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val billingClient: BillingClient
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.LOADING)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()
    private var reconnectAttempts = 0
    private val handler = Handler(Looper.getMainLooper())
    
    init {
        
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                handlePurchase(billingResult, purchases)
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()
        
        startConnection()
    }
    
    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                reconnectAttempts = 0
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptionStatus()
                } else {
                    _subscriptionState.value = SubscriptionState.ERROR("Billing setup failed. Please try again later.")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    val delayMs = min(1000L shl reconnectAttempts, 32_000L)
                    reconnectAttempts++
                    handler.postDelayed({ startConnection() }, delayMs)
                }
            }
        })
    }

    companion object {
        const val PRODUCT_ID = "termex_pro_subscription"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
    
    fun querySubscriptionStatus() {
        if (!billingClient.isReady) {
            return
        }
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchase = purchases.find { purchase ->
                    purchase.products.contains(PRODUCT_ID) && 
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                if (activePurchase != null) {
                    if (!activePurchase.isAcknowledged) {
                        acknowledgePurchase(activePurchase)
                    }
                    _subscriptionState.value = SubscriptionState.SUBSCRIBED(activePurchase)
                } else {
                    _subscriptionState.value = SubscriptionState.NOT_SUBSCRIBED
                }
            } else {
                _subscriptionState.value = SubscriptionState.ERROR(billingResult.debugMessage)
            }
        }
    }
    
    suspend fun getProductDetails(): ProductDetails? = suspendCancellableCoroutine { continuation ->
        if (!billingClient.isReady) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(productDetailsList.firstOrNull())
            } else {
                continuation.resume(null)
            }
        }
    }
    
    fun launchSubscriptionFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = selectPreferredOffer(productDetails)?.offerToken ?: return
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun selectPreferredOffer(
        productDetails: ProductDetails
    ): ProductDetails.SubscriptionOfferDetails? {
        val offers = productDetails.subscriptionOfferDetails ?: return null
        return offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: offers.firstOrNull()
    }
    
    private fun handlePurchase(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                    _subscriptionState.value = SubscriptionState.SUBSCRIBED(purchase)
                }
            }
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w("SubscriptionManager", "acknowledgePurchase failed: ${billingResult.debugMessage} (code ${billingResult.responseCode})")
            }
        }
    }
}

sealed class SubscriptionState {
    data object LOADING : SubscriptionState()
    data object NOT_SUBSCRIBED : SubscriptionState()
    data class SUBSCRIBED(val purchase: Purchase) : SubscriptionState()
    data class ERROR(val message: String) : SubscriptionState()
}
