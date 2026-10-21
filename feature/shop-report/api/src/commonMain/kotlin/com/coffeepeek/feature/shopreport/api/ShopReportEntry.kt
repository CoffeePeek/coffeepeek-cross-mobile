package com.coffeepeek.feature.shopreport.api

import androidx.compose.runtime.Composable

/** Public feature entry. The caller retains ownership of the app back stack. */
interface ShopReportEntry {
    @Composable
    fun Content(shopId: String, shopTitle: String, onBack: () -> Unit)
}
