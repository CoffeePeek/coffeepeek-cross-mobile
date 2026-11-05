package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.impl.ui.localDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class ContributionDatesTest {
    private val minsk = TimeZone.of("Europe/Minsk")

    @Test fun aUtcTimestampIsShownInTheLocalZone() {
        assertEquals("21.10.2026 11:00", localDateTime("2026-10-21T08:00:00Z", minsk))
    }

    @Test fun anOffsetLessTimestampIsUtcAndAnExplicitOffsetIsHonoured() {
        assertEquals("23.10.2026 11:00", localDateTime("2026-10-23T08:00:00", minsk))
        assertEquals("23.10.2026 08:00", localDateTime("2026-10-23T08:00:00+03:00", minsk))
    }

    @Test fun anUnreadableValueIsShownAsItCame() {
        assertEquals("yesterday", localDateTime("yesterday", minsk))
    }
}
