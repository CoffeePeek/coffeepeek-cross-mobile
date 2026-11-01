package com.coffeepeek.admin.ui.screen.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.discovery_map_truncated
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CpSearchField
import com.coffeepeek.admin.ui.component.GlassIconButton
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapHeader(
    state: MapUiState,
    actions: MapScreenActions,
    filterControl: (@Composable () -> Unit)?,
    filterBar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.statusBarsPadding().fillMaxWidth()
        .padding(vertical = CpDimens.spacing3).zIndex(2f),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        MapSearchControls(state.query, actions, filterControl)
        filterBar?.invoke()
        if (state.isTruncated) Box(Modifier.padding(horizontal = CpDimens.spacing4)) { MapTruncationNotice() }
        if (state.showSearchSuggestions) {
            Box(Modifier.padding(horizontal = CpDimens.spacing4)) {
                MapSearchSuggestions(state.searchResults, state.isSearchLoading, state.searchFailed, actions.onSearchSelect)
            }
        }
    }
}

@Composable
private fun MapSearchControls(query: String, actions: MapScreenActions, filterControl: (@Composable () -> Unit)?) {
    Row(Modifier.padding(horizontal = CpDimens.spacing4), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        actions.onCollapse?.let { collapse ->
            GlassIconButton(collapse, contentDescription = "Свернуть карту", hazeState = null) {
                Icon(CpIcons.Close, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        CpSearchField(value = query, onValueChange = actions.onQueryChange, placeholder = "Поиск кофейни…",
            fieldHeight = CpDimens.buttonHeight, modifier = Modifier.weight(1f))
        filterControl?.invoke()
    }
}

@Composable
private fun MapTruncationNotice() {
    Surface(shape = RoundedCornerShape(CpDimens.radiusMd), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp, shadowElevation = 3.dp) {
        Text(stringResource(Res.string.discovery_map_truncated), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing2))
    }
}

@Composable
private fun MapHeaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    MapHeader(MapUiState(query = "Coffee", isTruncated = true), MapScreenActions(onCollapse = {}),
        { DiscoveryMapFiltersButton(2, {}) }, null)
}

@Preview @Composable private fun MapHeaderLightPreview() = MapHeaderPreview(false)
@Preview @Composable private fun MapHeaderDarkPreview() = MapHeaderPreview(true)
