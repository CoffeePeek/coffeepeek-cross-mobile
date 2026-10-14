package com.coffeepeek.feature.checkinhistory.data.mapper

import com.coffeepeek.feature.checkinhistory.data.backend.HistoryAddressDto
import com.coffeepeek.feature.checkinhistory.data.backend.HistoryCheckInDto
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
    return PersonalCheckIn(
        id = id,
        shop = shop?.toDomain(),
        shopName = shopName,
        author = author?.toDomain(),
        username = username,
        note = text,
        createdAt = createdAtUtc,
        visitedAt = visitedAt,
        rating = rating?.let { HistoryRating(it.place.historyIntOrNull(), it.service.historyIntOrNull(), it.coffee.historyIntOrNull()) },
        photoUrls = photos.sortedBy { it.sortIndex.historyIntOrNull() ?: 0 }
            .mapNotNull { it.url.resolveHistoryUrl(apiBaseUrl) },
        drinkPhotoUrl = drinkPhotoUrl.resolveHistoryUrl(apiBaseUrl),
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
