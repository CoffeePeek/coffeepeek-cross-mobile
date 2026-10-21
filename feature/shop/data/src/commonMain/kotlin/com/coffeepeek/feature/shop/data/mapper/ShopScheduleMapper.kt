package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopScheduleDto
import com.coffeepeek.feature.shop.domain.model.ScheduleInterval
import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** The API supplies UTC weekly times, but no IANA zone for DST-stable conversion. */
internal fun List<ShopScheduleDto>.toLocalSchedules(utcOffsetMinutes: Int): List<ShopSchedule> {
    if (isEmpty()) return emptyList()
    val intervalsByDay = mutableMapOf<Int, MutableList<ScheduleInterval>>()
    filterNot(ShopScheduleDto::isClosed).forEach { schedule ->
        schedule.intervals.orEmpty().forEach { interval ->
            val open = shiftTime(interval.openTime, utcOffsetMinutes)
            val close = shiftTime(interval.closeTime, utcOffsetMinutes)
            val day = normalizeDay(schedule.dayOfWeek.toDayOfWeek() + open.dayDelta)
            intervalsByDay.getOrPut(day, ::mutableListOf) +=
                ScheduleInterval(open.value, close.value)
        }
    }
    return (0..6).map { day ->
        val intervals = intervalsByDay[day].orEmpty()
        ShopSchedule(day, intervals.isEmpty(), intervals)
    }
}

private fun kotlinx.serialization.json.JsonElement?.toDayOfWeek(): Int {
    val token = (this as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    return when (token.lowercase()) {
        "0", "sunday" -> 0
        "1", "monday" -> 1
        "2", "tuesday" -> 2
        "3", "wednesday" -> 3
        "4", "thursday" -> 4
        "5", "friday" -> 5
        "6", "saturday" -> 6
        else -> token.toIntOrNull()?.coerceIn(0, 6) ?: 0
    }
}

private data class ShiftedTime(val value: String, val dayDelta: Int)

private fun shiftTime(value: String, minutesDelta: Int): ShiftedTime {
    val parts = value.split(':')
    val hours = parts.getOrNull(0)?.toIntOrNull()
    val minutes = parts.getOrNull(1)?.toIntOrNull()
    if (hours !in 0..23 || minutes !in 0..59) return ShiftedTime(value, 0)

    val shifted = hours!! * 60 + minutes!! + minutesDelta
    val dayDelta = if (shifted < 0) (shifted - 1_439) / 1_440 else shifted / 1_440
    val normalized = ((shifted % 1_440) + 1_440) % 1_440
    return ShiftedTime(
        "${(normalized / 60).toString().padStart(2, '0')}:${(normalized % 60).toString().padStart(2, '0')}",
        dayDelta,
    )
}

private fun normalizeDay(day: Int): Int = ((day % 7) + 7) % 7
