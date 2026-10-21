package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopScheduleStatus

/** Local clock comes from composition; API day zero is Sunday. No platform clock in shared UI. */
internal fun shopScheduleStatus(
    schedules: List<ShopSchedule>, isOpen: Boolean?, dayOfWeek: Int, minuteOfDay: Int?,
): ShopScheduleStatus {
    val today = schedules.firstOrNull { it.dayOfWeek == dayOfWeek }
    if (isOpen == true) {
        val closing = today?.takeUnless { it.isClosed }?.intervals?.lastOrNull()?.closeTime
            ?.let(::formatShopScheduleTime)?.takeIf(String::isNotBlank)
        return ShopScheduleStatus.Open(closing)
    }
    if (dayOfWeek in 0..6 && minuteOfDay != null && minuteOfDay in 0..1439) {
        for (offset in 0..7) {
            val day = (dayOfWeek + offset) % 7
            val opening = schedules.filter { it.dayOfWeek == day && !it.isClosed }
                .flatMap { it.intervals }.mapNotNull { parseShopScheduleMinute(it.openTime) }
                .filter { offset > 0 || it > minuteOfDay }.minOrNull() ?: continue
            val time = "${(opening / 60).toString().padStart(2, '0')}:${(opening % 60).toString().padStart(2, '0')}"
            return ShopScheduleStatus.Opens(offset, day, time)
        }
    }
    return if (today?.isClosed == true) ShopScheduleStatus.TodayClosed else ShopScheduleStatus.Closed
}

internal fun formatShopScheduleTime(raw: String): String = raw.split(':').take(2).joinToString(":")

private fun parseShopScheduleMinute(raw: String): Int? {
    val parts = raw.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return (hour * 60 + minute).takeIf { hour in 0..23 && minute in 0..59 }
}
