package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import com.coffeepeek.feature.discovery.domain.rules.searchText
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.*
import com.coffeepeek.feature.discovery.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun CheckInShopPickerScreen(
    search: SearchViewModel, map: MapViewModel, host: DiscoveryHost,
    onBack: () -> Unit,
    onClose: () -> Unit, onSelect: (String) -> Unit, onSignIn: () -> Unit, modifier: Modifier,
) {
    val state by search.state.collectAsStateWithLifecycle()
    val select by rememberUpdatedState(onSelect)
    val signIn by rememberUpdatedState(onSignIn)
    var showMap by rememberSaveable { mutableStateOf(false) }
    host.locationPermission(state.locationPermissionRequest,
        { search.onAction(SearchAction.LocationGranted) }, { search.onAction(SearchAction.LocationDenied) })
    LaunchedEffect(search, map, showMap) {
        search.onAction(SearchAction.PresentationChanged(!showMap))
        if (showMap) map.onAction(MapAction.SyncCriteria(search.state.value.criteria))
        map.onAction(MapAction.PresentationChanged(showMap))
        if (showMap) search.state.map { it.criteria }.distinctUntilChanged().collect { map.onAction(MapAction.SyncCriteria(it)) }
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
    CheckInShopPickerContent(state, showMap, { showMap = it }, search::onAction, onClose, modifier, host.reduceMotion()) { mapModifier ->
        MapScreen(map, host, select, 0.dp, null, {}, mapModifier,
            onOpenFilters = null, onSearchAction = search::onAction, showSearchField = false)
    }
}

@Composable
internal fun CheckInShopPickerContent(
    state: SearchState, showMap: Boolean, onShowMap: (Boolean) -> Unit,
    onAction: (SearchAction) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier,
    reduceMotion: Boolean = false, mapContent: @Composable (Modifier) -> Unit = {},
) {
    val list = rememberLazyListState()
    val applied = state.pickerResultCriteria
    LaunchedEffect(searchText(applied?.text.orEmpty()), applied?.filters?.nearbyOnly == true) { list.scrollToItem(0) }
    Scaffold(modifier = modifier, topBar = { PickerHeader(onClose) },
        containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
            PickerSearchControls(state, showMap, onShowMap, onAction)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (showMap) mapContent(Modifier.fillMaxSize())
                else CheckInShopPickerList(state, onAction, list, Modifier.fillMaxSize(), reduceMotion)
                PickerLocationNotice(state, reduceMotion, onAction, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun PickerSearchControls(
    state: SearchState, showMap: Boolean, onShowMap: (Boolean) -> Unit, onAction: (SearchAction) -> Unit,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissInput: () -> Unit = {
        focus.clearFocus(force = true)
        keyboard?.hide()
    }
    CoffeePeekSearchField(state.query, { onAction(SearchAction.Query(it)) },
        placeholder = stringResource(Res.string.discovery_picker_search),
        clearDescription = stringResource(Res.string.discovery_search_clear),
        onSearch = dismissInput,
        modifier = Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2))
    PickerFilters(state, showMap, { visible ->
        dismissInput()
        onShowMap(visible)
    }, onAction)
}

@Composable
private fun PickerLocationNotice(state: SearchState, reduceMotion: Boolean, onAction: (SearchAction) -> Unit, modifier: Modifier) {
    val duration = if (reduceMotion) 0 else 150
    AnimatedVisibility(state.nearbyRequested || state.locationUnavailable, modifier,
        enter = fadeIn(tween(duration)), exit = fadeOut(tween(duration))) {
        DiscoveryLocationNotice(state.nearbyRequested, { onAction(SearchAction.DismissLocationFailure) })
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
            selected = (state.filters.nearbyOnly || state.nearbyRequested) && !showMap, onClick = {
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
    val state = SearchState(isLoading = false, shops = listOf(ShopSummary(id = "preview", title = "CoffeePeek")),
        pickerRecentShops = listOf(ShopSummary(id = "recent", title = "The Coffee Room")))
    CheckInShopPickerContent(state, showMap, {}, {}, {}) { mapModifier ->
        MapContent(MapState(), DiscoveryHost(), MapCanvasCallbacks({ _, _ -> }, {}, {}, {}, {}, {}),
            0.dp, {}, {}, mapModifier, onOpenFilters = null, onSearchAction = {}, showSearchField = false)
    }
}

@Preview @Composable private fun CheckInShopPickerLightPreview() = CheckInShopPickerPreview(false)
@Preview @Composable private fun CheckInShopPickerDarkPreview() = CheckInShopPickerPreview(true)
@Preview @Composable private fun CheckInShopPickerMapLightPreview() = CheckInShopPickerPreview(false, showMap = true)
@Preview @Composable private fun CheckInShopPickerMapDarkPreview() = CheckInShopPickerPreview(true, showMap = true)
