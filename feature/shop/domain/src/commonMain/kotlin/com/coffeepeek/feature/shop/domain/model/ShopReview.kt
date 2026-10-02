package com.coffeepeek.feature.shop.domain.model

data class ShopReview(
    val id: String,
    val moderationReviewId: String?,
    val userId: String,
    val shopId: String,
    val username: String,
    val header: String,
    val comment: String,
    val rating: ShopRating,
    val createdAtUtc: String,
    val photoUrls: List<String>,
    val helpfulCount: Int,
    val isHelpfulByCurrentUser: Boolean,
)

data class ShopRating(
    val place: Int,
    val service: Int,
    val coffee: Int,
) {
    val average: Double get() = (place + service + coffee) / 3.0
}

data class ShopHelpfulVote(
    val isHelpful: Boolean,
    val helpfulCount: Int,
)
