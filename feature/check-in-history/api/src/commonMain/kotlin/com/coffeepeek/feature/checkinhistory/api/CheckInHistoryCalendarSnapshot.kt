package com.coffeepeek.feature.checkinhistory.api

/** Immutable zone snapshot shared by month query bounds and response date grouping. */
interface CheckInHistoryCalendarSnapshot {
    val today: String
    val zoneId: String
    fun monthPeriod(year: Int, month: Int): CheckInHistoryDateBounds
    /** Local civil boundaries converted separately, so weeks crossing DST keep their real duration. */
    fun datePeriod(fromDate: String, toExclusiveDate: String): CheckInHistoryDateBounds
    fun localDate(instant: String): String?
}

data class CheckInHistoryDateBounds(val fromInclusive: String, val toExclusive: String)
