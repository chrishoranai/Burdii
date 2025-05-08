/*
 * BillingManager.kt
 * ------------------------------------------------------------
 * A dedicated, reusable helper for integrating one-time / non-consumable
 * Google Play Billing purchases (Billing Library 6+) in Kotlin.
 *
 * Product configured in Google Play Console:
 * ------------------------------------------------------------
 *   • ID  : "com.app.burdii.voice_feature"
 *   • Type: INAPP (one-time, non-consumable)
 *   • Price: $1.99 (one-time purchase)
 *
 * Usage Example (inside an Activity or Fragment):
 * ------------------------------------------------------------
 * private lateinit var billingManager: BillingManager
 *
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     ...
 *     billingManager = BillingManager(this /* Activity */) {
 *         // Feature unlocked callback → enable relevant UI
 *         updateUiToReflectFeatureState(true)
 *     }
 *     billingManager.startBillingConnection()
 * }
 *
 * purchaseButton.setOnClickListener {
 *     billingManager.startPurchaseFlow(this) // launches Google purchase UI
 * }
 *
 * Remember to call billingManager.destroy() in onDestroy() to release connection.
 * ------------------------------------------------------------
 * IMPORTANT GOOGLE PLAY POLICY REMINDERS (developer responsibility):
 * ------------------------------------------------------------
 *   • Display clear purchase terms BEFORE launching the purchase flow:
 *       – What exactly is unlocked ("Voice Scorekeeping").
 *       – Price ("$1.99 one-time fee").
 *       – Permanence (feature unlocked forever on this account).
 *   • Provide a way for users to restore purchases (handled automatically
 *     by queryExistingPurchases() on app start, but you should also offer
 *     a "Restore" button calling it explicitly so the user feels in control).
 */

/*
package com.app.burdii

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import com.android.billingclient.api.*
import kotlinx.coroutines.*

/**
 * A thin wrapper around [BillingClient] that handles:
 *   • Connecting / reconnecting & lifecycle.
 *   • Querying product details for ONE non-consumable item.
 *   • Launching the billing flow.
 *   • Handling purchase updates, verification placeholder & acknowledgement.
 *   • Exposing callbacks for UI updates and unlock persistence.
 */
class BillingManager(
    private val appContext: Context,
    /**
     * Called when the purchase is verified & acknowledged so the feature
     * should be enabled for the user interface & local storage.
     */
    private val onFeatureUnlocked: () -> Unit = {}
) : PurchasesUpdatedListener {

    companion object {
        /**
         * Product ID for voice feature unlock - configured in Google Play Console
         * as a non-consumable in-app purchase
         */
        const val PRODUCT_ID = "com.app.burdii.voice_feature"
        private const val TAG = "BillingManager"
    }

    /** Coroutine scope for background billing work */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Google Play Billing client instance */
    private var billingClient: BillingClient = BillingClient
        .newBuilder(appContext)
        .enablePendingPurchases() // Required by library policy
        .setListener(this)
        .build()

    /** Cached ProductDetails after query */
    var productDetails: ProductDetails? = null
        private set

    /** Public flag: feature unlocked? Persist as needed */
    var isFeatureUnlocked: Boolean = false
        private set

    // ---------------------------------------------------------------------
    // Connection
    // ---------------------------------------------------------------------

    /** Call once (e.g., during app launch) */
    fun startBillingConnection() {
        if (billingClient.isReady) return // Already connected.

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connection established")
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // Retry connection after a delay
                startBillingConnection()
            }
        })
    }

    // ---------------------------------------------------------------------
    // Product details
    // ---------------------------------------------------------------------

    private fun queryProductDetails() {
        scope.launch {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (productDetailsList.isNotEmpty()) {
                        productDetails = productDetailsList[0]
                        Log.d(TAG, "Product details retrieved: ${productDetails?.description}")
                    } else {
                        Log.w(TAG, "No product details found")
                    }
                } else {
                    Log.e(TAG, "Error loading product details: ${billingResult.debugMessage}")
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Purchase flow
    // ---------------------------------------------------------------------

    /**
     * Call this to launch the purchase flow dialog
     */
    fun startPurchaseFlow(activity: Activity) {
        // Verify billing client is ready
        if (!billingClient.isReady) {
            Log.e(TAG, "Billing client not ready")
            startBillingConnection()
            return
        }

        // Verify product details are available
        val details = productDetails
        if (details == null) {
            Log.e(TAG, "Product details unavailable, requerying...")
            queryProductDetails()
            return
        }

        // Create purchase params and launch the flow
        try {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching billing flow", e)
        }
    }

    // ---------------------------------------------------------------------
    // Purchase results callback
    // ---------------------------------------------------------------------

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    processPurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.i(TAG, "User canceled the purchase")
            else ->
                Log.e(TAG, "Purchase error: ${result.debugMessage}")
        }
    }

    // ---------------------------------------------------------------------
    // Purchase handling helpers
    // ---------------------------------------------------------------------

    private fun processPurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.products.contains(PRODUCT_ID)) {
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        handleAcknowledgement(purchase)
                    }
                    Purchase.PurchaseState.PENDING -> {
                        Log.d(TAG, "Purchase pending. Awaiting completion")
                    }
                    else -> Log.w(TAG, "Unhandled purchase state: ${purchase.purchaseState}")
                }
            }
        }
    }

    private fun handleAcknowledgement(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            grantEntitlement()
            return
        }
        scope.launch {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val result = billingClient.acknowledgePurchase(params)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
                grantEntitlement()
            } else {
                Log.e(TAG, "Error acknowledging purchase: ${result.debugMessage}")
            }
        }
    }

    @MainThread
    private fun grantEntitlement() {
        isFeatureUnlocked = true
        onFeatureUnlocked()
        Log.d(TAG, "Premium feature unlocked")
    }

    // ---------------------------------------------------------------------
    // Restore / query existing purchases
    // ---------------------------------------------------------------------

    fun queryExistingPurchases() {
        scope.launch {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (purchases.isNotEmpty()) {
                        processPurchases(purchases)
                    } else {
                        Log.d(TAG, "No existing purchases found")
                    }
                } else {
                    Log.e(TAG, "Error querying purchases: ${billingResult.debugMessage}")
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------------------

    fun destroy() {
        Log.d(TAG, "Ending billing connection")
        if (billingClient.isReady) billingClient.endConnection()
        scope.cancel()
    }
}
*/

package com.app.burdii

import android.app.Activity
import android.content.Context

/**
 * Implementation of BillingManager for the free phase.
 * This version provides all premium features at no cost.
 * Billing functionality will be implemented in a future update.
 */
class BillingManager(
    private val context: Context,
    private val onFeatureUnlocked: () -> Unit = {}
) {
    companion object {
        const val PRODUCT_ID = "com.app.burdii.voice_feature"
    }
    
    var isFeatureUnlocked = false
        private set
        
    fun startBillingConnection() {
        // Free phase implementation - enable all features
        isFeatureUnlocked = true
        onFeatureUnlocked.invoke()
    }
    
    fun queryExistingPurchases() {
        // Free phase implementation - all features available
        isFeatureUnlocked = true
        onFeatureUnlocked.invoke()
    }
    
    fun startPurchaseFlow(activity: Activity) {
        // Free phase implementation - features already enabled
        // Will be replaced with actual purchase flow in future
    }
    
    fun destroy() {
        // Free phase implementation - cleanup placeholder
    }
}
