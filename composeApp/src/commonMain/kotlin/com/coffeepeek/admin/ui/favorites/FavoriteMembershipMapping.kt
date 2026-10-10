package com.coffeepeek.admin.ui.favorites

import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails

/** Transitional mapping for legacy app screens; feature domain never imports these models. */
internal fun CoffeeShop.withFavoriteMembership(ids: Set<String>): CoffeeShop =
    copy(isFavorite = id in ids)

internal fun CoffeeShopDetails.withFavoriteMembership(ids: Set<String>): CoffeeShopDetails =
    copy(shop = shop.withFavoriteMembership(ids))
