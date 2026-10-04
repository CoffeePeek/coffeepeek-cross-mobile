package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.City
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.model.ShopSchedule
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
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

data class MapFiltersUi(
    val cityId: String? = null,
    val coffeeFocus: String? = null,
    val priceRange: Int? = null,
    val minRating: Double? = null,
    val roasterIds: Set<String> = emptySet(),
    val beanIds: Set<String> = emptySet(),
    val equipmentIds: Set<String> = emptySet(),
    val brewMethodIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
)

data class MapUiState(
    val nearbyShops: List<MapShop> = emptyList(),
    val nearbyOrigin: GeoPoint? = null,
    val shops: List<MapShop> = emptyList(),
    val clusters: List<MapCluster> = emptyList(),
    val zones: List<MapCoffeeZone> = emptyList(),
    val showZones: Boolean = true,
    val selectedShop: MapShop? = null,
    val selectedZone: MapCoffeeZone? = null,
    val selectedShopDetails: CoffeeShopDetails? = null,
    val isLoadingShopDetails: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val searchResults: List<CoffeeShop> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchFailed: Boolean = false,
    val showSearchSuggestions: Boolean = false,
    val filters: MapFiltersUi = MapFiltersUi(),
    val cities: List<City> = emptyList(),
    val beans: List<CatalogItem> = emptyList(),
    val equipment: List<CatalogItem> = emptyList(),
    val roasters: List<CatalogItem> = emptyList(),
    val brewMethods: List<CatalogItem> = emptyList(),
    val shopTags: List<CatalogItem> = emptyList(),
    val showFilters: Boolean = false,
    val showSearchArea: Boolean = false,
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
) : BaseViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private var boundsJob: Job? = null
    private var nearbyJob: Job? = null
    private var queryJob: Job? = null
    private var detailsJob: Job? = null
    private var boundsPauseJob: Job? = null
    private var selectionVersion = 0
    private val detailsCache = mutableMapOf<String, CoffeeShopDetails>()
    private var suppressBoundsUpdates = false
    private var isCityReady = false
    private var zonesCityId: String? = null
    // Area actually requested last time (visible bounds + prefetch margin) and its zoom level.
    private var loadedArea: MapBounds? = null
    private var loadedZoomLevel: Int? = null

    init {
        loadCatalogs()
        cityPreference.selectedCityId
            .onEach { cityId ->
                if (!isCityReady || cityId == null || cityId == _state.value.filters.cityId) {
                    return@onEach
                }
                _state.update { it.copy(filters = it.filters.copy(cityId = cityId)) }
                _state.value.nearbyOrigin?.let { loadNearbyShops(it) }
                searchCurrentArea()
            }
            .launchIn(workScope)
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
                showSearchArea = false,
            )
        }
        if (suppressBoundsUpdates) return
        // Small pans inside the prefetched area at the same zoom level need no new request.
        val area = loadedArea
        if (area != null && area.contains(bounds) && loadedZoomLevel == zoom.toInt()) return
        boundsJob?.cancel()
        loadBounds(bounds, zoom)
    }

    fun searchCurrentArea() {
        val bounds = _state.value.pendingBounds ?: _state.value.activeBounds ?: return
        val zoom = _state.value.pendingZoom ?: _state.value.activeZoom ?: return
        loadBounds(bounds, zoom)
    }

    fun onShopSelected(shop: MapShop) {
        onQueryChange("")
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
        val filters = _state.value.filters
        nearbyJob = workScope.launch {
            val result = shopRepository.getMapContent(nearbyMapBounds(origin), 22f, ShopFilters(cityId = filters.cityId))
            currentCoroutineContext().ensureActive()
            result.onSuccess { content ->
                val nearest = nearestMapShops(content.shops, origin)
                _state.update { it.copy(nearbyShops = nearest, shops = (it.shops + nearest).distinctBy { shop -> shop.id }) }
            }
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
        _state.update { it.copy(showZones = !it.showZones, selectedZone = null) }
    }

    fun clearZoneSelection() {
        _state.update { it.copy(selectedZone = null) }
    }

    fun onQueryChange(query: String) {
        queryJob?.cancel()
        _state.update {
            it.copy(
                query = query,
                searchResults = emptyList(),
                isSearchLoading = query.isNotBlank(),
                searchFailed = false,
                showSearchSuggestions = query.isNotBlank(),
            )
        }
        if (query.isBlank()) return

        queryJob = workScope.launch {
            delay(350)
            val state = _state.value
            val filters = state.filters
            shopRepository.searchShops(
                ShopFilters(
                    query = query.trim(),
                    cityId = filters.cityId,
                    coffeeFocus = filters.coffeeFocus,
                    roasterIds = filters.roasterIds.toList(),
                    beanIds = filters.beanIds.toList(),
                    equipmentIds = filters.equipmentIds.toList(),
                    brewMethodIds = filters.brewMethodIds.toList(),
                    tagIds = filters.tagIds.toList(),
                    priceRange = filters.priceRange,
                    minRating = filters.minRating,
                    page = 1,
                    pageSize = 3,
                ),
            ).onSuccess { page ->
                _state.update { current ->
                    if (current.query != query) current else current.copy(
                        searchResults = page.items.filter { it.toMapShopOrNull() != null },
                        isSearchLoading = false,
                    )
                }
            }.onFailure {
                _state.update { current ->
                    if (current.query != query) current else current.copy(
                        isSearchLoading = false,
                        searchFailed = true,
                    )
                }
            }
        }
    }

    fun onSearchResultSelected(shop: CoffeeShop) {
        val mapShop = shop.toMapShopOrNull() ?: return
        queryJob?.cancel()
        _state.update { current ->
            current.copy(
                query = "",
                searchResults = emptyList(),
                isSearchLoading = false,
                searchFailed = false,
                showSearchSuggestions = false,
                shops = (current.shops + mapShop).distinctBy { it.id },
                cameraTarget = mapShop.latitude to mapShop.longitude,
                cameraZoom = 16f,
            )
        }
        selectShop(mapShop)
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

        _state.update {
            it.copy(
                cameraTarget = (bounds.minLat + bounds.maxLat) / 2.0 to
                    (bounds.minLon + bounds.maxLon) / 2.0,
                cameraZoom = nextZoom,
                pendingZoom = nextZoom,
                showSearchArea = false,
            )
        }
        pauseBoundsUpdates(500)
    }

    fun focusOnShop(focus: com.coffeepeek.admin.ui.Navigator.MapShopFocus) {
        onQueryChange("")
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
                .onSuccess { catalogs ->
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
                    isCityReady = true
                    _state.value.nearbyOrigin?.let { loadNearbyShops(it) }
                    val current = _state.value
                    val bounds = current.pendingBounds
                    val zoom = current.pendingZoom
                    if (bounds != null && zoom != null) loadBounds(bounds, zoom)
                }
                .onFailure {
                    isCityReady = true
                    _state.value.nearbyOrigin?.let { loadNearbyShops(it) }
                    val current = _state.value
                    val bounds = current.pendingBounds
                    val zoom = current.pendingZoom
                    if (bounds != null && zoom != null) loadBounds(bounds, zoom)
                }
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
                    showSearchArea = false,
                )
            }
            val state = _state.value
            val filters = state.filters
            // Request a wider area than visible, so panning a few km shows already-loaded shops.
            val requestArea = bounds.expandedForPrefetch()
            val result = shopRepository.getMapContent(
                bounds = requestArea,
                zoom = zoom,
                filters = ShopFilters(
                    cityId = filters.cityId,
                    coffeeFocus = filters.coffeeFocus,
                    roasterIds = filters.roasterIds.toList(),
                    beanIds = filters.beanIds.toList(),
                    equipmentIds = filters.equipmentIds.toList(),
                    brewMethodIds = filters.brewMethodIds.toList(),
                    tagIds = filters.tagIds.toList(),
                    priceRange = filters.priceRange,
                    minRating = filters.minRating,
                ),
            )
            currentCoroutineContext().ensureActive()
            result.onSuccess { content ->
                    loadedArea = requestArea
                    loadedZoomLevel = zoom.toInt()
                    _state.update { current ->
                        val merged = mergeShops(content.shops + current.nearbyShops, current.selectedShop)
                        // The API only returns zones for the current viewport, so zooming in used to drop
                        // them. Keep every zone seen for this city; fresh data wins. Reset on city change.
                        val knownZones = if (zonesCityId == filters.cityId) current.zones else emptyList()
                        zonesCityId = filters.cityId
                        val zones = (content.zones + knownZones).distinctBy { it.id }
                        current.copy(
                            shops = merged,
                            clusters = content.clusters,
                            zones = zones,
                            isTruncated = content.isTruncated,
                            isLoading = false,
                            selectedShop = current.selectedShop?.let { selected ->
                                merged.find { it.id == selected.id } ?: selected
                            },
                            selectedZone = current.selectedZone?.takeIf { selected ->
                                zones.any { it.id == selected.id }
                            },
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showSearchArea = true,
                        )
                    }
                }
        }
    }

    private fun selectShop(shop: MapShop) {
        detailsJob?.cancel()
        val version = ++selectionVersion
        val cachedDetails = detailsCache[shop.id]

        _state.update {
            it.copy(
                selectedShop = shop,
                selectedShopDetails = cachedDetails,
                isLoadingShopDetails = cachedDetails == null,
                shops = if (it.shops.any { item -> item.id == shop.id }) {
                    it.shops
                } else {
                    it.shops + shop
                },
            )
        }

        if (cachedDetails != null) return

        detailsJob = workScope.launch {
            shopRepository.getShopDetails(shop.id)
                .onSuccess { details ->
                    detailsCache[shop.id] = details
                    _state.update { current ->
                        if (version != selectionVersion || current.selectedShop?.id != shop.id) return@update current
                        current.copy(
                            selectedShopDetails = details,
                            isLoadingShopDetails = false,
                        )
                    }
                }
                .onFailure {
                    _state.update { current ->
                        if (version != selectionVersion || current.selectedShop?.id != shop.id) return@update current
                        current.copy(isLoadingShopDetails = false)
                    }
                }
        }
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
