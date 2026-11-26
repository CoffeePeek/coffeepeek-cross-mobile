@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.coffeepeek.feature.profile.impl.ui.data

import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

internal data class PublicProfileWeekDay(val date: String, val count: Int, val photos: List<String>, val known: Boolean = true)

internal val PublicProfileTimeZone = TimeZone.of("Europe/Minsk")

internal fun currentPublicProfileDate(): String = Clock.System.now().toLocalDateTime(PublicProfileTimeZone).date.toString()

/** Public pages use Minsk civil days. The owner's calendar uses the device's zone. */
internal fun publicProfileDate(timestamp: String, zone: TimeZone = PublicProfileTimeZone): String? =
    runCatching {
        if (timestamp.length == 10) LocalDate.parse(timestamp).toString()
        else Instant.parse(timestamp).toLocalDateTime(zone).date.toString()
    }.getOrNull()

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
            rows.flatMap { it.photoPreviewUrls }.filter(String::isNotBlank).distinct(),
            known = activity != null || rows.isNotEmpty())
    }
}
