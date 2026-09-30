package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopDetailsBackend
import com.coffeepeek.feature.shop.data.mapper.toOverview
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.repository.ShopOverviewRepository
import io.ktor.client.HttpClient

/** Narrow construction surface; the caller supplies its authenticated client. */
fun createShopOverviewRepository(client: HttpClient): ShopOverviewRepository =
    DefaultShopOverviewRepository(ShopDetailsBackend(client))

private class DefaultShopOverviewRepository(
    private val backend: ShopDetailsBackend,
) : ShopOverviewRepository {
    override suspend fun getOverview(shopId: String): Result<ShopOverview> =
        backend.load(shopId).map { it.toOverview(shopId) }
}
