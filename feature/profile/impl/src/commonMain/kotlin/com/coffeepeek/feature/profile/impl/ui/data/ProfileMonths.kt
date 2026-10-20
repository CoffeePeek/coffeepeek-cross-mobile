package com.coffeepeek.feature.profile.impl.ui.data

internal data class ProfileMonth(val year: Int, val number: Int, val days: Int, val firstWeekday: Int)

/** Parses `yyyy-MM`; malformed input degrades to January 2000 so a bad value can never crash the calendar. */
internal fun profileMonth(value: String): ProfileMonth {
    val parts = value.split('-')
    val year = parts.firstOrNull()?.toIntOrNull()?.coerceIn(1, 9999) ?: 2000
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = when (month) { 2 -> if (leap) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
    // Gregorian weekday, shifted from Sunday-first to the Monday-first UI.
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val adjustedYear = if (month < 3) year - 1 else year
    val sundayFirst = (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + 1) % 7
    return ProfileMonth(year, month, days, (sundayFirst + 6) % 7)
}

/** The neighbouring month, or null outside the 2000..2100 range the server accepts. */
internal fun shiftProfileMonth(value: String, offset: Int): String? {
    val month = profileMonth(value)
    val shifted = month.year * 12 + month.number - 1 + offset
    val year = shifted / 12
    val number = shifted % 12 + 1
    if (year !in 2000..2100) return null
    return "$year-${number.toString().padStart(2, '0')}"
}
