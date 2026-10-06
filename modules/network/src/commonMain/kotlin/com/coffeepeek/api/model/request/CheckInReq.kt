@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.api.model.request

import com.coffeepeek.api.model.response.shop.RatingDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateCheckInReq(
    @SerialName("shop") val coffeeShopId: String,
    @SerialName("isPublic") val isPublic: Boolean,
    @SerialName("visitedAt") val visitedAt: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("header") val header: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("note") val note: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("photos") val photos: List<UploadedPhotoReq>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("rating") val rating: RatingDto? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val drinkSlug: String? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val customDrinkName: String? = null,
)
