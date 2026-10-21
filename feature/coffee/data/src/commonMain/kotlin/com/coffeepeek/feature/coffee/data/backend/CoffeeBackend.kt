package com.coffeepeek.feature.coffee.data.backend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess

internal val coffeeFilterCodes = setOf("brew", "caffeine", "roast", "acidity", "processing", "fermentation", "taste", "composition")

internal class CoffeeBackend(private val client: HttpClient) {
    suspend fun detail(slug: String): CoffeeDto {
        require(slug.isNotBlank() && slug.length <= 200)
        val response = client.get("/api/v1/coffees") { url { appendPathSegments(slug, encodeSlash = true) } }
        check(response.status.isSuccess()) { "Coffee details failed: ${response.status.value}" }
        return response.body<CoffeeResponseDto<CoffeeDto>>().requireData()
    }

    suspend fun search(request: CoffeeSearchRequestDto): CoffeePageDto {
        val response = client.post("/api/v1/coffees/search") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Coffee search failed: ${response.status.value}" }
        return response.body<CoffeeResponseDto<CoffeePageDto>>().requireData()
    }

    suspend fun filters(): List<CoffeeFilterGroupDto> {
        val response = client.get("/api/v1/catalogs/coffee-filter-values")
        check(response.status.isSuccess()) { "Coffee filters failed: ${response.status.value}" }
        return response.body<CoffeeResponseDto<List<CoffeeFilterGroupDto>>>().requireData()
    }
}
