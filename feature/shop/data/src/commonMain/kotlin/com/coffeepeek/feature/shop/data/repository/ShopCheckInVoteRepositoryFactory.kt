package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopCheckInVoteBackend
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import io.ktor.client.HttpClient

fun createShopCheckInVoteRepository(client: HttpClient): ShopCheckInVoteRepository =
    DefaultShopCheckInVoteRepository(ShopCheckInVoteBackend(client))

private class DefaultShopCheckInVoteRepository(private val backend: ShopCheckInVoteBackend) : ShopCheckInVoteRepository {
    override suspend fun setHelpful(checkInId: String, helpful: Boolean): Result<ShopHelpfulVote> =
        backend.setHelpful(checkInId, helpful)
}
