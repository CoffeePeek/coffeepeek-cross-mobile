package com.coffeepeek.feature.discovery.impl.search

import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.ShopKind

/** The values the filters report under their analytics keys, exactly as the legacy screen did (the kind by its old id, the price by its level). */
internal fun DiscoveryFilters.analyticsValues(): Map<String, Any> = buildMap {
    if (!nearbyOnly) cityId?.let { put("city", it) }
    kind?.let { put("type", it.analyticsId) }
    put("isOpen", openOnly); put("isNew", newOnly); put("visitedOnly", visitedOnly); put("favoritesOnly", favoritesOnly)
    if (nearbyOnly) put("nearby", true)
    priceLevel?.let { put("priceRange", it.level) }; minRating?.let { put("minRating", it) }
    listOf("roasters" to roasterIds, "beans" to beanIds, "equipments" to equipmentIds, "brewMethods" to brewMethodIds, "tags" to tagIds)
        .forEach { (key, values) -> if (values.isNotEmpty()) put(key, values) }
}

/** The keys whose value differs between the two states, sorted. */
internal fun changedFilterKeys(before: Map<String, Any>, after: Map<String, Any>): List<String> =
    (before.keys + after.keys).filter { before[it] != after[it] }.sorted()

private val ShopKind.analyticsId: String get() = when (this) { ShopKind.Specialty -> "specialty"; ShopKind.CoffeeBar -> "coffee_bar"; ShopKind.Cafe -> "cafe" }
