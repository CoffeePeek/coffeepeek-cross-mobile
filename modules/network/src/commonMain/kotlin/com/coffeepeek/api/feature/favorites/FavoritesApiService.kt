package com.coffeepeek.api.feature.favorites

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.model.response.shop.ShortPhotoMetadataDto
import com.coffeepeek.api.utils.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteEntryDto(
    val kind: String,
    val address: PublicAddressDto? = null,
    val createdAtUtc: String,
    val name: String = "",
    val coverPhoto: ShortPhotoMetadataDto? = null,
    val drink: FavoriteDrinkDto? = null,
)

@Serializable
data class FavoriteDrinkDto(val slug: String)

class FavoritesApiService(private val client: HttpClient) {
    suspend fun getFavorites(): Result<List<FavoriteEntryDto>> = favoriteRequest {
        val response = client.get("/api/v1/favorites")
        val envelope = response.body<ApiResponse<List<FavoriteEntryDto>>>()
        if (!response.status.isSuccess() || !envelope.isSuccess || envelope.data == null) {
            throw ApiException(envelope.message ?: "Не удалось загрузить избранное")
        }
        envelope.data
    }

    suspend fun setFavorite(kind: String, slug: String, favorite: Boolean): Result<Unit> = favoriteRequest {
        require(kind == "coffee_shop" || kind == "roaster" || kind == "drink")
        require(slug.isNotBlank())
        val response = if (favorite) {
            client.put("/api/v1/favorites") { url { appendPathSegments(kind, slug, encodeSlash = true) } }
        } else {
            client.delete("/api/v1/favorites") { url { appendPathSegments(kind, slug, encodeSlash = true) } }
        }
        // The contract returns 204 with no JSON body for both idempotent mutations.
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<ApiResponse<Unit>>().message }.getOrNull()
            throw ApiException(message ?: "Не удалось изменить избранное (${response.status.value})")
        }
    }
}

private suspend inline fun <T> favoriteRequest(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
