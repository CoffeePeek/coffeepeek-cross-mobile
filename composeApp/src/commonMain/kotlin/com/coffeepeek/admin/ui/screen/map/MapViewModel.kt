package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.analytics.*

import com.coffeepeek.admin.ui.screen.feed.FeedFiltersUi
import com.coffeepeek.admin.ui.screen.feed.toShopFilters
import com.coffeepeek.admin.ui.screen.feed.analyticsValues
import com.coffeepeek.admin.ui.favorites.withFavoriteMembership
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.model.ShopLocation
import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.City
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapContent
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.model.ShopSchedule
import com.coffeepeek.domain.model.ShopCatalogs
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

private const val MIN_MAP_ZOOM = 3f
private const val MAX_MAP_ZOOM = 20f

data class MapUiState(
    val nearbyShops: List<MapShop> = emptyList(),
    val nearbyOrigin: GeoPoint? = null,
    val shops: List<MapShop> = emptyList(),
    val clusters: List<MapCluster> = emptyList(),
    val zones: List<MapCoffeeZone> = emptyList(),
    val zoneGroups: List<MapCoffeeZone> = emptyList(),
    val showZones: Boolean = true,
    val selectedShop: MapShop? = null,
    val selectedZone: MapCoffeeZone? = null,
    val selectedShopDetails: CoffeeShopDetails? = null,
    val cardDetails: Map<String, CoffeeShopDetails> = emptyMap(),
    val searchOrigin: ShopLocation? = null,
    val isLoadingShopDetails: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val searchResults: List<CoffeeShop> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchFailed: Boolean = false,
    val showSearchSuggestions: Boolean = false,
    val filters: FeedFiltersUi = FeedFiltersUi(),
    val cities: List<City> = emptyList(),
    val beans: List<CatalogItem> = emptyList(),
    val equipment: List<CatalogItem> = emptyList(),
    val roasters: List<CatalogItem> = emptyList(),
    val brewMethods: List<CatalogItem> = emptyList(),
    val shopTags: List<CatalogItem> = emptyList(),
    val showFilters: Boolean = false,
    val activeBounds: MapBounds? = null,
    val pendingBounds: MapBounds? = null,
    val activeZoom: Float? = null,
    val pendingZoom: Float? = null,
    val isTruncated: Boolean = false,
    val myLocationRequest: Int = 0,
    val cameraTarget: Pair<Double, Double>? = null,
    val cameraZoom: Float? = null,
) {
}

class MapViewModel(
    private val shopRepository: ShopRepository,
    private val cityPreference: CityPreference,
    private val mapSource: MapContentSource,
    private val sessions: SessionReader,
    favoriteMembership: Flow<Result<Set<String>>>,
) : BaseViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val searchAnalytics = SearchTracker(Catalog.Shops, ResultSource.Map)
    private var selectionFromSearch = false
    fun openedShop() = searchAnalytics.open(Entity.Shop, includeSearch = selectionFromSearch)
    private var boundsJob: Job? = null
    private var nearbyJob: Job? = null
    private var queryJob: Job? = null
    private val searchActive = MutableStateFlow(false)
    private var detailsJob: Job? = null
    private var boundsPauseJob: Job? = null
    private var selectionVersion = 0
    private var suppressBoundsUpdates = false
    private var isCityReady = false
    private var zonesCityId: String? = null
    // Area actually requested last time (visible bounds + prefetch margin) and its zoom level.
    private var loadedArea: MapBounds? = null
    private var loadedZoomLevel: Int? = null
    private var loadedOwner: RequestOwner? = null
    private var accountKey = currentAccountKey()
    private var favoriteIds: Set<String>? = null
    private val requestGeneration = MutableStateFlow(0)
    private data class RequestOwner(val account: String?, val generation: Int)

    init {
        sessions.observeSession().onEach { onAccount() }.launchIn(workScope)
        favoriteMembership.onEach { result ->
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            result.getOrNull()?.let(::onFavorites)
        }.launchIn(workScope)
        loadCatalogs()
        cityPreference.selectedCityId
            .onEach { cityId ->
                if (!isCityReady || cityId == null || cityId == _state.value.filters.cityId) {
                    return@onEach
                }
                syncSearch(_state.value.query, _state.value.filters.copy(cityId = cityId), _state.value.searchOrigin)
            }
            .launchIn(workScope)
    }

    private fun currentAccountKey(): String? = sessions.peekSession()
        ?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }

    private fun requestOwner() = RequestOwner(currentAccountKey(), requestGeneration.value)

    private fun isCurrent(owner: RequestOwner): Boolean =
        owner.account == currentAccountKey() && owner.generation == requestGeneration.value

    private fun onAccount() {
        val current = currentAccountKey()
        if (current == accountKey) return
        accountKey = current
        favoriteIds = null
        _state.update { it.copy(cardDetails = emptyMap()) }
        refreshPersonalResults()
    }

    private fun onFavorites(ids: Set<String>) {
        onAccount()
        val changed = favoriteIds != null && favoriteIds != ids
        favoriteIds = ids
        _state.update { state ->
            state.copy(cardDetails = state.cardDetails.mapValues { it.value.withFavoriteMembership(ids) },
                selectedShopDetails = state.selectedShopDetails?.withFavoriteMembership(ids),
                searchResults = state.searchResults.map { it.withFavoriteMembership(ids) })
        }
        if (changed && _state.value.filters.favoritesOnly) refreshPersonalResults()
    }

    private fun refreshPersonalResults() {
        val suggestionsVisible = _state.value.showSearchSuggestions
        updateSearchCriteria(_state.value.query)
        if (suggestionsVisible) scheduleSuggestions(_state.value.query, trackSearch = false)
    }

    fun onBoundsChanged(bounds: MapBounds, zoom: Float) {
        if (_state.value.nearbyOrigin == null) {
            onNearbyOriginChanged((bounds.minLat + bounds.maxLat) / 2, (bounds.minLon + bounds.maxLon) / 2)
        }
        // Always remember the latest viewport — even while paused — so it can be loaded afterwards.
        _state.update {
            it.copy(
                pendingBounds = bounds,
                pendingZoom = zoom,
            )
        }
        if (suppressBoundsUpdates) return
        // Small pans inside the prefetched area at the same zoom level need no new request.
        val area = loadedArea
        if (area != null && area.contains(bounds) && loadedZoomLevel == zoom.toInt() && loadedOwner?.let(::isCurrent) == true) return
        boundsJob?.cancel()
        loadBounds(bounds, zoom)
    }

    fun searchCurrentArea() {
        val bounds = _state.value.pendingBounds ?: _state.value.activeBounds ?: return
        val zoom = _state.value.pendingZoom ?: _state.value.activeZoom ?: return
        loadBounds(bounds, zoom)
    }

    fun onShopSelected(shop: MapShop) {
        onAccount()
        selectionFromSearch = false
        Analytics.track(AnalyticsEvent.MAP_INTERACTION, "action" to "marker_selected")
        hideSearchSuggestions()
        if (_state.value.selectedShop?.id == shop.id) {
            _state.update { it.copy(selectedZone = null) }
            return
        }
        _state.update { it.copy(selectedZone = null) }
        selectShop(shop)
    }

    fun onCarouselShopSelected(shop: MapShop) {
        onShopSelected(shop)
        _state.update { it.copy(cameraTarget = shop.latitude to shop.longitude, cameraZoom = 16f) }
        pauseBoundsUpdates(700)
    }

    fun onNearbyOriginChanged(latitude: Double, longitude: Double) {
        if (!latitude.isFinite() || latitude !in -85.0..85.0 || !longitude.isFinite() || longitude !in -180.0..180.0) return
        val origin = GeoPoint(latitude, longitude)
        if (_state.value.nearbyOrigin == origin) return
        _state.update { it.copy(nearbyOrigin = origin) }
        loadNearbyShops(origin)
    }

    private fun loadNearbyShops(origin: GeoPoint) {
        if (!isCityReady) return
        nearbyJob?.cancel()
        val state = _state.value
        val request = state.filters.toShopFilters(state.query, state.searchOrigin)
        nearbyJob = workScope.launch {
            val owner = requestOwner()
            val operation = Analytics.service?.operationScope()
            val result = mapSource.load(nearbyMapBounds(origin), 22f, request)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(owner)) return@launch
            result.onSuccess { content ->
                if (operation != null) Analytics.service?.track(AnalyticsEvent.MAP_INTERACTION, mapOf("action" to "nearby_loaded", "result_count" to content.shops.size), operation)
                val nearest = nearestMapShops(content.shops, origin)
                _state.update { if (isCurrent(owner)) it.copy(nearbyShops = nearest) else it }
            }.onFailure { if (operation != null) Analytics.service?.track(AnalyticsEvent.MAP_INTERACTION, mapOf("action" to "nearby_failed"), operation) }
        }
    }

    fun onZoneSelected(zone: MapCoffeeZone) {
        _state.update {
            it.copy(
                selectedZone = zone,
            )
        }
    }

    fun showSelectedZoneShops() {
        val zone = _state.value.selectedZone ?: return
        _state.update {
            it.copy(
                selectedZone = null,
                cameraTarget = zone.latitude to zone.longitude,
                cameraZoom = 14.5f,
            )
        }
        pauseBoundsUpdates(700)
    }

    fun toggleZones() {
        Analytics.track(AnalyticsEvent.MAP_INTERACTION, "action" to "zones_toggled", "enabled" to !_state.value.showZones)
        _state.update { it.copy(showZones = !it.showZones, selectedZone = null) }
    }

    fun clearZoneSelection() {
        _state.update { it.copy(selectedZone = null) }
    }

    fun syncSearch(query: String, filters: FeedFiltersUi, origin: ShopLocation?) {
        val current = _state.value
        val resolved = filters.copy(cityId = filters.cityId ?: current.filters.cityId)
        if (current.query == query && current.filters == resolved && current.searchOrigin == origin) return
        _state.update { it.copy(filters = resolved, searchOrigin = origin) }
        updateSearchCriteria(query)
    }

    fun setSearchActive(active: Boolean) {
        if (searchActive.value == active) return
        searchActive.value = active
        if (!active) hideSearchSuggestions()
    }

    private fun invalidateSearchResults() {
        requestGeneration.update { it + 1 }
        boundsJob?.cancel()
        nearbyJob?.cancel()
        detailsJob?.cancel()
        selectionVersion++
        loadedArea = null
        zonesCityId = null
        _state.update {
            it.copy(shops = emptyList(), nearbyShops = emptyList(), clusters = emptyList(), zones = emptyList(),
                zoneGroups = emptyList(), selectedShop = null, selectedZone = null, selectedShopDetails = null,
                isLoadingShopDetails = false, isLoading = false, isTruncated = false)
        }
    }

    private fun hideSearchSuggestions() {
        queryJob?.cancel()
        _state.update { it.copy(searchResults = emptyList(), isSearchLoading = false, searchFailed = false, showSearchSuggestions = false) }
    }

    fun onQueryChange(query: String) {
        updateSearchCriteria(query)
        scheduleSuggestions(query, trackSearch = true)
    }

    private fun updateSearchCriteria(query: String) {
        invalidateSearchResults()
        hideSearchSuggestions()
        selectionFromSearch = false
        _state.update { it.copy(query = query) }
        _state.value.nearbyOrigin?.let { loadNearbyShops(it) }
        searchCurrentArea()
    }

    private fun scheduleSuggestions(query: String, trackSearch: Boolean) {
        if (!searchActive.value || query.isBlank()) return
        _state.update { it.copy(isSearchLoading = true, showSearchSuggestions = true) }
        queryJob = workScope.launch {
            delay(350)
            requestSuggestions(query, trackSearch)
        }
    }

    private suspend fun requestSuggestions(query: String, trackSearch: Boolean) {
        if (!searchActive.value) return
        val owner = requestOwner()
        val state = _state.value
        val filters = state.filters
        val search = if (trackSearch) searchAnalytics.begin(query.trim() to filters, query, filters.analyticsValues().keys.toList()) else null
        val result = shopRepository.searchShops(filters.toShopFilters(query, state.searchOrigin, pageSize = 3))
        currentCoroutineContext().ensureActive()
        if (!searchActive.value || !isCurrent(owner) || _state.value.query != query || _state.value.filters != filters) return
        result.onSuccess { page ->
            search?.results(page.totalCount)
            _state.update { if (isCurrent(owner)) it.copy(searchResults = page.items, isSearchLoading = false) else it }
        }.onFailure {
            search?.failed(safeAnalyticsError(it))
            _state.update { if (isCurrent(owner)) it.copy(isSearchLoading = false, searchFailed = true) else it }
        }
    }

    fun onSearchResultSelected(shop: CoffeeShop) {
        onAccount()
        queryJob?.cancel()
        val marker = shop.toMapShopOrNull()
        if (marker != null) showSearchResult(marker)
        else queryJob = workScope.launch { resolveSearchResult(shop) }
    }

    private suspend fun resolveSearchResult(shop: CoffeeShop) {
        val owner = requestOwner()
        val query = _state.value.query
        _state.update { it.copy(isSearchLoading = true, searchFailed = false) }
        val result = _state.value.cardDetails[shop.id]?.let { Result.success(it) } ?: shopRepository.getShopDetails(shop.id)
        currentCoroutineContext().ensureActive()
        if (!isCurrent(owner) || _state.value.query != query) return
        val details = result.getOrNull()
        val marker = details?.shop?.toMapShopOrNull()
        if (details == null || marker == null) {
            _state.update { if (isCurrent(owner)) it.copy(isSearchLoading = false, searchFailed = true) else it }
            return
        }
        _state.update { if (isCurrent(owner)) it.copy(cardDetails = it.cardDetails + (shop.id to details)) else it }
        showSearchResult(marker, owner)
    }

    private fun showSearchResult(marker: MapShop, owner: RequestOwner = requestOwner()) {
        if (!isCurrent(owner)) return
        selectionFromSearch = true
        Analytics.track(AnalyticsEvent.MAP_INTERACTION, "action" to "search_selected")
        _state.update { current ->
            if (!isCurrent(owner)) return@update current
            current.copy(
                searchResults = emptyList(), isSearchLoading = false,
                searchFailed = false, showSearchSuggestions = false,
                shops = (current.shops + marker).distinctBy { it.id },
                cameraTarget = marker.latitude to marker.longitude, cameraZoom = 16f,
            )
        }
        selectShop(marker, owner)
        pauseBoundsUpdates(700)
    }

    fun requestMyLocation() {
        _state.update { it.copy(myLocationRequest = it.myLocationRequest + 1) }
    }

    fun zoomIn() = changeZoom(1f)

    fun zoomOut() = changeZoom(-1f)

    private fun changeZoom(delta: Float) {
        val current = _state.value
        val bounds = current.pendingBounds ?: current.activeBounds ?: return
        val zoom = current.pendingZoom ?: current.activeZoom ?: return
        val nextZoom = (zoom + delta).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
        if (nextZoom == zoom) return
        Analytics.track(AnalyticsEvent.MAP_INTERACTION, "action" to "zoomed")

        _state.update {
            it.copy(
                cameraTarget = (bounds.minLat + bounds.maxLat) / 2.0 to
                    (bounds.minLon + bounds.maxLon) / 2.0,
                cameraZoom = nextZoom,
                pendingZoom = nextZoom,
            )
        }
        pauseBoundsUpdates(500)
    }

    fun focusOnShop(focus: com.coffeepeek.admin.ui.Navigator.MapShopFocus) {
        onAccount()
        hideSearchSuggestions()
        val shop = MapShop(
            id = focus.shopId,
            title = focus.title,
            latitude = focus.latitude,
            longitude = focus.longitude,
        )
        workScope.launch {
            _state.update { current ->
                current.copy(
                    shops = (current.shops + shop).distinctBy { it.id },
                    cameraTarget = focus.latitude to focus.longitude,
                    cameraZoom = 16f,
                )
            }
            selectShop(shop)
            pauseBoundsUpdates(700)
        }
    }

    fun onCameraTargetApplied() {
        _state.update { it.copy(cameraTarget = null, cameraZoom = null) }
    }

    fun onMyLocationApplied(latitude: Double, longitude: Double) {
        if (_state.value.nearbyOrigin == GeoPoint(latitude, longitude)) {
            _state.value.nearbyOrigin?.let { loadNearbyShops(it) }
        } else {
            onNearbyOriginChanged(latitude, longitude)
        }
        _state.update {
            it.copy(
                cameraTarget = latitude to longitude,
                cameraZoom = 15f,
            )
        }
        pauseBoundsUpdates(700)
    }

    private fun loadCatalogs() {
        workScope.launch {
            shopRepository.getCatalogs()
                .onSuccess { applyCatalogs(it) }
                .onFailure { if (it is CancellationException) throw it }
            isCityReady = true
            _state.value.nearbyOrigin?.let { loadNearbyShops(it) }
            val current = _state.value
            val bounds = current.pendingBounds
            val zoom = current.pendingZoom
            if (bounds != null && zoom != null) loadBounds(bounds, zoom)
        }
    }

    private suspend fun applyCatalogs(catalogs: ShopCatalogs) {
        val cityId = cityPreference.resolve(catalogs.cities)
        _state.update {
            it.copy(
                filters = it.filters.copy(cityId = cityId),
                cities = catalogs.cities,
                beans = catalogs.beans,
                equipment = catalogs.equipment,
                roasters = catalogs.roasters,
                brewMethods = catalogs.brewMethods,
                shopTags = catalogs.shopTags,
            )
        }
    }

    private fun loadBounds(bounds: MapBounds, zoom: Float) {
        if (!isCityReady) return
        boundsJob?.cancel()
        // Filters/query may have changed: until this request succeeds, the old area isn't trusted.
        loadedArea = null
        boundsJob = workScope.launch {
            delay(250)
            _state.update {
                it.copy(
                    isLoading = true,
                    activeBounds = bounds,
                    pendingBounds = bounds,
                    activeZoom = zoom,
                    pendingZoom = zoom,
                )
            }
            requestBounds(bounds.expandedForPrefetch(), zoom)
        }
    }

    private suspend fun requestBounds(area: MapBounds, zoom: Float) {
        val owner = requestOwner()
        val state = _state.value
        val filters = state.filters
        val operation = Analytics.service?.operationScope()
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        val result = mapSource.load(area, zoom, filters.toShopFilters(state.query, state.searchOrigin))
        currentCoroutineContext().ensureActive()
        if (!isCurrent(owner)) return
        result.onSuccess { content ->
            if (operation != null) Analytics.service?.track(AnalyticsEvent.MAP_INTERACTION, mapOf("action" to "loaded", "result_count" to content.shops.size, "duration_ms" to started.elapsedNow().inWholeMilliseconds), operation)
            loadedArea = area
            loadedZoomLevel = zoom.toInt()
            loadedOwner = owner
            applyContent(content, filters.cityId, owner)
        }.onFailure {
            if (operation != null) Analytics.service?.track(AnalyticsEvent.MAP_INTERACTION, mapOf("action" to "load_failed", "duration_ms" to started.elapsedNow().inWholeMilliseconds), operation)
            _state.update { if (isCurrent(owner)) it.copy(isLoading = false) else it }
        }
    }

    private fun applyContent(content: MapContent, cityId: String?, owner: RequestOwner) {
        _state.update { current ->
            if (!isCurrent(owner)) return@update current
            val merged = mergeShops(content.shops, current.selectedShop)
            val knownZones = if (zonesCityId == cityId) current.zones else emptyList()
            zonesCityId = cityId
            val zones = (content.zones + knownZones).distinctBy { it.id }
            current.copy(shops = merged, clusters = content.clusters, zoneGroups = content.zones, zones = zones,
                isTruncated = content.isTruncated, isLoading = false,
                selectedShop = current.selectedShop?.let { selected -> merged.find { it.id == selected.id } ?: selected },
                selectedZone = current.selectedZone?.takeIf { selected -> zones.any { it.id == selected.id } })
        }
    }

    private fun selectShop(shop: MapShop, owner: RequestOwner = requestOwner()) {
        if (!isCurrent(owner)) return
        detailsJob?.cancel()
        val version = ++selectionVersion
        val cachedDetails = _state.value.cardDetails[shop.id]

        _state.update {
            if (!isCurrent(owner)) return@update it
            it.copy(
                selectedShop = shop,
                selectedShopDetails = cachedDetails,
                isLoadingShopDetails = cachedDetails == null,
                shops = mergeShops(it.shops, shop),
            )
        }

        if (cachedDetails != null) return
        detailsJob = workScope.launch { requestDetails(shop.id, version) }
    }

    private suspend fun requestDetails(shopId: String, version: Int) {
        val owner = requestOwner()
        val result = shopRepository.getShopDetails(shopId)
        currentCoroutineContext().ensureActive()
        if (!isCurrent(owner) || version != selectionVersion || _state.value.selectedShop?.id != shopId) return
        result.onSuccess { details ->
            _state.update { if (isCurrent(owner)) it.copy(cardDetails = it.cardDetails + (shopId to details),
                selectedShopDetails = details, isLoadingShopDetails = false) else it }
        }.onFailure { _state.update { if (isCurrent(owner)) it.copy(isLoadingShopDetails = false) else it } }
    }

    private fun pauseBoundsUpdates(durationMs: Long = 600) {
        boundsPauseJob?.cancel()
        boundsPauseJob = workScope.launch {
            suppressBoundsUpdates = true
            try {
                delay(durationMs)
            } finally {
                suppressBoundsUpdates = false
            }
            // Camera moves during the pause were only recorded; load where the camera ended up.
            val current = _state.value
            val bounds = current.pendingBounds ?: return@launch
            val zoom = current.pendingZoom ?: return@launch
            onBoundsChanged(bounds, zoom)
        }
    }

    private fun mergeShops(
        loaded: List<MapShop>,
        pinned: MapShop?,
    ): List<MapShop> = (loaded + listOfNotNull(pinned)).distinctBy { it.id }

}

internal fun formatMapHoursSummary(schedules: List<ShopSchedule>): String? {
    val openSchedule = schedules.firstOrNull { !it.isClosed && it.intervals.isNotEmpty() }
        ?: schedules.firstOrNull()
        ?: return null

    if (openSchedule.isClosed) return "Закрыто"

    val interval = openSchedule.intervals.firstOrNull() ?: return null
    val open = interval.openTime.split(":").take(2).joinToString(":")
    val close = interval.closeTime.split(":").take(2).joinToString(":")
    return "$open – $close"
}

private const val KM_PER_DEGREE_LAT = 111.32
private const val PREFETCH_MIN_MARGIN_KM = 3.0
private const val PREFETCH_VIEWPORT_FRACTION = 0.5

/**
 * Visible bounds padded on every side by half a viewport, but at least [minMarginKm], so moving
 * the map a couple of km stays inside the loaded area.
 * ponytail: ignores the antimeridian (fine for city maps); larger areas raise isTruncated sooner.
 */
internal fun MapBounds.expandedForPrefetch(minMarginKm: Double = PREFETCH_MIN_MARGIN_KM): MapBounds {
    val midLat = (minLat + maxLat) / 2.0
    val kmPerDegreeLon = KM_PER_DEGREE_LAT * cos(midLat * PI / 180.0).coerceAtLeast(0.01)
    val latMargin = max((maxLat - minLat) * PREFETCH_VIEWPORT_FRACTION, minMarginKm / KM_PER_DEGREE_LAT)
    val lonMargin = max((maxLon - minLon) * PREFETCH_VIEWPORT_FRACTION, minMarginKm / kmPerDegreeLon)
    return MapBounds(
        minLat = (minLat - latMargin).coerceAtLeast(-85.0),
        minLon = (minLon - lonMargin).coerceAtLeast(-180.0),
        maxLat = (maxLat + latMargin).coerceAtMost(85.0),
        maxLon = (maxLon + lonMargin).coerceAtMost(180.0),
    )
}

internal fun MapBounds.contains(other: MapBounds): Boolean =
    other.minLat >= minLat && other.maxLat <= maxLat && other.minLon >= minLon && other.maxLon <= maxLon

internal fun CoffeeShop.toMapShopOrNull(): MapShop? {
    val latitude = location?.latitude ?: return null
    val longitude = location?.longitude ?: return null
    return MapShop(
        id = id,
        title = title,
        latitude = latitude,
        longitude = longitude,
        type = type,
    )
}
