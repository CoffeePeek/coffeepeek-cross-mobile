package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn as LegacyCheckIn
import com.coffeepeek.feature.profile.domain.PublicCheckInDrink
import com.coffeepeek.feature.profile.domain.PublicCheckInPreparation
import com.coffeepeek.feature.profile.domain.PublicCheckInScores
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn

/** One-way compatibility read until the public feed source moves to profile/data; never used to render legacy UI. */
internal fun LegacyCheckIn.toFeatureModel(): PublicProfileCheckIn {
    val row = PublicProfileCheckIn(id, coffeeShopSlug, venue, title, details, rating, note, occurredAt, photoUrls)
    val content = source ?: return row.withSummaryPresentation()
    return row.copy(photoPreviewUrls = content.photoThumbnailUrls ?: photoUrls, createdAt = content.createdAt,
        authorName = content.username, authorAvatarUrl = content.authorAvatarUrl,
        scores = content.rating?.let { PublicCheckInScores(it.place, it.service, it.coffee) },
        drink = PublicCheckInDrink(content.customDrinkName, content.drinkNameRu, content.drinkNameEn),
        isHome = content.isHome, preparation = content.homePreparation?.let {
            PublicCheckInPreparation(it.beanName, it.brewMethodCode, it.doseGrams, it.brewDurationSeconds, it.tasteScore)
        }, helpfulCount = content.helpfulCount, isHelpfulByCurrentUser = content.isHelpfulByCurrentUser,
        photoDetailUrls = content.photoDetailUrls)
}

private fun PublicProfileCheckIn.withSummaryPresentation(): PublicProfileCheckIn {
    val atHome = venue == "Дома"
    return copy(isHome = atHome, drink = details?.takeUnless { atHome }?.let { PublicCheckInDrink(it, null, null) },
        preparation = if (atHome) PublicCheckInPreparation(title, null, null, null, rating?.toInt()) else null)
}
