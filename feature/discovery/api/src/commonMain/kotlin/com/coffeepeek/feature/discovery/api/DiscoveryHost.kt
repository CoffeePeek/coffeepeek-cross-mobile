package com.coffeepeek.feature.discovery.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyListState

/** The parts of the screen the application measures: an element asks for its exposure to be tracked. */
enum class DiscoveryExposure { Search, Filters, ShopCatalog, Favorite, MapPreview, MapFull }

/**
 * What the discovery screens borrow from the application: images with their placeholder policy, the brew method icons, the branded illustration of the
 * «Не нашли?» card, the exposure tracking and the haptic tick. Defaults keep previews and tests free of app services.
 */
class DiscoveryHost(
    /** A remote image (a shop cover, a roaster logo); [description] is for accessibility. */
    val image: @Composable (url: String, description: String?, modifier: Modifier, scale: ContentScale) -> Unit = { _, _, _, _ -> },
    /** What a shop without a photo shows. */
    val coverPlaceholder: @Composable (description: String?, modifier: Modifier) -> Unit = { _, _ -> },
    /** The same two for a very small photo box (the card on the map), where the placeholder label must be tiny. */
    val compactImage: @Composable (url: String, description: String?, modifier: Modifier, scale: ContentScale) -> Unit = image,
    val compactCoverPlaceholder: @Composable (description: String?, modifier: Modifier) -> Unit = coverPlaceholder,
    /** The full-size load error with the application's illustration, the wording of the other lists and the retry button. */
    val loadError: @Composable (onRetry: () -> Unit, modifier: Modifier) -> Unit = { _, _ -> },
    val brewMethodIcon: @Composable (name: String, size: Dp) -> Unit = { _, _ -> },
    val tagIcon: @Composable (tagId: String) -> Unit = {},
    val roasterLogo: @Composable (name: String, logoUrl: String?) -> Unit = { _, _ -> },
    /** The roaster's avatar in a list row (the logo above sits on a shop card). */
    val roasterAvatar: @Composable (name: String, logoUrl: String?) -> Unit = { _, _ -> },
    /** The price level (1..4, null for any) as the application's slider, and the hint it shows beside the title. */
    val priceSelector: @Composable (selected: Int?, onSelect: (Int?) -> Unit, modifier: Modifier) -> Unit = { _, _, _ -> },
    val priceHint: (Int?) -> String? = { null },
    val suggestionIllustration: @Composable () -> Painter = { error("The application provides the illustration") },
    val exposure: (Modifier, DiscoveryExposure) -> Modifier = { modifier, _ -> modifier },
    val haptic: () -> Unit = {},
    /** The application's map: it draws [MapCanvasState] and reports through [MapCanvasCallbacks]. */
    /** Keeps native content (the map) mounted but paused and not placed while hidden. */
    val retained: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit = { visible, content -> if (visible) content() },
    /** Asks for the location permission once, with the result; the effect has no UI of its own. */
    val locationPermission: @Composable (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit = { _, _ -> },
    /** Warms the image cache for the photos a list will show next: item id with its photo urls. */
    val photoPrefetch: @Composable (listState: LazyListState, photos: List<Pair<String, List<String>>>) -> Unit = { _, _ -> },
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
