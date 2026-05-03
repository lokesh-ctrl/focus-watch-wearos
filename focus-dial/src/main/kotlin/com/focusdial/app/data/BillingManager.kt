package com.focusdial.app.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val preferences = FocusPreferences(context)

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        scope.launch {
            _isPro.value = preferences.isPro()
        }
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { restorePurchases() }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry on next user action
            }
        })
    }

    suspend fun launchPurchaseFlow(activity: Activity): Boolean {
        if (!billingClient.isReady) {
            connect()
            return false
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result: ProductDetailsResult = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return false
        }

        val productDetails = result.productDetailsList?.firstOrNull() ?: return false

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()

        val flowResult = billingClient.launchBillingFlow(activity, flowParams)
        return flowResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    private suspend fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasPro = result.purchasesList.any { purchase ->
                purchase.products.contains(PRODUCT_PRO) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasPro) {
                preferences.setPro(true)
                _isPro.value = true
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val ackParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams)
            }
            if (purchase.products.contains(PRODUCT_PRO)) {
                preferences.setPro(true)
                _isPro.value = true
            }
        }
    }

    companion object {
        const val PRODUCT_PRO = "focus_dial_pro"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
