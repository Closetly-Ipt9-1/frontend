package com.m306.closetly.premium.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.m306.closetly.premium.model.PremiumBillingConfig
import com.m306.closetly.premium.model.PremiumOffer
import com.m306.closetly.premium.model.PremiumPlan
import com.m306.closetly.premium.model.SubscriptionStatus
import java.util.Calendar

class PremiumRepository(
    context: Context,
    private val onPurchasesChanged: () -> Unit,
    private val onBillingMessage: (String) -> Unit
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val premiumAccessRepository = PremiumAccessRepository()
    private var productDetails: ProductDetails? = null
    private var selectedPlan: PremiumPlan? = null

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startBillingConnection(
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (billingClient.isReady) {
            onReady()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                } else {
                    onError(billingResult.debugMessage.ifBlank { "Billing is not available." })
                }
            }

            override fun onBillingServiceDisconnected() {
                onError("Billing service disconnected. Please try again.")
            }
        })
    }

    fun loadOffers(
        onSuccess: (List<PremiumOffer>) -> Unit,
        onError: (String) -> Unit
    ) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PremiumBillingConfig.PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onError(billingResult.debugMessage.ifBlank { "Could not load subscription products." })
                return@queryProductDetailsAsync
            }

            val details = result.productDetailsList.firstOrNull()
            productDetails = details

            if (details == null) {
                onError("Subscription product is not configured in Google Play yet.")
                return@queryProductDetailsAsync
            }

            val offers = details.subscriptionOfferDetails.orEmpty().mapNotNull { offerDetails ->
                val plan = PremiumPlan.entries.firstOrNull {
                    it.basePlanId == offerDetails.basePlanId
                } ?: return@mapNotNull null

                PremiumOffer(
                    plan = plan,
                    offerToken = offerDetails.offerToken,
                    formattedPrice = plan.monthlyPrice
                )
            }.sortedBy { it.plan.durationMonths }

            onSuccess(offers)
        }
    }

    fun launchPurchase(
        activity: Activity,
        offer: PremiumOffer,
        onError: (String) -> Unit
    ) {
        val details = productDetails
        if (details == null) {
            onError("Subscription product is not ready yet.")
            return
        }

        selectedPlan = offer.plan
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()

        val billingParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onError(result.debugMessage.ifBlank { "Could not start the purchase flow." })
        }
    }

    fun syncActivePurchases(
        existingStatus: SubscriptionStatus,
        onSuccess: (SubscriptionStatus) -> Unit,
        onError: (String) -> Unit
    ) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onError(billingResult.debugMessage.ifBlank { "Could not restore purchases." })
                return@queryPurchasesAsync
            }

            val activePurchase = purchases.firstOrNull { purchase ->
                PremiumBillingConfig.PRODUCT_ID in purchase.products &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            if (activePurchase == null) {
                val freeStatus = existingStatus.copy(
                    isPremium = false,
                    isAutoRenewing = false,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
                saveSubscriptionStatus(freeStatus, onSuccess, onError)
                return@queryPurchasesAsync
            }

            acknowledgeIfNeeded(activePurchase)

            val plan = selectedPlan ?: existingStatus.selectedPlan ?: PremiumPlan.Monthly
            val premiumStatus = SubscriptionStatus(
                isPremium = true,
                planBaseId = plan.basePlanId,
                productId = PremiumBillingConfig.PRODUCT_ID,
                purchaseToken = activePurchase.purchaseToken,
                purchaseTimeMillis = activePurchase.purchaseTime,
                estimatedExpiryMillis = estimateExpiry(activePurchase.purchaseTime, plan),
                isAutoRenewing = activePurchase.isAutoRenewing,
                lastUpdatedMillis = System.currentTimeMillis()
            )

            saveSubscriptionStatus(premiumStatus, onSuccess, onError)
        }
    }

    fun loadSubscriptionStatus(
        onSuccess: (SubscriptionStatus) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError("User is not logged in.")
            return
        }

        db.collection("users")
            .document(userId)
            .collection("private")
            .document("subscription")
            .get()
            .addOnSuccessListener { document ->
                onSuccess(
                    SubscriptionStatus(
                        isPremium = document.getBoolean("isPremium") ?: false,
                        planBaseId = document.getString("planBaseId"),
                        productId = document.getString("productId"),
                        purchaseToken = document.getString("purchaseToken"),
                        purchaseTimeMillis = document.getLong("purchaseTimeMillis"),
                        estimatedExpiryMillis = document.getLong("estimatedExpiryMillis"),
                        isAutoRenewing = document.getBoolean("isAutoRenewing") ?: false,
                        lastUpdatedMillis = document.getLong("lastUpdatedMillis")
                            ?: System.currentTimeMillis()
                    )
                )
            }
            .addOnFailureListener {
                onError(it.message ?: "Could not load subscription status.")
            }
    }

    fun saveSubscriptionStatus(
        status: SubscriptionStatus,
        onSuccess: (SubscriptionStatus) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError("User is not logged in.")
            return
        }

        val data = mapOf(
            "isPremium" to status.isPremium,
            "planBaseId" to status.planBaseId,
            "productId" to status.productId,
            "purchaseToken" to status.purchaseToken,
            "purchaseTimeMillis" to status.purchaseTimeMillis,
            "estimatedExpiryMillis" to status.estimatedExpiryMillis,
            "isAutoRenewing" to status.isAutoRenewing,
            "lastUpdatedMillis" to status.lastUpdatedMillis
        )

        db.collection("users")
            .document(userId)
            .collection("private")
            .document("subscription")
            .set(data)
            .addOnSuccessListener {
                if (status.isPremium) {
                    onSuccess(status)
                } else {
                    premiumAccessRepository.enforceFreeLimits(
                        onComplete = { onSuccess(status) },
                        onError = { onError(it.message ?: "Could not enforce free limits.") }
                    )
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Could not save subscription status.")
            }
    }

    fun close() {
        billingClient.endConnection()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> onPurchasesChanged()
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                onBillingMessage("Purchase cancelled.")
            }
            else -> onBillingMessage(
                billingResult.debugMessage.ifBlank { "Purchase failed." }
            )
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onBillingMessage(result.debugMessage.ifBlank { "Could not acknowledge purchase." })
            }
        }
    }

    private fun estimateExpiry(purchaseTimeMillis: Long, plan: PremiumPlan): Long {
        return Calendar.getInstance().run {
            timeInMillis = purchaseTimeMillis
            add(Calendar.MONTH, plan.durationMonths)
            timeInMillis
        }
    }
}
