package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource

/** Month snapshots, never the selected day/current timeline page. Cutouts have visual priority. */
internal fun historyCalendarPhotos(state: CombinedHistoryState, date: String): List<HistoryCalendarPhoto> {
    if (!state.isLoggedIn) return emptyList()
    val shops = if (state.filter.includes(HistorySource.Shop)) state.shopCalendar.content[date].orEmpty().mapNotNull { row ->
        row.entry.drinkPhotoUrl?.takeIf(String::isNotBlank)?.let { HistoryCalendarPhoto(it, true) }
            ?: row.entry.photoUrls.firstOrNull(String::isNotBlank)?.let { HistoryCalendarPhoto(it, false) }
    } else emptyList()
    val homes = if (state.filter.includes(HistorySource.Home)) state.homeCalendarPhotos[date].orEmpty() else emptyList()
    return (shops + homes).filter { it.url.isNotBlank() }.sortedByDescending { it.isCutout }.distinctBy { it.url }.take(3)
}
