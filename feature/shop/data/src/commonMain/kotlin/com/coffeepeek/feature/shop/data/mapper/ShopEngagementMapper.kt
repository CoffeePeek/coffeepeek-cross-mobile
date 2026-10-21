package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopCheckInDto
import com.coffeepeek.feature.shop.data.backend.ShopCheckInPhotoVariantDto
import com.coffeepeek.feature.shop.data.backend.ShopRatingDto
import com.coffeepeek.feature.shop.data.backend.ShopReviewDto
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopCheckInModerationState
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReview
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

internal class ShopFileUrlResolver(baseUrl: String) {
    private val origin = baseUrl.trimEnd('/').also {
        require(it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)) {
            "File base URL must use HTTP(S)"
        }
    }

    fun resolve(storageKey: String?, fullUrl: String?): String? {
        fullUrl?.trim()?.takeIf(::isWebUrl)?.let { return it }
        val key = storageKey?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (isWebUrl(key)) return key
        if (key.startsWith('/') || key.contains('?') || key.contains('#') ||
            key.split('/').any { it == ".." || it == "." }) return null
        return "$origin/api/file/$key"
    }

    /** Check-in URLs are server-issued API URLs, not public file storage keys. */
    fun resolveApiUrl(url: String?): String? {
        val value = url?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (isWebUrl(value)) return value
        if (value.contains(":") || value.startsWith("//") ||
            value.split('/').any { it == ".." || it == "." }) return null
        return "$origin/${value.trimStart('/')}"
    }

    private fun isWebUrl(value: String): Boolean =
        (value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)) &&
            value.substringAfter("://").substringBefore('/').isNotBlank() &&
            value.none { it.isWhitespace() || it.code < 0x20 }
}

internal fun ShopReviewDto.toDomain(files: ShopFileUrlResolver): ShopReview? {
    if (id.isBlank()) return null
    return ShopReview(
        id = id,
        moderationReviewId = moderationReviewId,
        userId = userId,
        shopId = shopId,
        username = username.orEmpty(),
        header = header.orEmpty(),
        comment = comment.orEmpty(),
        rating = rating.toDomain(),
        createdAtUtc = createdAtUtc,
        photoUrls = photos.mapNotNull { files.resolve(it.storageKey, it.fullUrl) },
        helpfulCount = helpfulCount,
        isHelpfulByCurrentUser = isHelpfulByCurrentUser,
    )
}

internal fun ShopCheckInDto.toDomain(files: ShopFileUrlResolver): ShopCheckIn? {
    if (id.isBlank()) return null
    val orderedPhotos = photos.sortedBy { it.sortIndex.toFlexibleInt() }
        .filter { files.resolveApiUrl(it.url) != null }
    val resolvedPhotos = orderedPhotos.map { files.resolveApiUrl(it.url).orEmpty() }
    return ShopCheckIn(
        id = id,
        userId = author?.slug.orEmpty(),
        shopId = shop?.slug.orEmpty(),
        note = note,
        createdAt = createdAt,
        visitedAt = visitedAt,
        reviewId = null,
        photoUrls = resolvedPhotos,
        photoThumbnailUrls = orderedPhotos.map { files.resolveApiUrl(it.variants.photoUrl("feed", it.url)).orEmpty() },
        photoDetailUrls = orderedPhotos.map {
            files.resolveApiUrl(it.variants.photoUrl("detail", it.url).ifBlank { it.url }).orEmpty()
        },
        rating = rating?.toDomain(),
        username = username,
        drinkSlug = drinkSlug,
        customDrinkName = customDrinkName,
        drinkNameRu = drinkNameRu,
        drinkNameEn = drinkNameEn,
        authorAvatarUrl = files.resolveApiUrl(authorAvatarUrl),
        authorSlug = author?.slug?.takeIf(String::isNotBlank),
        authorId = authorId?.takeIf(String::isNotBlank),
        visibility = ShopCheckInVisibility.entries.firstOrNull { it.name == visibility }
            ?: ShopCheckInVisibility.Private,
        moderationState = ShopCheckInModerationState.entries.firstOrNull { it.name == moderationState }
            ?: ShopCheckInModerationState.Unknown,
        contentRevision = contentRevision.toFlexibleInt(),
        rejectionReason = rejectionReason,
        helpfulCount = helpfulCount.toFlexibleInt(),
        isHelpfulByCurrentUser = isHelpfulByCurrentUser,
    )
}

/** Missing manifest is legacy; an explicit empty/unsupported manifest must not fetch an original inline. */
private fun List<ShopCheckInPhotoVariantDto>?.photoUrl(purpose: String, original: String): String {
    if (this == null) return original
    val supported = filter { it.purpose == purpose && it.width > 0 && it.height > 0 && it.sizeBytes > 0 && it.url.isNotBlank() }
    return listOf("image/png", "image/jpeg", "image/webp").firstNotNullOfOrNull { type ->
        supported.firstOrNull { it.contentType == type }?.url
    }.orEmpty()
}

private fun ShopRatingDto.toDomain(): ShopRating = ShopRating(
    place = place.toFlexibleInt(),
    service = service.toFlexibleInt(),
    coffee = coffee.toFlexibleInt(),
)

internal fun JsonElement?.toFlexibleInt(): Int {
    val primitive = this as? JsonPrimitive ?: return 0
    return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
        ?: primitive.doubleOrNull?.toInt() ?: 0
}
