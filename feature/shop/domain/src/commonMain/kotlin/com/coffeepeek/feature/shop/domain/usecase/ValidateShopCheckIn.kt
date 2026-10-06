package com.coffeepeek.feature.shop.domain.usecase

import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput

enum class ShopCheckInValidationError {
    InvalidShop,
    MissingVisitDate,
    InvalidDrink,
    InvalidHeader,
    InvalidNote,
    InvalidRating,
    TooManyPhotos,
    InvalidPhoto,
}

/** Pure business validation; UI maps errors to localized resources. */
fun validateShopCheckIn(input: ShopCheckInCreateInput): ShopCheckInValidationError? = when {
    input.shopId.isBlank() || input.shopId.any { it == '/' || it == '?' || it == '#' } ->
        ShopCheckInValidationError.InvalidShop
    input.visitedAtIso.isBlank() -> ShopCheckInValidationError.MissingVisitDate
    input.drinkSlug == "other" && input.customDrinkName.orEmpty().trim().length !in 1..100 ->
        ShopCheckInValidationError.InvalidDrink
    input.drinkSlug != "other" && input.customDrinkName != null ->
        ShopCheckInValidationError.InvalidDrink
    input.isPublic && input.header.orEmpty().trim().length !in 3..120 ->
        ShopCheckInValidationError.InvalidHeader
    input.isPublic && input.note.orEmpty().trim().length !in 10..2000 ->
        ShopCheckInValidationError.InvalidNote
    !input.isPublic && input.note.orEmpty().length > 2000 ->
        ShopCheckInValidationError.InvalidNote
    input.rating != null && listOf(input.rating.place, input.rating.service, input.rating.coffee)
        .any { it !in 1..5 } -> ShopCheckInValidationError.InvalidRating
    input.photos.size > MAX_SHOP_CHECK_IN_PHOTOS -> ShopCheckInValidationError.TooManyPhotos
    input.photos.any { it.bytes.isEmpty() || it.fileName.isBlank() || it.contentType.isBlank() } ->
        ShopCheckInValidationError.InvalidPhoto
    else -> null
}
