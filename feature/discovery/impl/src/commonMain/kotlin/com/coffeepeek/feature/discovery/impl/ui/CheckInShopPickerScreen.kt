package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.MapCanvasCallbacks
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.*
import com.coffeepeek.feature.discovery.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun CheckInShopPickerScreen(
    search: SearchViewModel, map: MapViewModel, host: DiscoveryHost,
    recentShops: com.coffeepeek.feature.discovery.api.DiscoveryRecentShops, onBack: () -> Unit,
    onClose: () -> Unit, onSelect: (String) -> Unit, onSignIn: () -> Unit, modifier: Modifier,
) {
    val state by search.state.collectAsStateWithLifecycle()
    val select by rememberUpdatedState(onSelect)
    val signIn by rememberUpdatedState(onSignIn)
    val recent by produceState(emptyList<ShopSummary>(), recentShops) {
        val result = recentShops.load()
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        value = result.getOrNull().orEmpty()
    }
    var showMap by rememberSaveable { mutableStateOf(false) }
    host.locationPermission(state.locationPermissionRequest,
        { search.onAction(SearchAction.LocationGranted) }, { search.onAction(SearchAction.LocationDenied) })
    LaunchedEffect(search, map) {
        search.state.map { it.criteria }.distinctUntilChanged().collect { map.onAction(MapAction.SyncCriteria(it)) }
    }
    LaunchedEffect(search, map, showMap) {
        search.onAction(SearchAction.PresentationChanged(!showMap))
        map.onAction(MapAction.PresentationChanged(showMap))
    }
    LaunchedEffect(search) {
        search.events.collect { event ->
            when (event) {
                is SearchEvent.OpenShop -> select(event.shopId)
                SearchEvent.SignInRequired -> signIn()
            }
        }
    }
    CoffeePeekBackHandler(onBack = onBack)
    CheckInShopPickerContent(state, showMap, { showMap = it }, search::onAction, onClose, modifier, recent) { mapModifier, filters ->
        MapScreen(map, host, select, 0.dp, null, {}, mapModifier,
            onOpenFilters = null, onSearchAction = search::onAction, belowSearch = filters)
    }
}

@Composable
internal fun CheckInShopPickerContent(
    state: SearchState, showMap: Boolean, onShowMap: (Boolean) -> Unit,
    onAction: (SearchAction) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier,
    recentShops: List<ShopSummary> = emptyList(),
    mapContent: @Composable (Modifier, @Composable () -> Unit) -> Unit = { _, _ -> },
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val list = rememberLazyListState()
    LaunchedEffect(state.query, state.filters.nearbyOnly) { list.scrollToItem(0) }
    val filters: @Composable () -> Unit = {
        PickerFilters(state, showMap, { visible ->
            focus.clearFocus(force = true)
            keyboard?.hide()
            onShowMap(visible)
        }, onAction)
        if (state.nearbyRequested || state.locationUnavailable) {
            DiscoveryLocationNotice(state.nearbyRequested, { onAction(SearchAction.DismissLocationFailure) })
        }
    }
    Scaffold(modifier = modifier, topBar = { PickerHeader(onClose) },
        containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
            if (showMap) mapContent(Modifier.weight(1f), filters)
            else {
                CoffeePeekSearchField(state.query, { onAction(SearchAction.Query(it)) },
                    placeholder = stringResource(Res.string.discovery_picker_search),
                    clearDescription = stringResource(Res.string.discovery_search_clear),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2))
                filters()
                CheckInShopPickerList(state, recentShops, onAction, list, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PickerHeader(onClose: () -> Unit) {
    val close = stringResource(Res.string.discovery_picker_close)
    CoffeePeekTopBar(stringResource(Res.string.discovery_picker_title), close,
        nativeAction = CoffeePeekHeaderAction(close, onClose),
        actions = {
            CoffeePeekGlassIconButton(onClick = onClose,
                contentDescription = stringResource(Res.string.discovery_picker_close_description)) {
                Icon(CoffeePeekIcons.Close, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        })
}

@Composable
private fun PickerFilters(state: SearchState, showMap: Boolean, onShowMap: (Boolean) -> Unit, onAction: (SearchAction) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4)
        .horizontalScroll(rememberScrollState()).padding(vertical = CoffeePeekDimens.spacing2),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        CoffeePeekFilterChip(
            selected = state.filters.nearbyOnly && !showMap, onClick = {
                onShowMap(false)
                if (!showMap || !state.filters.nearbyOnly) onAction(SearchAction.ToggleNearby)
            }, label = stringResource(Res.string.discovery_picker_nearby), leadingIcon = CoffeePeekIcons.Location)
        CoffeePeekFilterChip(selected = showMap, onClick = { onShowMap(!showMap) },
            label = stringResource(if (showMap) Res.string.discovery_picker_list else Res.string.discovery_picker_map),
            leadingIcon = if (showMap) CoffeePeekIcons.Search else CoffeePeekIcons.Map)
    }
}

@Composable
private fun CheckInShopPickerPreview(dark: Boolean, showMap: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val state = SearchState(isLoading = false, shops = listOf(ShopSummary(id = "preview", title = "CoffeePeek")))
    CheckInShopPickerContent(state, showMap, {}, {}, {}) { mapModifier, filters ->
        MapContent(MapState(), DiscoveryHost(), MapCanvasCallbacks({ _, _ -> }, {}, {}, {}, {}, {}),
            0.dp, {}, {}, mapModifier, onOpenFilters = null, onSearchAction = {}, belowSearch = filters)
    }
}

@Preview @Composable private fun CheckInShopPickerLightPreview() = CheckInShopPickerPreview(false)
@Preview @Composable private fun CheckInShopPickerDarkPreview() = CheckInShopPickerPreview(true)
@Preview @Composable private fun CheckInShopPickerMapLightPreview() = CheckInShopPickerPreview(false, showMap = true)
@Preview @Composable private fun CheckInShopPickerMapDarkPreview() = CheckInShopPickerPreview(true, showMap = true)
