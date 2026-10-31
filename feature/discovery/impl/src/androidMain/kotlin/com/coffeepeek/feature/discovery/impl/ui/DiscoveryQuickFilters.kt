package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFilterChip
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
internal fun DiscoveryQuickFilters(
    filters: DiscoveryFilters, onAction: (SearchAction) -> Unit,
    cityName: String? = null, onChangeCity: (() -> Unit)? = null,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (onChangeCity != null) item { DiscoveryCityButton(cityName, onChangeCity) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_nearby), filters.nearbyOnly, { onAction(SearchAction.ToggleNearby) }, leadingIcon = CoffeePeekIcons.Distance) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_open), filters.openOnly, { onAction(SearchAction.ToggleOpen) }, leadingIcon = CoffeePeekIcons.Time) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_new), filters.newOnly, { onAction(SearchAction.ToggleNew) }, leadingIcon = CoffeePeekIcons.Sparkle) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_visited), filters.visitedOnly, { onAction(SearchAction.ToggleVisited) }, leadingIcon = CoffeePeekIcons.CheckCircle) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_favorites), filters.favoritesOnly, { onAction(SearchAction.ToggleFavorites) }, leadingIcon = CoffeePeekIcons.Favorite) }
        items(ShopKind.entries) { kind ->
            CoffeePeekFilterChip(stringResource(kind.label()), filters.kind == kind, { onAction(SearchAction.SetKind(kind.takeUnless { filters.kind == it })) })
        }
    }
}

private fun ShopKind.label() = when (this) {
    ShopKind.Specialty -> R.string.discovery_kind_specialty
    ShopKind.CoffeeBar -> R.string.discovery_kind_coffee_bar
    ShopKind.Cafe -> R.string.discovery_kind_cafe
}

@PreviewLightDark
@Composable
private fun DiscoveryQuickFiltersPreview() = CoffeePeekTheme {
    DiscoveryQuickFilters(DiscoveryFilters(openOnly = true), onAction = {}, cityName = "Минск", onChangeCity = {})
}

