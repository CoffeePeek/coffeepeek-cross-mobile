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
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.*
import com.coffeepeek.feature.discovery.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInShopPickerScreen(
    search: SearchViewModel, map: MapViewModel, host: DiscoveryHost, onBack: () -> Unit,
    onClose: () -> Unit, onSelect: (String) -> Unit, onSignIn: () -> Unit, modifier: Modifier,
) {
    val state by search.state.collectAsStateWithLifecycle()
    val select by rememberUpdatedState(onSelect)
    val signIn by rememberUpdatedState(onSignIn)
    var showMap by rememberSaveable { mutableStateOf(false) }
    host.locationPermission(1, { search.onAction(SearchAction.LocationGranted) }, { search.onAction(SearchAction.LocationDenied) })
    LaunchedEffect(search) {
        search.events.collect { event ->
            when (event) {
                is SearchEvent.OpenShop -> select(event.shopId)
                SearchEvent.SignInRequired -> signIn()
            }
        }
    }
    CoffeePeekBackHandler(onBack = onBack)
    CheckInShopPickerContent(state, host, showMap, { showMap = it }, search::onAction, onClose, modifier) { mapModifier ->
        MapScreen(map, host, select, 0.dp, null, {}, mapModifier)
    }
}

@Composable
internal fun CheckInShopPickerContent(
    state: SearchState, host: DiscoveryHost, showMap: Boolean, onShowMap: (Boolean) -> Unit,
    onAction: (SearchAction) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier,
    mapContent: @Composable (Modifier) -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val list = rememberLazyListState()
    LaunchedEffect(state.query, state.filters.nearbyOnly) { list.scrollToItem(0) }
    val dismissInput = { focus.clearFocus(force = true); keyboard?.hide(); Unit }
    Scaffold(modifier = modifier, topBar = { PickerHeader(onClose) },
        containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
            PickerFilters(state, showMap, { dismissInput(); onShowMap(it) }, onAction)
            if (showMap) mapContent(Modifier.weight(1f))
            else {
                CoffeePeekSearchField(state.query, { onAction(SearchAction.Query(it)) },
                    placeholder = stringResource(Res.string.discovery_picker_search),
                    clearDescription = stringResource(Res.string.discovery_search_clear),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2))
                CheckInShopPickerList(state, host, onAction, list, Modifier.weight(1f))
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
        if (state.canUseNearby) CoffeePeekFilterChip(
            selected = state.filters.nearbyOnly && !showMap, onClick = {
                onShowMap(false)
                onAction(SearchAction.ToggleNearby)
            }, label = stringResource(Res.string.discovery_picker_nearby), leadingIcon = CoffeePeekIcons.Location)
        CoffeePeekFilterChip(selected = showMap, onClick = { onShowMap(!showMap) },
            label = stringResource(if (showMap) Res.string.discovery_picker_list else Res.string.discovery_picker_map),
            leadingIcon = if (showMap) CoffeePeekIcons.Search else CoffeePeekIcons.Map)
    }
}

@Composable
private fun CheckInShopPickerPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInShopPickerContent(SearchState(isLoading = false, canUseNearby = true,
        shops = listOf(ShopSummary(id = "preview", title = "CoffeePeek", rating = 4.7))),
        DiscoveryHost(), false, {}, {}, {})
}

@Preview @Composable private fun CheckInShopPickerLightPreview() = CheckInShopPickerPreview(false)
@Preview @Composable private fun CheckInShopPickerDarkPreview() = CheckInShopPickerPreview(true)
