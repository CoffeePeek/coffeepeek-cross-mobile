package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryDrink
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth

/** Monday-first presentation grid, with no platform clock or query-bound calculations. */
internal fun historyCalendarCells(month: HistoryMonth): List<Int?> {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val year = if (month.month < 3) month.year - 1 else month.year
    val sundayFirst = (year + year / 4 - year / 100 + year / 400 + offsets[month.month - 1] + 1) % 7
    val leading = (sundayFirst + 6) % 7
    val cells = List(leading) { null } + (1..month.daysInMonth).toList()
    return cells + List((7 - cells.size % 7) % 7) { null }
}

internal data class HistoryDayStreak(val previous: Boolean, val next: Boolean) {
    val isConnected: Boolean get() = previous || next
}

internal fun historyDayStreak(month: HistoryMonth, day: Int, dates: Set<String>): HistoryDayStreak {
    if (month.date(day) !in dates) return HistoryDayStreak(false, false)
    return HistoryDayStreak(day > 1 && month.date(day - 1) in dates,
        day < month.daysInMonth && month.date(day + 1) in dates)
}

internal fun HistoryDrink?.displayName(language: String): String? = this?.let {
    it.customName?.takeIf(String::isNotBlank) ?: if (language == "en") {
        it.nameEn?.takeIf(String::isNotBlank) ?: it.nameRu?.takeIf(String::isNotBlank)
    } else it.nameRu?.takeIf(String::isNotBlank) ?: it.nameEn?.takeIf(String::isNotBlank)
}

/** The actual UI catalog is Russian-only; do not apply an English device's plural rule to it. */
internal fun historyCountForm(count: Int): Int = when {
    count % 100 in 11..14 -> 2
    count % 10 == 1 -> 0
    count % 10 in 2..4 -> 1
    else -> 2
}
