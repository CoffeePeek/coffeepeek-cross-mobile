package com.coffeepeek.feature.checkinhistory.impl

import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarContext
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarProvider

internal fun historyCalendarProvider(calendarSnapshot: () -> CheckInHistoryCalendarSnapshot): HistoryCalendarProvider {
    return HistoryCalendarProvider {
        val snapshot = calendarSnapshot()
        object : HistoryCalendarContext {
            override val today = snapshot.today
            override val zoneId = snapshot.zoneId
            override fun period(month: HistoryMonth): HistoryPeriod = snapshot.monthPeriod(month.year, month.month)
                .let { HistoryPeriod(it.fromInclusive, it.toExclusive) }
            override fun localDate(instant: String): String? = snapshot.localDate(instant)
        }
    }
}
