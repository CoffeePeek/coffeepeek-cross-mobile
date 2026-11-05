package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.api.MapOpeningHours
import com.coffeepeek.feature.discovery.impl.ui.compose.component.intervalLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapHoursFormattingTest {
    @Test fun intervalRetainsLegacySpacingAndDropsSeconds() {
        assertEquals("09:00 – 23:30", MapOpeningHours(false, "09:00:00", "23:30:59").intervalLabel())
        assertEquals("9:00 – 21:00", MapOpeningHours(false, "9:00", "21:00").intervalLabel())
    }

    @Test fun closedOrIncompleteIntervalsDoNotInventHours() {
        assertNull(MapOpeningHours(true, "09:00", "23:30").intervalLabel())
        assertNull(MapOpeningHours(false, null, "23:30").intervalLabel())
        assertNull(MapOpeningHours(false, "09:00", null).intervalLabel())
    }
}
