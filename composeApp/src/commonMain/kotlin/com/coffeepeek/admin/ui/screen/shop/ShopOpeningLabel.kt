package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.ShopSchedule

internal data class ShopOpeningLabels(val today: String, val tomorrow: String, val weekday: String, val weekdays: List<String>)

internal fun nextShopOpeningLabel(schedules: List<ShopSchedule>, currentDay: Int, minuteOfDay: Int, labels: ShopOpeningLabels): String? {
    for (offset in 0..7) {
        val day = (currentDay + offset) % 7
        val opening = schedules.filter { it.dayOfWeek == day && !it.isClosed }
            .flatMap { it.intervals }
            .mapNotNull { openingMinute(it.openTime)?.takeIf { minute -> offset > 0 || minute > minuteOfDay } }
            .minOrNull() ?: continue
        val time = "${(opening / 60).toString().padStart(2, '0')}:${(opening % 60).toString().padStart(2, '0')}"
        return when (offset) {
            0 -> labels.today.replace("%1\$s", time)
            1 -> labels.tomorrow.replace("%1\$s", time)
            else -> labels.weekday.replace("%1\$s", labels.weekdays[day]).replace("%2\$s", time)
        }
    }
    return null
}

private fun openingMinute(value: String): Int? {
    val parts = value.split(':')
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}
