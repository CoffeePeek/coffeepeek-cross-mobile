@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.community.data.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@Serializable
internal data class FeedPageDto(val items: List<FeedItemDto> = emptyList(), val nextCursor: String? = null)

@Serializable
internal data class FeedItemDto(val publishedAtUtc: String = "", val checkIn: PostDto)

@Serializable
internal data class MyPostsDto(val items: List<PostDto> = emptyList(), val totalCount: JsonElement? = null)

@Serializable
internal data class PostDto(
    val id: String = "",
    val shop: AddressDto? = null,
    val author: AddressDto? = null,
    val username: String = "",
    val shopName: String = "",
    @JsonNames("note") val text: String = "",
    @JsonNames("createdAt") val createdAtUtc: String = "",
    val visitedAt: String = "",
    val rating: RatingDto? = null,
    val visibility: String = "Private",
    val moderationState: String = "NotSubmitted",
    val contentRevision: JsonElement? = null,
    val rejectionReason: String? = null,
    val photos: List<PhotoDto> = emptyList(),
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
    val helpfulCount: JsonElement? = null,
    val isHelpfulByCurrentUser: Boolean = false,
    val kind: String = "Shop",
    val home: HomeDto? = null,
    val shelfBagPhotoUrl: String? = null,
    val shelfBagPhotoVariants: List<VariantDto>? = null,
    val authorAvatarUrl: String? = null,
    val authorId: String? = null,
)

@Serializable
internal data class AddressDto(val slug: String = "", val canonicalPath: String = "")

@Serializable
internal data class RatingDto(val place: JsonElement? = null, val service: JsonElement? = null, val coffee: JsonElement? = null)

@Serializable
internal data class HomeDto(
    val preparedOn: String = "",
    val brewMethodCode: String? = null,
    val beanName: String? = null,
    val roasterName: String? = null,
    val coffeeProductSlug: String? = null,
    val doseGrams: Double? = null,
    val brewDurationSeconds: Int? = null,
    val tasteScore: Int? = null,
)

@Serializable
internal data class PhotoDto(
    @JsonNames("fullUrl") val url: String = "",
    val sortIndex: JsonElement? = null,
    val variants: List<VariantDto>? = null,
)

@Serializable
internal data class VariantDto(
    val purpose: String = "",
    val contentType: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val url: String = "",
)

@Serializable
internal data class HelpfulDto(val isHelpful: Boolean = false, val helpfulCount: JsonElement? = null)

@Serializable
internal data class RatingRequest(val place: Int, val service: Int, val coffee: Int)

@Serializable
internal data class UpdateRequest(
    val text: String,
    val rating: RatingRequest? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
)

@Serializable
internal data class VisibilityRequest(val visibility: String)

@Serializable
internal data class ReportRequest(val text: String)
