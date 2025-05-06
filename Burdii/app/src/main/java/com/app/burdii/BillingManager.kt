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
/*
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
*/

/*
package com.app.burdii

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

// Original content of BillingManager.kt is now inside this block comment.
// This class was responsible for handling Google Play Billing.
// It has been temporarily disabled for the initial Play Store upload
// to acquire a Billing ID without active billing functionality.

class BillingManager_DEACTIVATED(private val context: Context, private val onFeatureUnlocked: () -> Unit) {
    private lateinit var billingClient: BillingClient
    private val productIds = listOf("com.app.burdii.voice_feature") 
    var isFeatureUnlocked = false
        private set

    companion object {
        private const val TAG = "BillingManager"
    }

    init {
        // setupBillingClient()
    }

    private fun setupBillingClient() {
        // billingClient = BillingClient.newBuilder(context)
        //     .setListener(purchasesUpdatedListener)
        //     .enablePendingPurchases()
        //     .build()
    }

    fun startBillingConnection() {
        // billingClient.startConnection(object : BillingClientStateListener {
        //     override fun onBillingSetupFinished(billingResult: BillingResult) {
        //         if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
        //             Log.d(TAG, "Billing client setup finished successfully.")
        //             queryProductDetails()
        //             queryExistingPurchases() 
        //         } else {
        //             Log.e(TAG, "Billing client setup failed: ${billingResult.debugMessage}")
        //         }
        //     }

        //     override fun onBillingServiceDisconnected() {
        //         Log.w(TAG, "Billing service disconnected. Retrying connection...")
        //     }
        // })
    }

    private fun queryProductDetails() {
        // val productList = productIds.map { productId ->
        //     QueryProductDetailsParams.Product.newBuilder()
        //         .setProductId(productId)
        //         .setProductType(BillingClient.ProductType.INAPP)
        //         .build()
        // }

        // val params = QueryProductDetailsParams.newBuilder()
        //     .setProductList(productList)
        //     .build()

        // billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
        //     if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
        //         Log.d(TAG, "Product details queried successfully: $productDetailsList")
        //     } else {
        //         Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
        //     }
        // }
    }

    fun startPurchaseFlow(activity: Activity) {
        // if (!billingClient.isReady) {
        //     Log.e(TAG, "Billing client not ready.")
        //     return
        // }

        // val productList = productIds.map { productId ->
        //     QueryProductDetailsParams.Product.newBuilder()
        //         .setProductId(productId)
        //         .setProductType(BillingClient.ProductType.INAPP)
        //         .build()
        // }
        // val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

        // billingClient.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->
        //     if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
        //         val productDetails = productDetailsList.find { it.productId == productIds.first() } 
        //         if (productDetails != null) {
        //             val flowParams = BillingFlowParams.newBuilder()
        //                 .setProductDetailsParamsList(
        //                     listOf(
        //                         BillingFlowParams.ProductDetailsParams.newBuilder()
        //                             .setProductDetails(productDetails)
        //                             .build()
        //                     )
        //                 )
        //                 .build()
        //             val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
        //             if (responseCode != BillingClient.BillingResponseCode.OK) {
        //                 Log.e(TAG, "Failed to launch billing flow: $responseCode")
        //             }
        //         } else {
        //             Log.e(TAG, "Product details not found for ${productIds.first()}.")
        //         }
        //     } else {
        //         Log.e(TAG, "Failed to query product details for purchase: ${billingResult.debugMessage}")
        //     }
        // }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener {
            // billingResult, purchases ->
        // if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
        //     for (purchase in purchases) {
        //         handlePurchase(purchase)
        //     }
        // } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
        //     Log.i(TAG, "User canceled the purchase.")
        // } else {
        //     Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
        // }
    }

    private fun handlePurchase(purchase: Purchase) {
        // if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
        //     if (!purchase.isAcknowledged) {
        //         acknowledgePurchase(purchase.purchaseToken)
        //     }
        //     if (productIds.contains(purchase.products.firstOrNull())) { 
        //         grantEntitlement()
        //     }
        // } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
        //     Log.i(TAG, "Purchase is pending. Please wait for completion.")
        // }
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        // val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
        //     .setPurchaseToken(purchaseToken)
        //     .build()
        // billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
        //     if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
        //         Log.d(TAG, "Purchase acknowledged successfully.")
        //     } else {
        //         Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
        //     }
        // }
    }

    private fun grantEntitlement() {
        // isFeatureUnlocked = true
        // onFeatureUnlocked.invoke() 
        // Log.d(TAG, "Feature unlocked.")
    }

    fun queryExistingPurchases() {
        // if (!billingClient.isReady) {
        //     Log.e(TAG, "Billing client not ready for querying purchases.")
        //     return
        // }

        // billingClient.queryPurchasesAsync(
        //     QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        // ) { billingResult, purchasesList ->
        //     if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
        //         var featureAlreadyUnlocked = false
        //         for (purchase in purchasesList) {
        //             if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && productIds.contains(purchase.products.firstOrNull())) {
        //                 if (!isFeatureUnlocked) { 
        //                     grantEntitlement()
        //                 }
        //                 featureAlreadyUnlocked = true 
        //                 if (!purchase.isAcknowledged) {
        //                     acknowledgePurchase(purchase.purchaseToken) 
        //                 }
        //             }
        //         }
        //         if (featureAlreadyUnlocked) {
        //             Log.d(TAG, "Existing purchase found and feature restored.")
        //         } else {
        //             Log.d(TAG, "No existing purchases found for the feature or feature already marked as unlocked.")
        //         }
        //         if (!featureAlreadyUnlocked && isFeatureUnlocked) {
        //             isFeatureUnlocked = false
        //             Log.d(TAG, "Feature marked as locked as no valid existing purchase found.")
        //         }

        //     } else {
        //         Log.e(TAG, "Failed to query existing purchases: ${billingResult.debugMessage}")
        //     }
        // }
    }

    fun destroy() {
        // if (billingClient.isReady) {
        //     billingClient.endConnection()
        //     Log.d(TAG, "Billing client connection ended.")
        // }
    }
}
*/
