package com.coffeepeek.admin.ui.screen.map

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.DpSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.MapShop

@Composable
fun MapScreen(
    vm: MapViewModel = platformViewModel(),
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    canvasSize: DpSize? = null,
    onOpenShop: (String) -> Unit = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
    onQueryChange: ((String) -> Unit)? = null,
    filterControl: (@Composable () -> Unit)? = null,
    filterBar: (@Composable () -> Unit)? = null,
    isSearchActive: Boolean = !isPreview,
) {
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissInput: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }
    val pendingFocus = MapScreenEffects(vm, isPreview, isSearchActive, dismissInput)
    val actions = mapScreenActions(vm, dismissInput, onToggleExpand, onOpenShop, onQueryChange)
    MapScreenContent(state, actions, isPreview, filterControl, filterBar,
        modifier.analyticsExposure("map", if (isPreview) "home" else "discovery", if (isPreview) "discovery" else "map")) { canvasModifier ->
        MapCanvas(state, actions, pendingFocus, isPreview, canvasSize, canvasModifier)
    }
}

@Composable
private fun MapScreenEffects(
    vm: MapViewModel,
    isPreview: Boolean,
    isSearchActive: Boolean,
    dismissInput: () -> Unit,
): MapShop? {
    val userLocation = rememberPermittedUserLocation()
    LaunchedEffect(userLocation) {
        userLocation?.let { vm.onNearbyOriginChanged(it.latitude, it.longitude) }
    }
    val pendingFocus by Navigator.pendingMapFocus.collectAsState()
    val focus = pendingFocus.takeUnless { isPreview }
    LaunchedEffect(focus) {
        focus?.let {
            dismissInput()
            vm.focusOnShop(it)
            Navigator.consumeMapFocus()
        }
    }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val visible = LocalAnalyticsVisible.current
    DisposableEffect(vm, lifecycle, isSearchActive, visible) {
        vm.setSearchActive(isSearchActive && visible && lifecycle.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { vm.setSearchActive(false) }
    }
    return focus?.let { MapShop(it.shopId, it.title, it.latitude, it.longitude) }
}

private fun mapScreenActions(
    vm: MapViewModel,
    dismissInput: () -> Unit,
    toggleExpand: (() -> Unit)?,
    openShop: (String) -> Unit,
    queryChange: ((String) -> Unit)?,
): MapScreenActions = mapSelectionActions(vm, dismissInput).copy(
    onQueryChange = queryChange ?: vm::onQueryChange,
    onExpand = {
        Analytics.track(AnalyticsEvent.MAP_INTERACTION, "action" to "expanded")
        toggleExpand?.invoke()
    },
    onCollapse = toggleExpand?.let { collapseMapAction(dismissInput, it) },
    onOpenShop = {
        vm.openedShop()
        openShop(it)
    },
)

private fun mapSelectionActions(vm: MapViewModel, dismissInput: () -> Unit) = mapCameraActions(vm).copy(
    onShopClick = {
        dismissInput()
        vm.onShopSelected(it)
    },
    onCarouselSelect = {
        dismissInput()
        vm.onCarouselShopSelected(it)
    },
    onSearchSelect = {
        dismissInput()
        vm.onSearchResultSelected(it)
    },
)

private fun mapCameraActions(vm: MapViewModel) = MapScreenActions(
    onBoundsChanged = vm::onBoundsChanged,
    onZoneClick = vm::onZoneSelected,
    onCameraApplied = vm::onCameraTargetApplied,
    onLocationFound = { lat, lon -> applyMapLocation(vm, lat, lon) },
    onLocationDenied = { trackMapLocation(vm, "unavailable") },
    onZoomIn = vm::zoomIn,
    onZoomOut = vm::zoomOut,
    onToggleZones = vm::toggleZones,
    onMyLocation = vm::requestMyLocation,
    onSearchArea = vm::searchCurrentArea,
    onShowZoneShops = vm::showSelectedZoneShops,
    onDismissZone = vm::clearZoneSelection,
)

private fun collapseMapAction(dismissInput: () -> Unit, collapse: () -> Unit): () -> Unit = {
    Analytics.track(AnalyticsEvent.MAP_INTERACTION, "action" to "collapsed")
    dismissInput()
    collapse()
}

private fun applyMapLocation(vm: MapViewModel, latitude: Double, longitude: Double) {
    trackMapLocation(vm, "available")
    vm.onMyLocationApplied(latitude, longitude)
}

private fun trackMapLocation(vm: MapViewModel, outcome: String) {
    Analytics.track(AnalyticsEvent.GEOLOCATION_RESULT, "placement" to "map",
        "trigger" to if (vm.state.value.myLocationRequest > 0) "user" else "automatic", "outcome" to outcome)
}
