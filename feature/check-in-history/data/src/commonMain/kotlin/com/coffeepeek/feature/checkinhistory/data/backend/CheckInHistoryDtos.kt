@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.checkinhistory.data.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@Serializable
internal data class HistoryEnvelope(
    val isSuccess: Boolean,
    val data: HistoryPageDto? = null,
)

@Serializable
internal data class HistoryPageDto(
    val items: List<HistoryCheckInDto> = emptyList(),
    val totalCount: JsonElement,
)

@Serializable
internal data class HistoryCheckInDto(
    val id: String,
    val shop: HistoryAddressDto? = null,
    val author: HistoryAddressDto? = null,
    val shopName: String = "",
    val username: String = "",
    val authorAvatarUrl: String? = null,
    @JsonNames("note") val text: String = "",
    @JsonNames("createdAt") val createdAtUtc: String = "",
    val visitedAt: String = "",
    val rating: HistoryRatingDto? = null,
    val photos: List<HistoryPhotoDto> = emptyList(),
    val drinkPhotoUrl: String? = null,
    val drinkPhotoVariants: List<HistoryPhotoVariantDto>? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
    val visibility: String = "Private",
    val moderationState: String = "NotSubmitted",
    val contentRevision: JsonElement? = null,
    val rejectionReason: String? = null,
    val helpfulCount: JsonElement? = null,
    val isHelpfulByCurrentUser: Boolean = false,
)

@Serializable
internal data class HistoryAddressDto(val slug: String, val canonicalPath: String = "")

@Serializable
internal data class HistoryRatingDto(
    val place: JsonElement? = null,
    val service: JsonElement? = null,
    val coffee: JsonElement? = null,
)

@Serializable
internal data class HistoryPhotoDto(
    @JsonNames("fullUrl") val url: String = "",
    val sortIndex: JsonElement? = null,
    val variants: List<HistoryPhotoVariantDto>? = null,
)

@Serializable
internal data class HistoryPhotoVariantDto(
    val purpose: String = "", val contentType: String = "", val url: String = "",
    val width: Int = 0, val height: Int = 0, val sizeBytes: Long = 0,
)
