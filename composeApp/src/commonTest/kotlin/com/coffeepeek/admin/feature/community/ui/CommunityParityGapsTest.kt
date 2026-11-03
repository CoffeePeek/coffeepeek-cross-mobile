@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.domain.feature.feed.*
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.CheckInHelpfulVote
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.*

/** Pins the behaviour of the legacy feed that the older tests did not reach (inventory section 1, A4/A6/B1-B3/B6/C3/C5). */
class CommunityParityGapsTest {
    private val publicationLabels = listOf("Приватный", "На проверке", "Опубликован", "Отклонён", "Публичный", "Статус проверки неизвестен")

    private val post = CheckIn("post", "shop", "Shop", "Coffee", "2020-01-01T00:00:00Z",
        visitedAt = "2019-01-01T00:00:00Z", rating = ReviewRating(4, 4, 5), username = "Other",
        authorAddress = PublicAddress("other", "/users/other", 1, false), visibility = CheckInVisibility.Public,
        moderationState = CheckInModerationState.Approved)
    private val ownPost = post.copy(id = "own", authorAddress = PublicAddress("me", "/users/me", 1, false))
    private fun item(checkIn: CheckIn) = FeedItem("2026-10-07T10:00:00Z", checkIn)
    private val signedIn get() = FeedSessions(Session("token", userId = "one"))

    private suspend fun CommunityViewModel.await(predicate: (CommunityUiState) -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { state.first(predicate) } }

    private suspend fun TestScope.startAnalytics(): RecordingTransport {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity("fixture-owner")
        service.screenShown("feed", "fixture-visit")
        Analytics.install(service)
        return sdk
    }

    private fun RecordingTransport.feedActions() = events.filter { it.first == "feed_action_clicked" }.map { it.second["action"] to it.second["trigger"] }
    private fun RecordingTransport.feedItems() = events.filter { it.first == "feed_item_interacted" }.map { it.second["action"] to it.second["check_in_kind"] }

    @Test fun feedActionsAreTrackedWithTheirTriggerOnThePublicTimelineOnly() = runTest {
        val sdk = startAnalytics()
        var fail = true
        val feed = TestPublicFeed().apply { load = { _, cursor, _ ->
            when {
                cursor == null -> Result.success(FeedPage(listOf(item(post)), "next"))
                fail -> { fail = false; Result.failure(IllegalStateException("Offline")) }
                else -> Result.success(FeedPage(listOf(item(post.copy(id = "second"))), null))
            }
        } }
        val vm = CommunityViewModel(FeedCheckIns(), signedIn, FeedShops(), feed, FeedUsers())
        try {
            vm.await { it.page == 1 && !it.isLoading }
            vm.refresh("gesture"); vm.await { it.page == 1 && !it.isLoading }
            vm.refresh() // no trigger: nothing is reported
            vm.await { it.page == 1 && !it.isLoading }
            vm.loadMore(); vm.await { it.failedPage == 2 && !it.isLoadingMore }
            vm.retry(); vm.await { it.page == 2 && !it.isLoadingMore }
            runCurrent()
            // The page-2 retry goes through loadMore without a second load_more event.
            assertEquals(listOf("refresh" to "gesture", "load_more" to "scroll", "retry" to "button"), sdk.feedActions())
            vm.selectTimeline(CommunityTimeline.Mine); vm.await { it.timeline == CommunityTimeline.Mine && !it.isLoading }
            vm.refresh("gesture"); vm.await { !it.isLoading }
            runCurrent()
            assertEquals(3, sdk.feedActions().size) // the personal timeline reports no feed events
        } finally { vm.close() }
    }

    @Test fun likeAndUnlikeReportTheCheckInKindAndAGuestGetsTheGateAndTheSignInRequest() = runTest {
        val sdk = startAnalytics()
        val home = post.copy(id = "home", isHome = true)
        var helpful = false
        val repo = FeedCheckIns().apply { this.helpful = { _, value -> helpful = value; Result.success(CheckInHelpfulVote(value, if (value) 1 else 0)) } }
        val feed = TestPublicFeed().apply { load = { _, _, _ -> Result.success(FeedPage(listOf(item(post), item(home)), null)) } }
        val vm = CommunityViewModel(repo, signedIn, FeedShops(), feed, FeedUsers())
        try {
            vm.await { it.items.size == 2 && !it.isLoading }
            vm.toggleHelpful("post"); vm.await { it.helpfulId == null && it.items.first().isHelpfulByCurrentUser }
            vm.toggleHelpful("post"); vm.await { it.helpfulId == null && !it.items.first().isHelpfulByCurrentUser }
            vm.toggleHelpful("home"); vm.await { it.helpfulId == null && it.items.last().isHelpfulByCurrentUser }
            vm.itemAction("shop", post); vm.itemAction("photo", home)
            runCurrent()
            assertEquals(listOf("like" to "shop", "unlike" to "shop", "like" to "home", "shop" to "shop", "photo" to "home"), sdk.feedItems())
            assertTrue(helpful)
        } finally { vm.close() }

        val guestRepo = FeedCheckIns().apply { this.helpful = { _, _ -> error("A guest cannot vote") } }
        val guest = CommunityViewModel(guestRepo, FeedSessions(null), FeedShops(), feed, FeedUsers())
        try {
            guest.await { it.items.size == 2 && !it.isLoading }
            guest.toggleHelpful("post")
            runCurrent()
            assertTrue(guest.state.value.needsLogin)
            assertTrue(sdk.events.any { it.first == "auth_gate_shown" && it.second["feature"] == "feed_like" })
            guest.loginHandled(); assertFalse(guest.state.value.needsLogin)
            guest.selectTimeline(CommunityTimeline.Mine)
            assertTrue(guest.state.value.needsLogin); assertEquals(CommunityTimeline.Public, guest.state.value.timeline)
        } finally { guest.close() }
    }

    @Test fun publicFeedWaitsForTheVisibilitySnapshotAndAFailedRefreshShowsTheBlocksMessageWithoutRequestingPosts() = runTest {
        var requests = 0
        var refreshes = 0
        var succeed = false
        val visibility = object : AuthorVisibility {
            override val visibility = MutableStateFlow(AuthorVisibilitySnapshot(ready = false))
            override suspend fun refresh(): Result<Unit> {
                refreshes++
                return if (succeed) { visibility.value = AuthorVisibilitySnapshot(1); Result.success(Unit) } else Result.failure(IllegalStateException("Blocks unavailable"))
            }
        }
        val feed = TestPublicFeed().apply { load = { _, _, _ -> requests++; Result.success(FeedPage(listOf(item(post)), null)) } }
        val vm = CommunityViewModel(FeedCheckIns(), signedIn, FeedShops(), feed, FeedUsers(), visibility)
        try {
            val failed = vm.await { it.error != null && !it.isLoading }
            assertEquals("Не удалось загрузить блокировки. Повторите попытку.", failed.error)
            assertEquals(0, requests)
            succeed = true
            vm.retry(); vm.await { it.items.size == 1 && !it.isLoading }
            // Quirk: the snapshot change that the refresh publishes also restarts the load, so the posts may be requested twice.
            assertTrue(requests in 1..2); assertEquals(2, refreshes)
        } finally { vm.close() }
    }

    @Test fun aFailedFirstPageReportsTheFeedFailureAndRetryRefreshes() = runTest {
        var attempts = 0
        val feed = TestPublicFeed().apply { load = { _, _, _ ->
            if (attempts++ == 0) Result.failure(IllegalStateException("Offline")) else Result.success(FeedPage(listOf(item(post)), null))
        } }
        val vm = CommunityViewModel(FeedCheckIns(), FeedSessions(null), FeedShops(), feed, FeedUsers())
        try {
            val failed = vm.await { it.error != null && !it.isLoading }
            assertEquals("Не удалось загрузить ленту", failed.error); assertEquals(1, failed.failedPage); assertFalse(failed.restartPagination)
            vm.retry(); vm.await { it.items.size == 1 && it.error == null }
        } finally { vm.close() }
    }

    @Test fun reportTextIsCutAtTheLimitBlankIsRejectedAndSuccessClosesTheSheetWithAMessage() = runTest {
        val sent = mutableListOf<Pair<String, String>>()
        val repo = FeedCheckIns().apply { report = { id, text -> sent += id to text; Result.success(Unit) } }
        val feed = TestPublicFeed().apply { load = { _, _, _ -> Result.success(FeedPage(listOf(item(post), item(ownPost)), null)) } }
        val vm = CommunityViewModel(repo, signedIn, FeedShops(), feed, FeedUsers())
        try {
            vm.await { it.items.size == 2 && it.ownAuthorSlug == "me" }
            vm.openReport("own"); assertNull(vm.state.value.reportingId) // an own post cannot be reported
            vm.openReport("post"); assertEquals("post", vm.state.value.reportingId)
            vm.updateReport("x".repeat(2500)); assertEquals(2000, vm.state.value.reportText.length)
            vm.updateReport("   "); vm.submitReport()
            assertEquals("Опишите проблему: от 1 до 2000 символов", vm.state.value.reportError); assertTrue(sent.isEmpty())
            vm.updateReport("y".repeat(2000)); assertNull(vm.state.value.reportError)
            vm.submitReport()
            val done = vm.await { !it.isReporting && it.reportingId == null }
            assertEquals("Жалоба отправлена", done.actionMessage); assertEquals("", done.reportText)
            assertEquals(listOf("post" to "y".repeat(2000)), sent)
            vm.clearActionMessage(); assertNull(vm.state.value.actionMessage)
        } finally { vm.close() }
    }

    @Test fun anOwnPublicPostMadePrivateLeavesTheFeedAndAFailedChangeKeepsItWithTheServerMessage() = runTest {
        val release = CompletableDeferred<Unit>()
        var fail = true
        val repo = FeedCheckIns().apply { visibility = { id, next ->
            release.await()
            if (fail) { fail = false; Result.failure(IllegalStateException("Server said no")) }
            else Result.success(ownPost.copy(id = id, visibility = next, moderationState = CheckInModerationState.NotSubmitted))
        } }
        val feed = TestPublicFeed().apply { load = { _, _, _ -> Result.success(FeedPage(listOf(item(ownPost), item(post)), null)) } }
        val vm = CommunityViewModel(repo, signedIn, FeedShops(), feed, FeedUsers())
        try {
            vm.await { it.items.size == 2 && it.ownAuthorSlug == "me" }
            vm.toggleVisibility("post"); assertNull(vm.state.value.changingVisibilityId) // not own
            vm.toggleVisibility("own"); vm.await { it.changingVisibilityId == "own" }
            // While a mutation runs, refresh, filters, timeline, edit and a second mutation are ignored (B6).
            vm.refresh(); vm.setFilters(FeedFilters(citySlug = "x")); vm.selectTimeline(CommunityTimeline.Mine); vm.edit("own"); vm.toggleHelpful("post")
            assertEquals(FeedFilters(), vm.state.value.filters); assertEquals(CommunityTimeline.Public, vm.state.value.timeline)
            assertNull(vm.state.value.editing); assertNull(vm.state.value.helpfulId)
            release.complete(Unit)
            val failed = vm.await { it.changingVisibilityId == null && it.actionMessage != null }
            assertEquals("Server said no", failed.actionMessage); assertEquals(2, failed.items.size)
            vm.toggleVisibility("own")
            val hidden = vm.await { it.changingVisibilityId == null && it.items.size == 1 }
            assertEquals(listOf("post"), hidden.items.map { it.id })
        } finally { release.complete(Unit); vm.close() }
    }

    @Test fun visibilityFailureWithoutAMessageUsesTheDefaultText() = runTest {
        val repo = FeedCheckIns().apply { visibility = { _, _ -> Result.failure(IllegalStateException()) } }
        val feed = TestPublicFeed().apply { load = { _, _, _ -> Result.success(FeedPage(listOf(item(ownPost)), null)) } }
        val vm = CommunityViewModel(repo, signedIn, FeedShops(), feed, FeedUsers())
        try {
            vm.await { it.items.size == 1 && it.ownAuthorSlug == "me" }
            vm.toggleVisibility("own")
            assertEquals("Не удалось изменить видимость", vm.await { it.actionMessage != null }.actionMessage)
        } finally { vm.close() }
    }

    @Test fun editingAnItemOfAnotherAuthorOrWhileReportingIsIgnoredAndCancelClosesTheEditor() = runTest {
        val feed = TestPublicFeed().apply { load = { _, _, _ -> Result.success(FeedPage(listOf(item(ownPost), item(post)), null)) } }
        val vm = CommunityViewModel(FeedCheckIns(), signedIn, FeedShops(), feed, FeedUsers())
        try {
            vm.await { it.items.size == 2 && it.ownAuthorSlug == "me" }
            vm.openReport("post")
            vm.edit("own"); assertNull(vm.state.value.editing) // a report sheet is open
            vm.dismissReport()
            vm.edit("own"); assertEquals("own", vm.state.value.editing?.source?.id)
            vm.dismissEdit(); assertNull(vm.state.value.editing)
        } finally { vm.close() }
    }

    @Test fun publicationLabelsFollowTheVisibilityAndModerationState() {
        fun label(visibility: CheckInVisibility, state: CheckInModerationState) = post.copy(visibility = visibility, moderationState = state).publicationLabel(publicationLabels)
        assertEquals("Приватный", label(CheckInVisibility.Private, CheckInModerationState.Approved))
        assertEquals("На проверке", label(CheckInVisibility.Public, CheckInModerationState.Pending))
        assertEquals("Опубликован", label(CheckInVisibility.Public, CheckInModerationState.Approved))
        assertEquals("Отклонён", label(CheckInVisibility.Public, CheckInModerationState.Rejected))
        assertEquals("Публичный", label(CheckInVisibility.Public, CheckInModerationState.NotSubmitted))
        assertEquals("Статус проверки неизвестен", label(CheckInVisibility.Public, CheckInModerationState.Unknown))
    }

    @Test fun publicationLabelsUsePreparedBelarusianCopy() {
        val labels = listOf("Прыватны", "На праверцы", "Апублікаваны", "Адхілены", "Публічны", "Статус праверкі невядомы")
        assertEquals("На праверцы", post.copy(visibility = CheckInVisibility.Public, moderationState = CheckInModerationState.Pending).publicationLabel(labels))
    }
}
