package com.coffeepeek.feature.discovery.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** The parts of the screen the application measures: an element asks for its exposure to be tracked. */
enum class DiscoveryExposure { Search, Filters, ShopCatalog, Favorite, MapPreview, MapFull }

/**
 * Platform and application integration only. Feature UI owns images, catalog visuals and resources.
 * Defaults keep previews and tests free of application services.
 */
class DiscoveryHost(
    val exposure: (Modifier, DiscoveryExposure) -> Modifier = { modifier, _ -> modifier },
    val haptic: () -> Unit = {},
    /** The application's map: it draws [MapCanvasState] and reports through [MapCanvasCallbacks]. */
    /** Keeps native content (the map) mounted but paused and not placed while hidden. */
    val retained: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit = { visible, content -> if (visible) content() },
    /** Asks for the location permission once, with the result; the effect has no UI of its own. */
    val locationPermission: @Composable (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit = { _, _ -> },
    val reduceMotion: @Composable () -> Boolean = { false },
    val mapCanvas: @Composable (state: MapCanvasState, callbacks: MapCanvasCallbacks, modifier: Modifier) -> Unit = { _, _, _ -> },
)

/** What the roaster sections need to know about the search to filter themselves. */
class DiscoveryRoasterContext(val query: String, val selectedRoasterIds: Set<String>, val favoritesOnly: Boolean)

/** The roaster owner's pieces the tab embeds; the application fills them from the roaster renderer. */
class DiscoveryRoasterSlots(
    /** The section under «Обжарщики» in the discovery and above the shops of a text search. */
    val preview: @Composable (DiscoveryRoasterContext) -> Unit,
    /** The full roaster search laid over the tab; [onCancel] closes it and clears the search. */
    val search: @Composable (context: DiscoveryRoasterContext, onQueryChange: (String) -> Unit, onCancel: () -> Unit) -> Unit,
)

/**
 * The whole discovery tab: the search with its header and quick filters, the discovery content (the mini map that expands to the full screen, the shops,
 * the roaster section) while nothing is searched, the results otherwise, and the roaster search over it. The caller owns navigation and the ViewModel store.
 */
interface DiscoveryTabEntry {
    @Composable
    fun Content(
        onOpenShop: (slug: String) -> Unit,
        onSignIn: () -> Unit,
        onAddShop: () -> Unit,
        /** Tells the host that the map covers the screen, so it can hide the floating navigation and the create button. */
        onMapExpandedChange: (Boolean) -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
        /** A shop to bring into view on the expanded map; [onFocusConsumed] tells the caller it was taken. */
        focus: MapFocus?,
        onFocusConsumed: () -> Unit,
    )
}
