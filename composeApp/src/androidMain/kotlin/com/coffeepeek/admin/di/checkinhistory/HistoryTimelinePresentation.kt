package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.ReviewRating
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryRating
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckIn
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInPhoto
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInVisibility
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInModeration

internal fun PersonalCheckIn.toTimelineCheckIn(): CheckIn = CheckIn(
    id = id, shopId = shop?.slug.orEmpty(), shopName = shopName, note = note,
    createdAt = createdAt, visitedAt = visitedAt,
    photoUrls = photoUrls, photoThumbnailUrls = photoThumbnailUrls ?: photoUrls,
    photoDetailUrls = photoDetailUrls, photoCalendarUrls = photoCalendarUrls,
    rating = rating?.toTimelineRating(), drinkSlug = drink?.slug,
    customDrinkName = drink?.customName, drinkNameRu = drink?.nameRu, drinkNameEn = drink?.nameEn,
    shopAddress = shop?.let { PublicAddress(it.slug, it.canonicalPath, 0, false) },
    authorAddress = author?.let { PublicAddress(it.slug, it.canonicalPath, 0, false) },
    username = username, authorAvatarUrl = authorAvatarUrl,
    visibility = CheckInVisibility.valueOf(visibility.name),
    moderationState = CheckInModerationState.valueOf(moderation.name),
    contentRevision = contentRevision, rejectionReason = rejectionReason,
    helpfulCount = helpfulCount, isHelpfulByCurrentUser = isHelpfulByCurrentUser,
)

private fun HistoryRating.toTimelineRating(): ReviewRating? {
    return ReviewRating(place ?: return null, service ?: return null, coffee ?: return null)
}

internal fun HomeHistoryEntry.toTimelineHome(): HomeCheckIn = HomeCheckIn(
    id = id, preparedAtUtc = preparedAtUtc, preparedOn = preparedOn, createdAtUtc = createdAtUtc,
    text = note, brewMethodCode = recipe.brewMethodCode, coffeeProductId = recipe.coffeeProductId,
    beanName = recipe.beanName, doseGrams = recipe.doseGrams,
    brewDurationSeconds = recipe.brewDurationSeconds, tasteScore = recipe.tasteScore,
    drinkName = drinkName, drinkSlug = drinkSlug, customDrinkName = customDrinkName,
    photos = photos.map { HomeCheckInPhoto(it.id, it.url, it.thumbnailUrl ?: it.url,
        it.detailUrl ?: it.url, it.calendarUrl ?: it.thumbnailUrl ?: it.url) },
    visibility = visibility?.let { HomeCheckInVisibility.valueOf(it.name) },
    moderation = HomeCheckInModeration.valueOf(moderation.name),
)

/** The legacy home model has no bag: the timeline card takes it, with the roaster and catalogue link, from the history entry. */
internal fun CheckIn.withShelfBag(entry: HomeHistoryEntry): CheckIn = copy(homePreparation = homePreparation?.copy(
    roasterName = entry.recipe.roasterName, coffeeProductSlug = entry.recipe.coffeeProductSlug,
    shelfBagCardUrl = entry.shelfBagCard?.let { card -> card.calendarUrl?.takeIf(String::isNotBlank) ?: card.url },
))
