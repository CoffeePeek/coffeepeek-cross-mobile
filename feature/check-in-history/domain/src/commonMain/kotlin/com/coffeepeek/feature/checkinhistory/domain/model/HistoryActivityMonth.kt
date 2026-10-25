package com.coffeepeek.feature.checkinhistory.domain.model

/** Account-owned month snapshot. Missing sources are explicit, never reported as an empty history. */
data class HistoryActivityMonth(
    val month: HistoryMonth,
    val days: Map<String, HistoryActivityDay>,
    val isPartial: Boolean,
)

data class HistoryActivityDay(val count: Long, val photo: HistoryCalendarPhoto?)
