package com.coffeepeek.feature.discovery.impl.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val mutable = MutableStateFlow(initial)
    val state = mutable.asStateFlow()
    val filters get() = mutable.value

    fun toggle(groupKey: String, id: String) {
        val group = DiscoveryFilterGroup.entries.firstOrNull { it.name == groupKey } ?: return
        toggle(group, id)
    }

    fun toggle(group: DiscoveryFilterGroup, id: String) {
        val selected = group.selected(filters)
        mutable.value = group.withSelection(filters, if (id in selected) selected - id else selected + id)
    }

    fun price(index: Int) {
        if (index in priceFilterStops.indices) selectPrice(priceFilterStops[index])
    }

    fun selectPrice(level: PriceLevel?) {
        mutable.value = filters.copy(priceLevel = level)
    }

    fun reset() {
        mutable.value = filters.clearSelections()
    }

    fun applyTo(current: DiscoveryFilters): DiscoveryFilters = filters.copy(cityId = current.cityId)
}
