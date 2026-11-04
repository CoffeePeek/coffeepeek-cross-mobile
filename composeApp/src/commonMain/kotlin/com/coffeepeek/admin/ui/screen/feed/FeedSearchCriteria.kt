package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.model.ShopLocation

/** The existing list and map share exactly one request criteria mapping. */
internal fun FeedFiltersUi.toShopFilters(query: String, origin: ShopLocation?, page: Int = 1, pageSize: Int = 20): ShopFilters = ShopFilters(
    query = query.trim().takeIf { it.isNotBlank() }, cityId = cityId.takeUnless { nearbyOnly && origin != null },
    coffeeFocus = coffeeFocus, roasterIds = roasterIds.toList(), beanIds = beanIds.toList(),
    equipmentIds = equipmentIds.toList(), brewMethodIds = brewMethodIds.toList(), tagIds = tagIds.toList(),
    priceRange = priceRange, minRating = minRating, page = page, pageSize = pageSize,
    origin = origin.takeIf { nearbyOnly }, openOnly = openOnly, newOnly = newOnly,
    visitedOnly = visitedOnly, favoritesOnly = favoritesOnly,
)
