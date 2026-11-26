package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.analytics.AnalyticsState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosCoffeeDetailAnalyticsTest {
    @Test fun previousVisitToSamePageDoesNotCountForTheNewController() {
        val previous = AnalyticsState(page = "coffee_detail", visit = "previous", generation = 2,
            visitSequence = 5, ready = true, foreground = true)

        assertNull(coffeeDetailVisit(previous, "current"))
        assertEquals("2:current:6", coffeeDetailVisit(previous.copy(visit = "current", visitSequence = 6), "current"))
    }

    @Test fun hiddenBackgroundOrUnconsentedVisitDoesNotCount() {
        val current = AnalyticsState(page = "coffee_detail", visit = "controller", ready = true, foreground = true)

        assertNull(coffeeDetailVisit(current, null))
        assertNull(coffeeDetailVisit(current.copy(foreground = false), "controller"))
        assertNull(coffeeDetailVisit(current.copy(ready = false), "controller"))
    }
}
