package com.coffeepeek.admin.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ReviewDateTest {
    private val createdAt = "2026-10-08T12:00:00Z"
    private val createdMillis = Instant.parse(createdAt).toEpochMilliseconds()

    @Test
    fun recentCheckInsUseRussianMinuteAndHourForms() {
        assertEquals("только что", formatReviewDisplayDate(createdAt, createdMillis + 59_999))
        val minutes = mapOf(1 to "минуту", 2 to "минуты", 5 to "минут", 11 to "минут", 14 to "минут",
            15 to "минут", 21 to "минуту", 22 to "минуты", 25 to "минут", 59 to "минут")
        for ((count, word) in minutes) assertEquals("$count $word назад",
            formatReviewDisplayDate(createdAt, createdMillis + count * 60_000L))
        val hours = mapOf(1 to "час", 2 to "часа", 4 to "часа", 5 to "часов", 11 to "часов", 14 to "часов", 21 to "час", 23 to "часа")
        for ((count, word) in hours) assertEquals("$count $word назад",
            formatReviewDisplayDate(createdAt, createdMillis + count * 3_600_000L))
    }

    @Test
    fun exactlyOneDayAndFutureDatesUseTheCalendarDate() {
        assertEquals("23 часа назад", formatReviewDisplayDate(createdAt, createdMillis + 86_399_999))
        assertEquals("8 окт. 2026", formatReviewDisplayDate(createdAt, createdMillis + 86_400_000))
        assertEquals("8 окт. 2026", formatReviewDisplayDate(createdAt, createdMillis - 1))
    }

    @Test
    fun parsesOffsetsFractionsAndBackendDatesWithoutTheUtcSuffix() {
        val now = createdMillis + 15 * 60_000L
        assertEquals("15 минут назад", formatReviewDisplayDate("2026-10-08T15:00:00+03:00", now))
        assertEquals("15 минут назад", formatReviewDisplayDate("2026-10-08T12:00:00.0000000Z", now))
        assertEquals("15 минут назад", formatReviewDisplayDate("2026-10-08T12:00:00", now))
        assertEquals("15 минут назад", formatReviewDisplayDate(" 2026-10-08T12:00:00Z ", now))
    }

    @Test
    fun malformedDatesKeepTheExistingFallback() {
        assertEquals("", formatReviewDisplayDate("", createdMillis))
        assertEquals("invalid", formatReviewDisplayDate("invalid", createdMillis))
        assertEquals("2026-99-08", formatReviewDisplayDate("2026-99-08", createdMillis))
    }
}
