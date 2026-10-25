package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Both platforms select the feature gallery; the legacy screen remains a fallback. */
interface ShopMenuGalleryScreenRenderer {
    @Composable
    fun Content(shopId: String)

    /** Optional legacy affordance; the migrated detail owns its menu-photo actions. */
    @Composable
    fun MenuGalleryAction(shopId: String) {}
}

internal object LegacyShopMenuGalleryScreenRenderer : ShopMenuGalleryScreenRenderer {
    @Composable
    override fun Content(shopId: String) {
        ShopMenuGalleryScreen(shopId)
    }
}
