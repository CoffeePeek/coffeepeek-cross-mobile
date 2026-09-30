package com.coffeepeek.feature.shop.domain.model

data class MenuGallery(
    val shopTitle: String,
    val photos: List<MenuPhoto>,
)

data class MenuPhoto(
    val id: String,
    val fullUrl: String,
    val previewUrl: String = fullUrl,
    val sortIndex: Int = 0,
)
