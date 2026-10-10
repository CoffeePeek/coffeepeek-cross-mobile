package com.coffeepeek.feature.shop.domain.model

data class ShopReviewCreateInput(
    val shopId: String,
    val header: String,
    val comment: String,
    val rating: ShopRating,
    val photos: List<ShopReviewPhoto> = emptyList(),
)

/** Current update API accepts text/rating only; adding photos is not an edit capability. */
data class ShopReviewUpdateInput(
    val reviewId: String,
    val header: String,
    val comment: String,
    val rating: ShopRating,
)
