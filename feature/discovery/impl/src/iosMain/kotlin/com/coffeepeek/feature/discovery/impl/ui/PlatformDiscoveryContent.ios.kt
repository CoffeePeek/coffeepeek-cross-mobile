package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.impl.map.*
import com.coffeepeek.feature.discovery.impl.search.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private var nativeDiscoveryPresenter: (@Composable (DiscoveryIosPresentation, Modifier) -> Unit)? = null

/** Called once by application composition; iOS has no Compose screen fallback. */
fun configureIosDiscoveryContent(presenter: @Composable (DiscoveryIosPresentation, Modifier) -> Unit) {
    nativeDiscoveryPresenter = presenter
}

@Composable
internal actual fun PlatformDiscoveryContent(
    search: SearchViewModel, map: MapViewModel, host: DiscoveryHost,
    roasters: DiscoveryRoasterSlots?, analytics: DiscoveryAnalytics, callbacks: TabCallbacks,
    bottomClearance: Dp, focus: MapFocus?, modifier: Modifier, picker: Boolean,
): Boolean {
    val state by search.state.collectAsStateWithLifecycle()
    val mapState by map.state.collectAsStateWithLifecycle()
    val ui = rememberDiscoveryTabState()
    var revision by remember { mutableIntStateOf(0) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val active = host.isVisible() && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val scope = rememberCoroutineScope()
    val scrolling = remember(scope) { DiscoveryNativeScrolling(scope) }
    LaunchedEffect(active, state.showFilters, ui.mapExpanded, ui.showRoasters) {
        if (!active || state.showFilters || ui.mapExpanded || ui.showRoasters) scrolling.reset()
    }
    NativeDiscoveryEffects(search, map, host, ui, callbacks, focus, active)
    if (!picker) DiscoveryTabEffects(ui, state.showsDiscovery, analytics, callbacks, focus, search::onAction)
    else CoffeePeekBackHandler(onBack = callbacks.onBack)
    NativeDiscoveryReselection(ui, callbacks.reselections, search) { revision++ }
    if (state.showFilters) PlatformDiscoveryFilterSheet(state, search::onAction)
    if (nativeDiscoveryCanPromptReview(state, picker, active, ui.mapExpanded, ui.showRoasters, scrolling.active)) host.onIdle()
    val viewState = discoveryIosState(state, mapState, ui, picker, active && !state.showFilters,
        revision, bottomClearance.value, callbacks.onContinueCheckIn != null, roasters?.hasResults?.invoke(
            DiscoveryRoasterContext(state.query, state.filters.roasterIds, state.filters.favoritesOnly)))
    val toggle = discoveryMapToggle(ui, analytics)
    val presentation = DiscoveryIosPresentation(viewState, discoveryIosCanvas(mapState, !ui.mapExpanded),
        nativeCanvasCallbacks(map), mapState.iosViewportCamera(),
        { name, id -> if (!scrolling.record(name)) {
            if (name == "add-roaster") roasters?.onAdd?.invoke()
            else nativeDiscoveryIntent(name, id, search, map, ui, callbacks, toggle, host, picker)
        } },
        { search.onAction(SearchAction.Query(it)) }, host.openLocationSettings,
        { expanded -> NativeRoasterSlot(roasters, state, expanded, ui, search) })
    checkNotNull(nativeDiscoveryPresenter) { "Native Discovery must be configured before opening iOS screens" }(presentation, modifier)
    return true
}

@Composable
private fun NativeDiscoveryEffects(search: SearchViewModel, map: MapViewModel, host: DiscoveryHost,
    ui: DiscoveryTabState, callbacks: TabCallbacks, focus: MapFocus?, active: Boolean) {
    val state by search.state.collectAsStateWithLifecycle()
    val mapState by map.state.collectAsStateWithLifecycle()
    val currentCallbacks by rememberUpdatedState(callbacks)
    LaunchedEffect(search, map) {
        search.state.map { it.criteria }.distinctUntilChanged().collect { map.onAction(MapAction.SyncCriteria(it)) }
    }
    LaunchedEffect(search, map, ui.mapExpanded, ui.showRoasters, active) {
        search.onAction(SearchAction.PresentationChanged(active && !ui.mapExpanded && !ui.showRoasters))
        map.onAction(MapAction.PresentationChanged(active && ui.mapExpanded && !ui.showRoasters))
    }
    DisposableEffect(search, map) { onDispose {
        search.onAction(SearchAction.PresentationChanged(false))
        map.onAction(MapAction.PresentationChanged(false))
    } }
    LaunchedEffect(search) { search.events.collect { event -> when (event) {
        is SearchEvent.OpenShop -> currentCallbacks.onOpenShop(event.shopId)
        SearchEvent.SignInRequired -> currentCallbacks.onSignIn()
    } } }
    LaunchedEffect(map) { map.events.collect { if (it is MapEvent.OpenShop) currentCallbacks.onOpenShop(it.shopId) } }
    LaunchedEffect(focus) { if (focus != null) {
        ui.mapExpanded = true
        map.onAction(MapAction.Focus(focus))
        currentCallbacks.onFocusConsumed()
    } }
    host.locationPermission(state.locationPermissionRequest, { search.onAction(SearchAction.LocationGranted) },
        { search.onAction(SearchAction.LocationDenied) }, { search.onAction(SearchAction.LocationPermanentlyDenied) })
    host.locationPermission(mapState.locationPermissionRequest, { map.onAction(MapAction.LocationGranted) },
        { map.onAction(MapAction.LocationDenied) }, { map.onAction(MapAction.LocationPermanentlyDenied) })
}

private fun nativeDiscoveryIntent(name: String, id: String, search: SearchViewModel, map: MapViewModel,
    ui: DiscoveryTabState, callbacks: TabCallbacks, toggle: () -> Unit, host: DiscoveryHost, picker: Boolean) {
    when (name) {
        "map" -> { map.onAction(MapAction.Interact); toggle() }
        "roasters" -> ui.showRoasters = true
        "cancel-roasters" -> ui.closeRoasters(search::onAction)
        "continue" -> callbacks.onContinueCheckIn?.invoke()
        "add" -> callbacks.onAddShop()
        "close" -> callbacks.onClose()
        "back" -> callbacks.onBack()
        "settings" -> host.openLocationSettings()
        "dismiss-location" -> dismissNativeLocation(search, map)
        else -> {
            if (name == "favorite") host.haptic()
            discoveryIosSearchIntent(name, id, search.state.value, picker)?.let(search::onAction)
                ?: discoveryIosMapIntent(name, id)?.let(map::onAction)
        }
    }
}

private fun nativeCanvasCallbacks(map: MapViewModel) = MapCanvasCallbacks(
    { bounds, zoom -> map.onAction(MapAction.BoundsChanged(bounds, zoom)) },
    { map.onAction(MapAction.SelectMarker(it)) }, { map.onAction(MapAction.SelectZone(it)) },
    { map.onAction(MapAction.CameraApplied) }, { map.onAction(MapAction.MyLocationFound(it)) },
    { map.onAction(MapAction.LocationDenied) },
)

@Composable
private fun NativeRoasterSlot(roasters: DiscoveryRoasterSlots?, state: SearchState, expanded: Boolean,
    ui: DiscoveryTabState, search: SearchViewModel) {
    val owner = roasters ?: return
    val context = DiscoveryRoasterContext(state.query, state.filters.roasterIds, state.filters.favoritesOnly)
    if (expanded) owner.search(context, { search.onAction(SearchAction.Query(it)) }, { ui.closeRoasters(search::onAction) })
    else owner.preview(context)
}

private fun dismissNativeLocation(search: SearchViewModel, map: MapViewModel) {
    search.onAction(SearchAction.DismissLocationFailure)
    map.onAction(MapAction.DismissLocationSettings)
}

@Composable
private fun NativeDiscoveryReselection(ui: DiscoveryTabState, requests: Flow<Unit>, search: SearchViewModel,
    onReselected: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val callback by rememberUpdatedState(onReselected)
    LaunchedEffect(ui, requests, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            requests.collect {
                ui.mapExpanded = false
                ui.showRoasters = false
                search.onAction(SearchAction.CloseFilters)
                search.onAction(SearchAction.DismissLocationFailure)
                callback()
            }
        }
    }
}
