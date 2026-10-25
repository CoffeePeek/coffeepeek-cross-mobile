package com.coffeepeek.admin.di.checkinhistory.widget

import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryActivityMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.usecase.LoadHistoryActivityMonth
import com.coffeepeek.feature.checkinhistory.impl.ui.widget.historyWidgetDatesToLoad

/** One captured zone for all month reads, including the preceding month's visible week. */
internal suspend fun loadHistoryWidgetActivity(load: LoadHistoryActivityMonth,
    calendar: CheckInHistoryCalendarSnapshot): List<HistoryActivityMonth> {
    val dates = historyWidgetDatesToLoad(calendar.today)
    val results = dates.mapNotNull(HistoryMonth::fromDate).distinct().associateWith { month ->
        val bounds = calendar.monthPeriod(month.year, month.month)
        load(month, HistoryPeriod(bounds.fromInclusive, bounds.toExclusive), calendar::localDate)
    }
    if (results.values.all { it.isFailure }) throw results.values.first().exceptionOrNull()!!
    return results.map { (month, result) ->
        val activity = result.getOrElse { HistoryActivityMonth(month, emptyMap(), isPartial = true) }
        activity.copy(days = activity.days.filterKeys { it in dates })
    }
}
