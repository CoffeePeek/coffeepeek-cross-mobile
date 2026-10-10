package com.coffeepeek.feature.shop.domain.model

/** Moderation eligibility for this viewer and shop, independent of listed published reviews. */
data class ShopReviewAccess(
    val canCreate: Boolean,
    val existingReviewId: String?,
)
