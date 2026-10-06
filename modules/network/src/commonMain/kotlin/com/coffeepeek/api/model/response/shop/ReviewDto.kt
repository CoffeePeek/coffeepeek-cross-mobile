package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.PublicAddressDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    @SerialName("id") val id: String,
    @SerialName("moderationReviewId") val moderationReviewId: String? = null,
    @SerialName("author") val author: PublicAddressDto? = null,
    @SerialName("shop") val shop: PublicAddressDto? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("header") val header: String? = null,
    @SerialName("comment") val comment: String? = null,
    @SerialName("rating") val rating: RatingDto = RatingDto(),
    @SerialName("photos") val photos: List<UploadedPhotoDto> = emptyList(),
    @SerialName("createdAtUtc") val createdAtUtc: String = "",
    @SerialName("helpfulCount") val helpfulCount: Int = 0,
    @SerialName("isHelpfulByCurrentUser") val isHelpfulByCurrentUser: Boolean = false,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
)
