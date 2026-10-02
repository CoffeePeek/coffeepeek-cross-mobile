package com.coffeepeek.feature.shop.domain.model

data class ShopCheckIn(
    val id: String,
    val userId: String,
    val shopId: String,
    val note: String,
    val createdAt: String,
    val visitedAt: String,
    val reviewId: String?,
    val photoUrls: List<String>,
    val photoThumbnailUrls: List<String>,
    val rating: ShopRating?,
)
