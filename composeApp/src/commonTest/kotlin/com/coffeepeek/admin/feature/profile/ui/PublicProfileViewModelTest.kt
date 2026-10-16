package com.coffeepeek.admin.feature.profile.ui

import com.coffeepeek.admin.feature.profile.domain.*
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PublicProfileViewModelTest {
    @Test
    fun canonicalAuthorPaginationDeduplicatesAndFailedCursorRetryStartsAtFirstPage() = runTest {
        val requests = mutableListOf<Pair<String, String?>>()
        var failNextPage = true
        val repository = ProfileRepositoryFake().apply {
            checkIns = { slug, cursor ->
                requests += slug to cursor
                when {
                    cursor == null -> Result.success(PublicProfileCheckInPage(listOf(checkIn("one")), "next"))
                    failNextPage -> { failNextPage = false; Result.failure(IllegalStateException("Cursor expired")) }
                    else -> Result.success(PublicProfileCheckInPage(listOf(checkIn("one"), checkIn("two")), null))
                }
            }
        }
        val vm = PublicProfileViewModel("alias", repository, ProfileSessions(), taskScope(testScheduler))
        try {
            runCurrent()
            assertEquals<List<Pair<String, String?>>>(listOf("canonical" to null), requests)
            vm.loadMoreCheckIns(); runCurrent()
            assertNotNull(vm.state.value.checkInsError)
            vm.loadMoreCheckIns(); runCurrent()
            assertEquals(2, requests.size)
            vm.retryCheckIns(); runCurrent()
            assertEquals<Pair<String, String?>>("canonical" to null, requests.last())
            vm.loadMoreCheckIns(); runCurrent()
            assertEquals(listOf("one", "two"), vm.state.value.checkIns.map { it.id })
            assertFalse(vm.state.value.hasMoreCheckIns)
        } finally { vm.close() }
    }

    @Test
    fun favoriteAndActivityFailuresDoNotHideProfileAndCanBeRetriedIndependently() = runTest {
        val repository = ProfileRepositoryFake().apply {
            favorites = { Result.failure(IllegalStateException("Favorites unavailable")) }
            activity = { _, _ -> Result.failure(IllegalStateException("Activity unavailable")) }
        }
        val vm = PublicProfileViewModel("alias", repository, ProfileSessions(), taskScope(testScheduler))
        try {
            runCurrent()
            assertNotNull(vm.state.value.profile)
            assertNull(vm.state.value.error)
            assertNotNull(vm.state.value.favoritesError)
            assertNotNull(vm.state.value.activityError)
            assertTrue(vm.state.value.profile!!.favorites.isEmpty())
            assertNull(vm.state.value.profile!!.activity)
            repository.favorites = { Result.success(listOf(PublicProfileFavorite("drink:matcha", "Матча", PublicFavoriteKind.Drink, drinkSlug = "matcha"))) }
            vm.retryFavorites(); runCurrent()
            assertEquals("Матча", vm.state.value.profile!!.favorites.single().title)
            assertNull(vm.state.value.favoritesError)
            assertNotNull(vm.state.value.activityError)
            repository.activity = { year, month -> Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", setOf(3), totalCheckIns = 7, checkInCounts = mapOf(3 to 7))) }
            vm.retryActivity(); runCurrent()
            assertEquals(7, vm.state.value.profile!!.activity!!.totalCheckIns)
            assertNull(vm.state.value.activityError)
        } finally { vm.close() }
    }

    @Test
    fun guestFollowOpensLoginAndAuthenticatedFollowUsesExactServerCount() = runTest {
        val sessions = ProfileSessions()
        var mutations = 0
        val repository = ProfileRepositoryFake().apply {
            follow = { slug, desired ->
                assertEquals("canonical", slug)
                mutations++
                Result.success(PublicProfileFollowState(desired, if (desired) 91 else 2))
            }
        }
        val vm = PublicProfileViewModel("alias", repository, sessions, taskScope(testScheduler))
        try {
            runCurrent()
            vm.toggleFollow(); runCurrent()
            assertTrue(vm.state.value.needsLogin)
            assertEquals(0, mutations)
            vm.loginHandled()
            sessions.signIn("alice"); runCurrent()
            vm.toggleFollow(); vm.toggleFollow(); runCurrent()
            assertEquals(1, mutations)
            assertTrue(vm.state.value.isFollowing)
            assertEquals(91, vm.state.value.profile!!.stats.followers)
            vm.toggleFollow(); runCurrent()
            assertFalse(vm.state.value.isFollowing)
            assertEquals(2, vm.state.value.profile!!.stats.followers)
        } finally { vm.close() }
    }

    @Test
    fun failedFollowPreservesPreviousStateAndUnauthorizedResponseRequestsLogin() = runTest {
        val repository = ProfileRepositoryFake().apply { follow = { _, _ -> Result.failure(PublicProfileAuthenticationRequired()) } }
        val vm = PublicProfileViewModel("canonical", repository, ProfileSessions("alice"), taskScope(testScheduler))
        try {
            runCurrent()
            val oldCount = vm.state.value.profile!!.stats.followers
            vm.toggleFollow(); runCurrent()
            assertFalse(vm.state.value.isFollowing)
            assertEquals(oldCount, vm.state.value.profile!!.stats.followers)
            assertFalse(vm.state.value.isFollowLoading)
            assertNotNull(vm.state.value.followError)
            assertTrue(vm.state.value.needsLogin)
        } finally { vm.close() }
    }

    @Test
    fun oldAccountsLateFollowCannotChangeNewViewersProfile() = runTest {
        val deferred = CompletableDeferred<PublicProfileFollowState>()
        val sessions = ProfileSessions("alice")
        val repository = ProfileRepositoryFake().apply {
            follow = { _, _ -> withContext(NonCancellable) { Result.success(deferred.await()) } }
        }
        val vm = PublicProfileViewModel("canonical", repository, sessions, taskScope(testScheduler))
        try {
            runCurrent()
            vm.toggleFollow(); runCurrent()
            assertTrue(vm.state.value.isFollowLoading)
            sessions.signIn("bob"); runCurrent()
            deferred.complete(PublicProfileFollowState(true, 99)); runCurrent()
            assertFalse(vm.state.value.isFollowing)
            assertEquals(17, vm.state.value.profile!!.stats.followers)
            assertFalse(vm.state.value.isFollowLoading)
        } finally { deferred.complete(PublicProfileFollowState(true, 99)); vm.close() }
    }

    @Test
    fun changingActivityMonthRejectsLateOldMonthResponse() = runTest {
        val deferred = CompletableDeferred<PublicProfileActivity>()
        var first = true
        val repository = ProfileRepositoryFake().apply {
            activity = { year, month ->
                if (first) { first = false; withContext(NonCancellable) { Result.success(deferred.await()) } }
                else Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", setOf(2)))
            }
        }
        val vm = PublicProfileViewModel("canonical", repository, ProfileSessions(), taskScope(testScheduler))
        try {
            runCurrent()
            vm.changeActivityMonth("2025-02"); runCurrent()
            assertEquals("2025-02", vm.state.value.profile!!.activity!!.month)
            deferred.complete(PublicProfileActivity("2026-10", setOf(10))); runCurrent()
            assertEquals("2025-02", vm.state.value.profile!!.activity!!.month)
            assertEquals(setOf(2), vm.state.value.profile!!.activity!!.activeDays)
        } finally { deferred.complete(PublicProfileActivity("2026-10", emptySet())); vm.close() }
    }

    @Test
    fun connectionsRespectServerPaginationDeduplicateAndSwitchKinds() = runTest {
        val requests = mutableListOf<Pair<PublicProfileConnectionKind, Int>>()
        val repository = ProfileRepositoryFake().apply {
            connections = { kind, page ->
                requests += kind to page
                Result.success(PublicProfileConnectionsPage(
                    if (page == 1) listOf(person("one")) else listOf(person("one"), person("two")),
                    21, page, 20,
                ))
            }
        }
        val vm = PublicProfileViewModel("canonical", repository, ProfileSessions(), taskScope(testScheduler))
        try {
            runCurrent()
            vm.openConnections(PublicProfileConnectionKind.Followers); runCurrent()
            assertTrue(vm.state.value.hasMoreConnections)
            vm.loadMoreConnections(); runCurrent()
            assertEquals(listOf("one", "two"), vm.state.value.connections.map { it.slug })
            assertFalse(vm.state.value.hasMoreConnections)
            vm.openConnections(PublicProfileConnectionKind.Following); runCurrent()
            assertEquals(PublicProfileConnectionKind.Following to 1, requests.last())
            assertEquals(listOf("one"), vm.state.value.connections.map { it.slug })
            vm.closeConnections()
            assertNull(vm.state.value.connectionKind)
        } finally { vm.close() }
    }

    @Test
    fun unavailableProfileHasAnErrorWithoutInventingFallbackSections() = runTest {
        val repository = ProfileRepositoryFake().apply { profile = { Result.failure(IllegalStateException("Missing")) } }
        val vm = PublicProfileViewModel("missing", repository, ProfileSessions(), taskScope(testScheduler))
        try {
            runCurrent()
            assertFalse(vm.state.value.isLoading)
            assertNull(vm.state.value.profile)
            assertNotNull(vm.state.value.error)
            assertTrue(vm.state.value.checkIns.isEmpty())
        } finally { vm.close() }
    }

    @Test
    fun unavailableRefreshClearsPreviouslyVisibleProfileAndSections() = runTest {
        val repository = ProfileRepositoryFake().apply {
            favorites = { Result.success(listOf(PublicProfileFavorite("drink:matcha", "Матча", PublicFavoriteKind.Drink, drinkSlug = "matcha"))) }
            checkIns = { _, _ -> Result.success(PublicProfileCheckInPage(listOf(checkIn("visible")), null)) }
        }
        val vm = PublicProfileViewModel("canonical", repository, ProfileSessions(), taskScope(testScheduler))
        try {
            runCurrent()
            assertNotNull(vm.state.value.profile)
            assertTrue(vm.state.value.checkIns.isNotEmpty())
            assertTrue(vm.state.value.profile!!.favorites.isNotEmpty())
            repository.profile = { Result.failure(PublicProfileUnavailable()) }
            vm.refresh(); runCurrent()
            assertNull(vm.state.value.profile)
            assertTrue(vm.state.value.checkIns.isEmpty())
            assertTrue(vm.state.value.connections.isEmpty())
            assertEquals("Профиль недоступен", vm.state.value.error)
        } finally { vm.close() }
    }

    private fun taskScope(scheduler: TestCoroutineScheduler) = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scheduler))
    private fun checkIn(id: String) = PublicProfileCheckIn(id, null, "Дома", "Кофе", null, null, "", "2026-10-15", emptyList())
    private fun person(slug: String) = PublicProfileConnection(slug, null, slug, "/users/$slug")
}

private class ProfileRepositoryFake : PublicProfileRepository {
    var profile: suspend (String) -> Result<PublicProfile> = { Result.success(PublicProfile("canonical", "nickname", null, null, PublicProfileStats(2, 1, 0, 17), emptyList(), null)) }
    var favorites: suspend (String) -> Result<List<PublicProfileFavorite>> = { Result.success(emptyList()) }
    var activity: suspend (Int, Int) -> Result<PublicProfileActivity> = { year, month -> Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", emptySet())) }
    var checkIns: suspend (String, String?) -> Result<PublicProfileCheckInPage> = { _, _ -> Result.success(PublicProfileCheckInPage(emptyList(), null)) }
    var follow: suspend (String, Boolean) -> Result<PublicProfileFollowState> = { _, _ -> error("Unexpected follow") }
    var connections: suspend (PublicProfileConnectionKind, Int) -> Result<PublicProfileConnectionsPage> = { _, page -> Result.success(PublicProfileConnectionsPage(emptyList(), 0, page, 20)) }
    override suspend fun getProfile(slug: String) = profile(slug)
    override suspend fun getFavorites(slug: String) = favorites(slug)
    override suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String) = activity(year, month)
    override suspend fun getCheckIns(slug: String, cursor: String?) = checkIns(slug, cursor)
    override suspend fun setFollowing(slug: String, isFollowing: Boolean) = follow(slug, isFollowing)
    override suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int, pageSize: Int) = connections(kind, page)
}

private class ProfileSessions(user: String? = null) : SessionRepository {
    private val state = MutableStateFlow(user?.let { Session("$it-token", userId = it) })
    fun signIn(user: String?) { state.value = user?.let { Session("$it-token", userId = it) } }
    override fun peekSession() = state.value
    override fun applySession(session: Session?) { state.value = session }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = state.value
    override suspend fun persistSession(session: Session?) { state.value = session }
    override suspend fun saveSession(session: Session?) { state.value = session }
    override suspend fun warmCache() = Unit
    override fun observeSession() = state
    override suspend fun isLoggedIn() = state.value != null
}
