package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote

interface ShopReviewVoteRepository {
    suspend fun setHelpful(reviewId: String, helpful: Boolean): Result<ShopHelpfulVote>
}
