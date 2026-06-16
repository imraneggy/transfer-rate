package com.transferrate.app.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
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

/**
 * Manages Google Play Billing for the Pro subscription.
 *
 * Pro SKU: "transfer_rate_pro_monthly" (subscription, $0.99/month).
 *
 * State machine:
 *   - App starts → connect() called from MainActivity.
 *   - On connected → query existing purchases → set isProActive.
 *   - launchBillingFlow() triggers Play purchase UI.
 *   - On purchase acknowledged → isProActive = true.
 *
 * Pro features unlocked:
 *   - Up to 3 simultaneous rate-target alerts (free: 1).
 */
class BillingRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isProActive = MutableStateFlow(false)
    val isProActive: StateFlow<Boolean> = _isProActive.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var proProductDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handlePurchases(purchases) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun connect() {
        if (billingClient.isReady) return
        _connectionState.value = ConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = ConnectionState.CONNECTED
                    scope.launch {
                        queryExistingPurchases()
                        queryProductDetails()
                    }
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
            override fun onBillingServiceDisconnected() {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        })
    }

    fun launchBillingFlow(activity: Activity): BillingResult {
        val details = proProductDetails
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Product details not loaded yet")
                .build()

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("No offer token available")
                .build()

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, flowParams)
    }

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchases(result.purchasesList)
        }
    }

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            proProductDetails = result.productDetailsList?.firstOrNull()
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        var hasActivePro = false
        for (purchase in purchases) {
            if (PRO_SKU in purchase.products && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                hasActivePro = true
                if (!purchase.isAcknowledged) {
                    val ackParams = com.android.billingclient.api.AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams)
                }
            }
        }
        _isProActive.value = hasActivePro
    }

    /** Cached price string for display in the upgrade screen ("$0.99/month"). */
    fun priceString(): String? {
        val details = proProductDetails ?: return null
        return details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

    companion object {
        const val PRO_SKU = "transfer_rate_pro_monthly"
        const val PRO_MAX_TARGETS = 3
        const val FREE_MAX_TARGETS = 1
    }
}
