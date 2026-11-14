package com.coffeepeek.feature.settings.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class AppReviewUsageTest {
    private val first = 20_000.days.inWholeSeconds + 3_600
    private val day = 1.days.inWholeSeconds

    @Test
    fun requiresThreeActiveDaysAndThreeElapsedDays() {
        val second = AppReviewUsage.firstUse(first).recordUse(first + day)
        assertFalse(second.canRequest(first + 3 * day))
        val third = second.recordUse(first + 2 * day)
        assertFalse(third.canRequest(first + 3 * day - 1))
        assertTrue(third.canRequest(first + 3 * day))
    }

    @Test
    fun repeatVisitsAndClockRollbackDoNotAddActiveDays() {
        val second = AppReviewUsage.firstUse(first).recordUse(first + day)
        assertEquals(second, second.recordUse(first + day + 60))
        assertEquals(second, second.recordUse(first))
    }

    @Test
    fun waitsAtLeast30DaysAfterAnAttempt() {
        val request = first + 3 * day
        val usage = AppReviewUsage.firstUse(first).recordUse(first + day)
            .recordUse(request).copy(lastRequestedAt = request)
        assertFalse(usage.canRequest(request))
        assertFalse(usage.canRequest(request + 30 * day - 1))
        assertTrue(usage.canRequest(request + 30 * day))
    }
}
