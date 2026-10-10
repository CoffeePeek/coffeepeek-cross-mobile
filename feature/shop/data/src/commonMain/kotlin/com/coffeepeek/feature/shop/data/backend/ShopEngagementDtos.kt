@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.shop.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@Serializable
internal data class ShopReviewDto(
    @SerialName("id") val id: String = "",
    @SerialName("moderationReviewId") val moderationReviewId: String? = null,
    @SerialName("userId") val userId: String = "",
    @SerialName("coffeeShopId") val shopId: String = "",
    @SerialName("username") val username: String? = null,
    @SerialName("header") val header: String? = null,
    @SerialName("comment") val comment: String? = null,
    @SerialName("rating") val rating: ShopRatingDto = ShopRatingDto(),
    @SerialName("photos") val photos: List<ShopReviewPhotoDto> = emptyList(),
    @SerialName("createdAtUtc") val createdAtUtc: String = "",
    @SerialName("helpfulCount") val helpfulCount: Int = 0,
    @SerialName("isHelpfulByCurrentUser") val isHelpfulByCurrentUser: Boolean = false,
)

@Serializable
internal data class ShopCheckInDto(
    @SerialName("id") val id: String = "",
    @SerialName("author") val author: ShopAddressDto? = null,
    @SerialName("shop") val shop: ShopAddressDto? = null,
    @SerialName("username") val username: String = "",
    @SerialName("text") @JsonNames("note") val note: String = "",
    @SerialName("createdAtUtc") @JsonNames("createdAt") val createdAt: String = "",
    @SerialName("visitedAt") val visitedAt: String = "",
    @SerialName("photos") val photos: List<ShopCheckInPhotoDto> = emptyList(),
    @SerialName("rating") val rating: ShopRatingDto? = null,
    @SerialName("drinkSlug") val drinkSlug: String? = null,
    @SerialName("customDrinkName") val customDrinkName: String? = null,
    @SerialName("drinkNameRu") val drinkNameRu: String? = null,
    @SerialName("drinkNameEn") val drinkNameEn: String? = null,
    @SerialName("visibility") val visibility: String = "Private",
    @SerialName("moderationState") val moderationState: String = "NotSubmitted",
    @SerialName("contentRevision") val contentRevision: JsonElement? = null,
    @SerialName("rejectionReason") val rejectionReason: String? = null,
    @SerialName("helpfulCount") val helpfulCount: JsonElement? = null,
    @SerialName("isHelpfulByCurrentUser") val isHelpfulByCurrentUser: Boolean = false,
)

@Serializable
internal data class ShopCheckInPhotoDto(
    @SerialName("url") @JsonNames("fullUrl") val url: String = "",
    @SerialName("sortIndex") val sortIndex: JsonElement? = null,
)

@Serializable
internal data class ShopRatingDto(
    @SerialName("place") val place: JsonElement? = null,
    @SerialName("service") val service: JsonElement? = null,
    @SerialName("coffee") val coffee: JsonElement? = null,
)

@Serializable
internal data class ShopReviewPhotoDto(
    @SerialName("storageKey") val storageKey: String? = null,
    @SerialName("fullUrl") val fullUrl: String? = null,
)
