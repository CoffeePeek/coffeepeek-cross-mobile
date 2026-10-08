package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.domain.feature.feed.*
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.CheckInHelpfulVote
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.*

class PublicCommunityViewModelTest {
    private val visit = CheckIn("first", "same-shop", "Shop", "Coffee", "2020-01-01T00:00:00Z",
        visitedAt = "2019-01-01T00:00:00Z", rating = ReviewRating(4, 4, 5), username = "Me",
        authorAddress = PublicAddress("other", "/users/other", 1, false), visibility = CheckInVisibility.Public,
        moderationState = CheckInModerationState.Approved)
    private fun item(checkIn: CheckIn, published: String = "2026-10-07T10:00:00Z") = FeedItem(published, checkIn)

    @Test
    fun anonymousFeedPreservesPublicationOrderAndRepeatedVisitsWithoutLoadingPrivateHistory() = runBlocking {
        var historyRequests = 0
        var mutations = 0
        val repo = FeedCheckIns().apply {
            list = { _, _ -> historyRequests++; error("Private history must not load") }
            helpful = { _, _ -> mutations++; error("Guest cannot vote") }
        }
        val second = visit.copy(id = "second", createdAt = "2026-10-07T09:00:00Z")
        val feed = TestPublicFeed().apply { load = { size, cursor, filters ->
            assertEquals(20, size); assertNull(cursor); assertEquals(FeedFilters(), filters)
            Result.success(FeedPage(listOf(item(visit), item(second, "2026-10-07T09:00:00Z")), null))
        } }
        val vm = CommunityViewModel(repo, FeedSessions(null), FeedShops(), feed, FeedUsers())
        try {
            val state = vm.await { it.items.size == 2 && !it.isLoading }
            assertEquals(listOf("first", "second"), state.items.map { it.id })
            assertEquals("2026-10-07T10:00:00Z", state.publishedAt["first"])
            assertFalse(state.hasMore)
            assertFalse(state.owns(visit))
            assertEquals(0, historyRequests)
            vm.edit("first"); assertNull(vm.state.value.editing)
            vm.toggleHelpful("first"); assertTrue(vm.state.value.needsLogin)
            vm.loginHandled(); vm.selectTimeline(CommunityTimeline.Mine)
            assertEquals(CommunityTimeline.Public, vm.state.value.timeline)
            assertEquals(0, mutations)
        } finally { vm.close() }
    }

    @Test
    fun cursorRetryAppendsWithoutDuplicatesAndRefreshReplacesFromFirstPage() = runBlocking {
        val cursors = mutableListOf<String?>()
        var fail = true
        var refreshed = false
        val second = visit.copy(id = "second")
        val newest = visit.copy(id = "new-publication")
        val feed = TestPublicFeed().apply { load = { _, cursor, _ ->
            cursors += cursor
            when {
                cursor == null -> Result.success(FeedPage(listOf(item(if (refreshed) newest else visit)), if (refreshed) null else "next_-opaque"))
                fail -> { fail = false; Result.failure(IllegalStateException("Offline")) }
                else -> Result.success(FeedPage(listOf(item(visit.copy(note = "Current content")), item(second)), null))
            }
        } }
        val vm = CommunityViewModel(FeedCheckIns(), FeedSessions(null), FeedShops(), feed, FeedUsers())
        try {
            vm.await { it.page == 1 && !it.isLoading }
            vm.loadMore(); vm.await { it.error == "Offline" }
            assertEquals("next_-opaque", vm.state.value.nextCursor)
            vm.retry()
            val all = vm.await { it.page == 2 && !it.isLoadingMore }
            assertEquals(listOf("first", "second"), all.items.map { it.id })
            assertEquals("Current content", all.items.first().note)
            assertFalse(all.hasMore)
            refreshed = true; vm.refresh()
            vm.await { it.items.singleOrNull()?.id == "new-publication" && !it.isLoading }
            assertEquals(listOf(null, "next_-opaque", "next_-opaque", null), cursors)
        } finally { vm.close() }
    }

    @Test
    fun changingFiltersResetsCursorAndLateOldResponseCannotReplaceNewScope() = runBlocking {
        repeat(100) { checkFilterSwitch() }
    }

    private suspend fun checkFilterSwitch() {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val oldRequest = CompletableDeferred<Job>()
        val target = FeedFilters(citySlug = "city", coffeeShopSlug = "shop", authorSlug = "author")
        val feed = TestPublicFeed().apply { load = { _, cursor, filters ->
            if (filters == FeedFilters()) {
                oldRequest.complete(currentCoroutineContext().job)
                withContext(NonCancellable) {
                    started.complete(Unit); release.await()
                    Result.success(FeedPage(listOf(item(visit)), "wrong-scope"))
                }
            } else {
                assertEquals(target, filters); assertNull(cursor)
                Result.success(FeedPage(listOf(item(visit.copy(id = "filtered"))), null))
            }
        } }
        val vm = CommunityViewModel(FeedCheckIns(), FeedSessions(null), FeedShops(), feed, FeedUsers())
        try {
            withTimeout(5_000) { started.await() }
            vm.setFilters(target)
            vm.await { it.items.singleOrNull()?.id == "filtered" && !it.isLoading }
            release.complete(Unit); withTimeout(5_000) { oldRequest.await().join() }
            assertEquals("filtered", vm.state.value.items.single().id)
            assertNull(vm.state.value.nextCursor)
        } finally { release.complete(Unit); vm.close() }
    }

    @Test
    fun initialSessionLoadCannotRestartTimelineAlreadyChosenByTheUser() = runBlocking {
        val initialLoadPaused = CompletableDeferred<Unit>()
        val resumeInitialLoad = CompletableDeferred<Unit>()
        val initialized = CompletableDeferred<Unit>()
        val validations = MutableStateFlow(0)
        val sessionStore = FeedSessions(Session("token", userId = "one"))
        val sessions = object : SessionRepository by sessionStore {
            override fun observeSession() = flow {
                emit(sessionStore.peekSession())
                initialized.complete(Unit)
            }
            override fun isActiveSession(session: Session?): Boolean {
                if (validations.updateAndGet { it + 1 } == 2) {
                    initialLoadPaused.complete(Unit)
                    runBlocking { withTimeout(5_000) { resumeInitialLoad.await() } }
                }
                return sessionStore.isActiveSession(session)
            }
        }
        val historyRequests = MutableStateFlow(0)
        val repo = FeedCheckIns().apply { list = { _, _ ->
            historyRequests.updateAndGet { it + 1 }
            Result.success(PagedResult(listOf(visit), 1, 1, 1))
        } }
        val vm = CommunityViewModel(repo, sessions, FeedShops(), TestPublicFeed(), FeedUsers())
        try {
            withTimeout(5_000) { initialLoadPaused.await() }
            vm.selectTimeline(CommunityTimeline.Mine)
            vm.await { it.items == listOf(visit) && !it.isLoading }
            resumeInitialLoad.complete(Unit)
            withTimeout(5_000) { initialized.await() }
            val state = vm.state.value
            assertEquals(CommunityTimeline.Mine, state.timeline)
            assertFalse(state.isLoading)
            assertEquals(listOf(visit), state.items)
            assertEquals(1, historyRequests.value)
        } finally { resumeInitialLoad.complete(Unit); vm.close() }
    }

    @Test
    fun invalidCursorRetryStartsFreshWithSameFilters() = runBlocking {
        val requests = mutableListOf<String?>()
        val feed = TestPublicFeed().apply { load = { _, cursor, _ ->
            requests += cursor
            if (cursor == null) Result.success(FeedPage(listOf(item(visit)), "stale"))
            else Result.failure(FeedLoadException("Refresh needed", true))
        } }
        val vm = CommunityViewModel(FeedCheckIns(), FeedSessions(null), FeedShops(), feed, FeedUsers())
        try {
            vm.await { !it.isLoading && it.page == 1 }
            vm.loadMore(); vm.await { it.restartPagination && it.error != null }
            vm.retry(); vm.await { !it.isLoading && it.error == null }
            assertEquals(listOf(null, "stale", null), requests)
            assertEquals(1, vm.state.value.page)
        } finally { vm.close() }
    }

    @Test
    fun onlyCanonicalOwnerCanEditAndPendingVersionLeavesPublicFeedButRemainsInMine() = runBlocking {
        val own = visit.copy(id = "own", authorAddress = PublicAddress("me", "/users/me", 1, false))
        val pending = own.copy(note = "Edited", contentRevision = 2, moderationState = CheckInModerationState.Pending)
        var edits = 0
        val repo = FeedCheckIns().apply {
            update = { id, _ -> assertEquals("own", id); edits++; Result.success(pending) }
            list = { _, _ -> Result.success(PagedResult(listOf(pending), 1, 1, 1)) }
        }
        val feed = TestPublicFeed().apply { load = { _, _, _ -> Result.success(FeedPage(listOf(item(own), item(visit)), "next")) } }
        val vm = CommunityViewModel(repo, FeedSessions(Session("token", userId = "one")), FeedShops(), feed, FeedUsers())
        try {
            val state = vm.await { !it.isLoading && it.items.size == 2 && it.ownAuthorSlug == "me" }
            assertTrue(state.owns(own)); assertFalse(state.owns(visit)) // same username does not establish ownership
            vm.edit(visit.id); assertNull(vm.state.value.editing)
            vm.edit("own"); vm.updateEdit(vm.state.value.editing!!.copy(text = "Edited")); vm.saveEdit()
            val updated = vm.await { !it.isSaving && it.editing == null && it.items.size == 1 }
            assertEquals(listOf(visit), updated.items)
            assertFalse("own" in updated.publishedAt)
            assertEquals("next", updated.nextCursor)
            assertEquals(1, edits)
            vm.selectTimeline(CommunityTimeline.Mine)
            val mine = vm.await { it.items == listOf(pending) && !it.isLoading }
            assertTrue(mine.owns(pending)); assertEquals("На проверке", pending.publicationLabel())
        } finally { vm.close() }
    }

    @Test
    fun votesUseAuthoritativeCountsAndOwnVoteIsBlockedAndReportRetainsFailedDraft() = runBlocking {
        val own = visit.copy(id = "own", authorAddress = PublicAddress("me", "/users/me", 1, false))
        val voteStarted = CompletableDeferred<Unit>()
        val voteResult = CompletableDeferred<CheckInHelpfulVote>()
        var votes = 0
        var reports = 0
        val repo = FeedCheckIns().apply {
            helpful = { id, helpful ->
                assertEquals(visit.id, id); assertTrue(helpful); votes++; voteStarted.complete(Unit)
                Result.success(voteResult.await())
            }
            report = { id, text ->
                assertEquals(visit.id, id); assertEquals("Reason", text); reports++
                Result.failure(IllegalStateException("Try later"))
            }
        }
        val feed = TestPublicFeed().apply { load = { _, _, _ -> Result.success(FeedPage(listOf(item(own), item(visit)), null)) } }
        val vm = CommunityViewModel(repo, FeedSessions(Session("token", userId = "one")), FeedShops(), feed, FeedUsers())
        try {
            vm.await { !it.isLoading && it.items.size == 2 && it.ownAuthorSlug == "me" }
            vm.toggleHelpful("own"); assertEquals(0, votes)
            vm.toggleHelpful(visit.id); withTimeout(5_000) { voteStarted.await() }; vm.toggleHelpful(visit.id)
            assertEquals(1, votes)
            assertEquals(0, vm.state.value.items.last().helpfulCount)
            voteResult.complete(CheckInHelpfulVote(true, 8))
            vm.await { it.helpfulId == null && it.items.last().helpfulCount == 8 }
            assertTrue(vm.state.value.items.last().isHelpfulByCurrentUser)
            vm.openReport(visit.id); vm.submitReport()
            assertNotNull(vm.state.value.reportError); assertEquals(0, reports)
            vm.updateReport(" Reason "); vm.submitReport()
            val failed = vm.await { !it.isReporting && it.reportError == "Try later" }
            assertEquals(" Reason ", failed.reportText); assertEquals(visit.id, failed.reportingId)
            assertEquals(1, reports)
        } finally { voteResult.complete(CheckInHelpfulVote(true, 8)); vm.close() }
    }

    private suspend fun CommunityViewModel.await(predicate: (CommunityUiState) -> Boolean) = withTimeout(5_000) { state.first(predicate) }
}
