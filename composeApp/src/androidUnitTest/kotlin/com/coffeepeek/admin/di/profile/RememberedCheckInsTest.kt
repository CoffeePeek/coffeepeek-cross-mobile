package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RememberedCheckInsTest {
    private fun row(id: String) = PublicProfileCheckIn(id, "shop", "Venue", "Title", null, 4.0, "", "2026-10-16T10:00:00Z", emptyList())

    @Test fun aLongScrollKeepsEveryRowOfTheProfile() {
        val remembered = RememberedCheckIns()
        remembered.remember("anna", restart = true, (0 until 500).map { row("a$it") })
        assertNotNull(remembered.find("a0"))
        assertEquals("a499", remembered.find("a499")?.id)
    }

    @Test fun aRefreshStartsTheProfileAfreshAndOnlyTheLastProfilesAreKept() {
        val remembered = RememberedCheckIns(maxProfiles = 2)
        remembered.remember("anna", restart = true, listOf(row("old")))
        remembered.remember("anna", restart = true, listOf(row("new")))
        assertNull(remembered.find("old"))
        assertNotNull(remembered.find("new"))
        remembered.remember("bob", restart = true, listOf(row("b")))
        remembered.remember("carl", restart = true, listOf(row("c")))
        assertNull(remembered.find("new"))
        assertNotNull(remembered.find("b"))
    }
}
