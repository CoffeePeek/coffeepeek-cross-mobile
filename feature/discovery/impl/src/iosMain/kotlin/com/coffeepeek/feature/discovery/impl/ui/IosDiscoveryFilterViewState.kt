package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.rules.amenityTags
import com.coffeepeek.feature.discovery.impl.resources.*
import com.coffeepeek.feature.discovery.impl.search.CatalogLoadState
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.priceFilterStop
import com.coffeepeek.feature.discovery.impl.search.priceFilterStops

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

data class DiscoveryIosFilterChoice(val id: String, val title: String, val selected: Boolean, val photoUrl: String?, val icon: platform.UIKit.UIImage? = null)

internal suspend fun DiscoveryIosResources.discoveryIosFilterViewState(state: SearchState, draft: DiscoveryFilters): DiscoveryIosFilterViewState {
    val loading = state.catalogLoadState == CatalogLoadState.Loading
    val retry = if (state.catalogLoadState == CatalogLoadState.Failed) text(Res.string.discovery_retry) else null
    return DiscoveryIosFilterViewState(
        title = text(Res.string.discovery_filters),
        reset = text(Res.string.discovery_filters_reset),
        done = text(Res.string.discovery_filters_done),
        close = text(Res.string.discovery_filters_close),
        priceTitle = text(Res.string.discovery_section_price),
        prices = priceFilterStops.map { price(it) },
        priceIndex = priceFilterStop(draft.priceLevel),
        canReset = draft.activeCount > 0,
        catalogStatus = catalogStatus(state),
        isLoading = loading,
        retry = retry,
        sections = filterSections(state, draft),
    )
}

private suspend fun DiscoveryIosResources.catalogStatus(state: SearchState): String? = when (state.catalogLoadState) {
    CatalogLoadState.Loading -> text(Res.string.discovery_catalog_loading)
    CatalogLoadState.Failed -> text(Res.string.discovery_catalog_failed)
    CatalogLoadState.Ready -> if (state.hasNoCatalogChoices) text(Res.string.discovery_catalog_empty) else null
}

private suspend fun DiscoveryIosResources.filterSections(state: SearchState, draft: DiscoveryFilters): List<DiscoveryIosFilterSection> = with(state.catalogs) {
    listOf(
        filterSection(DiscoveryFilterGroup.Roasters, text(Res.string.discovery_section_roasters), roasters, draft),
        filterSection(DiscoveryFilterGroup.Beans, text(Res.string.discovery_section_beans), beans, draft),
        filterSection(DiscoveryFilterGroup.Equipment, text(Res.string.discovery_section_equipment), equipment, draft),
        filterSection(DiscoveryFilterGroup.BrewMethods, text(Res.string.discovery_section_brew_methods), brewMethods, draft),
        filterSection(DiscoveryFilterGroup.Tags, text(Res.string.discovery_section_tags), amenityTags(tags), draft),
    ).filter { it.choices.isNotEmpty() }
}

private suspend fun DiscoveryIosResources.filterSection(
    group: DiscoveryFilterGroup, title: String, entries: List<CatalogEntry>, draft: DiscoveryFilters,
): DiscoveryIosFilterSection {
    val selected = group.selected(draft)
    val count = entries.count { it.id in selected }
    return DiscoveryIosFilterSection(
        id = group.name,
        title = title,
        selectedLabel = if (count > 0) text(Res.string.discovery_filters_selected, count) else null,
        showAll = text(Res.string.discovery_filters_show_all, entries.size),
        collapse = text(Res.string.discovery_filters_collapse),
        choices = entries.map { DiscoveryIosFilterChoice(it.id, label(group, it), it.id in selected, it.photoUrl, filterIcon(group, it)) },
    )
}
