package com.coffeepeek.feature.shop.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    @SerialName("userId") val userId: String = "",
    @SerialName("shopId") val shopId: String = "",
    @SerialName("note") val note: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("visitedAt") val visitedAt: String = "",
    @SerialName("reviewId") val reviewId: String? = null,
    @SerialName("photos") val photos: List<ShopPhotoDto> = emptyList(),
    @SerialName("rating") val rating: ShopRatingDto? = null,
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
