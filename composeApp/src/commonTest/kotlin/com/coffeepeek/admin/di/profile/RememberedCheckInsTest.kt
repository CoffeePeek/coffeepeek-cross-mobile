package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

/** Common adapter contract, with fake repositories only. */
class RememberedCheckInsTest {
    @Test fun touchingAnUnchangedProfileUpdatesItsRecency() {
        val remembered = RememberedCheckIns(maxProfiles = 2)
        remembered.remember("anna", true, listOf(row("a")))
        remembered.remember("bob", true, listOf(row("b")))
        remembered.remember("anna", false, listOf(row("a")))
        remembered.remember("carl", true, listOf(row("c")))
        assertNotNull(remembered.find("a"))
        assertNull(remembered.find("b"))
    }

    @Test fun updatingAProfileMakesItRecentWithoutLosingItsExistingRows() {
        val remembered = RememberedCheckIns(maxProfiles = 2)
        remembered.remember("anna", true, listOf(row("a")))
        remembered.remember("bob", true, listOf(row("b")))
        remembered.remember("anna", false, listOf(row("a2")))
        remembered.remember("carl", true, listOf(row("c")))
        assertNotNull(remembered.find("a"))
        assertNotNull(remembered.find("a2"))
        assertNull(remembered.find("b"))
    }

    @Test fun concurrentPagesKeepAllRows() = runTest {
        val remembered = RememberedCheckIns()
        coroutineScope {
            repeat(100) { index -> launch(Dispatchers.Default) { remembered.remember("anna", false, listOf(row("a$index"))) } }
        }
        repeat(100) { index -> assertNotNull(remembered.find("a$index")) }
    }

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
