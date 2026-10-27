package com.coffeepeek.feature.discovery.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.network.errorMessage
import com.coffeepeek.core.network.envelopeData
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFailure
import com.coffeepeek.feature.discovery.domain.model.FailureKind
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

private const val SEARCH = "/api/v1/coffee-shops/search"
private const val MAP = "/api/Map"
private const val CATALOGS = "/api/Catalogs"

/** What the map sends: the filters of the search that the map understands. */
internal class MapQuery(
    val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double, val zoom: Int, val city: String?, val type: String?,
    val roasters: List<String>, val equipments: List<String>, val beans: List<String>, val brewMethods: List<String>, val tags: List<String>,
    val priceRange: String?, val minRating: Double?,
)

/** Plain endpoint calls. Every non-2xx answer becomes a typed failure; nothing here knows user-facing texts. */
internal class DiscoveryBackend(private val client: HttpClient) {
    suspend fun search(request: ShopSearchRequestDto): Result<ShopSearchPageDto> = requestResult {
        val response = client.post(SEARCH) { expectSuccess = false; contentType(ContentType.Application.Json); setBody(request) }
        response.requireSuccess()
        response.envelope()
    }

    suspend fun map(query: MapQuery): Result<MapDto> = requestResult {
        val response = client.get(MAP) {
            expectSuccess = false
            parameter("minLat", query.minLat); parameter("minLon", query.minLon); parameter("maxLat", query.maxLat); parameter("maxLon", query.maxLon)
            parameter("zoom", query.zoom)
            query.city?.let { parameter("city", it) }
            query.type?.let { parameter("type", it) }
            query.roasters.forEach { parameter("roasters", it) }
            query.equipments.forEach { parameter("equipments", it) }
            query.beans.forEach { parameter("beans", it) }
            query.brewMethods.forEach { parameter("brewMethods", it) }
            query.tags.forEach { parameter("tags", it) }
            query.priceRange?.let { parameter("priceRange", it) }
            query.minRating?.let { parameter("minRating", it) }
        }
        response.requireSuccess()
        response.envelope()
    }

    suspend fun cities(): Result<List<CityDto>> = catalog("cities")
    suspend fun beans(): Result<List<NamedDto>> = catalog("beans")
    suspend fun equipment(): Result<List<NamedDto>> = catalog("equipments")
    suspend fun brewMethods(): Result<List<NamedDto>> = catalog("brew-methods")
    suspend fun shopTags(): Result<List<NamedDto>> = catalog("shop-tags")

    private suspend inline fun <reified T> catalog(name: String): Result<List<T>> = requestResult {
        val response = client.get("$CATALOGS/$name") { expectSuccess = false }
        response.requireSuccess()
        response.envelope()
    }

    private suspend fun HttpResponse.requireSuccess() {
        if (status.isSuccess()) return
        throw DiscoveryFailure(if (status == HttpStatusCode.Unauthorized) FailureKind.SignInRequired else FailureKind.Other, errorMessage())
    }

    private suspend inline fun <reified T> HttpResponse.envelope(): T = envelopeData { DiscoveryFailure(FailureKind.Other, it) }

}
