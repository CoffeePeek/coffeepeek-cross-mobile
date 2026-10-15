package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod

/** Composition supplies platform date operations; no Android/clock APIs in MVI. */
internal fun interface HistoryCalendarProvider {
    fun snapshot(): HistoryCalendarContext
}

/** One immutable time-zone snapshot for query bounds and response grouping. */
internal interface HistoryCalendarContext {
    val today: String
    val zoneId: String
    fun period(month: HistoryMonth): HistoryPeriod
    fun localDate(instant: String): String?
}
