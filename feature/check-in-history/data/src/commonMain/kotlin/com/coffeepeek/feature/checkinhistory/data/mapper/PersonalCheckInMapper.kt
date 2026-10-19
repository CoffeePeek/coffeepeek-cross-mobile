package com.coffeepeek.feature.checkinhistory.data.mapper

import com.coffeepeek.feature.checkinhistory.data.backend.HistoryAddressDto
import com.coffeepeek.feature.checkinhistory.data.backend.HistoryCheckInDto
import com.coffeepeek.feature.checkinhistory.data.backend.HistoryPhotoVariantDto
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryAddress
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryDrink
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryRating
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal fun JsonElement?.historyIntOrNull(): Int? = (this as? JsonPrimitive)?.intOrNull

internal fun HistoryCheckInDto.toDomain(apiBaseUrl: String): PersonalCheckIn {
    require(id.isNotBlank()) { "Missing personal check-in ID" }
    val orderedPhotos = photos.sortedBy { it.sortIndex.historyIntOrNull() ?: 0 }
        .mapNotNull { photo -> photo.url.resolveHistoryUrl(apiBaseUrl)?.let { photo to it } }
    return PersonalCheckIn(
        id = id,
        shop = shop?.toDomain(),
        shopName = shopName,
        author = author?.toDomain(),
        username = username,
        authorAvatarUrl = authorAvatarUrl.resolveHistoryUrl(apiBaseUrl),
        note = text,
        createdAt = createdAtUtc,
        visitedAt = visitedAt,
        rating = rating?.let { HistoryRating(it.place.historyIntOrNull(), it.service.historyIntOrNull(), it.coffee.historyIntOrNull()) },
        photoUrls = orderedPhotos.map { it.second },
        photoThumbnailUrls = orderedPhotos.map { (photo, original) -> photo.variants.variantUrl("feed", original, apiBaseUrl) },
        photoDetailUrls = orderedPhotos.map { (photo, original) -> photo.variants.variantUrl("detail", original, apiBaseUrl) },
        photoCalendarUrls = orderedPhotos.map { (photo, original) -> photo.variants.variantUrl("thumb", original, apiBaseUrl) },
        drink = HistoryDrink(drinkSlug, customDrinkName, drinkNameRu, drinkNameEn)
            .takeIf { listOf(it.slug, it.customName, it.nameRu, it.nameEn).any { value -> !value.isNullOrBlank() } },
        visibility = HistoryVisibility.entries.firstOrNull { it.name == visibility } ?: HistoryVisibility.Private,
        moderation = HistoryModeration.entries.firstOrNull { it.name == moderationState } ?: HistoryModeration.Unknown,
        contentRevision = (contentRevision.historyIntOrNull() ?: 0).coerceAtLeast(0),
        rejectionReason = rejectionReason,
        helpfulCount = (helpfulCount.historyIntOrNull() ?: 0).coerceAtLeast(0),
        isHelpfulByCurrentUser = isHelpfulByCurrentUser,
    )
}

private fun List<HistoryPhotoVariantDto>?.variantUrl(purpose: String, original: String, baseUrl: String): String {
    if (this == null) return original
    return sequenceOf("image/png", "image/jpeg", "image/webp").flatMap { type ->
        asSequence().filter { it.purpose.equals(purpose, true) && it.contentType.equals(type, true) &&
            it.width > 0 && it.height > 0 && it.sizeBytes > 0 }
            .mapNotNull { it.url.resolveHistoryUrl(baseUrl) }
    }.firstOrNull().orEmpty()
}

private fun HistoryAddressDto.toDomain(): HistoryAddress? =
    slug.takeIf(String::isNotBlank)?.let { HistoryAddress(it, canonicalPath) }

/** Only server-issued URLs, never a guessed path from storageKey or an entry ID. */
private fun String?.resolveHistoryUrl(apiBaseUrl: String): String? {
    val value = this?.trim()?.takeIf(String::isNotBlank) ?: return null
    return when {
        value.startsWith("https://") || value.startsWith("http://") -> value
        value.startsWith("//") || ':' in value || value.startsWith('?') || value.startsWith('#') -> null
        else -> "${apiBaseUrl.trimEnd('/')}/${value.trimStart('/')}"
    }
}
