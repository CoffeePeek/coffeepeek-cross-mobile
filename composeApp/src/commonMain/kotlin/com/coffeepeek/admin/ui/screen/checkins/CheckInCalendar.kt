package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun localCalendarBoundary(date: String): String =
    datePickerMillisToUtcIsoInstant(Instant.parse("${date}T00:00:00Z").toEpochMilliseconds())

data class CalendarMonth(val year: Int, val month: Int) {
    init {
        require(month in 1..12)
    }

    fun plusMonths(offset: Int): CalendarMonth {
        val absoluteMonth = year * 12 + month - 1 + offset
        return CalendarMonth(
            year = absoluteMonth.floorDiv(12),
            month = absoluteMonth.mod(12) + 1,
        )
    }

    fun isoDate(day: Int): String = "$year-${month.twoDigits()}-${day.twoDigits()}"

    val fromUtc: String get() = localCalendarBoundary(isoDate(1))
    val toUtc: String get() = localCalendarBoundary(plusMonths(1).isoDate(1))
}

/** A complete Monday–Sunday range, including days in neighbouring months. */
data class CalendarWeek(val today: String) {
    val dates: List<String> = calendarWeekDates(today)
    init { require(dates.size == 7) }
    val fromUtc: String get() = localCalendarBoundary(dates.first())
    val toUtc: String get() = localCalendarBoundary(shiftCalendarDate(dates.last(), 1))
}

internal fun calendarMonthFromIsoDate(value: String): CalendarMonth? {
    val parts = value.substringBefore('T').split('-')
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return runCatching { CalendarMonth(year, month) }.getOrNull()
}

internal fun calendarCells(month: CalendarMonth): List<Int?> {
    val leadingEmptyDays = mondayFirstDayOfWeek(month.year, month.month, 1)
    val days = List(leadingEmptyDays) { null } + (1..daysInMonth(month.year, month.month)).toList()
    return days + List((7 - days.size % 7) % 7) { null }
}

internal fun shiftCalendarDate(value: String, offset: Int): String {
    var month = calendarMonthFromIsoDate(value) ?: return value
    var day = value.substringAfterLast('-').toIntOrNull() ?: return value
    if (day !in 1..daysInMonth(month.year, month.month)) return value
    var remaining = offset
    while (remaining > 0) {
        day++
        if (day > daysInMonth(month.year, month.month)) { month = month.plusMonths(1); day = 1 }
        remaining--
    }
    while (remaining < 0) {
        day--
        if (day < 1) { month = month.plusMonths(-1); day = daysInMonth(month.year, month.month) }
        remaining++
    }
    return month.isoDate(day)
}

internal fun calendarWeekDates(value: String): List<String> {
    val month = calendarMonthFromIsoDate(value) ?: return emptyList()
    val day = value.substringAfterLast('-').toIntOrNull() ?: return emptyList()
    val index = calendarCells(month).indexOf(day)
    if (index < 0) return emptyList()
    val monday = shiftCalendarDate(value, -(index % 7))
    return (0..6).map { shiftCalendarDate(monday, it) }
}

internal data class CheckInStreak(
    val hasPreviousDay: Boolean,
    val hasNextDay: Boolean,
) {
    val isPartOfStreak: Boolean get() = hasPreviousDay || hasNextDay
}

internal fun checkInStreak(month: CalendarMonth, day: Int, checkInDates: Set<String>): CheckInStreak {
    if (month.isoDate(day) !in checkInDates) return CheckInStreak(false, false)
    return CheckInStreak(
        hasPreviousDay = day > 1 && month.isoDate(day - 1) in checkInDates,
        hasNextDay = month.isoDate(day + 1) in checkInDates,
    )
}

private fun daysInMonth(year: Int, month: Int) = when (month) {
    2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun mondayFirstDayOfWeek(year: Int, month: Int, day: Int): Int {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val adjustedYear = if (month < 3) year - 1 else year
    val sundayFirst = (
        adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 +
            offsets[month - 1] + day
        ) % 7
    return (sundayFirst + 6) % 7
}

private fun Int.twoDigits() = toString().padStart(2, '0')

/** Refreshes must not override a day explicitly selected while the request was in flight. */
internal fun calendarSelection(month: CalendarMonth, today: String, selected: String?, dates: Set<String>): String? = when {
    selected != null && calendarMonthFromIsoDate(selected) == month && selected <= today -> selected
    calendarMonthFromIsoDate(today) == month && today in dates -> today
    dates.isNotEmpty() -> dates.filter { calendarMonthFromIsoDate(it) == month && it <= today }.maxOrNull()
    calendarMonthFromIsoDate(today) == month -> today
    else -> null
}

internal fun calendarWeekday(date: String): String {
    val month = calendarMonthFromIsoDate(date) ?: return ""
    val day = date.substringAfterLast('-').toIntOrNull()?.takeIf { it in 1..daysInMonth(month.year, month.month) } ?: return ""
    return listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")[
        mondayFirstDayOfWeek(month.year, month.month, day)
    ]
}
