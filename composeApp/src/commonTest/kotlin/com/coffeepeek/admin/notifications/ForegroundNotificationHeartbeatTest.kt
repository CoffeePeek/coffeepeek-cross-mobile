package com.coffeepeek.admin.notifications

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundNotificationHeartbeatTest {
    @Test fun foregroundRefreshesImmediatelyThenEveryFifteenMinutesUntilCancelled() = runTest {
        var refreshes = 0
        val child = backgroundScope.launch { foregroundNotificationHeartbeat { refreshes++ } }
        runCurrent()
        assertEquals(1, refreshes)
        advanceTimeBy(15 * 60 * 1_000L)
        runCurrent()
        assertEquals(2, refreshes)
        child.cancel()
        advanceTimeBy(30 * 60 * 1_000L)
        runCurrent()
        assertEquals(2, refreshes)
    }
}
