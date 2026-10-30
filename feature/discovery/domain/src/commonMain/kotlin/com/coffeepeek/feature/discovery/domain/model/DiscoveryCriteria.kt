package com.coffeepeek.feature.discovery.domain.model

/** The same selection feeds the list, map markers, map cards and suggestions. */
data class DiscoveryCriteria(
    val text: String = "",
    val filters: DiscoveryFilters = DiscoveryFilters(),
    val origin: Coordinates? = null,
) {
    /** Nearby is measured from the user, so its radius replaces the selected city. */
    fun searchQuery(page: Int = 1, pageSize: Int = 20): ShopQuery {
        val nearbyOrigin = origin?.takeIf { filters.nearbyOnly && it.isValid }
        return ShopQuery(text, if (nearbyOrigin != null) filters.copy(cityId = null) else filters, nearbyOrigin, page, pageSize)
    }
}
