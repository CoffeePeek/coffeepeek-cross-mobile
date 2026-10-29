package com.coffeepeek.feature.discovery.impl.ui


import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_kind_cafe
import com.coffeepeek.feature.discovery.impl.resources.discovery_kind_coffee_bar
import com.coffeepeek.feature.discovery.impl.resources.discovery_kind_specialty
import com.coffeepeek.feature.discovery.impl.resources.discovery_quick_favorites
import com.coffeepeek.feature.discovery.impl.resources.discovery_quick_nearby
import com.coffeepeek.feature.discovery.impl.resources.discovery_quick_new
import com.coffeepeek.feature.discovery.impl.resources.discovery_quick_open
import com.coffeepeek.feature.discovery.impl.resources.discovery_quick_visited
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFilterChip
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryQuickFilters(filters: DiscoveryFilters, canUseNearby: Boolean, onAction: (SearchAction) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (canUseNearby) item { CoffeePeekFilterChip(stringResource(Res.string.discovery_quick_nearby), filters.nearbyOnly, { onAction(SearchAction.ToggleNearby) }, leadingIcon = CoffeePeekIcons.Distance) }
        item { CoffeePeekFilterChip(stringResource(Res.string.discovery_quick_open), filters.openOnly, { onAction(SearchAction.ToggleOpen) }, leadingIcon = CoffeePeekIcons.Time) }
        item { CoffeePeekFilterChip(stringResource(Res.string.discovery_quick_new), filters.newOnly, { onAction(SearchAction.ToggleNew) }, leadingIcon = CoffeePeekIcons.Sparkle) }
        item { CoffeePeekFilterChip(stringResource(Res.string.discovery_quick_visited), filters.visitedOnly, { onAction(SearchAction.ToggleVisited) }, leadingIcon = CoffeePeekIcons.CheckCircle) }
        item { CoffeePeekFilterChip(stringResource(Res.string.discovery_quick_favorites), filters.favoritesOnly, { onAction(SearchAction.ToggleFavorites) }, leadingIcon = CoffeePeekIcons.Favorite) }
        items(ShopKind.entries) { kind ->
            CoffeePeekFilterChip(stringResource(kind.label()), filters.kind == kind, { onAction(SearchAction.SetKind(kind.takeUnless { filters.kind == it })) })
        }
    }
}

private fun ShopKind.label() = when (this) {
    ShopKind.Specialty -> Res.string.discovery_kind_specialty
    ShopKind.CoffeeBar -> Res.string.discovery_kind_coffee_bar
    ShopKind.Cafe -> Res.string.discovery_kind_cafe
}

@Preview
@Composable
private fun DiscoveryQuickFiltersPreview() = CoffeePeekTheme {
    DiscoveryQuickFilters(DiscoveryFilters(openOnly = true, nearbyOnly = true), canUseNearby = true, onAction = {})
}

