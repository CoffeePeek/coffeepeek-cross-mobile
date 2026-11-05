package com.coffeepeek.feature.profile.data.mapper

import com.coffeepeek.feature.community.api.PublicAuthorPost
import com.coffeepeek.feature.profile.domain.PublicCheckInDrink
import com.coffeepeek.feature.profile.domain.PublicCheckInPreparation
import com.coffeepeek.feature.profile.domain.PublicCheckInScores
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn

internal fun PublicAuthorPost.toPublicProfileCheckIn(): PublicProfileCheckIn {
    val rating = scores?.let { PublicCheckInScores(it.place, it.service, it.coffee) }?.average
    val recipe = home?.takeIf { isHome }
    val drinkName = drink?.let { it.customName?.takeIf(String::isNotBlank)
        ?: it.nameRu?.takeIf(String::isNotBlank) ?: it.nameEn?.takeIf(String::isNotBlank) }
    return PublicProfileCheckIn(
        id = id, coffeeShopSlug = shopSlug.takeIf { it.isNotBlank() && !isHome },
        venue = if (isHome) "Дома" else shopName.ifBlank { "Кофейня" },
        title = if (isHome) recipe?.beanName?.takeIf(String::isNotBlank) ?: "Кофе дома" else drinkName ?: "Чекин в кофейне",
        details = if (isHome) homeSummary() else drinkName,
        rating = if (isHome) recipe?.tasteScore?.toDouble() else rating,
        note = note, occurredAt = recipe?.preparedOn?.takeIf(String::isNotBlank) ?: visitedAt.ifBlank { createdAt },
        photoUrls = originals, photoPreviewUrls = thumbnails, photoDetailUrls = details,
        createdAt = createdAt, authorName = authorName, authorAvatarUrl = authorAvatarUrl,
    ).withContent(this)
}

private fun PublicProfileCheckIn.withContent(post: PublicAuthorPost) = copy(
    scores = post.scores?.let { PublicCheckInScores(it.place, it.service, it.coffee) },
    drink = post.drink?.let { PublicCheckInDrink(it.customName, it.nameRu, it.nameEn) },
    isHome = post.isHome, preparation = post.home?.takeIf { post.isHome }?.let {
        PublicCheckInPreparation(it.beanName, it.brewMethodCode, it.doseGrams, it.brewDurationSeconds, it.tasteScore)
    }, helpfulCount = post.helpfulCount, isHelpfulByCurrentUser = post.isHelpfulByCurrentUser,
)

private fun PublicAuthorPost.homeSummary(): String? = home?.takeIf { isHome }?.let {
    listOfNotNull(it.brewMethodCode, it.doseGrams?.let { dose -> "$dose г" },
        it.brewDurationSeconds?.let { seconds -> "$seconds с" }).joinToString(" · ").takeIf(String::isNotBlank)
}
