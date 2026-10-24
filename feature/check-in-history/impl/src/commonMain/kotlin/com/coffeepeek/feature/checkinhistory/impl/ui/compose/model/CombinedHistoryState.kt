package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto

internal data class CombinedHistoryState(
    val today: String,
    val month: HistoryMonth,
    val isLoggedIn: Boolean,
    val sessionGeneration: Int,
    val filter: HistoryKindFilter = HistoryKindFilter.All,
    val selectedDate: String? = null,
    val shopCalendar: CombinedHistoryLoadState<Map<String, List<HistoryTimelineItem.Shop>>> = CombinedHistoryLoadState(emptyMap()),
    val homeCalendar: CombinedHistoryLoadState<Map<String, Int>> = CombinedHistoryLoadState(emptyMap()),
    val shops: CombinedHistoryPageState<HistoryTimelineItem.Shop> = CombinedHistoryPageState(),
    val homes: CombinedHistoryPageState<HistoryTimelineItem.Home> = CombinedHistoryPageState(),
    val isCalendarUnavailable: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedShopKey: String? = null,
    val showCreate: Boolean = false,
    val homeCalendarPhotos: Map<String, List<HistoryCalendarPhoto>> = emptyMap(),
) {
    val selectedShop: HistoryTimelineItem.Shop? get() = selectedShopKey?.let { key ->
        timeline.firstOrNull { it.key == key } as? HistoryTimelineItem.Shop
    }
    val canGoNextMonth: Boolean get() = month.date(1).take(7) < today.take(7)

    val timeline: List<HistoryTimelineItem> get() {
        if (!isLoggedIn) return emptyList()
        val shopItems = if (!filter.includes(HistorySource.Shop)) emptyList()
            else if (selectedDate == null) shops.items else shopCalendar.content[selectedDate].orEmpty()
        val homeItems = if (filter.includes(HistorySource.Home)) homes.items else emptyList()
        return (shopItems + homeItems).sortedWith(HistoryTimelineOrder)
    }

    val calendarCounts: Map<String, Long> get() {
        if (!isLoggedIn) return emptyMap()
        val shops = if (filter.includes(HistorySource.Shop)) shopCalendar.content else emptyMap()
        val homes = if (filter.includes(HistorySource.Home)) homeCalendar.content else emptyMap()
        return (shops.keys + homes.keys).associateWith { shops[it].orEmpty().size.toLong() + (homes[it] ?: 0) }
    }

    val isTimelineLoading: Boolean get() =
        filter.includes(HistorySource.Shop) && (if (selectedDate == null) shops.isLoading else shopCalendar.isLoading) ||
            filter.includes(HistorySource.Home) && homes.isLoading

    val hasMore: Boolean get() =
        filter.includes(HistorySource.Shop) && selectedDate == null && shops.hasMore ||
            filter.includes(HistorySource.Home) && homes.hasMore

    /** Hide private snapshots before an asynchronously reduced account action reaches the VM. */
    fun visibleTo(generation: Int, loggedIn: Boolean): CombinedHistoryState =
        if (generation == sessionGeneration && loggedIn == isLoggedIn) this else copy(
            sessionGeneration = generation, isLoggedIn = loggedIn,
            shopCalendar = CombinedHistoryLoadState(emptyMap(), isLoading = loggedIn),
            homeCalendar = CombinedHistoryLoadState(emptyMap(), isLoading = loggedIn),
            shops = CombinedHistoryPageState(isLoading = loggedIn),
            homes = CombinedHistoryPageState(isLoading = loggedIn),
            selectedShopKey = null, showCreate = false, isRefreshing = false,
            homeCalendarPhotos = emptyMap(),
        )
}
