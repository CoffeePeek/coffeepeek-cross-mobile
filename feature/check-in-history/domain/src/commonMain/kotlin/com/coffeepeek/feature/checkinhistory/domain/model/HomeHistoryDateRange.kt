package com.coffeepeek.feature.checkinhistory.domain.model

/** Home's civil-date bounds, not the Shop instant-based exclusive HistoryPeriod. */
data class HomeHistoryDateRange(val from: String, val to: String) {
    init {
        require(HistoryMonth.fromDate(from) != null && HistoryMonth.fromDate(to) != null && from <= to)
    }

    companion object {
        fun month(month: HistoryMonth) = HomeHistoryDateRange(month.date(1), month.date(month.daysInMonth))
    }
}
