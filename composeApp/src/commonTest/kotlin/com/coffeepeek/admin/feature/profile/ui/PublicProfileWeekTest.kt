package com.coffeepeek.admin.feature.profile.ui

import com.coffeepeek.admin.ui.screen.checkins.CalendarWeek
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicProfileWeekTest {
    @Test
    fun publicPhotosUseTheActivityTimeZoneAndMalformedDatesAreIgnored() {
        val grouped = publicProfileWeekCheckIns(listOf(
            "2026-10-21T22:30:00Z" to listOf("public-photo"),
            "2026-10-22T01:45:00+03:00" to emptyList(),
            "invalid" to listOf("discarded"),
        ))
        assertEquals(mapOf("2026-10-22" to listOf(listOf("public-photo"), emptyList())), grouped)
    }

    @Test
    fun aWeekCrossingMonthsUsesTheCountsOfEachMonth() {
        val week = CalendarWeek("2026-11-01")
        val october = PublicProfileActivity("2026-10", setOf(26, 31), checkInCounts = mapOf(26 to 2, 31 to 4))
        val november = PublicProfileActivity("2026-11", setOf(1), checkInCounts = mapOf(1 to 3))
        val counts = publicProfileWeekCounts(week, mapOf("2026-10" to october, "2026-11" to november), emptyMap())
        assertEquals(listOf(2, 0, 0, 0, 0, 4, 3), week.dates.map { counts[it] })
    }

    @Test
    fun loadedPublicCheckInsCoverMissingActivityAndAnOlderCountSnapshot() {
        val week = CalendarWeek("2026-10-22")
        val activity = PublicProfileActivity("2026-10", setOf(19, 20), checkInCounts = mapOf(20 to 1))
        val grouped = mapOf("2026-10-20" to listOf(emptyList<String>(), listOf("photo")),
            "2026-10-21" to listOf(emptyList<String>()))
        val counts = publicProfileWeekCounts(week, mapOf("2026-10" to activity), grouped)
        assertEquals(listOf(1, 2, 1, 0, 0, 0, 0), week.dates.map { counts[it] })
    }
}
