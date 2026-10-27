package com.coffeepeek.core.datetime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ServerInstantTest {
    @Test fun aValueWithoutAnOffsetIsUtc() {
        assertEquals(parseServerInstant("2026-10-21T08:00:00Z"), parseServerInstant("2026-10-21T08:00:00"))
        assertEquals(parseServerInstant("2026-10-21T08:00:00Z"), parseServerInstant("  2026-10-21T08:00:00  "))
    }

    @Test fun anExplicitOffsetIsHonoured() {
        assertEquals(parseServerInstant("2026-10-21T08:00:00Z"), parseServerInstant("2026-10-21T11:00:00+03:00"))
        assertEquals(parseServerInstant("2026-10-21T08:00:00.5Z")?.toEpochMilliseconds(), parseServerInstant("2026-10-21T08:00:00.500")?.toEpochMilliseconds())
    }

    @Test fun blankOrUnreadableTextIsNull() {
        assertNull(parseServerInstant("")); assertNull(parseServerInstant("   "))
        assertNull(parseServerInstant("yesterday")); assertNull(parseServerInstant("2026-10-21T11:00:00+03:00Z"))
    }
}
