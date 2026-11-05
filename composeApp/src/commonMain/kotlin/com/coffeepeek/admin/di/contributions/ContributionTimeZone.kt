package com.coffeepeek.admin.di.contributions

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

/** Read at submission time so a form opened before a time-zone change does not retain the old offset. */
internal fun contributionUtcOffsetMinutes(): Int =
    contributionUtcOffsetMinutes(Clock.System.now(), TimeZone.currentSystemDefault())

internal fun contributionUtcOffsetMinutes(instant: Instant, zone: TimeZone): Int =
    zone.offsetAt(instant).totalSeconds / 60
