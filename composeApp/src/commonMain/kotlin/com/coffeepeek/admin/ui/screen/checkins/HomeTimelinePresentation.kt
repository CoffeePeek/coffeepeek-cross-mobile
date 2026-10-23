package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.domain.feature.feed.FeedHomePreparation
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility

internal fun HomeCheckIn.toTimelineCheckIn(): CheckIn = CheckIn(
    id = id, shopId = "", shopName = "", note = text.orEmpty(), createdAt = createdAtUtc,
    visitedAt = preparedAtUtc, isHome = true,
    photoUrls = photos.map { it.url }, photoThumbnailUrls = photos.map { it.thumbnailUrl },
    photoDetailUrls = photos.map { it.detailUrl }, photoCalendarUrls = photos.map { it.calendarUrl },
    drinkSlug = drinkSlug, drinkNameRu = drinkName, customDrinkName = customDrinkName,
    visibility = if (visibility == HomeCheckInVisibility.Public) CheckInVisibility.Public else CheckInVisibility.Private,
    moderationState = CheckInModerationState.valueOf(moderation.name),
    homePreparation = FeedHomePreparation(preparedOn, brewMethodCode, beanName, doseGrams?.toDoubleOrNull()?.takeIf(Double::isFinite),
        brewDurationSeconds, tasteScore),
)
