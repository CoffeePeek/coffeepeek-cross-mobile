package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.core.media.PrefetchListPhotos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.resources.*
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInShopPickerList(
    state: SearchState, recentShops: List<ShopSummary>,
    onAction: (SearchAction) -> Unit, list: LazyListState, modifier: Modifier,
) {
    val shops = state.visibleShops
    val recent = recentShops.takeIf { state.query.isBlank() && !state.filters.nearbyOnly }.orEmpty()
    val remaining = shops.filterNot { shop -> recent.any { it.id == shop.id } }
    val nearEnd by remember { derivedStateOf {
        (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 3
    } }
    LaunchedEffect(nearEnd, state.hasMore, state.currentPage, shops.size) {
        if (nearEnd && state.hasMore && shops.isNotEmpty() && state.error == null) onAction(SearchAction.LoadMore)
    }
    PrefetchListPhotos(list, (recent + remaining).map { it.id to listOfNotNull(it.photoUrl) })
    LazyColumn(modifier, state = list,
        contentPadding = coffeePeekScrollContentPadding(PaddingValues(CoffeePeekDimens.spacing4)),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        if (state.isLoading && shops.isEmpty()) item { CoffeePeekLoader(stringResource(Res.string.discovery_loading)) }
        if (recent.isNotEmpty()) {
            item { Text(stringResource(Res.string.discovery_picker_recent), style = MaterialTheme.typography.titleSmall) }
            itemsIndexed(recent, key = { _, shop -> "recent:${shop.id}" }) { _, shop ->
                CheckInShopPickerRow(shop, state.userLocation) { onAction(SearchAction.OpenShop(shop.id)) }
            }
        }
        itemsIndexed(remaining, key = { index, shop -> "${shop.id}:$index" }) { _, shop ->
            CheckInShopPickerRow(shop, state.userLocation) { onAction(SearchAction.OpenShop(shop.id)) }
        }
        item { PickerListStatus(state, recent.isNotEmpty(), { onAction(SearchAction.Refresh) }) }
        if (state.isLoadingMore) item { CoffeePeekLoader(stringResource(Res.string.discovery_loading)) }
    }
}

@Composable
private fun PickerListErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInShopPickerList(SearchState(isLoading = false,
        error = com.coffeepeek.feature.discovery.impl.search.SearchError(null)),
        emptyList(), {}, rememberLazyListState(), Modifier.fillMaxSize())
}

@Preview @Composable private fun PickerListErrorLightPreview() = PickerListErrorPreview(false)
@Preview @Composable private fun PickerListErrorDarkPreview() = PickerListErrorPreview(true)

@Composable
private fun PickerListStatus(state: SearchState, hasRecent: Boolean, retry: () -> Unit) {
    when {
        state.error != null -> {
            Text(state.error.serverMessage ?: stringResource(Res.string.discovery_picker_failed), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = retry) { Text(stringResource(Res.string.discovery_picker_retry)) }
        }
        state.visibleShops.isEmpty() && !hasRecent && !state.isLoading && !state.isLoadingMore && !state.hasMore ->
            Text(stringResource(Res.string.discovery_picker_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
