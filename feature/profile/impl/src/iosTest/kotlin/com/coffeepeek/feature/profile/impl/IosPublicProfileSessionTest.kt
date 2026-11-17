package com.coffeepeek.feature.profile.impl

import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosPublicProfileSessionTest {
    private val author = PublicProfile("author", "Anna", "Profile", "avatar", PublicProfileStats(1, 2, 3, 4),
        emptyList(), null, authorId = "profile-id")
    private val row = PublicProfileCheckIn("one", "coffee", "Coffee", "", null, null, "Note",
        "2026-11-17T11:00:00Z", listOf("photo"))
    private val open = PublicProfileState(isLoading = false, profile = author, checkIns = listOf(row),
        connectionKind = PublicProfileConnectionKind.Followers,
        connections = listOf(PublicProfileConnection("Follower", "avatar", "follower", null)))

    @Test
    fun hidesPreviouslyLoadedDataUntilVisibilityIsReadyAndAfterAccountReplacement() {
        val loadingBlocks = snapshot(open, ProfileAuthorVisibilityState(ready = false))
        val replacingAccount = snapshot(open, ProfileAuthorVisibilityState(), active = false)
        for (state in listOf(loadingBlocks, replacingAccount)) {
            assertNull(state.profile)
            assertTrue(state.checkIns.isEmpty())
            assertTrue(state.week.isEmpty())
            assertNull(state.connectionKind)
            assertTrue(state.connections.isEmpty())
        }
        assertTrue(loadingBlocks.loading)
        assertFalse(replacingAccount.active)
    }

    @Test
    fun hidesStaleRowsWhenBlockChangesAndKeepsOnlyTheUnblockContext() {
        val newBlocks = ProfileAuthorVisibilityState(revision = 1, blockedIds = setOf("profile-id"))
        assertNull(snapshot(open, newBlocks).profile)
        val resolved = snapshot(open.copy(profile = author.copy(isBlockedByViewer = true)), newBlocks)
        assertEquals("Anna", resolved.profile?.nickname)
        assertEquals(true, resolved.profile?.isBlockedByViewer)
        assertTrue(resolved.checkIns.isEmpty())
        assertTrue(resolved.week.isEmpty())
        assertTrue(resolved.connections.isEmpty())
        assertFalse(resolved.followEnabled)
    }

    @Test
    fun visibleProfileKeepsItsCheckInPhotoAndPublicWeekPreview() {
        val visible = snapshot(open, ProfileAuthorVisibilityState())
        assertEquals("one", visible.checkIns.single().checkIn.id)
        assertTrue(visible.checkIns.single().canOpenPhoto(0))
        assertEquals("photo", visible.week.single { it.today }.photo)
        assertEquals(1, visible.week.single { it.today }.count)
        assertEquals("follower", visible.connections.single().slug)
    }

    private fun snapshot(
        state: PublicProfileState, visibility: ProfileAuthorVisibilityState, active: Boolean = true,
    ) = PublicProfileSnapshot(state, visibility, active, emptyMap(), "2026-11-17")
}
