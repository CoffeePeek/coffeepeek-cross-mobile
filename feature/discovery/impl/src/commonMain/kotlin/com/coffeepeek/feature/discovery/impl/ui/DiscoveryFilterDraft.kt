package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.impl.search.priceFilterStops

internal enum class DiscoveryFilterGroup {
    Roasters, Beans, Equipment, BrewMethods, Tags;

    fun selected(filters: DiscoveryFilters): Set<String> = when (this) {
        Roasters -> filters.roasterIds
        Beans -> filters.beanIds
        Equipment -> filters.equipmentIds
        BrewMethods -> filters.brewMethodIds
        Tags -> filters.tagIds
    }

    fun withSelection(filters: DiscoveryFilters, selection: Set<String>): DiscoveryFilters = when (this) {
        Roasters -> filters.copy(roasterIds = selection)
        Beans -> filters.copy(beanIds = selection)
        Equipment -> filters.copy(equipmentIds = selection)
        BrewMethods -> filters.copy(brewMethodIds = selection)
        Tags -> filters.copy(tagIds = selection)
    }
}

internal class DiscoveryFilterDraft(initial: DiscoveryFilters) {
    var filters by mutableStateOf(initial)
        private set

    fun toggle(groupKey: String, id: String) {
        val group = DiscoveryFilterGroup.entries.firstOrNull { it.name == groupKey } ?: return
        toggle(group, id)
    }

    fun toggle(group: DiscoveryFilterGroup, id: String) {
        val selected = group.selected(filters)
        filters = group.withSelection(filters, if (id in selected) selected - id else selected + id)
    }

    fun price(index: Int) {
        if (index in priceFilterStops.indices) selectPrice(priceFilterStops[index])
    }

    fun selectPrice(level: PriceLevel?) {
        filters = filters.copy(priceLevel = level)
    }

    fun reset() {
        filters = filters.clearSelections()
    }

    fun applyTo(current: DiscoveryFilters): DiscoveryFilters = filters.copy(cityId = current.cityId)
}
