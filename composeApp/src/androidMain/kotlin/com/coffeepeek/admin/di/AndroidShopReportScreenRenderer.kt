package com.coffeepeek.admin.di

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shop.ShopReportScreenRenderer
import com.coffeepeek.feature.shopreport.api.ShopReportEntry

internal class AndroidShopReportScreenRenderer(
    private val entry: ShopReportEntry,
) : ShopReportScreenRenderer {
    @Composable
    override fun Content(shopId: String, shopTitle: String) {
        entry.Content(shopId = shopId, shopTitle = shopTitle, onBack = Navigator::popBack)
    }
}
