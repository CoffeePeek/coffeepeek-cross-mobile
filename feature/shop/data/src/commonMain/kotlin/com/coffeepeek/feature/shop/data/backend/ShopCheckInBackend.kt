package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInCreationUnconfirmed
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class ShopCheckInBackend(private val client: HttpClient) {
    suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>> = requestResult {
        val httpResponse = client.get("/api/catalogs/drinks")
        val response = httpResponse.body<DrinkOptionsResponse>()
        val options = response.data
        if (!httpResponse.status.isSuccess() || !response.isSuccess || options == null) throw ShopCheckInRejected()
        options.map { ShopConsumedDrinkOption(it.slug, it.nameRu, it.nameEn) }
    }

    suspend fun create(
        shopSlug: String,
        visitedAtIso: String,
        visibility: String,
        drinkSlug: String?,
        customDrinkName: String?,
        text: String,
        rating: ShopRating,
        photos: List<UploadedShopPhoto>,
    ): Result<Unit> = requestResult {
        val response = try {
            client.post("/api/v1/check-ins") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(CreateCheckInRequest(shopSlug, text, rating.toRequest(), visibility,
                    visitedAtIso, drinkSlug, customDrinkName, photos))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw ShopCheckInCreationUnconfirmed(error)
        }
        if (!response.status.isSuccess()) {
            val message = response.readEnvelopeOrNull()?.message?.takeIf(String::isNotBlank)
            throw ShopCheckInRejected(message ?: "Check-in request was rejected (${response.status.value})")
        }
        val envelope = response.readEnvelopeOrNull() ?: throw ShopCheckInCreationUnconfirmed()
        if (!envelope.isSuccess) throw ShopCheckInRejected(envelope.message?.takeIf(String::isNotBlank)
            ?: "Check-in request was rejected")
        if (envelope.data?.id.isNullOrBlank()) throw ShopCheckInCreationUnconfirmed()
    }
}

private suspend fun HttpResponse.readEnvelopeOrNull(): CheckInWriteResponse? = try {
    body<CheckInWriteResponse>()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

@Serializable
private data class DrinkOptionsResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: List<DrinkOptionDto>? = null,
)

@Serializable
private data class DrinkOptionDto(
    @SerialName("slug") val slug: String,
    @SerialName("nameRu") val nameRu: String = "",
    @SerialName("nameEn") val nameEn: String = "",
)

@Serializable
private data class CreateCheckInRequest(
    @SerialName("coffeeShopSlug") val shopSlug: String,
    @SerialName("text") val text: String,
    @SerialName("rating") val rating: CheckInRatingRequest,
    @SerialName("visibility") val visibility: String,
    @SerialName("visitedAt") val visitedAtIso: String,
    @SerialName("drinkSlug") val drinkSlug: String? = null,
    @SerialName("customDrinkName") val customDrinkName: String? = null,
    @SerialName("photos") val photos: List<UploadedShopPhoto> = emptyList(),
)

@Serializable
private data class CheckInRatingRequest(
    @SerialName("place") val place: Int,
    @SerialName("service") val service: Int,
    @SerialName("coffee") val coffee: Int,
)

@Serializable
private data class CheckInWriteResponse(
    @SerialName("isSuccess") val isSuccess: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: CreatedCheckInDto? = null,
)

@Serializable
private data class CreatedCheckInDto(@SerialName("id") val id: String)

private fun ShopRating.toRequest() = CheckInRatingRequest(place, service, coffee)

private class ShopCheckInRejected(message: String = "Check-in request was rejected") : Exception(message)
