@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.api.model.response

import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.model.response.shop.RatingDto
import com.coffeepeek.api.serialization.FlexibleIntSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class CheckInDto(
    val id: String,
    val shop: PublicAddressDto? = null,
    val author: PublicAddressDto? = null,
    val username: String = "",
    val shopName: String = "",
    @JsonNames("note") val text: String = "",
    @JsonNames("createdAt") val createdAtUtc: String = "",
    val visitedAt: String = "",
    val rating: RatingDto? = null,
    val visibility: String = "Private",
    val moderationState: String = "NotSubmitted",
    @Serializable(with = FlexibleIntSerializer::class) val contentRevision: Int = 0,
    val rejectionReason: String? = null,
    val photos: List<CheckInPhotoDto> = emptyList(),
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val helpfulCount: Int = 0,
    val isHelpfulByCurrentUser: Boolean = false,
)

@Serializable
data class CheckInPhotoDto(
    val id: String = "",
    val fileName: String = "",
    val contentType: String = "",
    val storageKey: String = "",
    val sizeBytes: Long = 0,
    @Serializable(with = FlexibleIntSerializer::class) val sortIndex: Int = 0,
    @JsonNames("fullUrl") val url: String = "",
)

@Serializable
data class GetUserCheckInsResponseDto(
    val items: List<CheckInDto> = emptyList(),
    @Serializable(with = FlexibleIntSerializer::class) val totalCount: Int = 0,
)
