package com.coffeepeek.feature.favorites.data.backend

import com.coffeepeek.core.network.ApiEnvelope
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

private const val FAVORITES = "/api/v1/favorites"

internal class FavoritesApiException(message: String) : Exception(message)

@Serializable
internal data class FavoriteAddressDto(val slug: String, val canonicalPath: String, val revision: Int, val isAlias: Boolean)

@Serializable
internal data class FavoritePhotoUrlsDto(val card: String? = null)

@Serializable
internal data class FavoritePhotoDto(val fullUrl: String? = null, val urls: FavoritePhotoUrlsDto? = null)

@Serializable
internal data class FavoriteDrinkDto(val slug: String)

@Serializable
internal data class FavoriteEntryDto(
    val kind: String,
    val address: FavoriteAddressDto? = null,
    val createdAtUtc: String,
    val name: String = "",
    val coverPhoto: FavoritePhotoDto? = null,
    val drink: FavoriteDrinkDto? = null,
)

internal class FavoritesBackend(private val client: HttpClient) {
    suspend fun getFavorites(): Result<List<FavoriteEntryDto>> = requestResult {
        val response = client.get(FAVORITES)
        val envelope = response.body<ApiEnvelope<List<FavoriteEntryDto>>>()
        if (!response.status.isSuccess() || !envelope.isSuccess || envelope.data == null) {
            throw FavoritesApiException(envelope.message ?: "Не удалось загрузить избранное")
        }
        envelope.data!!
    }

    suspend fun setFavorite(kind: String, slug: String, favorite: Boolean): Result<Unit> = requestResult {
        require(kind == "coffee_shop" || kind == "roaster" || kind == "drink")
        require(slug.isNotBlank())
        val response = if (favorite) {
            client.put(FAVORITES) { url { appendPathSegments(kind, slug, encodeSlash = true) } }
        } else {
            client.delete(FAVORITES) { url { appendPathSegments(kind, slug, encodeSlash = true) } }
        }
        // The contract returns 204 with no JSON body for both idempotent mutations.
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<ApiEnvelope<Unit>>().message }.getOrNull()
            throw FavoritesApiException(message ?: "Не удалось изменить избранное (${response.status.value})")
        }
    }
}
