package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Application composition selects the platform-appropriate Shop Report entry. */
interface ShopReportScreenRenderer {
    @Composable
    fun Content(shopId: String, shopTitle: String)
}
