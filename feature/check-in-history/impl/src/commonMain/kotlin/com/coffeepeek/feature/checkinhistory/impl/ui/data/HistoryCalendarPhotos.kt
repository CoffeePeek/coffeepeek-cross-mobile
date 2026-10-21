package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
import kotlin.time.Instant

/** Month snapshots, newest photographed visit first. */
internal fun historyCalendarPhotos(state: CombinedHistoryState, date: String): List<HistoryCalendarPhoto> {
    if (!state.isLoggedIn) return emptyList()
    val shops = if (state.filter.includes(HistorySource.Shop)) state.shopCalendar.content[date].orEmpty().mapNotNull { row ->
        val occurredAt = row.entry.visitedAt.ifBlank { row.entry.createdAt }
        (row.entry.photoCalendarUrls ?: row.entry.photoUrls).firstOrNull(String::isNotBlank)?.let { HistoryCalendarPhoto(it, false, occurredAt) }
    } else emptyList()
    val homes = if (state.filter.includes(HistorySource.Home)) state.homeCalendarPhotos[date].orEmpty() else emptyList()
    return (shops + homes).filter { it.url.isNotBlank() }
        .map { it to it.occurredAt?.instantOrNull() }.sortedByDescending { it.second }
        .map { it.first }.distinctBy { it.url }.take(3)
}

private fun String.instantOrNull(): Instant? = try {
    Instant.parse(this)
} catch (_: IllegalArgumentException) {
    null
}
