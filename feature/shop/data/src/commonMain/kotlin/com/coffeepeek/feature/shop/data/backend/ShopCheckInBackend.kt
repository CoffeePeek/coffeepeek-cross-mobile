package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.networkJson
import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.model.ShopRating
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class ShopCheckInBackend(private val client: HttpClient) {
    suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>> = requestResult {
        val response = client.get("/api/catalogs/drinks").body<DrinkOptionsResponse>()
        val options = response.data
        if (!response.isSuccess || options == null) throw ShopCheckInRejected()
        options.map { ShopConsumedDrinkOption(it.slug, it.nameRu, it.nameEn) }
    }

    suspend fun create(
        shopId: String,
        visitedAtIso: String,
        isPublic: Boolean,
        drinkSlug: String?,
        customDrinkName: String?,
        header: String?,
        note: String?,
        rating: ShopRating?,
        photos: List<UploadedShopPhoto>,
    ): Result<Unit> = requestResult {
        val response = client.post("/api/CheckIns") {
            contentType(ContentType.Application.Json)
            setBody(CreateCheckInRequest(shopId, isPublic, visitedAtIso, header, note,
                photos.takeIf { it.isNotEmpty() }, rating?.toRequest(), drinkSlug, customDrinkName))
        }
        val body = response.bodyAsText()
        if (body.isNotBlank() && !networkJson.decodeFromString<CheckInWriteResponse>(body).isSuccess) {
            throw ShopCheckInRejected()
        }
    }
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class CreateCheckInRequest(
    @SerialName("shop") val shopId: String,
    @SerialName("isPublic") val isPublic: Boolean,
    @SerialName("visitedAt") val visitedAtIso: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("header") val header: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("note") val note: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("photos") val photos: List<UploadedShopPhoto>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("rating") val rating: CheckInRatingRequest? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("drinkSlug") val drinkSlug: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("customDrinkName") val customDrinkName: String? = null,
)

@Serializable
private data class CheckInRatingRequest(
    @SerialName("place") val place: Int,
    @SerialName("service") val service: Int,
    @SerialName("coffee") val coffee: Int,
)

@Serializable
private data class CheckInWriteResponse(@SerialName("isSuccess") val isSuccess: Boolean = false)

private fun ShopRating.toRequest() = CheckInRatingRequest(place, service, coffee)

private class ShopCheckInRejected : Exception("Check-in request was rejected")
