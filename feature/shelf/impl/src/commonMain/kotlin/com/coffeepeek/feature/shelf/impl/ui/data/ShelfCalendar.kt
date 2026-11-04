package com.coffeepeek.feature.shelf.impl.ui.data

import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import com.coffeepeek.feature.shelf.domain.model.ShelfBag
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A calendar day in the user's time zone; [month] is 1–12. */
internal data class ShelfDay(val year: Int, val month: Int, val day: Int) {
    /** Days since 1970-01-01 (H. Hinnant's days_from_civil). */
    val epochDay: Long
        get() {
            val y = (if (month <= 2) year - 1 else year).toLong()
            val era = y.floorDiv(400L)
            val yearOfEra = y - era * 400
            val dayOfYear = (153 * (month + if (month > 2) -3 else 9) + 2) / 5 + day - 1
            val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
            return era * 146097 + dayOfEra - 719468
        }

    /** As an ISO calendar date, "2026-10-27". */
    val iso: String get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    companion object {
        /** The inverse of [epochDay] (civil_from_days). */
        fun ofEpochDay(epochDay: Long): ShelfDay {
            val shifted = epochDay + 719468
            val era = shifted.floorDiv(146097L)
            val dayOfEra = shifted - era * 146097
            val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val shiftedMonth = (5 * dayOfYear + 2) / 153
            val day = (dayOfYear - (153 * shiftedMonth + 2) / 5 + 1).toInt()
            val month = (if (shiftedMonth < 10) shiftedMonth + 3 else shiftedMonth - 9).toInt()
            return ShelfDay((yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt(), month, day)
        }

        /** An ISO calendar date such as the roast date. */
        fun parse(text: String?): ShelfDay? {
            val parts = text?.trim()?.split('-')?.takeIf { it.size == 3 } ?: return null
            val (year, month, day) = parts.map { it.toIntOrNull() ?: return null }
            return ShelfDay(year, month, day).takeIf { month in 1..12 && day in 1..31 && ofEpochDay(it.epochDay) == it }
        }
    }
}

/** Turns the server's UTC instants into the user's calendar days. */
@OptIn(ExperimentalTime::class)
internal class ShelfCalendar(private val zone: ShelfTimeZone, private val now: () -> Instant) {
    fun today(): ShelfDay = day(now())

    fun day(instant: Instant): ShelfDay {
        val seconds = instant.epochSeconds
        return ShelfDay.ofEpochDay((seconds + zone.offsetSeconds(seconds)).floorDiv(86_400L))
    }

    fun item(bag: ShelfBag) = ShelfBagItem(
        bag = bag,
        addedOn = parseServerInstant(bag.addedAtUtc)?.let(::day),
        finishedOn = bag.finishedAtUtc?.let(::parseServerInstant)?.let(::day),
        roastedOn = ShelfDay.parse(bag.roastedOn),
    )
}
