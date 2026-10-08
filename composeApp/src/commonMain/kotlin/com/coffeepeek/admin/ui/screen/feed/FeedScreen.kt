package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.feature.catalog.api.ShopCard
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.location.NEARBY_RADIUS_METERS
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.DesignFilterChip
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.SearchHeader
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.model.COFFEE_FOCUS_OPTIONS
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.coffeepeek.admin.ui.component.RetainedContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maskot_with_magnifying_glass
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.admin.di.platformViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FeedScreen(
    onSelectRoasters: () -> Unit,
    vm: FeedViewModel = platformViewModel(),
    mapPreview: (@Composable (Boolean, () -> Unit, DpSize, Modifier) -> Unit)? = null,
    roasterPreview: (@Composable () -> Unit)? = null,
    onMapExpandedChange: (Boolean) -> Unit = {},
) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val discoveryListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val showDiscovery = mapPreview != null && state.showDiscovery
    val showRoasterSearch = roasterPreview != null && state.query.isNotBlank()
    val discoveryTransition = updateTransition(showDiscovery, label = "discovery-shop-list")
    val discoveryProgress = discoveryTransition.animateFloat(
        transitionSpec = { tween(360) },
        label = "discovery-map-slide",
    ) { if (it) 1f else 0f }
    var mapExpanded by remember { mutableStateOf(false) }
    val expansion = animateFloatAsState(
        targetValue = if (mapExpanded) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "discovery-map-expansion",
    )
    var mapAnchor by remember { mutableStateOf(Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var viewportTop by remember { mutableStateOf(0f) }
    var viewportHeight by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val mapControlsExpanded by remember { derivedStateOf { mapExpanded && expansion.value == 1f } }
    val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
    LaunchedEffect(pendingMapFocus, mapAnchor.width > 0f) {
        if (mapPreview != null && pendingMapFocus != null) {
            vm.cancelSearch()
            if (mapAnchor.width > 0f) mapExpanded = true
        }
    }
    LaunchedEffect(mapExpanded) { onMapExpandedChange(mapExpanded) }
    DisposableEffect(Unit) { onDispose { onMapExpandedChange(false) } }
    LaunchedEffect(showDiscovery) { if (!showDiscovery) mapExpanded = false }
    val cancelSearch = {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        vm.cancelSearch()
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    BackHandler(enabled = lifecycleState.isAtLeast(Lifecycle.State.STARTED) &&
        (mapExpanded || (mapPreview != null && !showDiscovery))) {
        cancelSearch()
        mapExpanded = false
    }
    val userLocation = rememberPermittedUserLocation()
    val displayedShops = if (state.filters.nearbyOnly && userLocation != null) {
        state.visibleShops
            .mapNotNull { shop ->
                val meters = distanceToShopMeters(userLocation, shop.location) ?: return@mapNotNull null
                if (meters > NEARBY_RADIUS_METERS) null else shop to meters
            }
            .sortedBy { it.second }
            .map { it.first }
    } else {
        state.visibleShops
    }
    val fillingNearby = state.filters.nearbyOnly &&
        userLocation != null &&
        displayedShops.size < 8 &&
        state.hasMore &&
        state.shops.isNotEmpty() &&
        !state.isLoading &&
        !state.isRefreshing

    val shouldLoadMore by remember(showDiscovery) {
        derivedStateOf {
            if (showDiscovery || state.shops.isEmpty()) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= state.shops.size - 5 &&
                state.hasMore &&
                !state.isLoadingMore &&
                !state.isLoading &&
                !state.isRefreshing
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }
    LaunchedEffect(fillingNearby, state.currentPage, state.isLoadingMore) {
        if (fillingNearby && !state.isLoadingMore) vm.loadMore()
    }

    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { rootOrigin = it.positionInRoot() }) {
        val screenSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val canvasSize = DpSize(maxWidth, maxHeight)
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .then(if (mapExpanded) Modifier.clearAndSetSemantics {} else Modifier)
                        .graphicsLayer {
                            alpha = 1f - expansion.value
                            translationY = -size.height * expansion.value
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CpDimens.spacing4)
                                .padding(top = CpDimens.spacing3, bottom = CpDimens.spacing2),
                        ) {
                            SearchHeader(
                                query = state.query,
                                onQueryChange = vm::onQueryChange,
                                roastersSelected = false,
                                onSelectRoasters = { if (it) onSelectRoasters() },
                                filterCount = state.activeFilterCount,
                                onFilters = vm::toggleFilters,
                                showCategories = mapPreview == null,
                                onSearchFocus = { if (mapPreview != null) vm.activateSearch() },
                                onCancelSearch = cancelSearch.takeIf { mapPreview != null && !showDiscovery },
                            )
                            Spacer(modifier = Modifier.height(CpDimens.spacing2))
                            FeedQuickFilterBar(
                                nearbyOnly = state.filters.nearbyOnly,
                                showNearby = userLocation != null,
                                openOnly = state.filters.openOnly,
                                newOnly = state.filters.newOnly,
                                visitedOnly = state.filters.visitedOnly,
                                favoritesOnly = state.filters.favoritesOnly,
                                onToggleOpen = vm::toggleOpenOnly,
                                onToggleNew = vm::toggleNewOnly,
                                onToggleVisited = vm::toggleVisitedOnly,
                                onToggleFavorites = vm::toggleFavoritesOnly,
                                onToggleNearby = vm::toggleNearbyOnly,
                                coffeeFocusId = state.filters.coffeeFocus,
                                onCoffeeFocusChange = { id ->
                                    vm.setCoffeeFocus(
                                        if (state.filters.coffeeFocus == id) null else id,
                                    )
                                },
                            )
                        }
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            if (state.showFilters) {
                ShopFiltersScreen(
                    state = state,
                    onDismiss = vm::closeFilters,
                    onApply = vm::applyFilters,
                )
            }
            val contentModifier = Modifier.fillMaxSize()
            val navClearance = LocalFloatingNavClearance.current
            val listContentPadding = PaddingValues(
                start = CpDimens.spacing4,
                top = CpDimens.spacing4,
                end = CpDimens.spacing4,
                bottom = CpDimens.spacing4 + navClearance,
            )

            discoveryTransition.AnimatedContent(
                modifier = Modifier.fillMaxSize().padding(padding).clipToBounds().onGloballyPositioned {
                    viewportTop = it.positionInRoot().y - rootOrigin.y
                    viewportHeight = it.size.height.toFloat()
                },
                transitionSpec = {
                    val direction = if (targetState) AnimatedContentTransitionScope.SlideDirection.Down
                        else AnimatedContentTransitionScope.SlideDirection.Up
                    slideIntoContainer(direction, tween(360)) togetherWith
                        slideOutOfContainer(direction, tween(360))
                },
            ) { discoveryVisible ->
                when {
                    discoveryVisible -> {
                        CoffeePeekPullToRefresh(
                            listState = discoveryListState,
                            isRefreshing = state.isRefreshing,
                            onRefresh = vm::refresh,
                            modifier = contentModifier
                                .then(if (mapExpanded) Modifier.clearAndSetSemantics {} else Modifier)
                                .graphicsLayer {
                                    alpha = 1f - expansion.value
                                    translationY = screenSize.height * 0.3f * expansion.value
                            },
                        ) { scrollModifier ->
                            LazyColumn(
                                state = discoveryListState,
                                userScrollEnabled = !mapExpanded,
                                modifier = scrollModifier.fillMaxSize(),
                                contentPadding = listContentPadding,
                                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
                            ) {
                                item(key = "mini-map") {
                                    Box(Modifier.fillMaxWidth().height(240.dp).onGloballyPositioned {
                                        if (showDiscovery && discoveryProgress.value == 1f && !mapExpanded && expansion.value == 0f) {
                                            mapAnchor = Rect(it.positionInRoot() - rootOrigin, Size(it.size.width.toFloat(), it.size.height.toFloat()))
                                        }
                                    })
                                }
                                item(key = "shops-heading") {
                                    DiscoverySectionTitle("Кофейни", onShowAll = vm::activateSearch)
                                }
                                item(key = "shops-preview") {
                                    when {
                                        state.isLoading && state.shops.isEmpty() -> {
                                            Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) {
                                                CoffeePeekLoader()
                                            }
                                        }
                                        state.error != null && state.shops.isEmpty() -> {
                                            Column {
                                                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                TextButton(onClick = vm::refresh) { Text("Попробовать снова") }
                                            }
                                        }
                                        displayedShops.isEmpty() -> Text("Кофейни не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                            items(displayedShops.take(8), key = { it.id }) { shop ->
                                                ShopCard(
                                                    shop = shop,
                                                    distance = formatDistance(distanceToShopMeters(userLocation, shop.location)),
                                                    onClick = {
                                                        focusManager.clearFocus(force = true)
                                                        keyboard?.hide()
                                                        Navigator.navigate(Navigator.Screen.ShopDetail(shop.id))
                                                    },
                                                    onToggleFavorite = { vm.toggleFavorite(shop) },
                                                    modifier = Modifier.width(280.dp),
                                                    showCatalogDetails = false,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (roasterPreview != null) {
                                    item(key = "roasters-heading") {
                                        DiscoverySectionTitle("Обжарщики", onShowAll = onSelectRoasters)
                                    }
                                    item(key = "roasters-preview") { roasterPreview() }
                                }
                            }
                        }
                    }
                    state.isLoading && state.shops.isEmpty() && !showRoasterSearch -> {
                        Box(contentModifier, contentAlignment = Alignment.Center) {
                            CoffeePeekLoader()
                        }
                    }
                    state.error != null && state.shops.isEmpty() && !showRoasterSearch -> {
                        CoffeePeekPullToRefresh(
                            listState = listState,
                            isRefreshing = state.isRefreshing,
                            onRefresh = vm::refresh,
                            modifier = contentModifier,
                        ) { scrollModifier ->
                            LazyColumn(
                                state = listState,
                                modifier = scrollModifier.fillMaxSize(),
                                contentPadding = listContentPadding,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                item {
                                    Column(
                                        modifier = Modifier.fillParentMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            state.error ?: "Ошибка загрузки",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(CpDimens.spacing3))
                            Button(
                                onClick = vm::refresh,
                                modifier = Modifier.height(CpDimens.buttonHeight),
                                shape = RoundedCornerShape(percent = 50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) { Text("Попробовать снова") }
                                    }
                                }
                            }
                        }
                    }
                    fillingNearby && displayedShops.isEmpty() && !showRoasterSearch -> {
                        Box(contentModifier)
                    }
                    else -> {
                        CoffeePeekPullToRefresh(
                            listState = listState,
                            isRefreshing = state.isRefreshing,
                            onRefresh = vm::refresh,
                            modifier = contentModifier,
                        ) { scrollModifier ->
                            LazyColumn(
                                state = listState,
                                modifier = scrollModifier.fillMaxSize(),
                                contentPadding = listContentPadding,
                                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                            ) {
                                if (showRoasterSearch) {
                                    item(key = "roaster-search-heading") {
                                        DiscoverySectionTitle("Обжарщики", onShowAll = onSelectRoasters)
                                    }
                                    item(key = "roaster-search-results") { roasterPreview() }
                                    item(key = "shop-search-heading") {
                                        Text("Кофейни", style = MaterialTheme.typography.titleMedium)
                                    }
                                    when {
                                        state.isLoading && state.shops.isEmpty() -> item(key = "shop-search-loading") {
                                            Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) {
                                                CoffeePeekLoader()
                                            }
                                        }
                                        state.error != null && state.shops.isEmpty() -> item(key = "shop-search-error") {
                                            Column {
                                                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                TextButton(onClick = vm::refresh) { Text("Попробовать снова") }
                                            }
                                        }
                                        displayedShops.isEmpty() && !fillingNearby -> item(key = "shop-search-empty") {
                                            Text("Кофейни не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                items(displayedShops, key = { it.id }) { shop ->
                                    ShopCard(
                                        shop = shop,
                                        distance = formatDistance(distanceToShopMeters(userLocation, shop.location)),
                                        onClick = {
                                            focusManager.clearFocus(force = true)
                                            keyboard?.hide()
                                            Navigator.navigate(Navigator.Screen.ShopDetail(shop.id))
                                        },
                                        onToggleFavorite = { vm.toggleFavorite(shop) },
                                    )
                                }
                                if (!state.isLoading && state.error == null && !fillingNearby) item(key = "add-missing-shop") {
                                    AddMissingShopCard(
                                        onAddShop = {
                                            Navigator.navigate(Navigator.Screen.AddShop)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mapPreview != null && mapAnchor.width > 0f) {
            val mapOpacity = animateFloatAsState(
                if (mapExpanded || discoveryListState.firstVisibleItemIndex == 0) 1f else 0f,
                tween(360), label = "discovery-map-visibility",
            )
            val mapVisible by remember { derivedStateOf { mapOpacity.value > 0f && discoveryProgress.value > 0f } }
            val radiusPx = with(density) { CpDimens.radius2xl.toPx() }
            val outline = MaterialTheme.colorScheme.outline
            RetainedContent(visible = mapVisible) {
                // The viewport stays below the separator while its contents slide away.
                Box(Modifier.fillMaxSize().layout { measurable, constraints ->
                    val clipTop = (viewportTop * (1f - expansion.value)).roundToInt()
                        .coerceIn(0, constraints.maxHeight)
                    val height = constraints.maxHeight - clipTop
                    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, clipTop) }
                }.clipToBounds()) {
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        clip = true
                        alpha = mapOpacity.value
                        translationY = discoveryMapOffsetY(viewportHeight, discoveryProgress.value)
                    }) {
                        Box(Modifier.fillMaxSize()) {
                            mapPreview(
                                mapControlsExpanded,
                                { mapExpanded = !mapExpanded },
                                canvasSize,
                                Modifier.offset {
                                    val bounds = expandedMapBounds(mapAnchor, screenSize, expansion.value)
                                    val clipTop = (viewportTop * (1f - expansion.value)).roundToInt()
                                        .coerceIn(0, screenSize.height.roundToInt())
                                    IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt() - clipTop)
                                }.layout { measurable, _ ->
                                    val bounds = expandedMapBounds(mapAnchor, screenSize, expansion.value)
                                    val placeable = measurable.measure(Constraints.fixed(bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1)))
                                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                                }.graphicsLayer {
                                    clip = true
                                    shape = RoundedCornerShape(radiusPx * (1f - expansion.value))
                                }.drawWithContent {
                                    drawContent()
                                    drawRoundRect(outline, cornerRadius = CornerRadius(radiusPx * (1f - expansion.value)), style = Stroke(1.dp.toPx()))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun expandedMapBounds(preview: Rect, screenSize: Size, expansion: Float): Rect =
    lerp(preview, Rect(Offset.Zero, screenSize), expansion.coerceIn(0f, 1f))

internal fun discoveryMapOffsetY(viewportHeight: Float, progress: Float): Float =
    viewportHeight * (progress.coerceIn(0f, 1f) - 1f)

@Composable
private fun DiscoverySectionTitle(title: String, onShowAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        TextButton(onClick = onShowAll) {
            Text("Показать все")
            Spacer(Modifier.width(CpDimens.spacing1))
            Icon(CpIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddMissingShopCard(onAddShop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CpDimens.radiusXl))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing6),
        horizontalAlignment = Alignment.Start,
    ) {
        Image(
            painter = painterResource(Res.drawable.maskot_with_magnifying_glass),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(132.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(CpDimens.spacing3))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 20.dp),
        ) {
            Text(
                text = "Не нашли нужную кофейню?",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(CpDimens.spacing2))
            Text(
                text = "Предложите добавить её в CoffeePeek — после проверки она появится в каталоге.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(CpDimens.spacing2))
        TextButton(
            onClick = onAddShop,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = CpDimens.spacing2),
        ) {
            Icon(
                imageVector = CpIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(CpDimens.spacing2))
            Text("Добавить кофейню")
        }
    }
}

@Composable
private fun FeedQuickFilterBar(
    nearbyOnly: Boolean,
    showNearby: Boolean,
    openOnly: Boolean,
    newOnly: Boolean,
    visitedOnly: Boolean,
    favoritesOnly: Boolean,
    onToggleOpen: () -> Unit,
    onToggleNew: () -> Unit,
    onToggleVisited: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleNearby: () -> Unit,
    coffeeFocusId: String?,
    onCoffeeFocusChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        if (showNearby) {
            DesignFilterChip(
                label = "Рядом",
                selected = nearbyOnly,
                onClick = onToggleNearby,
                leadingIcon = CpIcons.Distance,
            )
        }
        DesignFilterChip(
            label = "Открыто",
            selected = openOnly,
            onClick = onToggleOpen,
            leadingIcon = CpIcons.Time,
        )
        DesignFilterChip(
            label = "Новые",
            selected = newOnly,
            onClick = onToggleNew,
            leadingIcon = CpIcons.Sparkle,
        )
        DesignFilterChip(
            label = "Уже был",
            selected = visitedOnly,
            onClick = onToggleVisited,
            leadingIcon = CpIcons.CheckCircle,
        )
        DesignFilterChip(
            label = "Избранное",
            selected = favoritesOnly,
            onClick = onToggleFavorites,
            leadingIcon = CpIcons.Favorite,
        )

        COFFEE_FOCUS_OPTIONS.forEach { option ->
            DesignFilterChip(
                label = option.label,
                selected = coffeeFocusId == option.id,
                onClick = { onCoffeeFocusChange(option.id) },
            )
        }
    }
}
