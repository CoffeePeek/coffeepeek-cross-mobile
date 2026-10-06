package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.ShopSchedule

internal fun nextShopOpeningLabel(schedules: List<ShopSchedule>, currentDay: Int, minuteOfDay: Int): String? {
    for (offset in 0..7) {
        val day = (currentDay + offset) % 7
        val opening = schedules.filter { it.dayOfWeek == day && !it.isClosed }
            .flatMap { it.intervals }
            .mapNotNull { interval ->
                val parts = interval.openTime.split(':')
                val hours = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                if (hours !in 0..23 || minutes !in 0..59) return@mapNotNull null
                (hours * 60 + minutes).takeIf { offset > 0 || it > minuteOfDay }
            }.minOrNull() ?: continue
        val time = "${(opening / 60).toString().padStart(2, '0')}:${(opening % 60).toString().padStart(2, '0')}"
        val whenOpens = when (offset) {
            0 -> "сегодня"
            1 -> "завтра"
            else -> "в ${listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб")[day]}"
        }
        return "Откроется $whenOpens в $time"
    }
    return null
}
