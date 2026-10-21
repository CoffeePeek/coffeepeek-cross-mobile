@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.parity.ParityHarness
import com.coffeepeek.admin.analytics.parity.ParityHarness.record
import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import com.coffeepeek.feature.profile.domain.ProfileAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidProfilePortsTest {
    private val anna = UserProfile("Anna", "anna@example.com", "About", "https://img/a.jpg", 0, 3, 2,
        PublicAddress("anna", "/users/anna", 2, true), 1, 4)

    private class Sessions(initial: Session?, private val active: (Session?) -> Boolean = { it != null }) : SessionRepository {
        val state = MutableStateFlow(initial)
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = active(session)
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) { state.value = session }
        override suspend fun saveSession(session: Session?) { state.value = session }
        override suspend fun warmCache() = Unit
        override fun observeSession(): Flow<Session?> = state
        override suspend fun isLoggedIn() = state.value != null
    }

    private class Users(cached: UserProfile?, private val profile: UserProfile) : UserRepository {
        val cache = MutableStateFlow(cached)
        val calls = mutableListOf<String>()
        override fun observeProfile(): StateFlow<UserProfile?> = cache
        override suspend fun refreshProfile(): Result<UserProfile> { calls += "refresh"; return Result.success(profile) }
        override suspend fun getMe(): Result<UserProfile> { calls += "getMe"; return Result.success(profile) }
        override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = Result.success(null)
        override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("unused")
        override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("unused")
        override suspend fun updateUsername(username: String) = Result.success(Unit)
        override suspend fun updateAbout(about: String) = Result.success(Unit)
        override suspend fun updateAvatar(photo: PendingPhotoUpload) = Result.success(Unit)
    }

    @Test fun accountKeyIsTheUserIdThenTheTokenAndNullForGuestsAndInactiveSessions() {
        fun key(session: Session?, active: Boolean = true) =
            AndroidProfileAccountPort(Sessions(session) { it != null && active }, Users(null, anna)).activeAccount()
        assertEquals("u1", key(Session("t", null, "u1")))
        assertEquals("t", key(Session("t", null, null)))
        assertNull(key(null))
        assertNull(key(Session("t", null, "u1"), active = false))
    }

    @Test fun observedAccountReplaysTheCurrentKeyAndEmitsOnlyChanges() = runTest {
        val sessions = Sessions(Session("t1", null, "a"))
        val port = AndroidProfileAccountPort(sessions, Users(null, anna))
        val seen = mutableListOf<String?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { port.observeActiveAccount().toList(seen) }
        sessions.state.value = Session("t2", null, "a") // token rotation, same account
        sessions.state.value = Session("t3", null, "b")
        sessions.state.value = null
        job.cancel()
        assertEquals(listOf("a", "b", null), seen)
    }

    @Test fun ownProfileIsMappedWithItsAddressAndFollowsTheSharedCache() = runTest {
        val users = Users(null, anna)
        val port = AndroidProfileAccountPort(Sessions(Session("t", null, "a")), users)
        assertNull(port.cachedOwnProfile())
        assertNull(port.observeOwnProfile().first())
        users.cache.update { anna }
        val own = port.cachedOwnProfile()!!
        assertEquals("Anna", own.userName)
        assertEquals(listOf(3, 2, 1, 4), listOf(own.checkInCount, own.addedShopsCount, own.addedRoastersCount, own.followersCount))
        assertEquals(ProfileAddress("anna", "/users/anna", 2, true), own.address)
        assertEquals(own, port.observeOwnProfile().first())
    }

    @Test fun loadReturnsTheCacheUnlessForcedAndFallsBackToGetMeThenRefresh() = runTest {
        val cached = Users(anna, anna)
        val withCache = AndroidProfileAccountPort(Sessions(Session("t", null, "a")), cached)
        assertEquals("Anna", withCache.loadOwnProfile(forceRefresh = false).getOrThrow().userName)
        assertTrue(cached.calls.isEmpty())
        withCache.loadOwnProfile(forceRefresh = true)
        assertEquals(listOf("refresh"), cached.calls)

        val empty = Users(null, anna)
        val port = AndroidProfileAccountPort(Sessions(Session("t", null, "a")), empty)
        port.loadOwnProfile(forceRefresh = false)
        assertEquals(listOf("getMe"), empty.calls)
    }

    @Test fun visibilityHidesEverythingUntilReadyThenByIdOrSlugAndRelaysChanges() = runTest {
        assertTrue(ProfileAuthorVisibilityState(ready = false).hides("x", "y"))
        val open = ProfileAuthorVisibilityState(blockedIds = setOf("id"), blockedSlugs = setOf("slug"))
        assertTrue(open.hides("id", null)); assertTrue(open.hides(null, "slug"))
        assertFalse(open.hides("other", "other")); assertFalse(open.hides(null, null))

        val source = object : AuthorVisibility {
            override val visibility = MutableStateFlow(AuthorVisibilitySnapshot(revision = 1, ready = true))
            var refreshes = 0
            override suspend fun refresh(): Result<Unit> { refreshes++; return Result.success(Unit) }
        }
        val port = AndroidProfileAuthorVisibility(source)
        assertEquals(ProfileAuthorVisibilityState(revision = 1), port.current())
        source.visibility.value = AuthorVisibilitySnapshot(2, true, setOf("id"), setOf("slug"))
        assertEquals(ProfileAuthorVisibilityState(2, true, setOf("id"), setOf("slug")), port.observe().first())
        assertTrue(port.refresh().isSuccess)
        assertEquals(1, source.refreshes)
        assertFalse(ProfileAuthorVisibility.None.current().hides("a", "b"))
    }

    @Test fun favoritesOpenedEmitsExactlyTheLegacyEvent() = runTest {
        val legacy = record { Analytics.track(AnalyticsEvent.FAVORITES_OPENED, "entry_point" to "profile") }
        val migrated = record { AndroidProfileAnalytics.favoritesOpened() }
        ParityHarness.assertParity("profile: open all favorites", legacy, migrated)
        assertEquals(1, migrated.size)
    }
}
