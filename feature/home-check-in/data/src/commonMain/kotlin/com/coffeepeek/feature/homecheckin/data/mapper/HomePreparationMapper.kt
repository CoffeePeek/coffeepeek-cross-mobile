package com.coffeepeek.feature.homecheckin.data.mapper

import com.coffeepeek.feature.homecheckin.data.backend.*
import com.coffeepeek.feature.homecheckin.domain.model.*
import kotlinx.serialization.json.*

internal fun HomeDto.toDomain(apiBaseUrl: String): HomePreparation {
    val orderedPhotos = photos.sortedBy(HomePhotoDto::sortIndex)
        .mapNotNull { photo -> photo.url.resolvePhotoUrl(apiBaseUrl)?.let { photo to it } }
    val cutout = when (val photo = drinkPhoto) {
        is JsonPrimitive -> photo.contentOrNull?.let { HomePhotoDto(url = it) }
        is JsonObject -> photoJson.decodeFromJsonElement<HomePhotoDto>(photo)
        else -> null
    }
    val cutoutOriginal = cutout?.url.resolvePhotoUrl(apiBaseUrl)
    return HomePreparation(
        id = id, preparedAtUtc = preparedAtUtc, preparedOn = preparedOn, createdAtUtc = createdAtUtc,
        text = text, brewMethodCode = brewMethodCode, coffeeProductId = coffeeProductId, beanName = beanName,
        doseGrams = doseGrams?.toString(), brewDurationSeconds = brewDurationSeconds, tasteScore = tasteScore,
        drinkName = drink.string("nameRu", "name", "customName", "customDrinkName"),
        drinkSlug = drink.string("slug"), customDrinkName = drink.string("customName", "customDrinkName"),
        photos = orderedPhotos.map { (photo, original) -> HomePreparationPhoto(photo.id, original,
            photo.variants.variantUrl("feed", original, apiBaseUrl),
            photo.variants.variantUrl("detail", original, apiBaseUrl),
            photo.variants.variantUrl("thumb", original, apiBaseUrl)) },
        drinkPhotoUrl = cutoutOriginal,
        drinkPhotoThumbnailUrl = cutoutOriginal?.let { cutout?.variants.variantUrl("thumb", it, apiBaseUrl) },
        drinkPhotoDetailUrl = cutoutOriginal?.let { cutout?.variants.variantUrl("detail", it, apiBaseUrl) },
        visibility = HomePreparationVisibility.entries.firstOrNull { it.name.equals(visibility, ignoreCase = true) },
        moderation = HomePreparationModeration.entries.firstOrNull { it.name.equals(moderationState, ignoreCase = true) }
            ?: HomePreparationModeration.Unknown,
    )
}

private val photoJson = Json { ignoreUnknownKeys = true }

private fun List<HomePhotoVariantDto>?.variantUrl(purpose: String, original: String, base: String): String {
    if (this == null) return original
    return sequenceOf("image/png", "image/jpeg", "image/webp").flatMap { type ->
        asSequence().filter { it.purpose.equals(purpose, true) && it.contentType.equals(type, true) &&
            it.width > 0 && it.height > 0 && it.sizeBytes > 0 }
            .mapNotNull { it.url.resolvePhotoUrl(base) }
    }.firstOrNull().orEmpty()
}

internal fun HomePreparationInput.toEditRequest() = HomeEditDto(
    preparedAt, preparedOn, brewMethodCode, coffeeProductSlug, coffeeProductId,
    customBeanName?.trim(), doseGrams?.toDouble(), brewDurationSeconds, tasteScore, text?.trim(),
    drinkSlug, customDrinkName?.trim(), visibility?.name,
)

internal fun HomePreparationInput.toCreateRequest(photos: List<UploadedPhotoDto>, cutout: UploadedPhotoDto?) = HomeCreateDto(
    preparedAt, preparedOn, brewMethodCode, coffeeProductSlug, coffeeProductId,
    customBeanName?.trim(), doseGrams?.toDouble(), brewDurationSeconds, tasteScore, text?.trim(),
    drinkSlug, customDrinkName?.trim(), photos, cutout, (visibility ?: HomePreparationVisibility.Private).name,
)

private fun JsonElement?.string(vararg keys: String): String? = (this as? JsonObject)?.let { value ->
    keys.firstNotNullOfOrNull { (value[it] as? JsonPrimitive)?.contentOrNull }
}

/** Preserve server-issued paths and signatures; never infer a URL from storage keys. */
private fun String?.resolvePhotoUrl(base: String): String? {
    val value = this?.trim()?.takeIf(String::isNotBlank) ?: return null
    return when {
        value.startsWith("https://") || value.startsWith("http://") -> value
        value.startsWith("//") || ':' in value || value.startsWith('?') || value.startsWith('#') -> null
        else -> "${base.trimEnd('/')}/${value.trimStart('/')}"
    }
}
