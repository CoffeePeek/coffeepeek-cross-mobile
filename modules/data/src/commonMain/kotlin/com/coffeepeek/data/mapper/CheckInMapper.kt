package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.response.CheckInDto
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.ReviewRating

internal fun CheckInDto.toDomain(fileUrls: FileUrlResolver): CheckIn = CheckIn(
    id = id,
    shopId = shop?.slug.orEmpty(),
    shopAddress = shop?.toDomain(),
    authorAddress = author?.toDomain(),
    authorAvatarUrl = fileUrls.resolveApiUrl(authorAvatarUrl),
    username = username,
    shopName = shopName,
    note = text,
    createdAt = createdAtUtc,
    visitedAt = visitedAt,
    rating = rating?.let { ReviewRating(place = it.place, service = it.service, coffee = it.coffee) },
    photoUrls = photos.sortedBy { it.sortIndex }.mapNotNull { fileUrls.resolveApiUrl(it.url) },
    photoThumbnailUrls = photos.sortedBy { it.sortIndex }.filter { fileUrls.resolveApiUrl(it.url) != null }
        .map { fileUrls.resolveApiUrl(it.variants.photoUrl("feed", it.url)).orEmpty() },
    photoDetailUrls = photos.sortedBy { it.sortIndex }.filter { fileUrls.resolveApiUrl(it.url) != null }
        .map { fileUrls.resolveApiUrl(it.variants.photoUrl("detail", it.url).ifBlank { it.url }).orEmpty() },
    photoCalendarUrls = photos.sortedBy { it.sortIndex }.filter { fileUrls.resolveApiUrl(it.url) != null }
        .map { fileUrls.resolveApiUrl(it.variants.photoUrl("thumb", it.url)).orEmpty() },
    drinkPhotoUrl = fileUrls.resolveApiUrl(drinkPhotoUrl),
    drinkPhotoThumbnailUrl = drinkPhotoUrl?.let { fileUrls.resolveApiUrl(drinkPhotoVariants.photoUrl("thumb", it)).orEmpty() },
    drinkPhotoDetailUrl = fileUrls.resolveApiUrl(drinkPhotoVariants.photoUrl("detail", drinkPhotoUrl.orEmpty())),
    drinkSlug = drinkSlug,
    customDrinkName = customDrinkName,
    drinkNameRu = drinkNameRu,
    drinkNameEn = drinkNameEn,
    visibility = CheckInVisibility.entries.firstOrNull { it.name == visibility } ?: CheckInVisibility.Private,
    moderationState = CheckInModerationState.entries.firstOrNull { it.name == moderationState } ?: CheckInModerationState.Unknown,
    contentRevision = contentRevision,
    rejectionReason = rejectionReason,
    helpfulCount = helpfulCount,
    isHelpfulByCurrentUser = isHelpfulByCurrentUser,
    isHome = kind.equals("Home", ignoreCase = true),
    homePreparation = home?.takeIf { kind.equals("Home", ignoreCase = true) }?.let {
        com.coffeepeek.domain.feature.feed.FeedHomePreparation(
            it.preparedOn, it.brewMethodCode, it.beanName, it.doseGrams, it.brewDurationSeconds, it.tasteScore,
        )
    },
)
