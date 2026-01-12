package com.termex.app.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val billingClient: BillingClient
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.LOADING)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()
    
    companion object {
        private const val TAG = "SubscriptionManager"
        const val PRODUCT_ID = "termex_pro_subscription"
        
        // Keep static instance for PaywallScreen access (temporary compatibility)
        @Volatile
        private var instance: SubscriptionManager? = null
        
        fun getInstance(context: Context): SubscriptionManager {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    init {
        instance = this
        
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
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing client connected")
                    querySubscriptionStatus()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _subscriptionState.value = SubscriptionState.ERROR("Setup failed")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                startConnection()
            }
        })
    }
    
    fun querySubscriptionStatus() {
        if (!billingClient.isReady) {
            Log.w(TAG, "Billing client not ready")
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
                Log.e(TAG, "Query purchases failed: ${billingResult.debugMessage}")
                _subscriptionState.value = SubscriptionState.ERROR(billingResult.debugMessage)
            }
        }
    }
    
    suspend fun getProductDetails(): ProductDetails? = suspendCancellableCoroutine { continuation ->
        if (!billingClient.isReady) {
            Log.w(TAG, "Billing client not ready")
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
                Log.e(TAG, "Query product details failed: ${billingResult.debugMessage}")
                continuation.resume(null)
            }
        }
    }
    
    fun launchSubscriptionFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        
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
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Purchase acknowledged")
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
