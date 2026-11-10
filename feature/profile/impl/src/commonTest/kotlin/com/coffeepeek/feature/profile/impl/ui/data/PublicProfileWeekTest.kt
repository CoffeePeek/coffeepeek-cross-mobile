package com.coffeepeek.feature.profile.impl.ui.data

import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PublicProfileWeekTest {
    @Test fun timestampsKeepMinskCivilDaysAndMalformedRowsAreIgnored() {
        assertEquals("2026-10-22", publicProfileDate("2026-10-21T22:30:00Z"))
        assertEquals("2026-10-22", publicProfileDate("2026-10-22T01:45:00+03:00"))
        assertNull(publicProfileDate("invalid"))
        val days = publicProfileWeek(publicProfileWeekDates("2026-10-22"), emptyMap(), listOf(row("invalid")))
        assertEquals(List(7) { 0 }, days.map { it.count })
    }

    @Test fun aWeekCrossingMonthsKeepsBothMonthsCounts() {
        val activities = mapOf(
            "2026-10" to PublicProfileActivity("2026-10", setOf(26, 31), checkInCounts = mapOf(26 to 2, 31 to 4)),
            "2026-11" to PublicProfileActivity("2026-11", setOf(1), checkInCounts = mapOf(1 to 3)),
        )
        val days = publicProfileWeek(publicProfileWeekDates("2026-11-01"), activities, emptyList())
        assertEquals("2026-10-26", days.first().date)
        assertEquals("2026-11-01", days.last().date)
        assertEquals(listOf(2, 0, 0, 0, 0, 4, 3), days.map { it.count })
    }

    @Test fun loadedPublicRowsCoverMissingAndOlderActivityCountsWithoutInflatingPhotoCount() {
        val activity = PublicProfileActivity("2026-10", setOf(19, 20), checkInCounts = mapOf(20 to 1))
        val days = publicProfileWeek(publicProfileWeekDates("2026-10-22"), mapOf("2026-10" to activity), listOf(
            row("2026-10-20T12:00:00Z", listOf("thumb", "", "thumb")),
            row("2026-10-20T13:00:00Z"), row("2026-10-21T12:00:00Z"),
        ))
        assertEquals(listOf(1, 2, 1, 0, 0, 0, 0), days.map { it.count })
        assertEquals(listOf("thumb"), days[1].photos)
    }

    @Test fun weekStartsOnMondayAcrossYearsAndLeapDays() {
        assertEquals(listOf("2025-12-29", "2025-12-30", "2025-12-31", "2026-01-01",
            "2026-01-02", "2026-01-03", "2026-01-04"), publicProfileWeekDates("2026-01-01"))
        assertEquals("2024-02-29", publicProfileWeekDates("2024-03-01")[3])
    }

    @Test fun previewUsesThumbnailsAndNeverSubstitutesOriginalPhotoUrls() {
        val checkIn = row("2026-10-20T12:00:00Z", listOf("preview")).copy(photoUrls = listOf("original"))
        val days = publicProfileWeek(publicProfileWeekDates("2026-10-22"), emptyMap(), listOf(checkIn))
        assertEquals(listOf("preview"), days[1].photos)
    }

    private fun row(timestamp: String, photos: List<String> = emptyList()) = PublicProfileCheckIn(
        id = timestamp, coffeeShopSlug = "shop", venue = "Shop", title = "Coffee", details = null,
        rating = null, note = "", occurredAt = timestamp, photoUrls = photos, photoPreviewUrls = photos,
    )
}
