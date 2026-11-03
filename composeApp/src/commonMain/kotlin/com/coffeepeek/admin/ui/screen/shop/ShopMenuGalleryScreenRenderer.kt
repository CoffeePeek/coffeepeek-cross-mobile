package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Both platforms select the feature gallery. */
interface ShopMenuGalleryScreenRenderer {
    @Composable
    fun Content(shopId: String)
}
