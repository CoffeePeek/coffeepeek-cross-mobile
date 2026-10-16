package com.coffeepeek.admin.feature.profile.ui

import com.coffeepeek.admin.feature.profile.domain.*
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
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
    fun ownCanonicalProfileOpenedThroughAliasCannotBeFollowed() = runTest {
        val users = ProfileUsers(ownProfile("canonical"))
        var mutations = 0
        val repository = ProfileRepositoryFake().apply {
            follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) }
        }
        val vm = PublicProfileViewModel("old-alias", repository, ProfileSessions("account-id"), taskScope(testScheduler), users)
        try {
            runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            vm.toggleFollow(); runCurrent()
            assertEquals(0, mutations)
            assertFalse(vm.state.value.isFollowing)
            assertFalse(vm.state.value.isFollowLoading)
            assertFalse(vm.state.value.needsLogin)
        } finally { vm.close() }
    }

    @Test
    fun accountIdMatchingAnotherPublicSlugDoesNotMakeItOwnProfile() = runTest {
        val users = ProfileUsers(ownProfile("different"))
        var mutations = 0
        val repository = ProfileRepositoryFake().apply {
            follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) }
        }
        val vm = PublicProfileViewModel("canonical", repository, ProfileSessions("canonical"), taskScope(testScheduler), users)
        try {
            runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            vm.toggleFollow(); runCurrent()
            assertEquals(1, mutations)
        } finally { vm.close() }
    }

    @Test
    fun ownCanonicalPathMatchesEvenWhenCachedSlugIsAnAlias() = runTest {
        val repository = ProfileRepositoryFake().apply {
            profile = { Result.success(PublicProfile("canonical", "nickname", null, null,
                PublicProfileStats(2, 1, 0, 17), emptyList(), null, canonicalPath = "/users/canonical")) }
        }
        val users = ProfileUsers(ownProfile("old-name", "/users/canonical/"))
        val vm = PublicProfileViewModel("alias", repository, ProfileSessions("account-id"), taskScope(testScheduler), users)
        try {
            runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            vm.toggleFollow(); runCurrent()
            assertFalse(vm.state.value.isFollowLoading)
        } finally { vm.close() }
    }

    @Test
    fun cachedAddressChangesUpdateOwnershipAndGuardFollowBeforeCollectorRuns() = runTest {
        val users = ProfileUsers(ownProfile("different"))
        var mutations = 0
        val repository = ProfileRepositoryFake().apply {
            follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) }
        }
        val vm = PublicProfileViewModel("canonical", repository, ProfileSessions("account-id"), taskScope(testScheduler), users)
        try {
            runCurrent()
            // The same display name is not an identity match.
            assertFalse(vm.state.value.isOwnProfile)
            users.publish(ownProfile("canonical"))
            vm.toggleFollow()
            runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            assertEquals(0, mutations)
            users.publish(ownProfile("different")); runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            vm.toggleFollow(); runCurrent()
            assertEquals(1, mutations)
        } finally { vm.close() }
    }

    @Test
    fun followWaitsForUnknownOwnIdentityAndDoesNotMutateOwnProfile() = runTest {
        val loaded = CompletableDeferred<UserProfile>()
        val users = ProfileUsers().apply { load = { Result.success(loaded.await()) } }
        var mutations = 0
        val repository = ProfileRepositoryFake().apply {
            follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) }
        }
        val vm = PublicProfileViewModel("canonical", repository, ProfileSessions("account-id"), taskScope(testScheduler), users)
        try {
            runCurrent()
            vm.toggleFollow(); runCurrent()
            assertEquals(0, mutations)
            loaded.complete(ownProfile("canonical")); runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            assertFalse(vm.state.value.isFollowLoading)
            assertEquals(0, mutations)
        } finally { loaded.complete(ownProfile("canonical")); vm.close() }
    }

    @Test
    fun accountChangeRejectsOldCachedOwnershipUntilCurrentIdentityLoadsAndLogoutClearsIt() = runTest {
        val loaded = CompletableDeferred<UserProfile>()
        val users = ProfileUsers(ownProfile("canonical"))
        val sessions = ProfileSessions("alice")
        val vm = PublicProfileViewModel("canonical", ProfileRepositoryFake(), sessions, taskScope(testScheduler), users)
        try {
            runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            users.load = { Result.success(loaded.await()) }
            sessions.signIn("bob"); runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            assertEquals("canonical", users.observeProfile().value?.address?.slug)
            loaded.complete(ownProfile("different")); runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            users.publish(ownProfile("canonical")); runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            sessions.signIn(null); runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            vm.toggleFollow(); runCurrent()
            assertTrue(vm.state.value.needsLogin)
        } finally { loaded.complete(ownProfile("different")); vm.close() }
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
    private fun ownProfile(slug: String, canonicalPath: String = "/users/$slug") =
        UserProfile("nickname", "", null, null, 0, 0, 0, PublicAddress(slug, canonicalPath, 1, false))
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

private class ProfileUsers(initial: UserProfile? = null) : UserRepository {
    private val profile = MutableStateFlow(initial)
    var load: suspend () -> Result<UserProfile> = { Result.failure(IllegalStateException("Profile unavailable")) }
    fun publish(value: UserProfile?) { profile.value = value }
    override fun observeProfile() = profile
    override suspend fun refreshProfile() = load().onSuccess(::publish)
    override suspend fun getMe() = profile.value?.let { Result.success(it) } ?: refreshProfile()
    override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = error("Unused")
    override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("Unused")
    override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("Unused")
    override suspend fun updateUsername(username: String): Result<Unit> = error("Unused")
    override suspend fun updateAbout(about: String): Result<Unit> = error("Unused")
    override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> = error("Unused")
}
