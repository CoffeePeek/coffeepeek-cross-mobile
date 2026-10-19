package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating

internal sealed interface ShopCheckInFormAction {
    data object Load : ShopCheckInFormAction
    data object RetryDrinks : ShopCheckInFormAction
    data class TextChanged(val value: String) : ShopCheckInFormAction
    data class RatingChanged(val rating: ShopRating) : ShopCheckInFormAction
    data class VisitDateChanged(val isoTimestamp: String) : ShopCheckInFormAction
    data class VisibilityChanged(val visibility: ShopCheckInVisibility) : ShopCheckInFormAction
    data class DrinkChanged(val slug: String?) : ShopCheckInFormAction
    data class CustomDrinkChanged(val value: String) : ShopCheckInFormAction
    data class PhotosAdded(val photos: List<ShopCheckInPhoto>) : ShopCheckInFormAction
    data class RemovePhoto(val index: Int) : ShopCheckInFormAction
    data object Submit : ShopCheckInFormAction
    data object Dismiss : ShopCheckInFormAction
    data object OpenHistory : ShopCheckInFormAction
    data object GoToFeed : ShopCheckInFormAction
    /** Explicit user confirmation after checking history, never an automatic network retry. */
    data object HistoryChecked : ShopCheckInFormAction
}
