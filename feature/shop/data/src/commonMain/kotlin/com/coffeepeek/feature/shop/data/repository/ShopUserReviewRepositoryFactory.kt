package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopUserReviewsBackend
import com.coffeepeek.feature.shop.data.mapper.ShopFileUrlResolver
import com.coffeepeek.feature.shop.domain.model.ShopReview
import com.coffeepeek.feature.shop.domain.repository.ShopUserReviewRepository
import io.ktor.client.HttpClient

fun createShopUserReviewRepository(client: HttpClient, fileBaseUrl: String): ShopUserReviewRepository =
    DefaultShopUserReviewRepository(ShopUserReviewsBackend(client, ShopFileUrlResolver(fileBaseUrl)))

private class DefaultShopUserReviewRepository(
    private val backend: ShopUserReviewsBackend,
) : ShopUserReviewRepository {
    override suspend fun findForEdit(userId: String, reviewId: String): Result<ShopReview?> =
        backend.findForEdit(userId, reviewId)
}
