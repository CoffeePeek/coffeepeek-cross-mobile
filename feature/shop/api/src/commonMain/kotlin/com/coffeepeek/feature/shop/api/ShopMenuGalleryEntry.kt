package com.coffeepeek.feature.shop.api

import androidx.compose.runtime.Composable

/** Root navigation and the existing photo viewer stay with application composition. */
interface ShopMenuGalleryEntry {
    @Composable
    fun Content(
        shopId: String,
        onBack: () -> Unit,
        onOpenPhoto: (imageUrls: List<String>, initialIndex: Int) -> Unit,
    )
}
