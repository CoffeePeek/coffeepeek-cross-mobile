package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopReviewVoteBackend
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import com.coffeepeek.feature.shop.domain.repository.ShopReviewVoteRepository
import io.ktor.client.HttpClient

fun createShopReviewVoteRepository(client: HttpClient): ShopReviewVoteRepository =
    DefaultShopReviewVoteRepository(ShopReviewVoteBackend(client))

private class DefaultShopReviewVoteRepository(
    private val backend: ShopReviewVoteBackend,
) : ShopReviewVoteRepository {
    override suspend fun setHelpful(reviewId: String, helpful: Boolean): Result<ShopHelpfulVote> =
        backend.setHelpful(reviewId, helpful)
}
