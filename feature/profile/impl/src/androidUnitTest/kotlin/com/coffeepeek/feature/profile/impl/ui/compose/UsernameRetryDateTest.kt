package com.coffeepeek.feature.profile.impl.ui.compose

import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UsernameRetryDateTest {
    private val locale = Locale.forLanguageTag("ru")
    private val zone = ZoneId.of("Europe/Minsk")

    @Test fun theServerInstantUsesTheUsersTimezoneAndReadableLocalDate() {
        val formatted = formatUsernameRetryAt("2026-10-29T17:12:31.5305240Z", locale, zone).orEmpty()
        assertTrue(formatted.contains("29"))
        assertTrue(formatted.contains("окт"))
        assertTrue(formatted.contains("2026"))
        assertTrue(formatted.contains("20:12"))
    }

    @Test fun anExplicitOffsetAndUtcRepresentTheSameLocalDate() {
        assertEquals(
            formatUsernameRetryAt("2026-10-29T17:12:31Z", locale, zone),
            formatUsernameRetryAt("2026-10-29T20:12:31+03:00", locale, zone),
        )
    }

    @Test fun missingAndMalformedDatesUseTheGenericLimitMessage() {
        for (value in listOf(null, "", "not a date", "2026-99-99T17:12:31Z")) {
            assertNull(formatUsernameRetryAt(value, locale, zone))
        }
    }
}
