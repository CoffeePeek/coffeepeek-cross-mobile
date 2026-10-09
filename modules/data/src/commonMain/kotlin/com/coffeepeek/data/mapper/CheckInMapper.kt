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
    username = username,
    shopName = shopName,
    note = text,
    createdAt = createdAtUtc,
    visitedAt = visitedAt,
    rating = rating?.let { ReviewRating(place = it.place, service = it.service, coffee = it.coffee) },
    photoUrls = photos.sortedBy { it.sortIndex }.mapNotNull { fileUrls.resolveApiUrl(it.url) },
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
)
