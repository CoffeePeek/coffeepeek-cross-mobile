package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryFavorites
import com.coffeepeek.feature.discovery.api.DiscoveryGate
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.api.FilterAction
import com.coffeepeek.feature.discovery.api.MapInteraction
import com.coffeepeek.feature.discovery.api.DiscoveryOverlay
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.api.SearchRun
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal fun shop(id: String, favorite: Boolean = false, visited: Boolean = false, open: Boolean = false, new: Boolean = false) =
    ShopSummary(id = id, title = "Shop $id", isFavorite = favorite, isVisited = visited, isOpen = open, isNew = new)

internal fun page(shops: List<ShopSummary>, current: Int = 1, pages: Int = 1) =
    Result.success(Paged(shops, totalCount = shops.size * pages, totalPages = pages, currentPage = current))

internal val minskCity = CityEntry("minsk", "Минск")
internal val brestCity = CityEntry("brest", "Брест")

internal class FakeRepository : DiscoveryRepository {
    var search: suspend (ShopQuery) -> Result<Paged<ShopSummary>> = { page(listOf(shop("a"), shop("b"))) }
    var catalogs: suspend () -> Result<DiscoveryCatalogs> = { Result.success(DiscoveryCatalogs(cities = listOf(minskCity, brestCity))) }
    val queries = mutableListOf<ShopQuery>()
    var catalogCalls = 0
    override suspend fun search(query: ShopQuery): Result<Paged<ShopSummary>> { queries += query; return search.invoke(query) }
    var map: suspend (MapBounds, Float, DiscoveryFilters) -> Result<MapContent> = { _, _, _ -> Result.success(MapContent()) }
    val mapRequests = mutableListOf<Triple<MapBounds, Float, DiscoveryFilters>>()
    val mapCriteria = mutableListOf<DiscoveryCriteria>()
    override suspend fun mapContent(bounds: MapBounds, zoom: Float, criteria: DiscoveryCriteria): Result<MapContent> {
        mapCriteria += criteria
        mapRequests += Triple(bounds, zoom, criteria.filters)
        return map.invoke(bounds, zoom, criteria.filters)
    }
    override suspend fun catalogs(): Result<DiscoveryCatalogs> { catalogCalls++; return catalogs.invoke() }
}

internal class FakeAccount(key: String? = "one") : DiscoveryAccount {
    val key = MutableStateFlow(key)
    override val accountKey = this.key
    override fun currentAccountKey() = key.value
}

internal class FakeFavorites : DiscoveryFavorites {
    val flow = MutableStateFlow<Set<String>>(emptySet())
    var set: suspend (ShopSummary, Boolean) -> Result<Unit> = { _, _ -> Result.success(Unit) }
    val changes = mutableListOf<Pair<String, Boolean>>()
    override val ids = flow
    override suspend fun favoriteIds() = flow.value
    override suspend fun setFavorite(shop: ShopSummary, favorite: Boolean): Result<Unit> { changes += shop.id to favorite; return set.invoke(shop, favorite) }
}

internal class FakeCity(saved: String? = null) : DiscoveryCity {
    val flow = MutableStateFlow(saved)
    override val selected = flow
    override suspend fun resolve(cities: List<CityEntry>): String? = (flow.value?.takeIf { id -> cities.any { it.id == id } } ?: cities.firstOrNull()?.id).also { flow.value = it }
}

internal class FakeLocation : DiscoveryLocation {
    val flow = MutableStateFlow<Coordinates?>(null)
    var refreshes = 0
    override val current = flow
    override fun refresh() { refreshes++ }
}

internal class RecordingAnalytics : DiscoveryAnalytics {
    val searches = mutableListOf<String>()
    val results = mutableListOf<Int>()
    val failures = mutableListOf<Throwable>()
    val filters = mutableListOf<Triple<List<String>, List<String>, FilterAction>>()
    val gates = mutableListOf<DiscoveryGate>()
    val opened = mutableListOf<Int?>()
    var filtersOpened = 0

    override fun searchStarted(text: String, filterKeys: List<String>, nearby: Boolean, citySlug: String?, criteria: Any): SearchRun {
        searches += "$text|${filterKeys.joinToString(",")}|$nearby|$citySlug"
        return object : SearchRun {
            override fun results(count: Int) { results += count }
            override fun failed(error: Throwable) { failures += error }
        }
    }
    override fun shopOpened(position: Int?) { opened += position }
    override fun filtersOpened() { filtersOpened++ }
    override fun filtersChanged(filterKeys: List<String>, changedKeys: List<String>, action: FilterAction) { filters += Triple(filterKeys, changedKeys, action) }
    override fun gateShown(gate: DiscoveryGate) { gates += gate }
    val geolocations = mutableListOf<Triple<String, Boolean, GeolocationOutcome>>()
    override fun geolocation(placement: String, userTriggered: Boolean, outcome: GeolocationOutcome) { geolocations += Triple(placement, userTriggered, outcome) }

    val interactions = mutableListOf<MapInteraction>()
    val mapSearches = mutableListOf<String>()
    val mapSearchResults = mutableListOf<Int>()
    val mapSearchFailures = mutableListOf<Throwable>()
    val mapOpened = mutableListOf<Boolean>()
    override fun mapInteracted(action: MapInteraction, resultCount: Int?, durationMs: Long?, enabled: Boolean?) { interactions += action }
    override fun mapSearchStarted(text: String, filterKeys: List<String>, citySlug: String?, criteria: Any): SearchRun {
        mapSearches += "$text|${filterKeys.joinToString(",")}|$citySlug"
        return object : SearchRun {
            override fun results(count: Int) { mapSearchResults += count }
            override fun failed(error: Throwable) { mapSearchFailures += error }
        }
    }
    override fun mapShopOpened(fromSearch: Boolean) { mapOpened += fromSearch }
    val overlays = mutableListOf<DiscoveryOverlay>()
    override fun overlayShown(overlay: DiscoveryOverlay) { overlays += overlay }
}

internal class FakeShopDetails : com.coffeepeek.feature.discovery.api.DiscoveryShopDetails {
    var answer: suspend (String) -> Result<com.coffeepeek.feature.discovery.api.MapShopDetails> = { id -> Result.success(sampleDetails(id)) }
    val requests = mutableListOf<String>()
    override suspend fun details(shopId: String): Result<com.coffeepeek.feature.discovery.api.MapShopDetails> { requests += shopId; return answer.invoke(shopId) }
}

internal fun sampleDetails(id: String) = com.coffeepeek.feature.discovery.api.MapShopDetails(photoUrl = "photo-$id", rating = 4.5, reviewCount = 3, isOpen = true,
    hours = com.coffeepeek.feature.discovery.api.MapOpeningHours(false, "9:00", "21:00"))
internal fun marker(id: String, lat: Double = 53.9, lon: Double = 27.56) = MapMarker(id, "Shop $id", Coordinates(lat, lon))
