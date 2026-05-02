package com.closetly.myapp.premium.model

object PremiumBillingConfig {
    const val PRODUCT_ID = "closetly_premium"
    const val MONTHLY_BASE_PLAN_ID = "monthly"
    const val YEARLY_BASE_PLAN_ID = "yearly"
    const val THREE_MONTHLY_BASE_PLAN_ID = "three-monthly"
}

enum class PremiumPlan(
    val basePlanId: String,
    val title: String,
    val displayPrice: String,
    val monthlyPrice: String,
    val durationMonths: Int
) {
    Monthly(
        basePlanId = PremiumBillingConfig.MONTHLY_BASE_PLAN_ID,
        title = "Monthly",
        displayPrice = "CHF 9.95",
        monthlyPrice = "CHF 9.95 / month",
        durationMonths = 1
    ),
    ThreeMonthly(
        basePlanId = PremiumBillingConfig.THREE_MONTHLY_BASE_PLAN_ID,
        title = "3 months",
        displayPrice = "CHF 22.50",
        monthlyPrice = "CHF 7.50 / month",
        durationMonths = 3
    ),
    Yearly(
        basePlanId = PremiumBillingConfig.YEARLY_BASE_PLAN_ID,
        title = "12 months",
        displayPrice = "CHF 59.40",
        monthlyPrice = "CHF 4.95 / month",
        durationMonths = 12
    )
}

data class SubscriptionStatus(
    val isPremium: Boolean = false,
    val planBaseId: String? = null,
    val productId: String? = null,
    val purchaseToken: String? = null,
    val purchaseTimeMillis: Long? = null,
    val estimatedExpiryMillis: Long? = null,
    val isAutoRenewing: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    val selectedPlan: PremiumPlan?
        get() = PremiumPlan.entries.firstOrNull { it.basePlanId == planBaseId }
}

data class PremiumOffer(
    val plan: PremiumPlan,
    val offerToken: String,
    val formattedPrice: String
)

data class PremiumUiState(
    val isLoading: Boolean = true,
    val isBillingReady: Boolean = false,
    val status: SubscriptionStatus = SubscriptionStatus(),
    val offers: List<PremiumOffer> = emptyList(),
    val message: String? = null
)
