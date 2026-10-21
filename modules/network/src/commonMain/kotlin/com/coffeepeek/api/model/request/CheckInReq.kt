package com.coffeepeek.api.model.request

import com.coffeepeek.api.model.response.shop.RatingDto
import kotlinx.serialization.Serializable

@Serializable
data class CreateCheckInReq(
    val coffeeShopSlug: String,
    val text: String = "",
    val rating: RatingDto? = null,
    val visibility: String = "Private",
    val visitedAt: String? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val photos: List<UploadedPhotoReq> = emptyList(),
)

@Serializable
data class UpdateCheckInReq(
    val text: String = "",
    val rating: RatingDto? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
)

@Serializable
data class CheckInVisibilityReq(val visibility: String)
