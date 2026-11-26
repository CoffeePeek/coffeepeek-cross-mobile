@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.feature.discovery.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.impl.map.*
import com.coffeepeek.feature.discovery.impl.search.*
import com.coffeepeek.feature.discovery.impl.tab.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

fun createNativeDiscoverySession(repository: DiscoveryRepository, favorites: DiscoveryFavorites,
    city: DiscoveryCity, location: DiscoveryLocation, account: DiscoveryAccount, details: DiscoveryShopDetails,
    analytics: DiscoveryAnalytics = NoDiscoveryAnalytics, picker: Boolean = false,
    recentShops: DiscoveryRecentShops? = null): NativeDiscoverySession {
    val purpose = if (picker) SearchPurpose.CheckInPicker else SearchPurpose.Discovery
    val nativeAccount = NativeDiscoveryAccount(account)
    val search = SearchViewModel(repository, favorites, city, location, nativeAccount, analytics, purpose, recentShops)
    val map = MapViewModel(repository, location, details, analytics, nativeAccount, favorites, !picker)
    val store = ViewModelStore().apply {
        put("native-search", search)
        put("native-map", map)
    }
    return NativeDiscoverySession(search, map, analytics, picker, nativeAccount) {
        nativeAccount.close()
        store.clear()
    }
}

/** Native owner of shared search/map models; no layout, renderer or composition is retained. */
class NativeDiscoverySession internal constructor(internal val search: SearchViewModel,
    internal val map: MapViewModel, private val analytics: DiscoveryAnalytics, val picker: Boolean,
    private val account: NativeDiscoveryAccount, private val dispose: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val identity = account.currentAccountKey()
    private val overlay = MutableStateFlow(NativeDiscoveryOverlay())
    private val scrolling = DiscoveryNativeScrolling(scope)
    private val filterDraft = MutableStateFlow<DiscoveryFilterDraft?>(null)
    internal val filters = filterDraft.flatMapLatest { draft ->
        if (draft == null) flowOf(null) else combine(search.state, draft.state) { search, filters ->
            NativeDiscoveryFilterSnapshot(search, filters).takeIf { search.showFilters }
        }
    }
    private val mutable = MutableStateFlow(snapshot(search.state.value, map.state.value, overlay.value))
    internal val state = mutable.asStateFlow()
    val runtime = state.map { NativeDiscoveryRuntimeState(it.roasterContext, it.overlay.active,
        it.search.locationPermissionRequest, it.map.locationPermissionRequest, it.reviewEligible) }.distinctUntilChanged()
    fun currentRuntime() = mutable.value.let { NativeDiscoveryRuntimeState(it.roasterContext, it.overlay.active,
        it.search.locationPermissionRequest, it.map.locationPermissionRequest, it.reviewEligible) }
    private val output = Channel<NativeDiscoveryEvent>(Channel.BUFFERED)
    val events = output.receiveAsFlow().filter { allowed() }
    val accountFailed = account.failed
    private var closed = false
    private var lastFocus: MapFocus? = null
    private var previousOverlay = DiscoveryOverlay.Discovery
    private var filterClosing = false

    init {
        observeModels()
        observePresentation()
    }
    internal fun current() = mutable.value
    private fun allowed() = !closed && !account.failed.value && account.currentAccountKey() == identity
    private fun interactive() = allowed() && overlay.value.active && !search.state.value.showFilters
    private fun emit(event: NativeDiscoveryEvent) { if (allowed()) scope.launch { output.send(event) } }

    private fun observeModels() {
        scope.launch { search.state.map { it.criteria }.distinctUntilChanged().collect { map.onAction(MapAction.SyncCriteria(it)) } }
        scope.launch { search.events.collect { emit(when (it) {
            is SearchEvent.OpenShop -> NativeDiscoveryEvent.OpenShop(it.shopId)
            SearchEvent.SignInRequired -> NativeDiscoveryEvent.SignIn
        }) } }
        scope.launch { map.events.collect { if (it is MapEvent.OpenShop) emit(NativeDiscoveryEvent.OpenShop(it.shopId)) } }
        scope.launch { account.accountKey.collect { if (it != identity) close() } }
        scope.launch { account.failed.collect { if (it) close() } }
    }

    private fun observePresentation() {
        scope.launch { combine(search.state, map.state, overlay, scrolling.activity) { search, map, ui, _ ->
            snapshot(search, map, ui)
        }.collect { value ->
            mutable.value = value
            updateFilterDraft(value.search)
            val ui = value.overlay
            if (!ui.active || value.search.showFilters || ui.mapExpanded || ui.roastersExpanded) scrolling.reset()
        } }
        scope.launch { overlay.map { Triple(it.active, it.mapExpanded, it.roastersExpanded) }.distinctUntilChanged().collect { (active, expanded, roasters) ->
            search.onAction(SearchAction.PresentationChanged(active && !expanded && !roasters))
            map.onAction(MapAction.PresentationChanged(active && expanded && !roasters))
        } }
    }

    private fun snapshot(search: SearchState, map: MapState, ui: NativeDiscoveryOverlay) = NativeDiscoverySnapshot(search, map, ui,
        nativeDiscoveryCanPromptReview(search, picker, ui.active, ui.mapExpanded, ui.roastersExpanded, scrolling.active))

    private fun updateFilterDraft(state: SearchState) {
        if (!state.showFilters) {
            filterDraft.value = null
            filterClosing = false
        } else if (filterDraft.value == null && !filterClosing) filterDraft.value = DiscoveryFilterDraft(state.filters)
    }
    fun resetFilterDraft() { if (allowed() && !filterClosing) filterDraft.value?.reset() }
    fun filterPrice(index: Int) { if (allowed() && !filterClosing) filterDraft.value?.price(index) }
    fun filterToggle(group: String, id: String) { if (allowed() && !filterClosing) filterDraft.value?.toggle(group, id) }
    fun applyFilters() {
        val draft = filterDraft.value ?: return
        if (!allowed() || filterClosing) return
        filterClosing = true
        search.onAction(SearchAction.ApplyFilters(draft.applyTo(search.state.value.filters)))
        search.onAction(SearchAction.CloseFilters)
    }
    fun dismissFilters() {
        if (!allowed() || filterClosing) return
        filterClosing = true
        search.onAction(SearchAction.CloseFilters)
    }
    internal fun retryFilterCatalogs() { if (allowed()) search.onAction(SearchAction.RetryCatalogs) }

    fun presentation(active: Boolean, bottomClearance: Float, canContinue: Boolean) {
        if (!allowed()) return
        overlay.update { it.copy(active = active, bottomClearance = bottomClearance.takeIf { value -> value.isFinite() }?.coerceAtLeast(0f) ?: 0f,
            canContinue = canContinue) }
    }
    fun roasterResults(hasResults: Boolean?) { if (allowed()) overlay.update { it.copy(hasRoasters = hasResults) } }
    fun query(text: String) { if (interactive()) search.onAction(SearchAction.Query(text)) }
    fun perform(name: String, id: String) {
        if (!interactive() || scrolling.record(name)) return
        when (name) {
            "map" -> toggleMap()
            "nearby" -> nearby()
            "roasters" -> changeOverlay(overlay.value.copy(roastersExpanded = true))
            "cancel-roasters" -> closeRoasters()
            "continue" -> if (overlay.value.canContinue) emit(NativeDiscoveryEvent.ContinueCheckIn)
            "add" -> emit(NativeDiscoveryEvent.AddShop)
            "add-roaster" -> emit(NativeDiscoveryEvent.AddRoaster)
            "close" -> emit(NativeDiscoveryEvent.Close)
            "back" -> emit(NativeDiscoveryEvent.Back)
            "settings" -> emit(NativeDiscoveryEvent.OpenLocationSettings)
            "dismiss-location" -> dismissLocation()
            else -> dispatchIntent(name, id)
        }
    }

    private fun dispatchIntent(name: String, id: String) {
        if (name == "favorite") emit(NativeDiscoveryEvent.FavoriteFeedback)
        discoveryIosSearchIntent(name, id, search.state.value, picker)?.let(search::onAction)
            ?: discoveryIosMapIntent(name, id)?.let(map::onAction)
    }
    private fun toggleMap() {
        map.onAction(MapAction.Interact)
        val expanded = !overlay.value.mapExpanded
        analytics.mapInteracted(if (expanded) MapInteraction.Expanded else MapInteraction.Collapsed)
        changeOverlay(overlay.value.copy(mapExpanded = expanded))
    }
    private fun nearby() {
        val ui = overlay.value
        val action = if (picker) pickerNearbyAction(ui.mapExpanded, search.state.value.filters.nearbyOnly) else SearchAction.ToggleNearby
        if (picker) changeOverlay(ui.copy(mapExpanded = false))
        action?.let(search::onAction)
    }
    private fun closeRoasters() {
        search.onAction(SearchAction.CancelSearch)
        changeOverlay(overlay.value.copy(roastersExpanded = false))
    }
    private fun changeOverlay(next: NativeDiscoveryOverlay) {
        val previous = overlay.value
        overlay.value = next
        if (previous.mapExpanded != next.mapExpanded) emit(NativeDiscoveryEvent.MapExpanded(next.mapExpanded))
        val surface = if (next.roastersExpanded) DiscoveryOverlay.Roasters else if (next.mapExpanded) DiscoveryOverlay.Map else DiscoveryOverlay.Discovery
        if (!picker && surface != previousOverlay) analytics.overlayShown(surface)
        previousOverlay = surface
    }

    fun focus(value: MapFocus?) {
        if (!allowed() || value == lastFocus) return
        lastFocus = value
        if (value == null) return
        changeOverlay(overlay.value.copy(roastersExpanded = false, mapExpanded = true))
        map.onAction(MapAction.Focus(value))
        emit(NativeDiscoveryEvent.FocusConsumed)
    }
    fun reselect() {
        if (!allowed()) return
        val ui = overlay.value
        changeOverlay(ui.copy(mapExpanded = false, roastersExpanded = false, scrollRevision = ui.scrollRevision + 1))
        search.onAction(SearchAction.CloseFilters)
        search.onAction(SearchAction.DismissLocationFailure)
    }
    fun back() {
        if (!interactive()) return
        if (picker) return emit(NativeDiscoveryEvent.Back)
        when (discoveryBackTarget(overlay.value.roastersExpanded, overlay.value.mapExpanded, search.state.value.showsDiscovery)) {
            DiscoveryBackTarget.Roasters -> closeRoasters()
            DiscoveryBackTarget.Map -> changeOverlay(overlay.value.copy(mapExpanded = false))
            DiscoveryBackTarget.Search -> search.onAction(SearchAction.CancelSearch)
            null -> Unit
        }
    }
    private fun dismissLocation() {
        search.onAction(SearchAction.DismissLocationFailure)
        map.onAction(MapAction.DismissLocationSettings)
    }

    fun permissionResult(target: NativeDiscoveryPermissionTarget, request: Int, outcome: NativeDiscoveryPermissionOutcome) {
        if (!allowed() || request <= 0) return
        if (target == NativeDiscoveryPermissionTarget.Search) {
            if (search.state.value.locationPermissionRequest != request) return
            search.onAction(when (outcome) {
                NativeDiscoveryPermissionOutcome.Granted -> SearchAction.LocationGranted
                NativeDiscoveryPermissionOutcome.Denied -> SearchAction.LocationDenied
                NativeDiscoveryPermissionOutcome.PermanentlyDenied -> SearchAction.LocationPermanentlyDenied
            })
        } else if (map.state.value.locationPermissionRequest == request) map.onAction(when (outcome) {
            NativeDiscoveryPermissionOutcome.Granted -> MapAction.LocationGranted
            NativeDiscoveryPermissionOutcome.Denied -> MapAction.LocationDenied
            NativeDiscoveryPermissionOutcome.PermanentlyDenied -> MapAction.LocationPermanentlyDenied
        })
    }
    internal fun canvasCallbacks() = MapCanvasCallbacks(
        { bounds, zoom -> mapAction(MapAction.BoundsChanged(bounds, zoom)) },
        { mapAction(MapAction.SelectMarker(it)) }, { mapAction(MapAction.SelectZone(it)) },
        { mapAction(MapAction.CameraApplied) }, { mapAction(MapAction.MyLocationFound(it)) },
        { mapAction(MapAction.LocationDenied) },
    )
    private fun mapAction(action: MapAction) { if (allowed()) map.onAction(action) }
    fun close() {
        if (closed) return
        closed = true
        search.onAction(SearchAction.PresentationChanged(false))
        map.onAction(MapAction.PresentationChanged(false))
        mutable.value = NativeDiscoverySnapshot(SearchState(), MapState(), NativeDiscoveryOverlay(), false)
        filterDraft.value = null
        scrolling.reset()
        scope.cancel()
        output.close()
        dispose()
    }
}
