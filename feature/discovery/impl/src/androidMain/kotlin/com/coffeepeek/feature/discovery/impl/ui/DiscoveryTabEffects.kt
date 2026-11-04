package com.coffeepeek.feature.discovery.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.tab.DiscoveryBackTarget
import com.coffeepeek.feature.discovery.impl.tab.discoveryBackTarget

@Composable
internal fun DiscoveryTabEffects(
    ui: DiscoveryTabState, showDiscovery: Boolean, analytics: DiscoveryAnalytics,
    callbacks: TabCallbacks, focus: MapFocus?, onAction: (SearchAction) -> Unit,
) {
    DiscoveryOverlayAnalytics(ui, analytics)
    LaunchedEffect(focus, ui.mapAnchor.width > 0f) {
        if (focus != null) {
            ui.showRoasters = false
            ui.mapExpanded = true
        }
    }
    LaunchedEffect(ui.mapExpanded) { callbacks.onMapExpandedChange(ui.mapExpanded) }
    DisposableEffect(Unit) { onDispose { callbacks.onMapExpandedChange(false) } }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val backTarget = discoveryBackTarget(ui.showRoasters, ui.mapExpanded, showDiscovery)
    BackHandler(enabled = lifecycle.isAtLeast(Lifecycle.State.STARTED) && backTarget != null) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        when (backTarget) {
            DiscoveryBackTarget.Roasters -> ui.closeRoasters(onAction)
            DiscoveryBackTarget.Map -> ui.mapExpanded = false
            DiscoveryBackTarget.Search -> onAction(SearchAction.CancelSearch)
            null -> Unit
        }
    }
}

@Composable
private fun DiscoveryOverlayAnalytics(ui: DiscoveryTabState, analytics: DiscoveryAnalytics) {
    val overlay = when {
        ui.showRoasters -> DiscoveryOverlay.Roasters
        ui.mapExpanded -> DiscoveryOverlay.Map
        else -> DiscoveryOverlay.Discovery
    }
    var previous by remember { mutableStateOf(DiscoveryOverlay.Discovery) }
    LaunchedEffect(overlay) {
        if (overlay != previous) {
            analytics.overlayShown(overlay)
            previous = overlay
        }
    }
}

@Composable
internal fun discoveryMapToggle(ui: DiscoveryTabState, analytics: DiscoveryAnalytics): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return {
        analytics.mapInteracted(if (ui.mapExpanded) MapInteraction.Collapsed else MapInteraction.Expanded)
        if (ui.mapExpanded) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
        ui.mapExpanded = !ui.mapExpanded
    }
}
