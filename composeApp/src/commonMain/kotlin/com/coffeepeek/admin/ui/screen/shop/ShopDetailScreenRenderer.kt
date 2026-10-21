package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Platform selection only; root navigation and feature business rules remain with their owners. */
interface ShopDetailScreenRenderer {
    @Composable
    fun Content(shopId: String, forCheckIn: Boolean)
}

internal object LegacyShopDetailScreenRenderer : ShopDetailScreenRenderer {
    @Composable
    override fun Content(shopId: String, forCheckIn: Boolean) = ShopDetailScreen(shopId, forCheckIn)
}
