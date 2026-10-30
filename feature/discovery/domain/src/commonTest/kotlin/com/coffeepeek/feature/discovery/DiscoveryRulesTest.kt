package com.coffeepeek.feature.discovery

import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.rules.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiscoveryRulesTest {
    private val minsk = Coordinates(53.9, 27.56)

    @Test fun kindsAndPriceLevelsReadTheServerNamesAndNumbers() {
        assertEquals(ShopKind.Specialty, ShopKind.fromApi(" Specialty ")); assertEquals(ShopKind.Specialty, ShopKind.fromApi("1"))
        assertEquals(ShopKind.Cafe, ShopKind.fromApi("3")); assertEquals(ShopKind.CoffeeBar, ShopKind.fromApi("unknown")); assertEquals(ShopKind.CoffeeBar, ShopKind.fromApi(null))
        assertEquals("CoffeeBar", ShopKind.CoffeeBar.apiName)
        assertEquals(PriceLevel.Cheap, PriceLevel.fromApi("$")); assertEquals(PriceLevel.Luxury, PriceLevel.fromApi("luxury")); assertNull(PriceLevel.fromApi("free"))
        assertEquals(PriceLevel.Moderate, PriceLevel.ofLevel(2)); assertNull(PriceLevel.ofLevel(5)); assertNull(PriceLevel.ofLevel(null))
    }

    @Test fun coordinatesMustBeAPlaceOnEarth() {
        assertTrue(minsk.isValid)
        assertFalse(Coordinates(91.0, 0.0).isValid); assertFalse(Coordinates(0.0, -181.0).isValid); assertFalse(Coordinates(Double.NaN, 0.0).isValid)
        assertNull(distanceMeters(Coordinates(91.0, 0.0), minsk)); assertNull(distanceMeters(null, minsk)); assertNull(distanceMeters(minsk, null))
    }

    @Test fun distanceIsMeasuredOverTheSurface() {
        assertEquals(0.0, distanceMeters(minsk, minsk)!!, 0.001)
        val oneDegreeOfLatitude = distanceMeters(Coordinates(53.0, 27.0), Coordinates(54.0, 27.0))!!
        assertEquals(111_195.0, oneDegreeOfLatitude, 100.0)
    }

    @Test fun theBadgeCountsSwitchesLevelsAndSelectedIdsButNotTheCity() {
        assertEquals(0, DiscoveryFilters(cityId = "minsk").activeCount)
        val filters = DiscoveryFilters(
            cityId = "minsk", kind = ShopKind.Cafe, openOnly = true, newOnly = true, visitedOnly = true, favoritesOnly = true, nearbyOnly = true,
            priceLevel = PriceLevel.Cheap, minRating = 4.0, roasterIds = setOf("a", "b"), beanIds = setOf("c"), equipmentIds = setOf("d"), brewMethodIds = setOf("e"), tagIds = setOf("f", "g"),
        )
        assertEquals(8 + 2 + 1 + 1 + 1 + 2, filters.activeCount)
        assertEquals(DiscoveryFilters(cityId = "minsk"), filters.clearSelections())
    }

    @Test fun discoveryShowsWhileNothingIsSearchedAndNearbyAloneKeepsIt() {
        assertTrue(showsDiscovery("", false, DiscoveryFilters()))
        assertTrue(showsDiscovery("  ", false, DiscoveryFilters(cityId = "minsk", nearbyOnly = true)))
        assertFalse(showsDiscovery("", true, DiscoveryFilters())); assertFalse(showsDiscovery("a", false, DiscoveryFilters()))
        assertFalse(showsDiscovery("", false, DiscoveryFilters(openOnly = true)))
    }

    @Test fun theSwitchesOfThePlainSearchFilterTheLoadedPage() {
        val shops = listOf(
            ShopSummary("a", "A", isFavorite = true, isOpen = true), ShopSummary("b", "B", isVisited = true), ShopSummary("c", "C", isNew = true),
        )
        assertEquals(listOf("a", "b", "c"), visibleShops(shops, DiscoveryFilters()).map { it.id })
        assertEquals(listOf("a"), visibleShops(shops, DiscoveryFilters(favoritesOnly = true)).map { it.id })
        assertEquals(listOf("b"), visibleShops(shops, DiscoveryFilters(visitedOnly = true)).map { it.id })
        assertEquals(listOf("c"), visibleShops(shops, DiscoveryFilters(newOnly = true)).map { it.id })
        assertTrue(visibleShops(shops, DiscoveryFilters(openOnly = true, newOnly = true)).isEmpty())
    }

    @Test fun theFavoritesOfTheAccountWinOverThePage() {
        val shops = listOf(ShopSummary("a", "A", isFavorite = true), ShopSummary("b", "B"))
        assertEquals(listOf(false, true), withFavoriteMembership(shops, setOf("b")).map { it.isFavorite })
    }

    @Test fun theTextIsTrimmedAndBlankMeansNoText() {
        assertEquals("cof", searchText("  cof  ")); assertNull(searchText("   ")); assertNull(searchText(""))
    }

    @Test fun prefetchPadsAtLeastThreeKmAndHalfAViewport() {
        val small = MapBounds(53.9, 27.55, 53.901, 27.551)
        val padded = small.expandedForPrefetch()
        assertTrue(padded.contains(small)); assertTrue(small.minLat - padded.minLat >= 3.0 / 111.32 - 1e-9)
        val wide = MapBounds(53.0, 27.0, 54.0, 28.0)
        assertEquals(52.5, wide.expandedForPrefetch().minLat, 1e-9); assertEquals(54.5, wide.expandedForPrefetch().maxLat, 1e-9)
        assertEquals(-85.0, MapBounds(-84.9, 0.0, -84.8, 1.0).expandedForPrefetch(minMarginKm = 50.0).minLat)
        assertFalse(small.contains(padded))
        assertEquals(Coordinates(53.9005, 27.5505), small.center)
    }

    @Test fun carouselIncludesNearbyShopsAtHighZoomWithoutGrowingBroadViews() {
        val tight = MapBounds(53.899, 27.559, 53.901, 27.561)
        val nearby = tight.expandedForCarousel()
        assertTrue(nearby.contains(tight))
        assertTrue(nearby.minLat < 53.887 && nearby.maxLat > 53.913)
        assertTrue(nearby.minLon < 27.538 && nearby.maxLon > 27.582)
        assertEquals(tight.center, nearby.center)
        val broad = MapBounds(53.88, 27.50, 53.92, 27.60)
        assertEquals(broad, broad.expandedForCarousel())
    }

    @Test fun viewportKeepsSurvivorsAndAddsNewShopsNearestTheCenter() {
        fun marker(id: String, lat: Double, lon: Double) = MapMarker(id, id, Coordinates(lat, lon))
        val bounds = MapBounds(53.88, 27.50, 53.92, 27.60)
        val near = marker("z", 53.901, 27.551)
        val far = marker("a", 53.919, 27.599)
        val outside = marker("outside", 54.5, 27.56)
        assertEquals(listOf("z", "a"), viewportMarkers(listOf(far, near, outside, near), bounds, emptyList()).map { it.id })
        assertEquals(listOf("a", "z"), viewportMarkers(listOf(near, far), bounds, listOf(far)).map { it.id })
        assertTrue(viewportMarkers(listOf(marker("bad", 91.0, 0.0)), bounds, emptyList()).isEmpty())
    }

    @Test fun viewportIncludesAllVisibleShopsAndHandlesWrapping() {
        val markers = (1..12).map { MapMarker("$it", "$it", Coordinates(53.9 + it * 0.001, 27.56)) }
        assertEquals(12, viewportMarkers(markers, MapBounds(53.88, 27.50, 53.92, 27.60), emptyList()).size)
        val wrapped = listOf(179.5, -179.5, 0.0).mapIndexed { index, lon -> MapMarker("$index", "$index", Coordinates(0.0, lon)) }
        assertEquals(2, viewportMarkers(wrapped, MapBounds(-1.0, 179.0, 1.0, -179.0), emptyList()).size)
        assertNotNull(clampZoom(1f)); assertEquals(3f, clampZoom(1f)); assertEquals(20f, clampZoom(25f)); assertEquals(14f, clampZoom(14f))
    }

    @Test fun theFilterSheetOffersTheAmenityTagsWithoutTheKindsOfPlaces() {
        val tags = listOf(CatalogEntry("wifi", "Wi-Fi"), CatalogEntry("specialty", "Specialty"), CatalogEntry("x", " Кафе "), CatalogEntry("kofeynya", "Любая"), CatalogEntry("pets", "Можно с животными"))
        assertEquals(listOf("wifi", "pets"), amenityTags(tags).map { it.id })
        assertTrue(CatalogEntry("coffee-house", "?").isVenueType()); assertFalse(CatalogEntry("garden", "Сад").isVenueType())
    }
}
