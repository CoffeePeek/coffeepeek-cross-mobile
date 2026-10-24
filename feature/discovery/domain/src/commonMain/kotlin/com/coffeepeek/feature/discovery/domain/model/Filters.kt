package com.coffeepeek.feature.discovery.domain.model

/** Everything the user can narrow the shops with. The city stays when the selections are cleared. */
data class DiscoveryFilters(
    val cityId: String? = null,
    val kind: ShopKind? = null,
    val openOnly: Boolean = false,
    val newOnly: Boolean = false,
    val visitedOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val nearbyOnly: Boolean = false,
    val priceLevel: PriceLevel? = null,
    val minRating: Double? = null,
    val roasterIds: Set<String> = emptySet(),
    val beanIds: Set<String> = emptySet(),
    val equipmentIds: Set<String> = emptySet(),
    val brewMethodIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
) {
    /** The badge of the filter button: every switch and level counts once, every selected id counts once; the city never counts. */
    val activeCount: Int
        get() = listOf(kind != null, openOnly, newOnly, visitedOnly, favoritesOnly, nearbyOnly, priceLevel != null, minRating != null).count { it } +
            roasterIds.size + beanIds.size + equipmentIds.size + brewMethodIds.size + tagIds.size

    fun clearSelections(): DiscoveryFilters = DiscoveryFilters(cityId = cityId)
}

/** One search request: the text, the filters and, with nearby, the point the distance is measured from. */
data class ShopQuery(
    val text: String? = null,
    val filters: DiscoveryFilters = DiscoveryFilters(),
    val origin: Coordinates? = null,
    val page: Int = 1,
    val pageSize: Int = 20,
)
