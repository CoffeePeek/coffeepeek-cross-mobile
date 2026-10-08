package com.coffeepeek.feature.shop.domain.usecase

import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import kotlin.time.Clock
import kotlin.time.Instant

enum class ShopCheckInValidationError {
    InvalidShop,
    InvalidVisitDate,
    InvalidDrink,
    InvalidNote,
    InvalidRating,
    TooManyPhotos,
    InvalidPhoto,
}

/** Pure business validation; UI maps errors to localized resources. */
fun validateShopCheckIn(
    input: ShopCheckInCreateInput,
    now: Instant = Clock.System.now(),
): ShopCheckInValidationError? = when {
    input.shopSlug.isBlank() || input.shopSlug.any { it == '/' || it == '?' || it == '#' } ->
        ShopCheckInValidationError.InvalidShop
    !validVisitDate(input.visitedAtIso, now) -> ShopCheckInValidationError.InvalidVisitDate
    input.drinkSlug == "other" && input.customDrinkName.orEmpty().trim().length !in 1..100 ->
        ShopCheckInValidationError.InvalidDrink
    input.drinkSlug != "other" && input.customDrinkName != null ->
        ShopCheckInValidationError.InvalidDrink
    input.text.trim().length !in 1..1000 ->
        ShopCheckInValidationError.InvalidNote
    listOf(input.rating.place, input.rating.service, input.rating.coffee)
        .any { it !in 1..5 } -> ShopCheckInValidationError.InvalidRating
    input.photos.size > MAX_SHOP_CHECK_IN_PHOTOS -> ShopCheckInValidationError.TooManyPhotos
    input.photos.any { it.bytes.isEmpty() || it.fileName.isBlank() || it.contentType.isBlank() } ->
        ShopCheckInValidationError.InvalidPhoto
    else -> null
}

private fun validVisitDate(value: String, now: Instant): Boolean = try {
    val date = Instant.parse(value)
    date.toEpochMilliseconds() > 0 && date <= now
} catch (_: IllegalArgumentException) {
    false
}
