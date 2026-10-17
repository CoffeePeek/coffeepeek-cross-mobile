package com.coffeepeek.feature.homecheckin.data.mapper

import com.coffeepeek.feature.homecheckin.data.backend.*
import com.coffeepeek.feature.homecheckin.domain.model.*
import kotlinx.serialization.json.*

internal fun HomeDto.toDomain(apiBaseUrl: String) = HomePreparation(
    id = id, preparedAtUtc = preparedAtUtc, preparedOn = preparedOn, createdAtUtc = createdAtUtc,
    text = text, brewMethodCode = brewMethodCode, coffeeProductId = coffeeProductId, beanName = beanName,
    doseGrams = doseGrams?.toString(), brewDurationSeconds = brewDurationSeconds, tasteScore = tasteScore,
    drinkName = drink.string("nameRu", "name", "customName", "customDrinkName"),
    drinkSlug = drink.string("slug"), customDrinkName = drink.string("customName", "customDrinkName"),
    photos = photos.filter { it.url.isNotBlank() }.map { HomePreparationPhoto(it.id, it.url.absoluteUrl(apiBaseUrl)) },
    drinkPhotoUrl = drinkPhoto.urlOrNull()?.takeIf(String::isNotBlank)?.absoluteUrl(apiBaseUrl),
    visibility = HomePreparationVisibility.entries.firstOrNull { it.name.equals(visibility, ignoreCase = true) },
    moderation = HomePreparationModeration.entries.firstOrNull { it.name.equals(moderationState, ignoreCase = true) }
        ?: HomePreparationModeration.Unknown,
)

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

private fun JsonElement?.urlOrNull(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> string("url", "fullUrl")
    else -> null
}

private fun String.absoluteUrl(base: String): String = when {
    startsWith("https://") -> this
    startsWith("/") -> base.trimEnd('/') + this
    else -> base.trimEnd('/') + "/" + this
}
