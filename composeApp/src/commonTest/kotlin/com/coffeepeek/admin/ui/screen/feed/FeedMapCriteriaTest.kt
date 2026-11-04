package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.admin.di.discovery.toDiscoveryCriteria
import com.coffeepeek.domain.model.ShopLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedMapCriteriaTest {
    @Test fun listAndMapCarryTheSameQueryAndEveryFilter() {
        val filters = FeedFiltersUi(cityId = "minsk", coffeeFocus = "specialty", openOnly = true,
            newOnly = true, visitedOnly = true, favoritesOnly = true, priceRange = 2, minRating = 4.5,
            roasterIds = setOf("roaster"), beanIds = setOf("bean"), equipmentIds = setOf("machine"),
            brewMethodIds = setOf("filter"), tagIds = setOf("laptop-friendly"))
        val request = filters.toShopFilters(" Coffee ", null)
        val map = request.toDiscoveryCriteria().searchQuery()
        assertEquals("Coffee", map.text)
        assertEquals("minsk", map.filters.cityId)
        assertEquals(request.tagIds.toSet(), map.filters.tagIds)
        assertEquals(request.roasterIds.toSet(), map.filters.roasterIds)
        assertEquals(request.beanIds.toSet(), map.filters.beanIds)
        assertEquals(request.equipmentIds.toSet(), map.filters.equipmentIds)
        assertEquals(request.brewMethodIds.toSet(), map.filters.brewMethodIds)
        assertEquals(request.priceRange, map.filters.priceLevel?.level)
        assertEquals(request.minRating, map.filters.minRating)
        assertTrue(map.filters.openOnly && map.filters.newOnly && map.filters.visitedOnly && map.filters.favoritesOnly)
    }

    @Test fun nearbyUsesActualUserOriginAndIgnoresCityInBothViews() {
        val filters = FeedFiltersUi(cityId = "minsk", nearbyOnly = true)
        val origin = ShopLocation(latitude = 53.9, longitude = 27.56)
        val request = filters.toShopFilters("", origin)
        val map = request.toDiscoveryCriteria().searchQuery()
        assertNull(request.cityId)
        assertNull(map.filters.cityId)
        assertEquals(origin.latitude, map.origin?.latitude)
        assertEquals(origin.longitude, map.origin?.longitude)
        assertNull(filters.toShopFilters("", null).origin)
        assertEquals("minsk", filters.toShopFilters("", null).cityId)
        assertFalse(filters.toShopFilters("", null).toDiscoveryCriteria().filters.nearbyOnly)
    }
}
