package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.rules.amenityTags
import com.coffeepeek.feature.discovery.impl.resources.*
import com.coffeepeek.feature.discovery.impl.search.CatalogLoadState
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.priceFilterStop
import com.coffeepeek.feature.discovery.impl.search.priceFilterStops
import org.jetbrains.compose.resources.stringResource

data class DiscoveryIosFilterViewState(
    val title: String,
    val reset: String,
    val done: String,
    val close: String,
    val priceTitle: String,
    val prices: List<String>,
    val priceIndex: Int,
    val canReset: Boolean,
    val catalogStatus: String?,
    val isLoading: Boolean,
    val retry: String?,
    val sections: List<DiscoveryIosFilterSection>,
)

data class DiscoveryIosFilterSection(
    val id: String,
    val title: String,
    val selectedLabel: String?,
    val showAll: String,
    val collapse: String,
    val choices: List<DiscoveryIosFilterChoice>,
)

data class DiscoveryIosFilterChoice(val id: String, val title: String, val selected: Boolean, val photoUrl: String?)

@Composable
internal fun discoveryIosFilterViewState(state: SearchState, draft: DiscoveryFilters): DiscoveryIosFilterViewState {
    val loading = state.catalogLoadState == CatalogLoadState.Loading
    val retry = if (state.catalogLoadState == CatalogLoadState.Failed) stringResource(Res.string.discovery_retry) else null
    return DiscoveryIosFilterViewState(
        title = stringResource(Res.string.discovery_filters),
        reset = stringResource(Res.string.discovery_filters_reset),
        done = stringResource(Res.string.discovery_filters_done),
        close = stringResource(Res.string.discovery_filters_close),
        priceTitle = stringResource(Res.string.discovery_section_price),
        prices = priceFilterStops.map { priceLabel(it) },
        priceIndex = priceFilterStop(draft.priceLevel),
        canReset = draft.activeCount > 0,
        catalogStatus = catalogStatus(state),
        isLoading = loading,
        retry = retry,
        sections = filterSections(state, draft),
    )
}

@Composable
private fun catalogStatus(state: SearchState): String? = when (state.catalogLoadState) {
    CatalogLoadState.Loading -> stringResource(Res.string.discovery_catalog_loading)
    CatalogLoadState.Failed -> stringResource(Res.string.discovery_catalog_failed)
    CatalogLoadState.Ready -> if (state.hasNoCatalogChoices) stringResource(Res.string.discovery_catalog_empty) else null
}

@Composable
private fun filterSections(state: SearchState, draft: DiscoveryFilters): List<DiscoveryIosFilterSection> = with(state.catalogs) {
    listOf(
        filterSection(DiscoveryFilterGroup.Roasters, stringResource(Res.string.discovery_section_roasters), roasters, draft),
        filterSection(DiscoveryFilterGroup.Beans, stringResource(Res.string.discovery_section_beans), beans, draft),
        filterSection(DiscoveryFilterGroup.Equipment, stringResource(Res.string.discovery_section_equipment), equipment, draft),
        filterSection(DiscoveryFilterGroup.BrewMethods, stringResource(Res.string.discovery_section_brew_methods), brewMethods, draft) {
            filterBrewMethodLabel(it)
        },
        filterSection(DiscoveryFilterGroup.Tags, stringResource(Res.string.discovery_section_tags), amenityTags(tags), draft) {
            filterTagLabel(it)
        },
    ).filter { it.choices.isNotEmpty() }
}

@Composable
private fun filterSection(
    group: DiscoveryFilterGroup, title: String, entries: List<CatalogEntry>, draft: DiscoveryFilters,
    label: @Composable (CatalogEntry) -> String = { it.name },
): DiscoveryIosFilterSection {
    val selected = group.selected(draft)
    val count = entries.count { it.id in selected }
    return DiscoveryIosFilterSection(
        id = group.name,
        title = title,
        selectedLabel = if (count > 0) stringResource(Res.string.discovery_filters_selected, count) else null,
        showAll = stringResource(Res.string.discovery_filters_show_all, entries.size),
        collapse = stringResource(Res.string.discovery_filters_collapse),
        choices = entries.map { DiscoveryIosFilterChoice(it.id, label(it), it.id in selected, it.photoUrl) },
    )
}
