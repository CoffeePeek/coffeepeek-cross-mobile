package com.coffeepeek.feature.shop.impl.ui.compose.model

internal sealed interface ShopMenuGalleryEvent {
    data object Back : ShopMenuGalleryEvent
    data class OpenPhoto(val imageUrls: List<String>, val initialIndex: Int) : ShopMenuGalleryEvent
}
