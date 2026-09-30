package com.coffeepeek.feature.shop.impl.ui.compose.model

internal sealed interface ShopMenuGalleryAction {
    data object Load : ShopMenuGalleryAction
    data object Back : ShopMenuGalleryAction
    data class OpenPhoto(val index: Int) : ShopMenuGalleryAction
}
