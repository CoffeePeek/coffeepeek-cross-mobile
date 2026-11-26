package com.coffeepeek.feature.profile.impl

import com.coffeepeek.feature.profile.api.*
import com.coffeepeek.feature.profile.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosPublicProfileSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun aNativeLikeShowsTheServerConfirmedReaction() = runTest(dispatcher) {
        val fixture = PublicProfileFixture()
        try {
            runCurrent()
            fixture.data.like = { _, helpful ->
                assertTrue(helpful)
                Result.success(ProfileCheckInLike(true, 9))
            }
            fixture.session.toggleCheckInLike("post")
            runCurrent()
            assertEquals(9, fixture.session.current().checkIns.single().checkIn.helpfulCount)
            assertTrue(fixture.session.current().checkIns.single().checkIn.isHelpfulByCurrentUser)
            assertTrue(fixture.session.current().pendingLikeIds.isEmpty())
            assertTrue(fixture.session.current().likeErrorIds.isEmpty())
        } finally { fixture.session.close() }
    }

    @Test fun pendingNativeVotesRejectDuplicatesAndFailureKeepsTheCount() = runTest(dispatcher) {
        val fixture = PublicProfileFixture()
        val reply = CompletableDeferred<Result<ProfileCheckInLike>>()
        try {
            fixture.data.like = { _, _ -> reply.await() }
            runCurrent()
            fixture.session.toggleCheckInLike("post")
            runCurrent()
            assertEquals(setOf("post"), fixture.session.current().pendingLikeIds)
            fixture.session.toggleCheckInLike("post")
            runCurrent()
            assertEquals(1, fixture.data.votes)
            reply.complete(Result.failure(IllegalStateException("offline")))
            runCurrent()
            assertEquals(4, fixture.session.current().checkIns.single().checkIn.helpfulCount)
            assertEquals(setOf("post"), fixture.session.current().likeErrorIds)
            assertTrue(fixture.session.current().pendingLikeIds.isEmpty())
        } finally { fixture.session.close() }
    }

    @Test fun aReplacementOfTheSameAccountRevokesPendingNativeVotes() = runTest(dispatcher) {
        val fixture = PublicProfileFixture()
        val reply = CompletableDeferred<Result<ProfileCheckInLike>>()
        try {
            fixture.data.like = { _, _ -> withContext(NonCancellable) { reply.await() } }
            runCurrent()
            fixture.session.toggleCheckInLike("post")
            runCurrent()
            fixture.accounts.replaceSession()
            assertFalse(fixture.session.isCurrent())
            assertTrue(fixture.session.current().checkIns.isEmpty())
            reply.complete(Result.success(ProfileCheckInLike(true, 99)))
            runCurrent()
            fixture.session.toggleCheckInLike("post")
            runCurrent()
            assertEquals(1, fixture.data.votes)
            assertTrue(fixture.session.current().pendingLikeIds.isEmpty())
            assertTrue(fixture.session.current().likeErrorIds.isEmpty())
        } finally { reply.complete(Result.success(ProfileCheckInLike(true, 99))); fixture.session.close() }
    }

    @Test fun returningToTheNativeProfileRefreshesExternalVotes() = runTest(dispatcher) {
        val fixture = PublicProfileFixture()
        try {
            fixture.session.resume()
            runCurrent()
            fixture.data.count = 7
            fixture.session.resume()
            runCurrent()
            assertEquals(7, fixture.session.current().checkIns.single().checkIn.helpfulCount)
        } finally { fixture.session.close() }
    }
}

private class PublicProfileFixture {
    val accounts = PublicProfileAccounts()
    val data = PublicProfileDataFixture()
    val session = createIosPublicProfileSession("author", data, data, accounts, data,
        ProfileAuthorVisibility.None, ProfileSnapshotStore.None)
}

private class PublicProfileAccounts : ProfileAccountPort {
    private val account = MutableStateFlow<String?>("viewer")
    private val scope = MutableStateFlow<String?>("first")
    private val profile = MutableStateFlow<OwnProfile?>(OwnProfile("Viewer", "", null, null, 0, 0, 0, 0,
        ProfileAddress("viewer", "/users/viewer", 1, false)))

    fun replaceSession() { scope.value = "replacement" }
    override fun activeAccount() = account.value
    override fun cacheScope() = scope.value
    override fun observeActiveAccount() = account
    override fun observeCacheScope() = scope
    override fun cachedOwnProfile() = profile.value
    override fun observeOwnProfile() = profile
    override suspend fun loadOwnProfile(forceRefresh: Boolean) = Result.success(requireNotNull(profile.value))
}

private class PublicProfileDataFixture : PublicProfileData, PublicCheckInsPort, ProfileCheckInLikes {
    var count = 4
    var votes = 0
    var like: suspend (String, Boolean) -> Result<ProfileCheckInLike> = { _, _ -> error("Unexpected vote") }

    override suspend fun getProfile(slug: String) = Result.success(PublicProfile("author", "Author", null, null,
        PublicProfileStats(1, 0, 0, 0), emptyList(), null, authorId = "public-author"))
    override suspend fun getFavorites(slug: String) = Result.success(emptyList<PublicProfileFavorite>())
    override suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String) =
        Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", emptySet()))
    override suspend fun setFollowing(slug: String, isFollowing: Boolean) = Result.success(PublicProfileFollowState(isFollowing, 0))
    override suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int, pageSize: Int) =
        Result.success(PublicProfileConnectionsPage(emptyList(), 0, page, pageSize))
    override suspend fun page(slug: String, cursor: String?) = Result.success(PublicProfileCheckInPage(listOf(
        PublicProfileCheckIn("post", "shop", "Shop", "Coffee", null, null, "", "2026-11-26", emptyList(),
            helpfulCount = count)), null))
    override suspend fun setHelpful(id: String, helpful: Boolean): Result<ProfileCheckInLike> {
        votes++
        return like(id, helpful)
    }
}
