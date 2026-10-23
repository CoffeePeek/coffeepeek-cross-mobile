package com.coffeepeek.feature.discovery.domain.rules

import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.ShopSummary

/** The text of a request: trimmed, and no text at all when it is blank. */
fun searchText(raw: String): String? = raw.trim().takeIf(String::isNotEmpty)

/**
 * The loaded page after the switches of the plain search, which the server does not apply. A page may show fewer shops than its size: the legacy list did the same.
 */
fun visibleShops(shops: List<ShopSummary>, filters: DiscoveryFilters): List<ShopSummary> = shops.filter { shop ->
    (!filters.openOnly || shop.isOpen) && (!filters.newOnly || shop.isNew) && (!filters.visitedOnly || shop.isVisited) && (!filters.favoritesOnly || shop.isFavorite)
}

/** The favorite flags of [ids] over a page; the favorites of the account win over what the page says. */
fun withFavoriteMembership(shops: List<ShopSummary>, ids: Set<String>): List<ShopSummary> = shops.map { it.copy(isFavorite = it.id in ids) }

/** Discovery (the map and the sections) shows while nothing is searched or filtered; the nearby switch alone keeps it. */
fun showsDiscovery(query: String, searchActive: Boolean, filters: DiscoveryFilters): Boolean =
    !searchActive && query.isBlank() && filters.copy(nearbyOnly = false).activeCount == 0
