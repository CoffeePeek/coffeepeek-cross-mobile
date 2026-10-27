package com.coffeepeek.feature.contributions.domain.rules

import com.coffeepeek.feature.contributions.domain.model.DayHours
import com.coffeepeek.feature.contributions.domain.model.TimeInterval

/** Monday first, Sunday (0) last, as the wizard lists and sends the week. */
val WeekOrder: List<Int> = listOf(1, 2, 3, 4, 5, 6, 0)

/** One day as the form holds it, in local time. */
data class DayInput(val day: Int, val closed: Boolean, val open: String, val close: String)

/** Moves a `HH:mm` time by [minutes] and keeps it inside the day (00:00 to 23:59); an unreadable time counts as 09:00. */
fun shiftClock(time: String, minutes: Int): String {
    val parts = time.split(":")
    val total = (parts.getOrNull(0)?.toIntOrNull() ?: 9) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0) + minutes
    val clamped = total.coerceIn(0, 23 * 60 + 59)
    return clock(clamped)
}

/** Every day open at the same hours except the closed ones. */
fun unifiedWeek(open: String, close: String, closedDays: Set<Int>): List<DayHours> = WeekOrder.map { day ->
    if (day in closedDays) DayHours(day, closed = true) else DayHours(day, closed = false, intervals = listOf(TimeInterval(open, close)))
}

/** The days as the user set them one by one. */
fun perDayWeek(days: List<DayInput>): List<DayHours> = WeekOrder.mapNotNull { order -> days.firstOrNull { it.day == order } }.map { input ->
    if (input.closed) DayHours(input.day, closed = true) else DayHours(input.day, closed = false, intervals = listOf(TimeInterval(input.open, input.close)))
}

/**
 * Moves a weekly schedule by [minutes] (local time to UTC is minus the UTC offset). An interval that crosses midnight moves to the neighbouring day,
 * a day without intervals is closed, and the result always lists the seven days Sunday (0) to Saturday (6).
 */
fun shiftWeek(week: List<DayHours>, minutes: Int): List<DayHours> {
    if (week.isEmpty()) return emptyList()
    val byDay = mutableMapOf<Int, MutableList<TimeInterval>>()
    week.filterNot { it.closed }.forEach { day ->
        day.intervals.forEach { interval ->
            val open = shifted(interval.open, minutes)
            val close = shifted(interval.close, minutes)
            byDay.getOrPut(((day.day + open.dayDelta) % 7 + 7) % 7, ::mutableListOf) += TimeInterval(open.value, close.value)
        }
    }
    return (0..6).map { day ->
        val intervals = byDay[day].orEmpty()
        DayHours(day, closed = intervals.isEmpty(), intervals = intervals)
    }
}

private class Shifted(val value: String, val dayDelta: Int)

private fun shifted(value: String, minutes: Int): Shifted {
    val parts = value.split(':')
    val hours = parts.getOrNull(0)?.toIntOrNull()
    val mins = parts.getOrNull(1)?.toIntOrNull()
    if (hours == null || mins == null || hours !in 0..23 || mins !in 0..59) return Shifted(value, 0)
    val moved = hours * 60 + mins + minutes
    val dayDelta = if (moved < 0) (moved - 1_439) / 1_440 else moved / 1_440
    return Shifted(clock(((moved % 1_440) + 1_440) % 1_440), dayDelta)
}

private fun clock(minutes: Int) = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
