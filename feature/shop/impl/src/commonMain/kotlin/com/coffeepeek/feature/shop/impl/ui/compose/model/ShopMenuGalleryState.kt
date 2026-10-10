package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.MenuPhoto

internal data class ShopMenuGalleryState(
    val shopTitle: String = "",
    val photos: List<MenuPhoto> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)
