package com.coffeepeek.feature.discovery.impl.map

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.api.DiscoveryShopDetails
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.api.MapCamera
import com.coffeepeek.feature.discovery.api.MapInteraction
import com.coffeepeek.feature.discovery.api.MapShopDetails
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.ShopQuery
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.domain.rules.center
import com.coffeepeek.feature.discovery.domain.rules.clampZoom
import com.coffeepeek.feature.discovery.domain.rules.contains
import com.coffeepeek.feature.discovery.domain.rules.expandedForPrefetch
import com.coffeepeek.feature.discovery.domain.rules.nearbyBounds
import com.coffeepeek.feature.discovery.domain.rules.nearestMarkers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

private const val BOUNDS_DEBOUNCE_MS = 250L
private const val SEARCH_DEBOUNCE_MS = 350L
private const val SUGGESTION_COUNT = 3
private const val FOCUS_ZOOM = 16f
private const val MY_LOCATION_ZOOM = 15f
private const val ZONE_ZOOM = 14.5f
private const val PAUSE_AFTER_MOVE_MS = 700L
private const val PAUSE_AFTER_ZOOM_MS = 500L

/**
 * The map as the legacy one kept it: the viewport is loaded 250 ms after it settles with a prefetch margin (a pan inside the loaded area at the same zoom
 * loads nothing), zones seen in a city are kept, the ten nearest shops feed the carousel, the search suggests three shops, and the camera moves the
 * view model asks for pause the viewport updates for a moment. A late answer for another selection or query is dropped.
 */
internal class MapViewModel(
    private val repository: DiscoveryRepository,
    private val city: DiscoveryCity,
    private val location: DiscoveryLocation,
    private val shopDetails: DiscoveryShopDetails,
    private val analytics: DiscoveryAnalytics,
) : MviViewModel<MapState, MapAction, MapEvent>(MapState()) {
    private var cityReady = false
    private var zonesCityId: String? = null
    private var selectionFromSearch = false
    private var selectionVersion = 0
    private var suppressBounds = false
    private var loadedArea: MapBounds? = null
    private var loadedZoomLevel: Int? = null
    private val detailsCache = mutableMapOf<String, MapShopDetails>()
    private var boundsJob: Job? = null
    private var nearbyJob: Job? = null
    private var queryJob: Job? = null
    private var detailsJob: Job? = null
    private var pauseJob: Job? = null

    init {
        viewModelScope.launch { city.selected.collect(::onCity) }
        viewModelScope.launch { location.current.collect { point -> if (point != null) changeNearbyOrigin(point) } }
        loadCatalogs()
    }

    override suspend fun handleActionInternal(action: MapAction) {
        when (action) {
            is MapAction.BoundsChanged -> onBoundsChanged(action.bounds, action.zoom)
            MapAction.SearchArea -> searchCurrentArea()
            is MapAction.SelectMarker -> findMarker(action.shopId)?.let(::selectMarker)
            is MapAction.SelectFromCarousel -> findMarker(action.shopId)?.let { marker ->
                selectMarker(marker)
                moveCamera(marker.coordinates, FOCUS_ZOOM, PAUSE_AFTER_MOVE_MS)
            }
            is MapAction.SelectZone -> currentState.zones.firstOrNull { it.id == action.zoneId }?.let { zone -> updateState { copy(selectedZone = zone) } }
            MapAction.ShowZoneShops -> currentState.selectedZone?.let { zone ->
                updateState { copy(selectedZone = null) }
                moveCamera(zone.center, ZONE_ZOOM, PAUSE_AFTER_MOVE_MS)
            }
            MapAction.ClearZone -> updateState { copy(selectedZone = null) }
            MapAction.ToggleZones -> {
                analytics.mapInteracted(MapInteraction.ZonesToggled, enabled = !currentState.showZones)
                updateState { copy(showZones = !showZones, selectedZone = null) }
            }
            is MapAction.Query -> changeQuery(action.text)
            is MapAction.SelectSuggestion -> selectSuggestion(action.shopId)
            MapAction.RequestMyLocation -> updateState { copy(myLocationRequest = myLocationRequest + 1) }
            is MapAction.MyLocationFound -> onMyLocationFound(action.point)
            MapAction.LocationDenied -> analytics.geolocation(PLACEMENT, userTriggered = currentState.myLocationRequest > 0, outcome = GeolocationOutcome.Unavailable)
            MapAction.ZoomIn -> changeZoom(1f)
            MapAction.ZoomOut -> changeZoom(-1f)
            is MapAction.Focus -> focusOnShop(action.focus.let { MapMarker(it.shopId, it.title, it.coordinates) })
            MapAction.CameraApplied -> updateState { copy(camera = null) }
            is MapAction.OpenShop -> {
                analytics.mapShopOpened(fromSearch = selectionFromSearch)
                sendEvent(MapEvent.OpenShop(action.shopId))
            }
        }
    }

    // ---- what comes from outside ----

    private fun onCity(cityId: String?) {
        if (!cityReady || cityId == null || cityId == currentState.cityId) return
        updateState { copy(cityId = cityId) }
        currentState.nearbyOrigin?.let(::loadNearby)
        searchCurrentArea()
    }

    private fun loadCatalogs() {
        viewModelScope.launch {
            repository.catalogs().onSuccess { catalogs ->
                val cityId = city.resolve(catalogs.cities)
                updateState { copy(cityId = cityId) }
            }
            // A failed catalog still lets the map load: it shows every city then.
            cityReady = true
            currentState.nearbyOrigin?.let(::loadNearby)
            val bounds = currentState.pendingBounds
            val zoom = currentState.pendingZoom
            if (bounds != null && zoom != null) loadBounds(bounds, zoom)
        }
    }

    // ---- the viewport ----

    private fun onBoundsChanged(bounds: MapBounds, zoom: Float) {
        // Until a position is known the middle of the first view is the origin of the carousel, as in the legacy map.
        if (currentState.nearbyOrigin == null) changeNearbyOrigin(bounds.center)
        // The latest viewport is remembered even while the updates are paused, so it can be loaded afterwards.
        updateState { copy(pendingBounds = bounds, pendingZoom = zoom, showSearchArea = false) }
        if (suppressBounds) return
        // A small pan inside the prefetched area at the same zoom level needs no new request.
        if (loadedArea?.contains(bounds) == true && loadedZoomLevel == zoom.toInt()) return
        boundsJob?.cancel()
        loadBounds(bounds, zoom)
    }

    private fun searchCurrentArea() {
        val bounds = currentState.pendingBounds ?: currentState.activeBounds ?: return
        val zoom = currentState.pendingZoom ?: currentState.activeZoom ?: return
        loadBounds(bounds, zoom)
    }

    private fun loadBounds(bounds: MapBounds, zoom: Float) {
        if (!cityReady) return
        boundsJob?.cancel()
        // The filters may have changed: until this request succeeds the old area is not trusted.
        loadedArea = null
        boundsJob = viewModelScope.launch {
            delay(BOUNDS_DEBOUNCE_MS)
            updateState { copy(isLoading = true, activeBounds = bounds, pendingBounds = bounds, activeZoom = zoom, pendingZoom = zoom, showSearchArea = false) }
            val cityId = currentState.cityId
            // A wider area than the visible one, so panning a few kilometers shows shops that are already loaded.
            val area = bounds.expandedForPrefetch()
            val started = TimeSource.Monotonic.markNow()
            val result = repository.mapContent(area, zoom, DiscoveryFilters(cityId = cityId))
            currentCoroutineContext().ensureActive()
            result.onSuccess { content ->
                analytics.mapInteracted(MapInteraction.Loaded, resultCount = content.markers.size, durationMs = started.elapsedNow().inWholeMilliseconds)
                loadedArea = area
                loadedZoomLevel = zoom.toInt()
                updateState {
                    val merged = (content.markers + nearby + listOfNotNull(selectedShop)).distinctBy { it.id }
                    // The server returns the zones of the viewport only; every zone seen in this city is kept, fresh data wins, a city change forgets them.
                    val known = if (zonesCityId == cityId) zones else emptyList()
                    zonesCityId = cityId
                    val allZones = (content.zones + known).distinctBy { it.id }
                    copy(
                        markers = merged, zones = allZones, isTruncated = content.isTruncated, isLoading = false,
                        selectedShop = selectedShop?.let { selected -> merged.firstOrNull { it.id == selected.id } ?: selected },
                        selectedZone = selectedZone?.takeIf { selected -> allZones.any { it.id == selected.id } },
                    )
                }
            }.onFailure {
                analytics.mapInteracted(MapInteraction.LoadFailed, durationMs = started.elapsedNow().inWholeMilliseconds)
                updateState { copy(isLoading = false, showSearchArea = true) }
            }
        }
    }

    // ---- nearby ----

    private fun changeNearbyOrigin(point: Coordinates) {
        if (!point.isValid || point.latitude !in -85.0..85.0 || currentState.nearbyOrigin == point) return
        updateState { copy(nearbyOrigin = point) }
        loadNearby(point)
    }

    private fun loadNearby(origin: Coordinates) {
        if (!cityReady) return
        nearbyJob?.cancel()
        val cityId = currentState.cityId
        nearbyJob = viewModelScope.launch {
            val result = repository.mapContent(nearbyBounds(origin), NEARBY_ZOOM, DiscoveryFilters(cityId = cityId))
            currentCoroutineContext().ensureActive()
            result.onSuccess { content ->
                analytics.mapInteracted(MapInteraction.NearbyLoaded, resultCount = content.markers.size)
                val nearest = nearestMarkers(content.markers, origin)
                updateState { copy(nearby = nearest, markers = (markers + nearest).distinctBy { it.id }) }
            }.onFailure { analytics.mapInteracted(MapInteraction.NearbyFailed) }
        }
    }

    private fun onMyLocationFound(point: Coordinates) {
        analytics.geolocation(PLACEMENT, userTriggered = currentState.myLocationRequest > 0, outcome = GeolocationOutcome.Available)
        if (currentState.nearbyOrigin == point) loadNearby(point) else changeNearbyOrigin(point)
        moveCamera(point, MY_LOCATION_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    // ---- selection ----

    private fun findMarker(id: String): MapMarker? = currentState.markers.firstOrNull { it.id == id } ?: currentState.nearby.firstOrNull { it.id == id }

    private fun selectMarker(marker: MapMarker) {
        selectionFromSearch = false
        analytics.mapInteracted(MapInteraction.MarkerSelected)
        setQuery("")
        updateState { copy(selectedZone = null) }
        if (currentState.selectedShop?.id == marker.id) return
        selectShop(marker)
    }

    private fun selectShop(marker: MapMarker) {
        detailsJob?.cancel()
        val version = ++selectionVersion
        val cached = detailsCache[marker.id]
        updateState { copy(selectedShop = marker, details = cached, isLoadingDetails = cached == null, markers = if (markers.any { it.id == marker.id }) markers else markers + marker) }
        if (cached != null) return
        detailsJob = viewModelScope.launch {
            val result = shopDetails.details(marker.id)
            currentCoroutineContext().ensureActive()
            if (version != selectionVersion) return@launch
            result.onSuccess { details ->
                detailsCache[marker.id] = details
                updateState { copy(details = details, isLoadingDetails = false) }
            }.onFailure { updateState { copy(isLoadingDetails = false) } }
        }
    }

    private fun focusOnShop(marker: MapMarker) {
        setQuery("")
        updateState { copy(markers = (markers + marker).distinctBy { it.id }) }
        selectShop(marker)
        moveCamera(marker.coordinates, FOCUS_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    // ---- search ----

    private fun changeQuery(text: String) {
        queryJob?.cancel()
        updateState { copy(query = text, suggestions = emptyList(), isSearchLoading = text.isNotBlank(), searchFailed = false, showSuggestions = text.isNotBlank()) }
        if (text.isBlank()) return
        queryJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val cityId = currentState.cityId
            val keys = if (cityId != null) listOf("city") else emptyList()
            val run = analytics.mapSearchStarted(text.trim(), keys, cityId)
            val result = repository.search(ShopQuery(text.trim(), DiscoveryFilters(cityId = cityId), page = 1, pageSize = SUGGESTION_COUNT))
            currentCoroutineContext().ensureActive()
            if (currentState.query != text) return@launch
            result.onSuccess { page ->
                run.results(page.totalCount)
                updateState { copy(suggestions = page.items.filter { it.coordinates != null }, isSearchLoading = false) }
            }.onFailure { error ->
                run.failed(error)
                updateState { copy(isSearchLoading = false, searchFailed = true) }
            }
        }
    }

    private fun setQuery(text: String) {
        if (currentState.query.isEmpty() && text.isEmpty()) return
        changeQuery(text)
    }

    private fun selectSuggestion(shopId: String) {
        val shop = currentState.suggestions.firstOrNull { it.id == shopId } ?: return
        val marker = shop.toMarker() ?: return
        selectionFromSearch = true
        analytics.mapInteracted(MapInteraction.SearchSelected)
        queryJob?.cancel()
        updateState { copy(query = "", suggestions = emptyList(), isSearchLoading = false, searchFailed = false, showSuggestions = false, markers = (markers + marker).distinctBy { it.id }) }
        selectShop(marker)
        moveCamera(marker.coordinates, FOCUS_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    // ---- the camera ----

    private fun changeZoom(delta: Float) {
        val bounds = currentState.pendingBounds ?: currentState.activeBounds ?: return
        val zoom = currentState.pendingZoom ?: currentState.activeZoom ?: return
        val next = clampZoom(zoom + delta)
        if (next == zoom) return
        analytics.mapInteracted(MapInteraction.Zoomed)
        updateState { copy(camera = MapCamera(bounds.center, next), pendingZoom = next, showSearchArea = false) }
        pauseBounds(PAUSE_AFTER_ZOOM_MS)
    }

    private fun moveCamera(target: Coordinates, zoom: Float, pauseMs: Long) {
        updateState { copy(camera = MapCamera(target, zoom)) }
        pauseBounds(pauseMs)
    }

    private fun pauseBounds(durationMs: Long) {
        pauseJob?.cancel()
        pauseJob = viewModelScope.launch {
            suppressBounds = true
            try {
                delay(durationMs)
            } finally {
                suppressBounds = false
            }
            // Camera moves during the pause were only recorded: load where the camera ended up.
            val bounds = currentState.pendingBounds ?: return@launch
            val zoom = currentState.pendingZoom ?: return@launch
            onBoundsChanged(bounds, zoom)
        }
    }
}

private const val PLACEMENT = "map"
private const val NEARBY_ZOOM = 22f

private fun ShopSummary.toMarker(): MapMarker? = coordinates?.let { MapMarker(id, title, it, kind) }
