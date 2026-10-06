package com.coffeepeek.api.model.response

import com.coffeepeek.api.model.DataResponse
import com.coffeepeek.api.model.request.ModerationStatusDto
import com.coffeepeek.api.model.response.shop.RatingDto
import com.coffeepeek.api.model.response.shop.ReviewDto
import com.coffeepeek.api.model.response.shop.UploadedPhotoDto
import com.coffeepeek.api.model.PublicAddressDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetReviewsByUserIdResponseDto(
    @SerialName("items") val reviewDtos: List<ReviewDto> = emptyList(),
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("pageSize") val pageSize: Int = 10,
) : DataResponse()

@Serializable
data class CreateEntityResponseDto(
    @SerialName("entityId") val entityId: String? = null,
) : DataResponse()

@Serializable
data class ReviewHelpfulResponseDto(
    @SerialName("isHelpful") val isHelpful: Boolean = false,
    @SerialName("helpfulCount") val helpfulCount: Int = 0,
) : DataResponse()

@Serializable
data class ModerationReviewDto(
    @SerialName("id") val id: String,
    @SerialName("header") val header: String? = null,
    @SerialName("comment") val comment: String = "",
    @SerialName("userName") val userName: String? = null,
    @SerialName("shop") val shop: PublicAddressDto? = null,
    @SerialName("rating") val rating: RatingDto = RatingDto(),
    @SerialName("rejectedReason") val rejectedReason: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("moderationStatus") val moderationStatus: ModerationStatusDto,
    @SerialName("photos") val photos: List<UploadedPhotoDto> = emptyList(),
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
)

@Serializable
data class MyModerationReviewsPageDto(
    @SerialName("items") val reviewDtos: List<ModerationReviewDto> = emptyList(),
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
) : DataResponse()
