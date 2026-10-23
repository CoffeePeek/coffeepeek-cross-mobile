package com.coffeepeek.feature.discovery.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.DiscoveryOverlay
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterContext
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterSlots
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.api.MapInteraction
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchEvent
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import com.coffeepeek.feature.discovery.impl.tab.Frame
import com.coffeepeek.feature.discovery.impl.tab.discoveryMapOffsetY
import com.coffeepeek.feature.discovery.impl.tab.expandedFrame
import com.coffeepeek.feature.discovery.impl.tab.mapClipTop
import com.coffeepeek.feature.discovery.impl.tab.mapCornerRadius
import com.coffeepeek.feature.discovery.impl.tab.previewFrame
import kotlin.math.roundToInt

internal class TabCallbacks(
    val onOpenShop: (String) -> Unit,
    val onSignIn: () -> Unit,
    val onAddShop: () -> Unit,
    val onMapExpandedChange: (Boolean) -> Unit,
    val onFocusConsumed: () -> Unit,
)

private const val MAP_ITEM_KEY = "mini-map"
private val MiniMapHeight = 240.dp
private val PreviewCardWidth = 280.dp
private const val PREVIEW_SHOP_COUNT = 8

/**
 * The discovery tab. Two layers share one screen: the search (header, quick filters, the discovery content or the results) and the map, which is drawn
 * over the search at the place of the mini-map item and expands to the whole screen. The geometry is plain maths in `tab/Choreography.kt`.
 */
@Composable
internal fun DiscoveryTab(
    searchViewModel: SearchViewModel,
    mapViewModel: MapViewModel,
    host: DiscoveryHost,
    roasters: DiscoveryRoasterSlots,
    analytics: DiscoveryAnalytics,
    callbacks: TabCallbacks,
    bottomClearance: Dp,
    focus: MapFocus?,
    modifier: Modifier = Modifier,
) {
    val state by searchViewModel.state.collectAsStateWithLifecycle()
    val onAction = searchViewModel::onAction
    LaunchedEffect(searchViewModel) {
        searchViewModel.events.collect { event ->
            when (event) {
                is SearchEvent.OpenShop -> callbacks.onOpenShop(event.shopId)
                SearchEvent.SignInRequired -> callbacks.onSignIn()
            }
        }
    }
    val reduceMotion = host.reduceMotion()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val showDiscovery = state.showsDiscovery

    var showRoasters by rememberSaveable { mutableStateOf(false) }
    var mapExpanded by remember { mutableStateOf(false) }
    val searchOpacity = animateFloatAsState(if (showRoasters) 0f else 1f, tween(if (reduceMotion) 0 else 360), label = "discovery-roaster-list")
    val discoveryProgress = animateFloatAsState(if (showDiscovery) 1f else 0f, tween(if (reduceMotion) 0 else 360), label = "discovery-map-slide")
    val expansion = animateFloatAsState(if (mapExpanded) 1f else 0f, tween(if (reduceMotion) 0 else 420, easing = FastOutSlowInEasing), label = "discovery-map-expansion")

    var mapAnchor by remember { mutableStateOf(Frame.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var viewportTop by remember { mutableStateOf(0f) }
    var viewportHeight by remember { mutableStateOf(0f) }
    val discoveryListState = rememberLazyListState()
    val discoveryShopsState = rememberLazyListState()
    val previewBounds by remember {
        derivedStateOf { previewFrame(mapAnchor, viewportTop, discoveryListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == MAP_ITEM_KEY }?.offset) }
    }
    val mapControlsExpanded by remember { derivedStateOf { mapExpanded && expansion.value == 1f } }

    val overlay = if (showRoasters) DiscoveryOverlay.Roasters else if (mapExpanded) DiscoveryOverlay.Map else DiscoveryOverlay.Discovery
    var previousOverlay by remember { mutableStateOf(DiscoveryOverlay.Discovery) }
    LaunchedEffect(overlay) { if (overlay != previousOverlay) { analytics.overlayShown(overlay); previousOverlay = overlay } }

    LaunchedEffect(focus, mapAnchor.width > 0f) {
        if (focus != null) {
            showRoasters = false
            onAction(SearchAction.CancelSearch)
            if (mapAnchor.width > 0f) mapExpanded = true
        }
    }
    LaunchedEffect(mapExpanded) { callbacks.onMapExpandedChange(mapExpanded) }
    DisposableEffect(Unit) { onDispose { callbacks.onMapExpandedChange(false) } }
    LaunchedEffect(showDiscovery) { if (!showDiscovery) mapExpanded = false }
    host.locationPermission({ onAction(SearchAction.LocationGranted) }, { onAction(SearchAction.LocationDenied) })

    val cancelSearch = { focusManager.clearFocus(force = true); keyboard?.hide(); onAction(SearchAction.CancelSearch) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    BackHandler(enabled = lifecycleState.isAtLeast(Lifecycle.State.STARTED) && (mapExpanded || !showDiscovery)) {
        cancelSearch()
        mapExpanded = false
    }
    val roasterContext = DiscoveryRoasterContext(state.query, state.filters.roasterIds, state.filters.favoritesOnly)

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { rootOrigin = it.positionInRoot() }) {
        val screenWidth = with(density) { maxWidth.toPx() }
        val screenHeight = with(density) { maxHeight.toPx() }
        val canvasSize = DpSize(maxWidth, maxHeight)
        host.retained(searchOpacity.value > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = searchOpacity.value; translationY = -size.height / 5f * (1f - searchOpacity.value) }) {
                SearchContent(
                    state, host, callbacks.onAddShop, bottomClearance,
                    discovery = {
                        DiscoveryContent(
                            state, host, roasters, roasterContext, discoveryListState, discoveryShopsState, mapExpanded, expansion.value, bottomClearance, callbacks.onAddShop,
                            showAllRoasters = { showRoasters = true }, onAction = onAction,
                            onMapSlot = { slot, ready -> if (ready && discoveryProgress.value == 1f && !mapExpanded && expansion.value == 0f) mapAnchor = Frame.of(slot.left - rootOrigin.x, slot.top - rootOrigin.y, slot.width, slot.height) },
                        )
                    },
                    textResults = { roasters.preview(roasterContext) },
                    onAction = onAction,
                    headerModifier = Modifier
                        .then(if (mapExpanded) Modifier.clearAndSetSemantics {} else Modifier)
                        .graphicsLayer { alpha = 1f - expansion.value; translationY = -size.height * expansion.value }
                        .statusBarsPadding(),
                    bodyModifier = Modifier.onGloballyPositioned { viewportTop = it.positionInRoot().y - rootOrigin.y; viewportHeight = it.size.height.toFloat() },
                    prefetch = !mapExpanded,
                )
                if (state.showFilters) FilterSheet(state, host, onAction = onAction)
            }
        }
        AnimatedVisibility(
            visible = showRoasters,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(if (reduceMotion) 0 else 300)) + slideInVertically(tween(if (reduceMotion) 0 else 360)) { it / 5 },
            exit = fadeOut(tween(if (reduceMotion) 0 else 240)) + slideOutVertically(tween(if (reduceMotion) 0 else 360)) { -it / 5 },
        ) {
            roasters.search(roasterContext, { onAction(SearchAction.Query(it)) }, { onAction(SearchAction.CancelSearch); showRoasters = false })
        }
        if (mapAnchor.width > 0f) {
            val radiusPx = with(density) { CoffeePeekDimens.radius2xl.toPx() }
            val outline = MaterialTheme.colorScheme.outline
            val strokePx = with(density) { 1.dp.toPx() }
            val mapVisible by remember { derivedStateOf { discoveryProgress.value > 0f } }
            host.retained(mapVisible) {
                // The viewport stays below the separator while its contents slide away.
                Box(
                    Modifier.fillMaxSize().layout { measurable, constraints ->
                        val clipTop = mapClipTop(viewportTop, expansion.value, constraints.maxHeight)
                        val height = constraints.maxHeight - clipTop
                        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, clipTop) }
                    }.clipToBounds(),
                ) {
                    Box(Modifier.fillMaxSize().graphicsLayer { clip = true; translationY = discoveryMapOffsetY(viewportHeight, discoveryProgress.value) }) {
                        MapScreen(
                            mapViewModel, host, callbacks.onOpenShop, bottomClearance, focus.takeIf { mapExpanded }, callbacks.onFocusConsumed,
                            modifier = Modifier.offset {
                                val bounds = expandedFrame(previewBounds, screenWidth, screenHeight, expansion.value)
                                val clipTop = mapClipTop(viewportTop, expansion.value, screenHeight.roundToInt())
                                IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt() - clipTop)
                            }.layout { measurable, _ ->
                                val bounds = expandedFrame(previewBounds, screenWidth, screenHeight, expansion.value)
                                val placeable = measurable.measure(Constraints.fixed(bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1)))
                                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                            }.graphicsLayer {
                                clip = true
                                shape = RoundedCornerShape(mapCornerRadius(radiusPx, expansion.value))
                            }.drawWithContent {
                                drawContent()
                                drawRoundRect(outline, cornerRadius = CornerRadius(mapCornerRadius(radiusPx, expansion.value)), style = Stroke(strokePx))
                            },
                            isPreview = !mapControlsExpanded,
                            onToggleExpand = {
                                analytics.mapInteracted(if (mapExpanded) MapInteraction.Collapsed else MapInteraction.Expanded)
                                if (mapExpanded) { focusManager.clearFocus(force = true); keyboard?.hide() }
                                mapExpanded = !mapExpanded
                            },
                            canvasSize = canvasSize,
                        )
                    }
                }
            }
        }
    }
}

/** What shows while nothing is searched: the mini-map slot, the first shops in a row and the roaster section. */
@Composable
private fun DiscoveryContent(
    state: SearchState,
    host: DiscoveryHost,
    roasters: DiscoveryRoasterSlots,
    roasterContext: DiscoveryRoasterContext,
    listState: LazyListState,
    shopsState: LazyListState,
    mapExpanded: Boolean,
    expansion: Float,
    bottomClearance: Dp,
    onAddShop: () -> Unit,
    showAllRoasters: () -> Unit,
    onAction: (SearchAction) -> Unit,
    onMapSlot: (slot: Frame, ready: Boolean) -> Unit,
) {
    val shops = state.visibleShops
    host.photoPrefetch(shopsState, shops.take(PREVIEW_SHOP_COUNT).map { it.id to listOfNotNull(it.photoUrl) }.takeIf { !mapExpanded }.orEmpty())
    // The map stays where it is while the shops below it are pulled down (its place comes from the item offset), and the loader shows under the map.
    CoffeePeekPullToRefresh(
        listState = listState,
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(SearchAction.Refresh) },
        refreshDescription = stringResource(R.string.discovery_refresh),
        loadingDescription = stringResource(R.string.discovery_refreshing),
        indicatorTopOffset = CoffeePeekDimens.spacing4 + MiniMapHeight,
        modifier = Modifier.fillMaxSize()
            .then(if (mapExpanded) Modifier.clearAndSetSemantics {} else Modifier)
            .graphicsLayer { alpha = 1f - expansion; translationY = size.height * 0.3f * expansion },
    ) { scrollModifier ->
        LazyColumn(
            state = listState,
            userScrollEnabled = !mapExpanded,
            modifier = scrollModifier.fillMaxSize(),
            contentPadding = PaddingValues(top = CoffeePeekDimens.spacing4, bottom = bottomClearance + 88.dp),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
        ) {
            item(key = MAP_ITEM_KEY) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4).height(MiniMapHeight).onGloballyPositioned {
                        onMapSlot(Frame.of(it.positionInRoot().x, it.positionInRoot().y, it.size.width.toFloat(), it.size.height.toFloat()), true)
                    },
                )
            }
            item(key = "shops-heading") {
                Box(host.exposure(Modifier, DiscoveryExposure.ShopCatalog)) {
                    SectionTitle(stringResource(R.string.discovery_section_shops), { onAction(SearchAction.ActivateSearch) }, Modifier.padding(horizontal = CoffeePeekDimens.spacing4))
                }
            }
            item(key = "shops-preview") {
                when {
                    state.isLoading && shops.isEmpty() -> Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) {
                        CoffeePeekLoader(stringResource(R.string.discovery_loading))
                    }
                    state.error != null && shops.isEmpty() -> Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                            Text(state.error.serverMessage ?: stringResource(R.string.discovery_load_failed), style = MaterialTheme.typography.bodyLarge)
                            CoffeePeekButton(stringResource(R.string.discovery_retry), { onAction(SearchAction.Refresh) })
                        }
                    }
                    shops.isEmpty() -> CoffeePeekCatalogSuggestionCard(
                        title = stringResource(R.string.discovery_suggestion_title),
                        description = stringResource(R.string.discovery_suggestion_description),
                        actionLabel = stringResource(R.string.discovery_suggestion_action),
                        illustration = host.suggestionIllustration(),
                        onAdd = onAddShop,
                        modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4),
                    )
                    else -> LazyRow(
                        state = shopsState,
                        contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4),
                        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
                    ) {
                        items(shops.take(PREVIEW_SHOP_COUNT), key = { it.id }) { shop ->
                            ShopRow(
                                shop, host, shop.id in state.favoriteUpdates, state.userLocation,
                                onOpen = { onAction(SearchAction.OpenShop(shop.id)) }, onFavorite = { onAction(SearchAction.ToggleFavorite(shop.id)) },
                                modifier = Modifier.width(PreviewCardWidth), showCatalogDetails = false,
                            )
                        }
                    }
                }
            }
            item(key = "roasters-heading") {
                SectionTitle(stringResource(R.string.discovery_section_roasters), showAllRoasters, Modifier.padding(horizontal = CoffeePeekDimens.spacing4))
            }
            item(key = "roasters-preview") { roasters.preview(roasterContext) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onShowAll) {
            Text(stringResource(R.string.discovery_show_all))
            Icon(CoffeePeekIcons.ChevronRight, null, Modifier.padding(start = CoffeePeekDimens.spacing1).height(16.dp).width(16.dp))
        }
    }
}
