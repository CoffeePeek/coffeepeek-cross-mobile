package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.api.PublicShopDetails
import com.coffeepeek.feature.shop.data.backend.ShopDetailsBackend
import com.coffeepeek.feature.shop.data.mapper.toPublicSummary
import io.ktor.client.HttpClient

/** Reuses the existing details endpoint without loading menus or exposing personalized rows. */
fun createPublicShopDetails(client: HttpClient): PublicShopDetails = object : PublicShopDetails {
    private val backend = ShopDetailsBackend(client)
    override suspend fun load(slug: String) = requestResult { backend.load(slug).getOrThrow().toPublicSummary() }
}
