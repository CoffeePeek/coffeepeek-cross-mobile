package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** ShopReviews is the existing route name for public check-ins, not a review editor. */
interface ShopCheckInListScreenRenderer {
    @Composable
    fun Content(shopId: String)
}

internal object LegacyShopCheckInListScreenRenderer : ShopCheckInListScreenRenderer {
    @Composable
    override fun Content(shopId: String) = ShopReviewsScreen(shopId)
}
