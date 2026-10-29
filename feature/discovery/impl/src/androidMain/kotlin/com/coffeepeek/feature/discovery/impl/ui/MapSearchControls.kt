package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
internal fun MapSearchControls(
    state: MapState, dismissInput: () -> Unit, onAction: (MapAction) -> Unit,
    onToggleExpand: (() -> Unit)?, onOpenFilters: (() -> Unit)?, modifier: Modifier = Modifier,
    onSearchAction: (SearchAction) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            if (onToggleExpand != null) {
                CoffeePeekGlassIconButton({ dismissInput(); onToggleExpand() }, stringResource(R.string.discovery_map_collapse), hazeState = null) {
                    Icon(CoffeePeekIcons.Close, null, Modifier.size(22.dp))
                }
            }
            CoffeePeekSearchField(
                value = state.query, onValueChange = { onSearchAction(SearchAction.Query(it)) },
                placeholder = stringResource(R.string.discovery_map_search_placeholder),
                clearDescription = stringResource(R.string.discovery_search_clear),
                fieldHeight = CoffeePeekDimens.buttonHeight, onSearch = dismissInput, modifier = Modifier.weight(1f),
            )
            if (onOpenFilters != null) DiscoveryFilterButton(state.filters.activeCount, { dismissInput(); onOpenFilters() })
        }
        if (onOpenFilters != null) {
            DiscoveryQuickFilters(state.filters, state.userLocation != null, onSearchAction)
        }
        if (state.isTruncated) MapTruncationNotice()
        if (state.showSuggestions) MapSuggestions(state, onSelect = { dismissInput(); onAction(MapAction.SelectSuggestion(it)) })
    }
}

@Composable
private fun MapTruncationNotice() {
    Surface(shape = RoundedCornerShape(CoffeePeekDimens.radiusMd), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp, shadowElevation = 3.dp) {
        Text(stringResource(R.string.discovery_map_truncated), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing2))
    }
}

@PreviewLightDark
@Composable
private fun MapSearchControlsPreview() = CoffeePeekTheme {
    MapSearchControls(MapState(filters = DiscoveryFilters(tagIds = setOf("laptop-friendly")), isTruncated = true),
        {}, {}, {}, {}, onSearchAction = {})
}

