package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError

internal enum class ShopCheckInEditorStep { Ratings, Details }

internal fun ShopCheckInValidationError.editorStep(): ShopCheckInEditorStep = when (this) {
    ShopCheckInValidationError.InvalidRating, ShopCheckInValidationError.InvalidNote,
    ShopCheckInValidationError.TooManyPhotos, ShopCheckInValidationError.InvalidPhoto -> ShopCheckInEditorStep.Ratings
    ShopCheckInValidationError.InvalidShop, ShopCheckInValidationError.InvalidVisitDate,
    ShopCheckInValidationError.InvalidDrink -> ShopCheckInEditorStep.Details
}
