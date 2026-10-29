package com.coffeepeek.feature.discovery

import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import kotlin.test.Test
import kotlin.test.assertEquals

class MapFiltersTest {
    @Test fun sharedCriteriaRetainsEverySelectionWithoutLocation() {
        val supported = DiscoveryFilters(cityId = "minsk", kind = ShopKind.Specialty,
            tagIds = setOf("laptop-friendly"), beanIds = setOf("arabica"), equipmentIds = setOf("machine"),
            brewMethodIds = setOf("filter"), roasterIds = setOf("roaster"), minRating = 4.0)
        val list = supported.copy(openOnly = true, newOnly = true, visitedOnly = true, favoritesOnly = true, nearbyOnly = true)
        val criteria = DiscoveryCriteria("coffee", list)
        assertEquals(list, criteria.searchQuery().filters)
        assertEquals("coffee", criteria.searchQuery().text)
        assertEquals(12, criteria.filters.activeCount)
    }

    @Test fun nearbyUsesUserOriginAndIgnoresCityForBothReadFormats() {
        val list = DiscoveryFilters(cityId = "minsk", openOnly = true, newOnly = true,
            visitedOnly = true, favoritesOnly = true, nearbyOnly = true, tagIds = setOf("old"))
        val origin = Coordinates(53.9, 27.56)
        val criteria = DiscoveryCriteria("coffee", list, origin)
        assertEquals(list.copy(cityId = null), criteria.searchQuery().filters)
        assertEquals(origin, criteria.searchQuery().origin)
        assertEquals("minsk", criteria.filters.cityId)
        assertEquals(null, criteria.copy(filters = list.copy(nearbyOnly = false)).searchQuery().origin)
    }
}
