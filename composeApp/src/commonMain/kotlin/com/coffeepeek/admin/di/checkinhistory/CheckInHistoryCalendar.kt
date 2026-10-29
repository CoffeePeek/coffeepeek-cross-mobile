package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot

/** The device's calendar for history: today, its zone, month bounds and local dates by that zone's rules. */
internal fun interface CheckInHistoryCalendar {
    fun snapshot(): CheckInHistoryCalendarSnapshot
}

internal expect fun deviceCheckInHistoryCalendar(): CheckInHistoryCalendar
