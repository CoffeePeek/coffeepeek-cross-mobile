package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Shared route adapter keeps the untouched iOS gallery on the legacy path. */
interface ShopMenuGalleryScreenRenderer {
    @Composable
    fun Content(shopId: String)
}

internal object LegacyShopMenuGalleryScreenRenderer : ShopMenuGalleryScreenRenderer {
    @Composable
    override fun Content(shopId: String) {
        ShopMenuGalleryScreen(shopId)
    }
}
