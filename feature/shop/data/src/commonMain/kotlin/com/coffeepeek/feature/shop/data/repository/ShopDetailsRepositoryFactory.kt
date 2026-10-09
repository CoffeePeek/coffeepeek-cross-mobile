package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopDetailsBackend
import com.coffeepeek.feature.shop.data.backend.ShopMenuCatalogBackend
import com.coffeepeek.feature.shop.data.mapper.alignedWithCatalog
import com.coffeepeek.feature.shop.data.mapper.toDomain
import com.coffeepeek.feature.shop.data.mapper.toLocalSchedules
import com.coffeepeek.feature.shop.data.mapper.toOverview
import com.coffeepeek.feature.shop.data.mapper.toCoffeeDetails
import com.coffeepeek.feature.shop.data.mapper.toDomainOrNull
import com.coffeepeek.feature.shop.data.mapper.toFeatures
import com.coffeepeek.feature.shop.data.mapper.ShopFileUrlResolver
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient

/** The composition root supplies the authenticated client and current device UTC offset. */
fun createShopDetailsRepository(
    client: HttpClient,
    fileBaseUrl: String,
    utcOffsetMinutes: () -> Int,
): ShopDetailsRepository = DefaultShopDetailsRepository(
    ShopDetailsBackend(client), ShopFileUrlResolver(fileBaseUrl), utcOffsetMinutes,
    ShopMenuCatalogBackend(client),
)

private class DefaultShopDetailsRepository(
    private val backend: ShopDetailsBackend,
    private val files: ShopFileUrlResolver,
    private val utcOffsetMinutes: () -> Int,
    private val menuCatalog: ShopMenuCatalogBackend,
) : ShopDetailsRepository {
    override suspend fun getDetails(shopId: String): Result<ShopDetails> = requestResult {
        val data = backend.load(shopId).getOrThrow()
        // A missing catalog must not hide usable shop details. Cancellation still propagates.
        val drinks = if (data.menu?.items?.isNotEmpty() == true) menuCatalog.load().getOrNull() else null
        ShopDetails(
            overview = data.toOverview(),
            menu = data.menu?.toDomain()?.alignedWithCatalog(drinks),
            schedules = data.schedules.orEmpty().toLocalSchedules(utcOffsetMinutes()),
            coffee = data.toCoffeeDetails(),
            contact = data.contact?.toDomainOrNull(),
            features = data.toFeatures(),
            reviews = data.reviews.mapNotNull { it.toDomain(files) },
            userCheckIns = data.userCheckIns.mapNotNull { it.toDomain(files) },
            checkIns = data.checkIns.mapNotNull { it.toDomain(files) },
        )
    }
}
