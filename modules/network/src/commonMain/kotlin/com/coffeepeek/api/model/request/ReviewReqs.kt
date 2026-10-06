@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.api.model.request

import com.coffeepeek.api.model.response.shop.RatingDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendReviewReq(
    @SerialName("shop") val shopId: String,
    @SerialName("header") val header: String,
    @SerialName("comment") val comment: String,
    @SerialName("rating") val rating: RatingDto,
    @SerialName("photos") val photos: List<UploadedPhotoReq>? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val drinkSlug: String? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val customDrinkName: String? = null,
)

// Matches UpdateCoffeeShopReviewCommand: reviewId is required in the body (server binds it to
// find the review — omitting it was the Guid.Empty → 404). The command has no photos field.
@Serializable
data class UpdateReviewReq(
    @SerialName("reviewId") val reviewId: String,
    @SerialName("header") val header: String,
    @SerialName("comment") val comment: String,
    @SerialName("rating") val rating: RatingDto,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val drinkSlug: String? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val customDrinkName: String? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val clearDrink: Boolean = false,
)
