@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.coffeepeek.feature.profile.impl.ui.data

import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

internal data class PublicProfileWeekDay(val date: String, val count: Int, val photos: List<String>)

/** The existing public activity contract uses Minsk civil days, including feed timestamps. */
internal fun publicProfileDate(timestamp: String): String? =
    runCatching { (Instant.parse(timestamp) + 3.hours).toString().take(10) }.getOrNull()

internal fun publicProfileWeekDates(today: String): List<String> {
    val date = LocalDate.parse(today)
    val monday = date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)
    return List(7) { monday.plus(it, DateTimeUnit.DAY).toString() }
}

/** Public feed rows only; no personal history reader is involved. */
internal fun publicProfileWeek(
    dates: List<String>, activities: Map<String, PublicProfileActivity>, checkIns: List<PublicProfileCheckIn>,
): List<PublicProfileWeekDay> {
    val grouped = checkIns.mapNotNull { row -> publicProfileDate(row.occurredAt)?.let { it to row } }
        .groupBy({ it.first }, { it.second })
    return dates.map { date ->
        val activity = activities[date.take(7)]
        val day = date.takeLast(2).toInt()
        val count = activity?.checkInCounts?.get(day) ?: if (day in activity?.activeDays.orEmpty()) 1 else 0
        val rows = grouped[date].orEmpty()
        PublicProfileWeekDay(date, maxOf(count, rows.size),
            rows.flatMap { it.photoPreviewUrls }.filter(String::isNotBlank).distinct())
    }
}
