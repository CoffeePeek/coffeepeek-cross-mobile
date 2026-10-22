package com.coffeepeek.admin.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosUpdateVersionCodeTest {
    @Test
    fun ciUploadRetriesKeepTheSamePolicyVersion() {
        assertEquals(48L, iosUpdateVersionCode("48.1"))
        assertEquals(48L, iosUpdateVersionCode("48.2"))
        assertEquals(49L, iosUpdateVersionCode("49.1"))
    }

    @Test
    fun integerBuildsAndLongBoundariesRemainSupported() {
        assertEquals(0L, iosUpdateVersionCode("0"))
        assertEquals(1L, iosUpdateVersionCode("1"))
        assertEquals(48L, iosUpdateVersionCode(" 48.1 "))
        assertEquals(Long.MAX_VALUE, iosUpdateVersionCode("${Long.MAX_VALUE}.1"))
        assertNull(iosUpdateVersionCode("9223372036854775808.1"))
    }

    @Test
    fun missingAndMalformedBuildsRemainUnknown() {
        for (value in listOf("", " ", "-1", "+1", "48.", ".1", "48..1", "48.-1", "48.beta", "1.2.3")) {
            assertNull(iosUpdateVersionCode(value), value)
        }
    }
}
