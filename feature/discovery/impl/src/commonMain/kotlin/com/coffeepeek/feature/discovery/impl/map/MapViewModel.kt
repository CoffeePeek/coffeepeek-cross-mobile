package com.coffeepeek.feature.discovery.impl.map

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import com.coffeepeek.feature.discovery.api.DiscoveryFavorites
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.api.DiscoveryShopDetails
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.api.MapCamera
import com.coffeepeek.feature.discovery.api.MapInteraction
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.MapContent
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.domain.rules.center
import com.coffeepeek.feature.discovery.domain.rules.clampZoom
import com.coffeepeek.feature.discovery.domain.rules.contains
import com.coffeepeek.feature.discovery.domain.rules.expandedForPrefetch
import com.coffeepeek.feature.discovery.domain.rules.viewportMarkers
import com.coffeepeek.feature.discovery.impl.search.analyticsValues
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

private const val BOUNDS_DEBOUNCE_MS = 250L
private const val CAROUSEL_DEBOUNCE_MS = 500L
private const val SEARCH_DEBOUNCE_MS = 350L
private const val SUGGESTION_COUNT = 3
private const val FOCUS_ZOOM = 16f
private const val MY_LOCATION_ZOOM = 15f
private const val ZONE_ZOOM = 14.5f
private const val PAUSE_AFTER_MOVE_MS = 700L
private const val PAUSE_AFTER_ZOOM_MS = 500L

/**
 * Prefetches the viewport after 250 ms and loads its carousel after 500 ms of camera idle.
 * Keeps known city zones, suggests three search results and pauses reads during requested camera moves.
 * Drops late replies for an obsolete viewport, selection or query.
 */
internal class MapViewModel(
    private val repository: DiscoveryRepository,
    private val city: DiscoveryCity,
    private val location: DiscoveryLocation,
    private val shopDetails: DiscoveryShopDetails,
    private val analytics: DiscoveryAnalytics,
    private val account: DiscoveryAccount,
    favorites: DiscoveryFavorites,
) : MviViewModel<MapState, MapAction, MapEvent>(MapState()) {
    private var cityReady = false
    private var zonesCityId: String? = null
    private var selectionFromSearch = false
    private var selectionVersion = 0
    private var suppressBounds = false
    private var loadedArea: MapBounds? = null
    private var loadedZoomLevel: Int? = null
    private var boundsJob: Job? = null
    private var carouselJob: Job? = null
    private var carouselArea: MapBounds? = null
    private var queryJob: Job? = null
    private var detailsJob: Job? = null
    private var pauseJob: Job? = null
    private var permissionRequestPending = false
    private var locateAfterPermission = false
    private var accountSeen = false
    private var criteriaSupplied = false
    private var searchPresented = false
    private var favoriteIds: Set<String>? = null

    init {
        viewModelScope.launch { city.selected.collect(::onCity) }
        viewModelScope.launch { location.current.collect { point -> if (point != null) changeUserLocation(point) } }
        viewModelScope.launch { account.accountKey.distinctUntilChanged().collect(::onAccount) }
        viewModelScope.launch { favorites.ids.distinctUntilChanged().collect(::onFavorites) }
        loadCatalogs()
    }

    override suspend fun handleActionInternal(action: MapAction) {
        when (action) {
            is MapAction.SyncCriteria -> acceptSharedCriteria(action.criteria)
            is MapAction.PresentationChanged -> setSearchPresentation(action.active)
            is MapAction.BoundsChanged -> onBoundsChanged(action.bounds, action.zoom)
            is MapAction.SelectSuggestion -> selectSuggestion(action.shopId)
            MapAction.Interact -> requestLocationPermission()
            MapAction.RequestMyLocation -> requestLocationPermission(locate = true)
            MapAction.LocationGranted -> onLocationGranted()
            is MapAction.MyLocationFound -> onMyLocationFound(action.point)
            MapAction.LocationDenied -> onLocationDenied()
            MapAction.ZoomIn -> changeZoom(1f)
            MapAction.ZoomOut -> changeZoom(-1f)
            is MapAction.Focus -> focusOnShop(action.focus.let { MapMarker(it.shopId, it.title, it.coordinates) })
            MapAction.CameraApplied -> updateState { copy(camera = null) }
            else -> handleSelection(action)
        }
    }

    private suspend fun handleSelection(action: MapAction) {
        when (action) {
            is MapAction.SelectMarker -> findMarker(action.shopId)?.let(::selectMarker)
            is MapAction.SelectFromCarousel -> selectFromCarousel(action.shopId)
            is MapAction.SelectZone -> currentState.zones.firstOrNull { it.id == action.zoneId }?.let { zone -> updateState { copy(selectedZone = zone) } }
            MapAction.ShowZoneShops -> showZoneShops()
            MapAction.ClearZone -> updateState { copy(selectedZone = null) }
            MapAction.ToggleZones -> {
                analytics.mapInteracted(MapInteraction.ZonesToggled, enabled = !currentState.showZones)
                updateState { copy(showZones = !showZones, selectedZone = null) }
            }
            is MapAction.OpenShop -> {
                analytics.mapShopOpened(fromSearch = selectionFromSearch)
                sendEvent(MapEvent.OpenShop(action.shopId))
            }
            else -> Unit
        }
    }

    private fun selectFromCarousel(shopId: String) {
        val marker = findMarker(shopId) ?: return
        selectMarker(marker)
        moveCamera(marker.coordinates, FOCUS_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    private fun showZoneShops() {
        val zone = currentState.selectedZone ?: return
        updateState { copy(selectedZone = null) }
        moveCamera(zone.center, ZONE_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    // ---- what comes from outside ----

    private fun requestLocationPermission(locate: Boolean = false) {
        if (locate) locateAfterPermission = true
        if (permissionRequestPending || (!locate && currentState.locationPermissionRequest > 0)) return
        permissionRequestPending = true
        updateState { copy(locationPermissionRequest = locationPermissionRequest + 1) }
    }

    private fun onLocationGranted() {
        permissionRequestPending = false
        location.refresh()
        if (locateAfterPermission) updateState { copy(myLocationRequest = myLocationRequest + 1) }
        else analytics.geolocation(PLACEMENT, userTriggered = true, outcome = GeolocationOutcome.Available)
        locateAfterPermission = false
    }

    private fun onLocationDenied() {
        permissionRequestPending = false
        locateAfterPermission = false
        analytics.geolocation(PLACEMENT, userTriggered = currentState.locationPermissionRequest > 0, outcome = GeolocationOutcome.Unavailable)
    }

    private fun onCity(cityId: String?) {
        if (criteriaSupplied || !cityReady || cityId == null || cityId == currentState.cityId) return
        syncCriteria(currentState.criteria.copy(filters = currentState.filters.copy(cityId = cityId)))
    }

    private fun loadCatalogs() {
        viewModelScope.launch {
            repository.catalogs().onSuccess { catalogs ->
                if (!criteriaSupplied) {
                    val cityId = city.resolve(catalogs.cities)
                    if (!criteriaSupplied) updateState { copy(cityId = cityId, filters = filters.copy(cityId = cityId)) }
                }
            }
            // A failed catalog still lets the map load: it shows every city then.
            cityReady = true
            val bounds = currentState.pendingBounds
            val zoom = currentState.pendingZoom
            if (bounds != null && zoom != null) onBoundsChanged(bounds, zoom)
        }
    }

    // ---- the viewport ----

    private fun acceptSharedCriteria(criteria: DiscoveryCriteria) {
        criteriaSupplied = true
        syncCriteria(criteria)
    }

    private fun syncCriteria(criteria: DiscoveryCriteria) {
        if (criteria == currentState.criteria) return
        val selectionChanged = criteria.text != currentState.query || criteria.filters != currentState.filters ||
            criteria.filters.nearbyOnly && criteria.origin != currentState.userLocation
        updateState { copy(query = criteria.text, filters = criteria.filters, cityId = criteria.filters.cityId, userLocation = criteria.origin) }
        if (selectionChanged) invalidateSelection()
    }

    private fun onAccount(key: String?) {
        val first = !accountSeen
        accountSeen = true
        if (first || key != account.currentAccountKey()) return
        updateState { copy(filters = filters.copy(visitedOnly = false, favoritesOnly = false), cardDetails = emptyMap()) }
        invalidateSelection()
    }

    private fun onFavorites(ids: Set<String>) {
        val changed = favoriteIds != null && favoriteIds != ids
        favoriteIds = ids
        if (changed && currentState.filters.favoritesOnly) invalidateSelection()
    }

    private fun invalidateSelection() {
        cancelViewportLoads()
        queryJob?.cancel()
        detailsJob?.cancel()
        selectionVersion++
        loadedArea = null
        carouselArea = null
        zonesCityId = null
        updateState {
            copy(markers = emptyList(), clusters = emptyList(),
                zones = emptyList(), zoneGroups = emptyList(), carouselShops = emptyList(),
                selectedShop = null, selectedZone = null, details = null, isLoadingDetails = false, isTruncated = false)
        }
        loadSuggestions()
        if (!suppressBounds) searchCurrentArea()
    }

    private fun onBoundsChanged(bounds: MapBounds, zoom: Float) {
        // The latest viewport is remembered even while the updates are paused, so it can be loaded afterwards.
        updateState { copy(pendingBounds = bounds, pendingZoom = zoom) }
        if (suppressBounds) return
        loadCarousel(bounds)
        // A small pan inside the prefetched area at the same zoom level needs no new request.
        if (loadedArea?.contains(bounds) == true && loadedZoomLevel == zoom.toInt()) return
        boundsJob?.cancel()
        loadBounds(bounds, zoom)
    }

    private fun searchCurrentArea() {
        val bounds = currentState.pendingBounds ?: currentState.activeBounds ?: return
        val zoom = currentState.pendingZoom ?: currentState.activeZoom ?: return
        carouselArea = null
        loadCarousel(bounds)
        loadBounds(bounds, zoom)
    }

    private fun loadBounds(bounds: MapBounds, zoom: Float) {
        if (!cityReady || !canRequest(currentState.criteria)) return
        boundsJob?.cancel()
        // The filters may have changed: until this request succeeds the old area is not trusted.
        loadedArea = null
        boundsJob = viewModelScope.launch {
            delay(BOUNDS_DEBOUNCE_MS)
            updateState { copy(isLoading = true, activeBounds = bounds, activeZoom = zoom) }
            requestBounds(bounds.expandedForPrefetch(), zoom, currentState.criteria)
        }
    }

    private suspend fun requestBounds(area: MapBounds, zoom: Float, criteria: DiscoveryCriteria) {
        if (!canRequest(criteria)) return
        val started = TimeSource.Monotonic.markNow()
        val accountKey = account.currentAccountKey()
        val result = repository.mapContent(area, zoom, criteria)
        currentCoroutineContext().ensureActive()
        if (account.currentAccountKey() != accountKey || criteria.searchQuery() != currentState.criteria.searchQuery()) return
        result.onSuccess { content ->
            analytics.mapInteracted(MapInteraction.Loaded, resultCount = content.markers.size + content.clusters.sumOf { it.count } + content.zones.sumOf { it.shopCount }, durationMs = started.elapsedNow().inWholeMilliseconds)
            loadedArea = area
            loadedZoomLevel = zoom.toInt()
            applyContent(content, criteria.filters.cityId)
        }.onFailure {
            analytics.mapInteracted(MapInteraction.LoadFailed, durationMs = started.elapsedNow().inWholeMilliseconds)
            updateState { copy(isLoading = false) }
        }
    }

    private fun applyContent(content: MapContent, cityId: String?) {
        val viewport = currentState.pendingBounds ?: currentState.activeBounds
        if (viewport != carouselArea) carouselArea = null
        updateState {
            val merged = (content.markers + listOfNotNull(selectedShop)).distinctBy { it.id }
            val visibleCards = if (viewport != null)
                viewportMarkers(content.markers + carouselShops, viewport, carouselShops) else carouselShops
            val known = if (zonesCityId == cityId) zones else emptyList()
            zonesCityId = cityId
            val allZones = (content.zones + known).distinctBy { it.id }
            copy(
                markers = merged, clusters = content.clusters, zones = allZones, zoneGroups = content.zones,
                carouselShops = visibleCards,
                isTruncated = content.isTruncated, isLoading = false,
                selectedShop = selectedShop?.let { selected -> merged.firstOrNull { it.id == selected.id } ?: selected },
                selectedZone = selectedZone?.takeIf { selected -> allZones.any { it.id == selected.id } },
            )
        }
    }

    // ---- viewport carousel and distance origin ----

    private fun changeUserLocation(point: Coordinates) {
        if (criteriaSupplied || !point.isValid) return
        syncCriteria(currentState.criteria.copy(origin = point))
    }

    private fun loadCarousel(bounds: MapBounds) {
        if (!cityReady) return
        carouselJob?.cancel()
        if (carouselArea == bounds) return
        val criteria = currentState.criteria
        if (!canRequest(criteria)) return
        val accountKey = account.currentAccountKey()
        carouselJob = viewModelScope.launch {
            delay(CAROUSEL_DEBOUNCE_MS)
            val result = repository.mapContent(bounds, CAROUSEL_ZOOM, criteria)
            currentCoroutineContext().ensureActive()
            if (account.currentAccountKey() != accountKey || criteria.searchQuery() != currentState.criteria.searchQuery()) return@launch
            result.onSuccess { content ->
                analytics.mapInteracted(MapInteraction.NearbyLoaded, resultCount = content.markers.size)
                carouselArea = bounds
                applyCarousel(content.markers, bounds)
            }.onFailure { analytics.mapInteracted(MapInteraction.NearbyFailed) }
        }
    }

    private fun applyCarousel(markers: List<MapMarker>, bounds: MapBounds) {
        val shops = viewportMarkers(markers + currentState.markers + listOfNotNull(currentState.selectedShop),
            bounds, currentState.carouselShops)
        val selected = currentState.selectedShop?.takeIf { shop -> shops.any { it.id == shop.id } }
        if (selected == null) {
            detailsJob?.cancel()
            selectionVersion++
        }
        updateState { copy(carouselShops = shops, selectedShop = selected,
            details = details.takeIf { selected != null }, isLoadingDetails = selected != null && isLoadingDetails) }
    }

    private fun onMyLocationFound(point: Coordinates) {
        analytics.geolocation(PLACEMENT, userTriggered = currentState.myLocationRequest > 0, outcome = GeolocationOutcome.Available)
        changeUserLocation(point)
        moveCamera(point, MY_LOCATION_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    // ---- selection ----

    private fun findMarker(id: String): MapMarker? = currentState.markers.firstOrNull { it.id == id } ?: currentState.carouselShops.firstOrNull { it.id == id }

    private fun selectMarker(marker: MapMarker) {
        selectionFromSearch = false
        analytics.mapInteracted(MapInteraction.MarkerSelected)
        hideSuggestions()
        updateState { copy(selectedZone = null) }
        if (currentState.selectedShop?.id == marker.id) return
        selectShop(marker)
    }

    private fun selectShop(marker: MapMarker) {
        detailsJob?.cancel()
        val version = ++selectionVersion
        val cached = currentState.cardDetails[marker.id]
        updateState { copy(selectedShop = marker, details = cached, isLoadingDetails = cached == null, markers = if (markers.any { it.id == marker.id }) markers else markers + marker) }
        if (cached != null) return
        detailsJob = viewModelScope.launch {
            val result = shopDetails.details(marker.id)
            currentCoroutineContext().ensureActive()
            if (version != selectionVersion) return@launch
            result.onSuccess { details ->
                updateState { copy(details = details, cardDetails = cardDetails + (marker.id to details), isLoadingDetails = false) }
            }.onFailure { updateState { copy(isLoadingDetails = false) } }
        }
    }

    private fun focusOnShop(marker: MapMarker) {
        hideSuggestions()
        updateState { copy(markers = (markers + marker).distinctBy { it.id }) }
        selectShop(marker)
        moveCamera(marker.coordinates, FOCUS_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    // ---- search ----

    private fun setSearchPresentation(active: Boolean) {
        if (searchPresented == active) return
        searchPresented = active
        if (active) loadSuggestions() else hideSuggestions()
    }

    private fun loadSuggestions() {
        queryJob?.cancel()
        val criteria = currentState.criteria
        val searching = searchPresented && criteria.text.isNotBlank() && canRequest(criteria)
        updateState { copy(suggestions = emptyList(), isSearchLoading = searching, searchFailed = false, showSuggestions = searching) }
        if (!searching) return
        queryJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            requestSuggestions(criteria)
        }
    }

    private suspend fun requestSuggestions(criteria: DiscoveryCriteria) {
        if (!searchPresented) return
        val query = criteria.searchQuery(pageSize = SUGGESTION_COUNT).copy(text = criteria.text.trim())
        val accountKey = account.currentAccountKey()
        val run = analytics.mapSearchStarted(criteria.text.trim(), criteria.filters.analyticsValues().keys.toList(), query.filters.cityId, criteria = criteria)
        val result = repository.search(query)
        currentCoroutineContext().ensureActive()
        if (!searchPresented || account.currentAccountKey() != accountKey || currentState.criteria.searchQuery() != criteria.searchQuery()) return
        result.onSuccess { page ->
            run.results(page.totalCount)
            updateState { copy(suggestions = page.items, isSearchLoading = false) }
        }.onFailure { error ->
            run.failed(error)
            updateState { copy(isSearchLoading = false, searchFailed = true) }
        }
    }

    private fun canRequest(criteria: DiscoveryCriteria): Boolean =
        account.currentAccountKey() != null || !criteria.filters.visitedOnly && !criteria.filters.favoritesOnly

    private fun hideSuggestions() {
        queryJob?.cancel()
        updateState { copy(suggestions = emptyList(), isSearchLoading = false, searchFailed = false, showSuggestions = false) }
    }

    private fun selectSuggestion(shopId: String) {
        val shop = currentState.suggestions.firstOrNull { it.id == shopId } ?: return
        queryJob?.cancel()
        val marker = shop.toMarker()
        if (marker != null) showSuggestion(marker)
        else queryJob = viewModelScope.launch { resolveSuggestion(shop) }
    }

    private suspend fun resolveSuggestion(shop: ShopSummary) {
        val query = currentState.query
        updateState { copy(isSearchLoading = true, searchFailed = false) }
        val result = currentState.cardDetails[shop.id]?.let { Result.success(it) } ?: shopDetails.details(shop.id)
        currentCoroutineContext().ensureActive()
        if (currentState.query != query) return
        val details = result.getOrNull()
        val coordinates = details?.coordinates?.takeIf { it.isValid }
        if (details == null || coordinates == null) {
            updateState { copy(isSearchLoading = false, searchFailed = true) }
            return
        }
        updateState { copy(cardDetails = cardDetails + (shop.id to details)) }
        showSuggestion(MapMarker(shop.id, shop.title, coordinates, shop.kind))
    }

    private fun showSuggestion(marker: MapMarker) {
        selectionFromSearch = true
        analytics.mapInteracted(MapInteraction.SearchSelected)
        updateState { copy(suggestions = emptyList(), isSearchLoading = false, searchFailed = false, showSuggestions = false, markers = (markers + marker).distinctBy { it.id }) }
        selectShop(marker)
        moveCamera(marker.coordinates, FOCUS_ZOOM, PAUSE_AFTER_MOVE_MS)
    }

    // ---- the camera ----

    private fun cancelViewportLoads() {
        boundsJob?.cancel()
        carouselJob?.cancel()
        updateState { copy(isLoading = false) }
    }

    private fun changeZoom(delta: Float) {
        requestLocationPermission()
        val bounds = currentState.pendingBounds ?: currentState.activeBounds ?: return
        val zoom = currentState.pendingZoom ?: currentState.activeZoom ?: return
        val next = clampZoom(zoom + delta)
        if (next == zoom) return
        analytics.mapInteracted(MapInteraction.Zoomed)
        updateState { copy(camera = MapCamera(bounds.center, next), pendingZoom = next) }
        pauseBounds(PAUSE_AFTER_ZOOM_MS)
    }

    private fun moveCamera(target: Coordinates, zoom: Float, pauseMs: Long) {
        updateState { copy(camera = MapCamera(target, zoom)) }
        pauseBounds(pauseMs)
    }

    private fun pauseBounds(durationMs: Long) {
        cancelViewportLoads()
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
private const val CAROUSEL_ZOOM = 22f

private fun ShopSummary.toMarker(): MapMarker? = coordinates?.let { MapMarker(id, title, it, kind) }
