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
            override fun onBillingServiceDisconnected() {
                // Connection or service is unavailable, try to reconnect
                retryConnectionWithDelay()
            }

            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Connection successful, can now make requests
                    Log.d(TAG, "Billing connection success - can query and make purchases")
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup error: ${result.debugMessage}")
                }
            }
        })
    }

    private fun retryConnectionWithDelay() {
        scope.launch {
            delay(5000) // 5 seconds delay before retry
            Log.d(TAG, "Retrying billing connection...")
            startBillingConnection()
        }
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
                ).build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                // Process result on main thread
                scope.launch(Dispatchers.Main) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        productDetails = productDetailsList?.firstOrNull()
                        if (productDetails == null) {
                            Log.e(TAG, "Product $PRODUCT_ID not found in Play Console")
                            // TODO showErrorMessage("Voice feature unavailable")
                        } else {
                            Log.d(TAG, "Product details loaded: ${productDetails!!.title}")
                        }
                    } else {
                        Log.e(TAG, "Error loading product details: ${billingResult.debugMessage}")
                    }
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
        if (billingClient.isReady) {
            if (productDetails == null) {
                Log.e(TAG, "Unable to launch purchase - product details not loaded")
                return
            }
            
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails!!)
                    .build()
            )

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient.launchBillingFlow(activity, flowParams)
        } else {
            Log.e(TAG, "Billing client not ready - cannot launch purchase flow")
            // Reconnect and try again
            startBillingConnection()
        }
    }

    // ---------------------------------------------------------------------
    // Purchase results callback
    // ---------------------------------------------------------------------

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) processPurchases(purchases)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
                // TODO showErrorMessage("Purchase canceled")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned – refreshing entitlements")
                queryExistingPurchases()
            }
            else -> Log.e(TAG, "Purchase failed: ${result.debugMessage}")
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
                        // TODO: Implement secure token verification on backend.
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
            billingClient.acknowledgePurchase(params) { billingResult ->
                // Process result on main thread
                scope.launch(Dispatchers.Main) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged")
                        grantEntitlement()
                    } else {
                        Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }

    private fun grantEntitlement() {
        if (!isFeatureUnlocked) {
            isFeatureUnlocked = true
            Log.d(TAG, "Voice feature unlocked!")
            onFeatureUnlocked.invoke()
            // TODO grantVoiceFeatureAccess()
            // TODO updateUiToReflectFeatureState(true)
        }
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
            ) { billingResult, purchasesList ->
                // Process result on main thread
                scope.launch(Dispatchers.Main) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        processPurchases(purchasesList ?: emptyList())
                        if (!isFeatureUnlocked) {
                            // TODO revokeVoiceFeatureAccess()
                            // TODO updateUiToReflectFeatureState(false)
                        }
                    } else {
                        Log.e(TAG, "queryPurchasesAsync failed: ${billingResult.debugMessage}")
                    }
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
