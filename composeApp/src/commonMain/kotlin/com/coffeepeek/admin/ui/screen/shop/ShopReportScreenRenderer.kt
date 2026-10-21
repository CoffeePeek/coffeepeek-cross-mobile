package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Application composition selects the platform-appropriate Shop Report entry. */
interface ShopReportScreenRenderer {
    @Composable
    fun Content(shopId: String, shopTitle: String)
}

/** Keeps the existing shared/iOS flow active until that platform is migrated. */
internal object LegacyShopReportScreenRenderer : ShopReportScreenRenderer {
    @Composable
    override fun Content(shopId: String, shopTitle: String) {
        ShopReportScreen(shopId = shopId, shopTitle = shopTitle)
    }
}
