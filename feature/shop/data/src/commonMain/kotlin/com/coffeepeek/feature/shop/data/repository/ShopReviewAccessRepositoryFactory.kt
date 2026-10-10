package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopReviewAccessBackend
import com.coffeepeek.feature.shop.domain.model.ShopReviewAccess
import com.coffeepeek.feature.shop.domain.repository.ShopReviewAccessRepository
import io.ktor.client.HttpClient

fun createShopReviewAccessRepository(client: HttpClient): ShopReviewAccessRepository =
    DefaultShopReviewAccessRepository(ShopReviewAccessBackend(client))

private class DefaultShopReviewAccessRepository(
    private val backend: ShopReviewAccessBackend,
) : ShopReviewAccessRepository {
    override suspend fun getAccess(shopId: String): Result<ShopReviewAccess> = backend.getAccess(shopId)
}
