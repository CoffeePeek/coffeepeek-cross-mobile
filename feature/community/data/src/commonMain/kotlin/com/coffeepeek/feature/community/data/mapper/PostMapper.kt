package com.coffeepeek.feature.community.data.mapper

import com.coffeepeek.feature.community.data.backend.PostDto
import com.coffeepeek.feature.community.data.backend.VariantDto
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.HomePreparation
import com.coffeepeek.feature.community.domain.model.PostAuthor
import com.coffeepeek.feature.community.domain.model.PostDrink
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostPhotos
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.core.network.flexibleIntOrNull
import kotlinx.serialization.json.JsonElement

internal fun JsonElement?.intOrNullValue(): Int? = flexibleIntOrNull()

/** Absolute URLs stay as issued; relative ones are resolved against the API base. */
internal fun String?.resolveUrl(apiBaseUrl: String): String? = this?.trim()?.takeIf(String::isNotBlank)?.let {
    if (it.startsWith("https://") || it.startsWith("http://")) it else "${apiBaseUrl.trimEnd('/')}/${it.trimStart('/')}"
}

internal fun PostDto.toDomain(apiBaseUrl: String): CommunityPost {
    require(id.isNotBlank()) { "Missing check-in ID" }
    val home = kind.equals("Home", ignoreCase = true)
    val usable = photos.sortedBy { it.sortIndex.intOrNullValue() ?: 0 }
        .mapNotNull { photo -> photo.url.resolveUrl(apiBaseUrl)?.let { photo to it } }
    return CommunityPost(
        id = id,
        shopSlug = shop?.slug.orEmpty(),
        shopName = shopName,
        author = PostAuthor(authorId, author?.slug?.takeIf(String::isNotBlank), username, authorAvatarUrl.resolveUrl(apiBaseUrl)),
        note = text,
        createdAt = createdAtUtc,
        visitedAt = visitedAt,
        photos = PostPhotos(
            originals = usable.map { it.second },
            thumbnails = usable.map { (photo, original) -> photo.variants.variantUrl("feed", original).resolveUrl(apiBaseUrl).orEmpty() },
            details = usable.map { (photo, original) -> photo.variants.variantUrl("detail", original).ifBlank { original }.resolveUrl(apiBaseUrl).orEmpty() },
        ),
        rating = rating?.let { PostRating(it.place.intOrNullValue() ?: 0, it.service.intOrNullValue() ?: 0, it.coffee.intOrNullValue() ?: 0) },
        drink = PostDrink(drinkSlug, customDrinkName, drinkNameRu, drinkNameEn)
            .takeIf { listOf(it.slug, it.customName, it.nameRu, it.nameEn).any { value -> !value.isNullOrBlank() } },
        visibility = PostVisibility.entries.firstOrNull { it.name == visibility } ?: PostVisibility.Private,
        moderation = PostModeration.entries.firstOrNull { it.name == moderationState } ?: PostModeration.Unknown,
        contentRevision = contentRevision.intOrNullValue() ?: 0,
        rejectionReason = rejectionReason,
        helpfulCount = helpfulCount.intOrNullValue() ?: 0,
        isHelpfulByCurrentUser = isHelpfulByCurrentUser,
        isHome = home,
        home = this.home?.takeIf { home }?.let {
            HomePreparation(it.preparedOn, it.brewMethodCode, it.beanName, it.doseGrams, it.brewDurationSeconds, it.tasteScore)
        },
    )
}

/** The feed variant of a photo. Without variants the original is used; with variants but no usable match the result is empty (as before). */
private fun List<VariantDto>?.variantUrl(purpose: String, original: String): String {
    if (this == null) return original
    val usable = filter { it.purpose == purpose && it.width > 0 && it.height > 0 && it.sizeBytes > 0 && it.url.isNotBlank() }
    return listOf("image/png", "image/jpeg", "image/webp").firstNotNullOfOrNull { type -> usable.firstOrNull { it.contentType == type }?.url }.orEmpty()
}
