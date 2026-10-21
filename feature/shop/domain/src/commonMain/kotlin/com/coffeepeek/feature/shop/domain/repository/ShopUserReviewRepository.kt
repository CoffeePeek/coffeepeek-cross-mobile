package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopReview

/** Finds a published review owned by the signed-in user before editing its moderation record. */
interface ShopUserReviewRepository {
    suspend fun findForEdit(userId: String, reviewId: String): Result<ShopReview?>
}
