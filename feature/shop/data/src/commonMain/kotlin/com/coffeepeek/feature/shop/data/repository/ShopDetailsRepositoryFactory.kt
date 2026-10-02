package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopDetailsBackend
import com.coffeepeek.feature.shop.data.mapper.toDomain
import com.coffeepeek.feature.shop.data.mapper.toLocalSchedules
import com.coffeepeek.feature.shop.data.mapper.toOverview
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient

/** The composition root supplies the authenticated client and current device UTC offset. */
fun createShopDetailsRepository(
    client: HttpClient,
    utcOffsetMinutes: () -> Int,
): ShopDetailsRepository = DefaultShopDetailsRepository(ShopDetailsBackend(client), utcOffsetMinutes)

private class DefaultShopDetailsRepository(
    private val backend: ShopDetailsBackend,
    private val utcOffsetMinutes: () -> Int,
) : ShopDetailsRepository {
    override suspend fun getDetails(shopId: String): Result<ShopDetails> = requestResult {
        val data = backend.load(shopId).getOrThrow()
        ShopDetails(
            overview = data.toOverview(shopId),
            menu = (data.shop.menu ?: data.menu)?.toDomain(),
            schedules = data.shop.schedules.orEmpty().toLocalSchedules(utcOffsetMinutes()),
        )
    }
}
