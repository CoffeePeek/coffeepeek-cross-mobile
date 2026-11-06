package com.coffeepeek.feature.ownership.impl

import com.coffeepeek.feature.ownership.impl.ui.compose.formatOwnershipClaimDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OwnershipClaimDateTest {
    private val minsk = TimeZone.of("Europe/Minsk")

    @Test
    fun convertsServerInstantsToLocalTimeWithoutSeconds() {
        assertEquals("06.11.2026, 12:42", formatOwnershipClaimDate("2026-11-06T09:42:55.0752117Z", minsk))
        assertEquals("07.11.2026, 02:05", formatOwnershipClaimDate("2026-11-06T23:05:00", minsk))
        assertEquals("06.11.2026, 12:42", formatOwnershipClaimDate("2026-11-06T11:42:55+02:00", minsk))
    }

    @Test
    fun keepsDateOnlyValuesAndOmitsUnreadableValues() {
        assertEquals("06.11.2026", formatOwnershipClaimDate("2026-11-06", minsk))
        assertNull(formatOwnershipClaimDate("invalid", minsk))
        assertNull(formatOwnershipClaimDate("", minsk))
    }
}
