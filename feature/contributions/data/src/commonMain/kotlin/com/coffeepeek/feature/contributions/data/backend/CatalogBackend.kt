package com.coffeepeek.feature.contributions.data.backend

import com.coffeepeek.core.network.envelopeData
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments

private const val CATALOGS = "/api/Catalogs"

/** The public reference reads of the forms. A rejected envelope or a missing `data` fails with the server's message. */
internal class CatalogBackend(private val client: HttpClient) {
    suspend fun cities(): Result<List<RefCityDto>> = list("$CATALOGS/cities")
    suspend fun beans(): Result<List<RefCatalogItemDto>> = list("$CATALOGS/beans")
    suspend fun equipment(): Result<List<RefCatalogItemDto>> = list("$CATALOGS/equipments")
    suspend fun brewMethods(): Result<List<RefCatalogItemDto>> = list("$CATALOGS/brew-methods")
    suspend fun shopTags(): Result<List<RefCatalogItemDto>> = list("$CATALOGS/shop-tags")

    suspend fun drinks(): Result<List<RefDrinkDto>> = requestResult {
        client.get("/api/menu/drinks").envelopeData<RefDrinksDto> { IllegalStateException(it ?: "Menu drinks were rejected") }.drinks
    }

    suspend fun shop(slug: String): Result<RefShopDto> = requestResult {
        client.get("/api/CoffeeShops") { url { appendPathSegments(slug, encodeSlash = true) } }
            .envelopeData { IllegalStateException(it ?: "The shop was rejected") }
    }

    private suspend inline fun <reified T> list(path: String): Result<List<T>> = requestResult {
        client.get(path).envelopeData<List<T>> { IllegalStateException(it ?: "The catalog was rejected") }
    }
}
