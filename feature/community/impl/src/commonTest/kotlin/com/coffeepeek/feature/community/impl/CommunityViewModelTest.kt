package com.coffeepeek.feature.community.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedLoadException
import com.coffeepeek.feature.community.domain.model.FeedPage
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.community.domain.model.MyPostsPage
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.CommunityEvent
import com.coffeepeek.feature.community.impl.timeline.CommunityTimeline
import com.coffeepeek.feature.community.impl.timeline.CommunityViewModel
import com.coffeepeek.feature.community.impl.timeline.MessageKind
import com.coffeepeek.feature.community.impl.timeline.TimelineError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.model(
        repository: FakeRepository = FakeRepository(), account: FakeAccount = FakeAccount("one"), analytics: FakeAnalytics = FakeAnalytics(),
        authors: FakeAuthors = FakeAuthors(), drinks: FakeDrinks = FakeDrinks(),
    ) = CommunityViewModel(repository, account, analytics, drinks, authors).also { store.put("community", it); runCurrent() }

    private fun TestScope.events(vm: CommunityViewModel) = mutableListOf<CommunityEvent>().also { list -> backgroundScope.launch { vm.events.collect { list += it } } }
    private fun CommunityViewModel.send(action: CommunityAction, scope: TestScope) { onAction(action); scope.runCurrent() }
    private fun page(vararg ids: String, next: String? = null) = FeedPage(ids.map { entry(post(it)) }, next)

    @Test fun aGuestSeesThePublicFeedInServerOrderWithoutPersonalRequestsAndALikeAsksToSignIn() = runTest(dispatcher) {
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(page("first", "second")) }; mine = { _, _ -> error("Private history must not load") } }
        val analytics = FakeAnalytics()
        val vm = model(repo, FakeAccount(null), analytics); val events = events(vm)
        assertEquals(listOf("first", "second"), vm.state.value.items.map { it.id })
        assertEquals(false, vm.state.value.isLoggedIn); assertFalse(vm.state.value.hasMore)
        assertEquals("2026-10-07T10:00:00Z", vm.state.value.publishedAt["first"])
        vm.send(CommunityAction.ToggleHelpful("first"), this)
        assertEquals(listOf<CommunityEvent>(CommunityEvent.SignInRequested), events)
        assertEquals(listOf("item:Like:shop", "gate:like"), analytics.events)
        events.clear()
        vm.send(CommunityAction.SelectTimeline(CommunityTimeline.Mine), this)
        assertEquals(listOf<CommunityEvent>(CommunityEvent.SignInRequested), events)
        assertEquals(CommunityTimeline.Public, vm.state.value.timeline)
    }

    @Test fun aSessionChangeResetsTheStateAndALateAnswerForThePreviousAccountIsDropped() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepository().apply { feed = { _, _, _ -> gate.await(); Result.success(page("private-of-one")) } }
        val account = FakeAccount("one")
        val vm = model(repo, account)
        assertTrue(vm.state.value.isLoading)
        account.switchTo(null); runCurrent()
        assertEquals(false, vm.state.value.isLoggedIn)
        gate.complete(Unit); runCurrent()
        assertTrue(vm.state.value.items.none { it.id == "private-of-one" } || vm.state.value.isLoggedIn == false)
        assertEquals(2L, vm.state.value.sessionGeneration)
    }

    @Test fun aLoadMoreFailureRetriesWithTheSameCursorAndAppendsWithoutDuplicates() = runTest(dispatcher) {
        var fail = true
        val repo = FakeRepository().apply { feed = { _, cursor, _ ->
            when {
                cursor == null -> Result.success(page("first", next = "next_-opaque"))
                fail -> { fail = false; Result.failure(IllegalStateException("Offline")) }
                else -> Result.success(page("first", "second"))
            }
        } }
        val analytics = FakeAnalytics(); val vm = model(repo, analytics = analytics)
        vm.send(CommunityAction.LoadMore(), this)
        assertEquals(2, vm.state.value.failedPage); assertEquals(TimelineError.LoadFailed, vm.state.value.error)
        assertEquals("next_-opaque", vm.state.value.nextCursor)
        vm.send(CommunityAction.Retry, this)
        assertEquals(listOf("first", "second"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.hasMore)
        assertEquals(listOf(null, "next_-opaque", "next_-opaque"), repo.feedRequests.map { it.second })
        // The retry of page 2 is a load more without a second load_more event.
        assertEquals(listOf("feed:LoadMore:Scroll", "feed:Retry:Button"), analytics.events)
    }

    @Test fun refreshReplacesTheListFromTheFirstPageAndReportsItsTrigger() = runTest(dispatcher) {
        var round = 0
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(page(if (round++ == 0) "old" else "new")) } }
        val analytics = FakeAnalytics(); val vm = model(repo, analytics = analytics)
        vm.send(CommunityAction.Refresh(FeedTrigger.Gesture), this)
        vm.send(CommunityAction.Refresh(), this) // no trigger: no event
        assertEquals(listOf("new"), vm.state.value.items.map { it.id })
        assertEquals(listOf("feed:Refresh:Gesture"), analytics.events)
    }

    @Test fun anInvalidCursorRestartsFromTheFirstPageWithTheSameFilters() = runTest(dispatcher) {
        val filters = FeedFilters(citySlug = "minsk")
        val repo = FakeRepository().apply { feed = { _, cursor, _ ->
            if (cursor == null) Result.success(page("first", next = "stale")) else Result.failure(FeedLoadException("Refresh needed", true))
        } }
        val vm = model(repo); vm.send(CommunityAction.SetFilters(filters), this)
        vm.send(CommunityAction.LoadMore(), this)
        assertTrue(vm.state.value.restartPagination)
        vm.send(CommunityAction.Retry, this)
        assertEquals(listOf(null, null, "stale", null), repo.feedRequests.map { it.second })
        assertEquals(filters, repo.feedRequests.last().third)
        assertEquals(1, vm.state.value.page); assertFalse(vm.state.value.restartPagination)
    }

    @Test fun changingFiltersResetsTheCursorAndALateOldResponseCannotReplaceTheNewScope() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val target = FeedFilters(citySlug = "city", coffeeShopSlug = "shop", authorSlug = "author")
        val repo = FakeRepository().apply { feed = { _, _, filters ->
            if (filters == FeedFilters()) { release.await(); Result.success(page("wrong-scope", next = "wrong")) } else Result.success(page("filtered"))
        } }
        val vm = model(repo)
        vm.send(CommunityAction.SetFilters(target), this)
        assertEquals(listOf("filtered"), vm.state.value.items.map { it.id })
        release.complete(Unit); runCurrent()
        assertEquals(listOf("filtered"), vm.state.value.items.map { it.id }); assertNull(vm.state.value.nextCursor)
    }

    @Test fun aFirstPageFailureShowsTheErrorAndRetryRefreshes() = runTest(dispatcher) {
        var attempts = 0
        val repo = FakeRepository().apply { feed = { _, _, _ -> if (attempts++ == 0) Result.failure(IllegalStateException("Offline")) else Result.success(page("first")) } }
        val vm = model(repo)
        assertEquals(TimelineError.LoadFailed, vm.state.value.error); assertEquals(1, vm.state.value.failedPage)
        assertFalse(vm.state.value.restartPagination); assertFalse(vm.state.value.isLoading)
        vm.send(CommunityAction.Retry, this)
        assertNull(vm.state.value.error); assertEquals(listOf("first"), vm.state.value.items.map { it.id })
    }

    @Test fun thePersonalTimelineLoadsPagesAndReportsNoFeedEvents() = runTest(dispatcher) {
        val repo = FakeRepository().apply { mine = { number, _ -> Result.success(MyPostsPage(listOf(post("own-$number", slug = null)), hasMore = number < 2)) } }
        val analytics = FakeAnalytics(); val vm = model(repo, analytics = analytics)
        vm.send(CommunityAction.SelectTimeline(CommunityTimeline.Mine), this)
        assertEquals(listOf("own-1"), vm.state.value.items.map { it.id }); assertTrue(vm.state.value.hasMore)
        assertTrue(vm.state.value.owns(vm.state.value.items.single()))
        vm.send(CommunityAction.LoadMore(), this)
        assertEquals(listOf("own-1", "own-2"), vm.state.value.items.map { it.id }); assertFalse(vm.state.value.hasMore)
        vm.send(CommunityAction.Refresh(FeedTrigger.Gesture), this)
        assertTrue(analytics.events.isEmpty())
    }

    @Test fun blockingIsOfferedOnlyAfterTheCanonicalOwnSlugIsKnown() = runTest(dispatcher) {
        val profile = CompletableDeferred<Result<String?>>()
        val own = post("own", slug = "me"); val other = post("other")
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(own), entry(other)), null)) } }
        val account = FakeAccount("one").apply { ownSlug = { profile.await() } }
        val vm = model(repo, account)
        assertNull(vm.state.value.ownAuthorSlug)
        assertFalse(vm.state.value.canBlockAuthor(own)); assertFalse(vm.state.value.canBlockAuthor(other))
        profile.complete(Result.success("me")); runCurrent()
        assertFalse(vm.state.value.canBlockAuthor(own)); assertTrue(vm.state.value.canBlockAuthor(other))
        assertTrue(vm.state.value.owns(own))
    }

    @Test fun blockingStaysHiddenWhenTheOwnershipLookupFailsAndAGuestMayOpenSignIn() = runTest(dispatcher) {
        val other = post("other")
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(other)), null)) } }
        val vm = model(repo, FakeAccount("one").apply { ownSlug = { Result.failure(IllegalStateException("Profile unavailable")) } })
        assertFalse(vm.state.value.canBlockAuthor(other))
        val guest = model(repo, FakeAccount(null))
        assertTrue(guest.state.value.canBlockAuthor(other))
    }

    @Test fun votesUseTheAuthoritativeCountAndAReportedFailureShowsTheServerMessage() = runTest(dispatcher) {
        val own = post("own", slug = "me"); val other = post("other", home = true)
        val release = CompletableDeferred<HelpfulVote>()
        var votes = 0
        val repo = FakeRepository().apply {
            feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(own), entry(other)), null)) }
            helpful = { id, helpful -> assertEquals("other", id); votes++; if (helpful) Result.success(release.await()) else Result.failure(CommunityFailure(FailureKind.Other, "Server says no")) }
        }
        val analytics = FakeAnalytics(); val vm = model(repo, analytics = analytics); val events = events(vm)
        vm.send(CommunityAction.ToggleHelpful("other"), this)
        assertEquals("other", vm.state.value.helpfulId)
        vm.send(CommunityAction.ToggleHelpful("other"), this); assertEquals(1, votes) // one mutation at a time
        release.complete(HelpfulVote(true, 8)); runCurrent()
        assertNull(vm.state.value.helpfulId)
        assertEquals(8, vm.state.value.items.last().helpfulCount); assertTrue(vm.state.value.items.last().isHelpfulByCurrentUser)
        vm.send(CommunityAction.ToggleHelpful("other"), this) // unlike, which the fake refuses
        val message = assertIs<CommunityEvent.Message>(events.single())
        assertEquals(MessageKind.VoteFailed, message.message.kind); assertEquals("Server says no", message.message.serverMessage)
        assertEquals(listOf("item:Like:home", "item:Unlike:home"), analytics.events)
        assertTrue(vm.state.value.items.last().isHelpfulByCurrentUser) // a failed vote changes nothing
    }

    @Test fun ownPublishedPostsCanBeLiked() = runTest(dispatcher) {
        var voted: String? = null
        val repo = FakeRepository().apply {
            feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(post("own", slug = "me"))), null)) }
            helpful = { id, _ -> voted = id; Result.success(HelpfulVote(true, 1)) }
        }
        val vm = model(repo)
        vm.send(CommunityAction.ToggleHelpful("own"), this); runCurrent()
        assertEquals("own", voted); assertEquals(1, vm.state.value.items.single().helpfulCount)
    }

    @Test fun aVoteFailureThatIsNotAServerAnswerUsesTheDefaultTextAndNeverTheRawErrorMessage() = runTest(dispatcher) {
        val repo = FakeRepository().apply {
            feed = { _, _, _ -> Result.success(page("other")) }
            helpful = { _, _ -> Result.failure(IllegalStateException("Unable to resolve host api.example")) }
        }
        val vm = model(repo); val events = events(vm)
        vm.send(CommunityAction.ToggleHelpful("other"), this)
        assertNull(assertIs<CommunityEvent.Message>(events.single()).message.serverMessage)
    }

    @Test fun whileAMutationRunsRefreshFiltersTimelineAndAnotherVoteAreIgnored() = runTest(dispatcher) {
        val release = CompletableDeferred<HelpfulVote>()
        val repo = FakeRepository().apply {
            feed = { _, _, _ -> Result.success(page("a", "b")) }
            helpful = { _, _ -> Result.success(release.await()) }
        }
        val vm = model(repo)
        vm.send(CommunityAction.ToggleHelpful("a"), this)
        val requests = repo.feedRequests.size
        vm.send(CommunityAction.Refresh(), this); vm.send(CommunityAction.SetFilters(FeedFilters(citySlug = "x")), this)
        vm.send(CommunityAction.SelectTimeline(CommunityTimeline.Mine), this); vm.send(CommunityAction.ToggleHelpful("b"), this)
        assertEquals(requests, repo.feedRequests.size)
        assertEquals(FeedFilters(), vm.state.value.filters); assertEquals(CommunityTimeline.Public, vm.state.value.timeline)
        assertEquals("a", vm.state.value.helpfulId)
        release.complete(HelpfulVote(true, 1)); runCurrent()
    }

    @Test fun itemInteractionsAreReportedOnlyOnThePublicTimeline() = runTest(dispatcher) {
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(post("shop")), entry(post("home", home = true))), null)) }
            mine = { _, _ -> Result.success(MyPostsPage(listOf(post("shop")), false)) } }
        val analytics = FakeAnalytics(); val vm = model(repo, analytics = analytics)
        vm.send(CommunityAction.ItemInteracted(FeedItemAction.Shop, "shop"), this)
        vm.send(CommunityAction.ItemInteracted(FeedItemAction.Photo, "home"), this)
        vm.send(CommunityAction.SelectTimeline(CommunityTimeline.Mine), this)
        vm.send(CommunityAction.ItemInteracted(FeedItemAction.Photo, "shop"), this)
        assertEquals(listOf("item:Shop:shop", "item:Photo:home"), analytics.events)
    }

    @Test fun aNotReadyBlockListIsRefreshedOnceAndTheFeedIsRequestedOnce() = runTest(dispatcher) {
        val authors = FakeAuthors(ready = false)
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(page("first")) } }
        val vm = model(repo, authors = authors)
        assertEquals(1, authors.refreshes)
        assertEquals(1, repo.feedRequests.size) // the snapshot that this refresh publishes does not restart the load
        assertEquals(listOf("first"), vm.state.value.items.map { it.id })
    }

    @Test fun aFailedBlockListRefreshShowsTheErrorWithoutRequestingPostsAndRetryLoads() = runTest(dispatcher) {
        val authors = FakeAuthors(ready = false)
        var succeed = false
        authors.refresh = { if (succeed) { authors.visibility.value = AuthorVisibilitySnapshot(1); Result.success(Unit) } else Result.failure(IllegalStateException("Blocks unavailable")) }
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(page("first")) } }
        val vm = model(repo, authors = authors)
        assertEquals(TimelineError.BlocksFailed, vm.state.value.error); assertTrue(repo.feedRequests.isEmpty())
        succeed = true
        vm.send(CommunityAction.Retry, this)
        assertEquals(listOf("first"), vm.state.value.items.map { it.id }); assertEquals(1, repo.feedRequests.size)
    }

    @Test fun aBlockInvalidatesTheCursorAndALatePageCannotRestoreTheAuthorAndAnUnblockReloads() = runTest(dispatcher) {
        val blocked = post("blocked", authorId = "author", slug = "old-slug"); val visible = post("visible", authorId = "other", slug = null)
        val release = CompletableDeferred<Unit>()
        val authors = FakeAuthors()
        val repo = FakeRepository().apply { feed = { _, cursor, _ ->
            if (cursor != null) { release.await(); Result.success(FeedPage(listOf(entry(blocked)), null)) }
            else Result.success(FeedPage(listOf(entry(blocked), entry(visible)), "next"))
        } }
        val vm = model(repo, authors = authors)
        assertEquals(2, vm.state.value.items.size)
        vm.send(CommunityAction.LoadMore(), this)
        authors.visibility.value = AuthorVisibilitySnapshot(1, blockedIds = setOf("author")); runCurrent()
        assertEquals(listOf("visible"), vm.state.value.items.map { it.id })
        release.complete(Unit); runCurrent()
        assertEquals(listOf("visible"), vm.state.value.items.map { it.id }); assertFalse(vm.state.value.isLoadingMore)
        authors.visibility.value = AuthorVisibilitySnapshot(2); runCurrent()
        assertEquals(2, vm.state.value.items.size); assertEquals(1, vm.state.value.page)
    }

    @Test fun postsOfBlockedAuthorsAreDroppedWhenAPageArrives() = runTest(dispatcher) {
        val authors = FakeAuthors().apply { visibility.value = AuthorVisibilitySnapshot(1, blockedSlugs = setOf("blocked-slug")) }
        val repo = FakeRepository().apply { feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(post("a", slug = "blocked-slug")), entry(post("b"))), null)) } }
        assertEquals(listOf("b"), model(repo, authors = authors).state.value.items.map { it.id })
    }
}
