package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryFavorites
import com.coffeepeek.feature.discovery.api.DiscoveryGate
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.api.FilterAction
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
    override suspend fun mapContent(bounds: MapBounds, zoom: Float, filters: DiscoveryFilters): Result<MapContent> = error("Unused")
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

    override fun searchStarted(text: String, filterKeys: List<String>, nearby: Boolean, citySlug: String?): SearchRun {
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
    override fun geolocation(placement: String, userTriggered: Boolean, outcome: GeolocationOutcome) = Unit
}
