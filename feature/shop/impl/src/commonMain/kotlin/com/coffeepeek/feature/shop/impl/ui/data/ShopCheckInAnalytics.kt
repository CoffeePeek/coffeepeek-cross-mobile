package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.api.ShopCheckInAnalytics
import com.coffeepeek.feature.shop.api.ShopCheckInField
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput

internal fun ShopCheckInAnalytics.changed(before: ShopCheckInCreateInput, after: ShopCheckInCreateInput) {
    val changes = listOf(
        ShopCheckInField.Note to (before.text != after.text),
        ShopCheckInField.Date to (before.visitedAtIso != after.visitedAtIso),
        ShopCheckInField.Drink to (before.drinkSlug != after.drinkSlug || before.customDrinkName != after.customDrinkName),
        ShopCheckInField.RatingCoffee to (before.rating.coffee != after.rating.coffee),
        ShopCheckInField.RatingService to (before.rating.service != after.rating.service),
        ShopCheckInField.RatingPlace to (before.rating.place != after.rating.place),
        ShopCheckInField.Photos to (before.photos != after.photos),
        ShopCheckInField.Visibility to (before.visibility != after.visibility),
    )
    changes.filter { it.second }.forEach { field(it.first) }
}
