package com.coffeepeek.feature.profile.impl.ui

import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileError
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.ProfileAddress
import com.coffeepeek.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.domain.PublicProfileAuthenticationRequired
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileCheckInPage
import com.coffeepeek.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionsPage
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.feature.profile.domain.PublicProfileFollowState
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.feature.profile.domain.PublicProfileUnavailable
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Port of the legacy `PublicProfileViewModelTest`: the same account, pagination and staleness guarantees, now against ports. */
@OptIn(ExperimentalCoroutinesApi::class)
class PublicProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(
        slug: String = "canonical",
        data: FakeData = FakeData(),
        accounts: FakeAccounts = FakeAccounts(),
        visibility: ProfileAuthorVisibility = ProfileAuthorVisibility.None,
    ) = PublicProfileViewModel(slug, data, data, accounts, visibility).also { store.put("profile-${nextKey++}", it) }

    private fun TestScope.events(model: PublicProfileViewModel): List<PublicProfileEvent> =
        mutableListOf<PublicProfileEvent>().also { list -> backgroundScope.launch { model.events.collect { list += it } } }

    @Test
    fun canonicalAuthorPaginationDeduplicatesAndFailedCursorRetryStartsAtFirstPage() = runTest(dispatcher) {
        val requests = mutableListOf<Pair<String, String?>>()
        var failNextPage = true
        val data = FakeData().apply {
            checkIns = { slug, cursor ->
                requests += slug to cursor
                when {
                    cursor == null -> Result.success(PublicProfileCheckInPage(listOf(checkIn("one")), "next"))
                    failNextPage -> { failNextPage = false; Result.failure(IllegalStateException("Cursor expired")) }
                    else -> Result.success(PublicProfileCheckInPage(listOf(checkIn("one"), checkIn("two")), null))
                }
            }
        }
        val vm = model("alias", data); runCurrent()
        assertEquals<List<Pair<String, String?>>>(listOf("canonical" to null), requests)
        vm.onAction(PublicProfileAction.LoadMoreCheckIns); runCurrent()
        assertNotNull(vm.state.value.checkInsError)
        vm.onAction(PublicProfileAction.LoadMoreCheckIns); runCurrent()
        assertEquals(2, requests.size)
        vm.onAction(PublicProfileAction.RetryCheckIns); runCurrent()
        assertEquals<Pair<String, String?>>("canonical" to null, requests.last())
        vm.onAction(PublicProfileAction.LoadMoreCheckIns); runCurrent()
        assertEquals(listOf("one", "two"), vm.state.value.checkIns.map { it.id })
        assertFalse(vm.state.value.hasMoreCheckIns)
    }

    @Test
    fun favoriteAndActivityFailuresDoNotHideProfileAndCanBeRetriedIndependently() = runTest(dispatcher) {
        val data = FakeData().apply {
            favorites = { Result.failure(IllegalStateException("Favorites unavailable")) }
            activity = { _, _ -> Result.failure(IllegalStateException("Activity unavailable")) }
        }
        val vm = model("alias", data); runCurrent()
        assertNotNull(vm.state.value.profile)
        assertNull(vm.state.value.error)
        assertNotNull(vm.state.value.favoritesError)
        assertNotNull(vm.state.value.activityError)
        assertTrue(vm.state.value.profile!!.favorites.isEmpty())
        assertNull(vm.state.value.profile!!.activity)
        data.favorites = { Result.success(listOf(PublicProfileFavorite("drink:matcha", "Матча", PublicFavoriteKind.Drink, drinkSlug = "matcha"))) }
        vm.onAction(PublicProfileAction.RetryFavorites); runCurrent()
        assertEquals("Матча", vm.state.value.profile!!.favorites.single().title)
        assertNull(vm.state.value.favoritesError)
        assertNotNull(vm.state.value.activityError)
        data.activity = { year, month -> Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", setOf(3), totalCheckIns = 7, checkInCounts = mapOf(3 to 7))) }
        vm.onAction(PublicProfileAction.RetryActivity); runCurrent()
        assertEquals(7, vm.state.value.profile!!.activity!!.totalCheckIns)
        assertNull(vm.state.value.activityError)
    }

    @Test
    fun guestFollowRequestsLoginAndAuthenticatedFollowUsesExactServerCount() = runTest(dispatcher) {
        val accounts = FakeAccounts()
        var mutations = 0
        val data = FakeData().apply {
            follow = { slug, desired ->
                assertEquals("canonical", slug)
                mutations++
                Result.success(PublicProfileFollowState(desired, if (desired) 91 else 2))
            }
        }
        val vm = model("alias", data, accounts)
        val events = events(vm); runCurrent()
        vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertEquals(listOf<PublicProfileEvent>(PublicProfileEvent.RequireLogin), events)
        assertEquals(0, mutations)
        accounts.signIn("alice"); runCurrent()
        vm.onAction(PublicProfileAction.ToggleFollow); vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertEquals(1, mutations)
        assertTrue(vm.state.value.isFollowing)
        assertEquals(91, vm.state.value.profile!!.stats.followers)
        vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertFalse(vm.state.value.isFollowing)
        assertEquals(2, vm.state.value.profile!!.stats.followers)
    }

    @Test
    fun failedFollowPreservesPreviousStateAndUnauthorizedResponseRequestsLogin() = runTest(dispatcher) {
        val data = FakeData().apply { follow = { _, _ -> Result.failure(PublicProfileAuthenticationRequired()) } }
        val vm = model("canonical", data, FakeAccounts("alice", ownProfile("someone-else")))
        val events = events(vm); runCurrent()
        val oldCount = vm.state.value.profile!!.stats.followers
        vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertFalse(vm.state.value.isFollowing)
        assertEquals(oldCount, vm.state.value.profile!!.stats.followers)
        assertFalse(vm.state.value.isFollowLoading)
        assertNotNull(vm.state.value.followError)
        assertEquals(listOf<PublicProfileEvent>(PublicProfileEvent.RequireLogin), events)
    }

    @Test
    fun ownCanonicalProfileOpenedThroughAliasCannotBeFollowed() = runTest(dispatcher) {
        var mutations = 0
        val data = FakeData().apply { follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) } }
        val vm = model("old-alias", data, FakeAccounts("account-id", ownProfile("canonical")))
        val events = events(vm); runCurrent()
        assertTrue(vm.state.value.isOwnProfile)
        vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertEquals(0, mutations)
        assertFalse(vm.state.value.isFollowing)
        assertFalse(vm.state.value.isFollowLoading)
        assertTrue(events.isEmpty())
    }

    @Test
    fun accountIdMatchingAnotherPublicSlugDoesNotMakeItOwnProfile() = runTest(dispatcher) {
        var mutations = 0
        val data = FakeData().apply { follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) } }
        val vm = model("canonical", data, FakeAccounts("canonical", ownProfile("different"))); runCurrent()
        assertFalse(vm.state.value.isOwnProfile)
        vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertEquals(1, mutations)
    }

    @Test
    fun ownCanonicalPathMatchesEvenWhenCachedSlugIsAnAlias() = runTest(dispatcher) {
        val data = FakeData().apply {
            profile = { Result.success(PublicProfile("canonical", "nickname", null, null,
                PublicProfileStats(2, 1, 0, 17), emptyList(), null, canonicalPath = "/users/canonical")) }
        }
        val vm = model("alias", data, FakeAccounts("account-id", ownProfile("old-name", "/users/canonical/"))); runCurrent()
        assertTrue(vm.state.value.isOwnProfile)
        vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertFalse(vm.state.value.isFollowLoading)
    }

    @Test
    fun cachedAddressChangesUpdateOwnershipAndGuardFollowBeforeCollectorRuns() = runTest(dispatcher) {
        val accounts = FakeAccounts("account-id", ownProfile("different"))
        var mutations = 0
        val data = FakeData().apply { follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) } }
        val vm = model("canonical", data, accounts); runCurrent()
        // The same display name is not an identity match.
        assertFalse(vm.state.value.isOwnProfile)
        accounts.publish(ownProfile("canonical"))
        vm.onAction(PublicProfileAction.ToggleFollow)
        runCurrent()
        assertTrue(vm.state.value.isOwnProfile)
        assertEquals(0, mutations)
        accounts.publish(ownProfile("different")); runCurrent()
        assertFalse(vm.state.value.isOwnProfile)
        vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
        assertEquals(1, mutations)
    }

    @Test
    fun followWaitsForUnknownOwnIdentityAndDoesNotMutateOwnProfile() = runTest(dispatcher) {
        val loaded = CompletableDeferred<OwnProfile>()
        val accounts = FakeAccounts("account-id").apply { load = { Result.success(loaded.await()) } }
        var mutations = 0
        val data = FakeData().apply { follow = { _, desired -> mutations++; Result.success(PublicProfileFollowState(desired, 18)) } }
        val vm = model("canonical", data, accounts)
        try {
            runCurrent()
            vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
            assertEquals(0, mutations)
            loaded.complete(ownProfile("canonical")); runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            assertFalse(vm.state.value.isFollowLoading)
            assertEquals(0, mutations)
        } finally { loaded.complete(ownProfile("canonical")) }
    }

    @Test
    fun accountChangeRejectsOldCachedOwnershipUntilCurrentIdentityLoadsAndLogoutClearsIt() = runTest(dispatcher) {
        val loaded = CompletableDeferred<OwnProfile>()
        val accounts = FakeAccounts("alice", ownProfile("canonical"))
        val vm = model("canonical", FakeData(), accounts)
        val events = events(vm)
        try {
            runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            accounts.load = { Result.success(loaded.await()) }
            accounts.signIn("bob"); runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            assertEquals("canonical", accounts.cachedOwnProfile()?.address?.slug)
            loaded.complete(ownProfile("different")); runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            accounts.publish(ownProfile("canonical")); runCurrent()
            assertTrue(vm.state.value.isOwnProfile)
            accounts.signIn(null); runCurrent()
            assertFalse(vm.state.value.isOwnProfile)
            vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
            assertEquals(listOf<PublicProfileEvent>(PublicProfileEvent.RequireLogin), events)
        } finally { loaded.complete(ownProfile("different")) }
    }

    @Test
    fun oldAccountsLateFollowCannotChangeNewViewersProfile() = runTest(dispatcher) {
        val deferred = CompletableDeferred<PublicProfileFollowState>()
        val accounts = FakeAccounts("alice", ownProfile("someone-else"))
        val data = FakeData().apply { follow = { _, _ -> withContext(NonCancellable) { Result.success(deferred.await()) } } }
        val vm = model("canonical", data, accounts)
        try {
            runCurrent()
            vm.onAction(PublicProfileAction.ToggleFollow); runCurrent()
            assertTrue(vm.state.value.isFollowLoading)
            accounts.signIn("bob"); runCurrent()
            deferred.complete(PublicProfileFollowState(true, 99)); runCurrent()
            assertFalse(vm.state.value.isFollowing)
            assertEquals(17, vm.state.value.profile!!.stats.followers)
            assertFalse(vm.state.value.isFollowLoading)
        } finally { deferred.complete(PublicProfileFollowState(true, 99)) }
    }

    @Test
    fun changingActivityMonthRejectsLateOldMonthResponse() = runTest(dispatcher) {
        val deferred = CompletableDeferred<PublicProfileActivity>()
        var first = true
        val data = FakeData().apply {
            activity = { year, month ->
                if (first) { first = false; withContext(NonCancellable) { Result.success(deferred.await()) } }
                else Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", setOf(2)))
            }
        }
        val vm = model("canonical", data)
        try {
            runCurrent()
            vm.onAction(PublicProfileAction.ChangeActivityMonth("2025-02")); runCurrent()
            assertEquals("2025-02", vm.state.value.profile!!.activity!!.month)
            deferred.complete(PublicProfileActivity("2026-10", setOf(10))); runCurrent()
            assertEquals("2025-02", vm.state.value.profile!!.activity!!.month)
            assertEquals(setOf(2), vm.state.value.profile!!.activity!!.activeDays)
        } finally { deferred.complete(PublicProfileActivity("2026-10", emptySet())) }
    }

    @Test
    fun invalidActivityMonthsAreIgnored() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        val month = vm.state.value.activityMonth
        for (bad in listOf("2026-13", "1999-12", "2101-01", "2026-1", "soon", "")) {
            vm.onAction(PublicProfileAction.ChangeActivityMonth(bad)); runCurrent()
            assertEquals(month, vm.state.value.activityMonth, "'$bad'")
        }
    }

    @Test
    fun connectionsRespectServerPaginationDeduplicateAndSwitchKinds() = runTest(dispatcher) {
        val requests = mutableListOf<Pair<PublicProfileConnectionKind, Int>>()
        val data = FakeData().apply {
            connections = { kind, page ->
                requests += kind to page
                Result.success(PublicProfileConnectionsPage(
                    if (page == 1) listOf(person("one")) else listOf(person("one"), person("two")), 21, page, 20))
            }
        }
        val vm = model("canonical", data); runCurrent()
        vm.onAction(PublicProfileAction.OpenConnections(PublicProfileConnectionKind.Followers)); runCurrent()
        assertTrue(vm.state.value.hasMoreConnections)
        vm.onAction(PublicProfileAction.LoadMoreConnections); runCurrent()
        assertEquals(listOf("one", "two"), vm.state.value.connections.map { it.slug })
        assertFalse(vm.state.value.hasMoreConnections)
        vm.onAction(PublicProfileAction.OpenConnections(PublicProfileConnectionKind.Following)); runCurrent()
        assertEquals(PublicProfileConnectionKind.Following to 1, requests.last())
        assertEquals(listOf("one"), vm.state.value.connections.map { it.slug })
        vm.onAction(PublicProfileAction.CloseConnections); runCurrent()
        assertNull(vm.state.value.connectionKind)
    }

    @Test
    fun unavailableProfileHasAnErrorWithoutInventingFallbackSections() = runTest(dispatcher) {
        val vm = model("missing", FakeData().apply { profile = { Result.failure(IllegalStateException("Missing")) } }); runCurrent()
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.profile)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.checkIns.isEmpty())
    }

    @Test
    fun unavailableRefreshClearsPreviouslyVisibleProfileAndSections() = runTest(dispatcher) {
        val data = FakeData().apply {
            favorites = { Result.success(listOf(PublicProfileFavorite("drink:matcha", "Матча", PublicFavoriteKind.Drink, drinkSlug = "matcha"))) }
            checkIns = { _, _ -> Result.success(PublicProfileCheckInPage(listOf(checkIn("visible")), null)) }
        }
        val vm = model("canonical", data); runCurrent()
        assertNotNull(vm.state.value.profile)
        assertTrue(vm.state.value.checkIns.isNotEmpty())
        assertTrue(vm.state.value.profile!!.favorites.isNotEmpty())
        data.profile = { Result.failure(PublicProfileUnavailable()) }
        vm.onAction(PublicProfileAction.Refresh); runCurrent()
        assertNull(vm.state.value.profile)
        assertTrue(vm.state.value.checkIns.isEmpty())
        assertTrue(vm.state.value.connections.isEmpty())
        assertEquals(PublicProfileError.Unavailable, vm.state.value.error)
    }

    @Test
    fun blockedAuthorHidesSectionsAndANewVisibilityRevisionReloadsThePage() = runTest(dispatcher) {
        val visibility = FakeVisibility()
        var checkInRequests = 0
        val data = FakeData().apply { checkIns = { _, _ -> checkInRequests++; Result.success(PublicProfileCheckInPage(listOf(checkIn("one")), null)) } }
        val vm = model("canonical", data, visibility = visibility); runCurrent()
        assertEquals(1, checkInRequests)
        assertTrue(vm.state.value.checkIns.isNotEmpty())
        data.profile = { Result.success(PublicProfile("canonical", "nickname", null, null, PublicProfileStats(2, 1, 0, 17), emptyList(), null, authorId = "author")) }
        visibility.publish(ProfileAuthorVisibilityState(revision = 2, blockedIds = setOf("author"))); runCurrent()
        assertTrue(vm.state.value.profile!!.isBlockedByViewer)
        assertTrue(vm.state.value.checkIns.isEmpty())
        assertTrue(vm.state.value.profile!!.favorites.isEmpty())
        assertNull(vm.state.value.profile!!.activity)
        assertFalse(vm.state.value.isFollowing)
        assertEquals(1, checkInRequests)
    }

    @Test
    fun anUnreadyVisibilityListIsRefreshedBeforeTheProfileLoadsAndItsFailureBlocksTheScreen() = runTest(dispatcher) {
        val visibility = FakeVisibility(ProfileAuthorVisibilityState(ready = false)).apply { refreshResult = Result.failure(IllegalStateException("down")) }
        var profileRequests = 0
        val data = FakeData().apply { profile = { profileRequests++; Result.success(PublicProfile("canonical", "n", null, null, PublicProfileStats(0, 0, 0, 0), emptyList(), null)) } }
        val vm = model("canonical", data, visibility = visibility); runCurrent()
        assertEquals(1, visibility.refreshes)
        assertEquals(0, profileRequests)
        assertNull(vm.state.value.profile)
        assertEquals(PublicProfileError.BlocksLoad, vm.state.value.error)
    }

    private fun checkIn(id: String) = PublicProfileCheckIn(id, null, "Дома", "Кофе", null, null, "", "2026-10-15", emptyList())
    private fun person(slug: String) = PublicProfileConnection(slug, null, slug, "/users/$slug")
}

private fun ownProfile(slug: String, canonicalPath: String = "/users/$slug") =
    OwnProfile("nickname", "", null, null, 0, 0, 0, 0, ProfileAddress(slug, canonicalPath, 1, false))

private class FakeData : PublicProfileData, PublicCheckInsPort {
    var profile: suspend (String) -> Result<PublicProfile> = { Result.success(PublicProfile("canonical", "nickname", null, null, PublicProfileStats(2, 1, 0, 17), emptyList(), null)) }
    var favorites: suspend (String) -> Result<List<PublicProfileFavorite>> = { Result.success(emptyList()) }
    var activity: suspend (Int, Int) -> Result<PublicProfileActivity> = { year, month -> Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", emptySet())) }
    var checkIns: suspend (String, String?) -> Result<PublicProfileCheckInPage> = { _, _ -> Result.success(PublicProfileCheckInPage(emptyList(), null)) }
    var follow: suspend (String, Boolean) -> Result<PublicProfileFollowState> = { _, _ -> error("Unexpected follow") }
    var connections: suspend (PublicProfileConnectionKind, Int) -> Result<PublicProfileConnectionsPage> = { _, page -> Result.success(PublicProfileConnectionsPage(emptyList(), 0, page, 20)) }
    override suspend fun getProfile(slug: String) = profile(slug)
    override suspend fun getFavorites(slug: String) = favorites(slug)
    override suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String) = activity(year, month)
    override suspend fun page(slug: String, cursor: String?) = checkIns(slug, cursor)
    override suspend fun setFollowing(slug: String, isFollowing: Boolean) = follow(slug, isFollowing)
    override suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int, pageSize: Int) = connections(kind, page)
}

/** Account keys are plain user ids; [load] answers a profile request the way `getMe`/`refreshProfile` do: cache first unless forced. */
private class FakeAccounts(user: String? = null, own: OwnProfile? = null) : ProfileAccountPort {
    private val account = MutableStateFlow(user)
    private val cache = MutableStateFlow(own)
    var load: suspend () -> Result<OwnProfile> = { Result.success(ownProfile("other-user")) }
    fun signIn(user: String?) { account.value = user }
    fun publish(value: OwnProfile?) { cache.value = value }
    override fun activeAccount() = account.value
    override fun observeActiveAccount(): Flow<String?> = account
    override fun cachedOwnProfile() = cache.value
    override fun observeOwnProfile(): Flow<OwnProfile?> = cache
    override suspend fun loadOwnProfile(forceRefresh: Boolean): Result<OwnProfile> {
        if (!forceRefresh) cache.value?.let { return Result.success(it) }
        return load().onSuccess { cache.value = it }
    }
}

private class FakeVisibility(initial: ProfileAuthorVisibilityState = ProfileAuthorVisibilityState()) : ProfileAuthorVisibility {
    private val state = MutableStateFlow(initial)
    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshes = 0
    fun publish(value: ProfileAuthorVisibilityState) { state.update { value } }
    override fun current() = state.value
    override fun observe(): Flow<ProfileAuthorVisibilityState> = state
    override suspend fun refresh(): Result<Unit> { refreshes++; return refreshResult }
}
