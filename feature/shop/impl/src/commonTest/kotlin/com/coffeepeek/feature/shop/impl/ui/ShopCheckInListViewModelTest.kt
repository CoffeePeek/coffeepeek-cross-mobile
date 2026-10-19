package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
import kotlin.test.assertTrue

/** Authored for the completion gate; only fake repositories, no app DI or HTTP. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShopCheckInListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Details : ShopDetailsRepository {
        var calls = 0
        var result = Result.success(details())
        var gate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false
        override suspend fun getDetails(shopId: String): Result<ShopDetails> {
            assertEquals("shop", shopId)
            calls++
            val response = result
            val pending = gate
            if (ignoreCancellation) withContext(NonCancellable) { pending?.await() } else pending?.await()
            return response
        }
    }

    private class Votes : ShopCheckInVoteRepository {
        val calls = mutableListOf<Pair<String, Boolean>>()
        var result = Result.success(ShopHelpfulVote(true, 7))
        var gate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false
        override suspend fun setHelpful(checkInId: String, helpful: Boolean): Result<ShopHelpfulVote> {
            calls += checkInId to helpful
            val response = result
            val pending = gate
            if (ignoreCancellation) withContext(NonCancellable) { pending?.await() } else pending?.await()
            return response
        }
    }

    private fun viewModel(
        details: Details = Details(),
        votes: Votes = Votes(),
        viewer: suspend () -> Result<ShopViewer> = { Result.success(ShopViewer(true, "viewer")) },
    ) = ShopCheckInListViewModel("shop", details, votes, viewer, 0).also { store.put("list", it) }

    @Test fun guestStateContainsOnlyFirstCardAndAggregateRatings() = runTest(dispatcher) {
        val viewModel = viewModel(viewer = { Result.success(ShopViewer(false, "stale-user")) })
        runCurrent()
        val state = viewModel.state.value
        assertEquals(listOf("first"), state.checkIns.map { it.id })
        assertEquals(2, state.hiddenCheckInCount)
        assertEquals(3, state.ratingSampleSize)
        assertEquals(4.0, state.ratingSummary?.coffee)
        assertTrue(state.ownCheckInIds.isEmpty())
        assertEquals(null, state.currentUserId)
        assertTrue(state.hasLoaded)
    }

    @Test fun authorNavigationUsesOnlyVisibleCanonicalAddressNotUserIdOrHiddenGuestRows() = runTest(dispatcher) {
        val repository = Details().apply { result = Result.success(details().let { it.copy(checkIns = it.checkIns.map { row ->
            row.copy(authorSlug = "public-${row.id}", username = "Author", authorAvatarUrl = "issued-avatar")
        }) }) }
        val model = viewModel(details = repository, viewer = { Result.success(ShopViewer(false, null)) })
        val events = mutableListOf<ShopCheckInListEvent>()
        backgroundScope.launch { model.events.collect { events += it.event } }
        runCurrent()
        model.onAction(ShopCheckInListAction.OpenAuthor("second"))
        model.onAction(ShopCheckInListAction.OpenAuthor("missing"))
        model.onAction(ShopCheckInListAction.OpenAuthor("first")); runCurrent()
        assertEquals(listOf<ShopCheckInListEvent>(ShopCheckInListEvent.OpenAuthor("public-first", "Author", "issued-avatar")), events)
        repository.result = Result.success(details()) // user IDs without public addresses are not routes.
        model.onAction(ShopCheckInListAction.Retry); runCurrent()
        model.onAction(ShopCheckInListAction.OpenAuthor("first")); runCurrent()
        assertEquals(1, events.size)
    }

    @Test fun blockRequestsUseOnlyVisiblePublicIdentityAndDoNotVote() = runTest(dispatcher) {
        val repository = Details().apply { result = Result.success(details().let { source -> source.copy(
            checkIns = source.checkIns.map { it.copy(authorId = "public-${it.id}", authorSlug = "slug-${it.id}", username = "Author") }) }) }
        val votes = Votes()
        val model = viewModel(repository, votes, viewer = { Result.success(ShopViewer(false, null)) })
        val events = mutableListOf<ShopCheckInListEvent>()
        backgroundScope.launch { model.events.collect { events += it.event } }
        runCurrent()
        model.onAction(ShopCheckInListAction.BlockAuthor("second"))
        model.onAction(ShopCheckInListAction.BlockAuthor("missing"))
        model.onAction(ShopCheckInListAction.BlockAuthor("first")); runCurrent()
        assertEquals(listOf<ShopCheckInListEvent>(ShopCheckInListEvent.BlockAuthor(
            com.coffeepeek.feature.shop.api.ShopCheckInAuthor("public-first", "slug-first", "Author", null))), events)
        assertTrue(votes.calls.isEmpty())
    }

    @Test fun blockingRejectsOwnersAndUnresolvedSignedInViewer() = runTest(dispatcher) {
        for (viewer in listOf(ShopViewer(true, null), ShopViewer(true, "viewer"))) {
            val repository = Details().apply { result = Result.success(details().let { source -> source.copy(
                checkIns = source.checkIns.map { it.copy(authorId = "public-${it.id}", authorSlug = "viewer") }) }) }
            val model = viewModel(repository, viewer = { Result.success(viewer) })
            val events = mutableListOf<ShopCheckInListEvent>()
            backgroundScope.launch { model.events.collect { events += it.event } }
            runCurrent()
            listOf("first", "second", "third").forEach { model.onAction(ShopCheckInListAction.BlockAuthor(it)) }
            runCurrent()
            assertTrue(events.isEmpty())
        }
    }

    @Test fun refreshingRowsCannotTriggerAnOldBlockRequest() = runTest(dispatcher) {
        val repository = Details().apply { result = Result.success(details().let { source -> source.copy(
            checkIns = source.checkIns.map { it.copy(authorSlug = "public-${it.id}") }) }) }
        val model = viewModel(repository)
        val events = mutableListOf<ShopCheckInListEvent>()
        backgroundScope.launch { model.events.collect { events += it.event } }
        runCurrent()
        repository.gate = CompletableDeferred()
        model.onAction(ShopCheckInListAction.Retry); runCurrent()
        model.onAction(ShopCheckInListAction.BlockAuthor("first")); runCurrent()
        assertTrue(events.isEmpty())
        repository.gate!!.complete(Unit); runCurrent()
        model.onAction(ShopCheckInListAction.BlockAuthor("first")); runCurrent()
        assertEquals(1, events.size)
    }

    @Test fun hiddenGuestActionsDoNotEmitEffectsOrMutate() = runTest(dispatcher) {
        val votes = Votes()
        val viewModel = viewModel(votes = votes, viewer = { Result.success(ShopViewer(false, null)) })
        val events = mutableListOf<ShopCheckInListEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it.event } }
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.OpenPhoto("second", 0))
        viewModel.onAction(ShopCheckInListAction.Report("second"))
        viewModel.onAction(ShopCheckInListAction.ToggleHelpful("second"))
        runCurrent()
        assertTrue(events.isEmpty())
        assertTrue(votes.calls.isEmpty())
        viewModel.onAction(ShopCheckInListAction.ToggleHelpful("first"))
        runCurrent()
        assertEquals(listOf<ShopCheckInListEvent>(ShopCheckInListEvent.SignIn), events)
    }

    @Test fun ownerIsRecognizedByUserOrPersonalHistoryId() = runTest(dispatcher) {
        val votes = Votes()
        val viewModel = viewModel(votes = votes)
        val events = mutableListOf<ShopCheckInListEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it.event } }
        runCurrent()
        listOf("second", "third").forEach {
            viewModel.onAction(ShopCheckInListAction.ToggleHelpful(it))
            viewModel.onAction(ShopCheckInListAction.Report(it))
        }
        runCurrent()
        assertTrue(votes.calls.isEmpty())
        assertTrue(events.isEmpty())
        viewModel.onAction(ShopCheckInListAction.Report("first"))
        runCurrent()
        assertEquals(listOf<ShopCheckInListEvent>(ShopCheckInListEvent.Report("first")), events)
    }

    @Test fun initialResumeDoesNotDuplicateLoadButReturnRefreshes() = runTest(dispatcher) {
        val details = Details()
        val viewModel = viewModel(details)
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.Resume)
        runCurrent()
        assertEquals(1, details.calls)
        viewModel.onAction(ShopCheckInListAction.Resume)
        runCurrent()
        assertEquals(2, details.calls)
    }

    @Test fun sameSessionNotificationDoesNotRepeatInitialRequest() = runTest(dispatcher) {
        val details = Details()
        val viewModel = viewModel(details)
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.SessionChanged(0))
        runCurrent()
        assertEquals(1, details.calls)
    }

    @Test fun duplicateLoadIsSuppressedAndInitialFailureCanRetry() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val details = Details().apply {
            this.gate = gate
            result = Result.failure(IllegalStateException("transport detail must not reach UI"))
        }
        val viewModel = viewModel(details)
        viewModel.onAction(ShopCheckInListAction.Retry)
        runCurrent()
        assertEquals(1, details.calls)
        gate.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.hasLoadError)
        assertFalse(viewModel.state.value.hasLoaded)
        details.result = Result.success(details())
        viewModel.onAction(ShopCheckInListAction.Retry)
        runCurrent()
        assertTrue(viewModel.state.value.hasLoaded)
        assertFalse(viewModel.state.value.hasLoadError)
    }

    @Test fun refreshFailureRetainsSameSessionContent() = runTest(dispatcher) {
        val details = Details()
        val viewModel = viewModel(details)
        runCurrent()
        details.result = Result.failure(IllegalStateException("offline"))
        viewModel.onAction(ShopCheckInListAction.Retry)
        runCurrent()
        assertTrue(viewModel.state.value.hasLoaded)
        assertTrue(viewModel.state.value.hasLoadError)
        assertEquals("Coffee", viewModel.state.value.shopTitle)
        assertEquals(3, viewModel.state.value.checkIns.size)
    }

    @Test fun duplicateVoteIsSuppressedAndPendingFlagIsCleared() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val votes = Votes().apply { this.gate = gate }
        val viewModel = viewModel(votes = votes)
        runCurrent()
        repeat(2) { viewModel.onAction(ShopCheckInListAction.ToggleHelpful("first")) }
        runCurrent()
        assertEquals(listOf("first" to true), votes.calls)
        assertEquals("first", viewModel.state.value.pendingVoteId)
        gate.complete(Unit)
        runCurrent()
        assertEquals(null, viewModel.state.value.pendingVoteId)
        assertEquals(7, viewModel.state.value.checkIns.first().helpfulCount)
        assertTrue(viewModel.state.value.checkIns.first().isHelpfulByCurrentUser)
    }

    @Test fun refreshWaitsForVoteAndRepeatedRequestsAreCoalesced() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val votes = Votes().apply { this.gate = gate }
        val details = Details()
        val viewModel = viewModel(details, votes)
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.ToggleHelpful("first"))
        runCurrent()
        repeat(3) { viewModel.onAction(ShopCheckInListAction.Retry) }
        runCurrent()
        assertEquals(1, details.calls)
        details.result = Result.success(details().let { snapshot -> snapshot.copy(checkIns = snapshot.checkIns.map {
            if (it.id == "first") it.copy(helpfulCount = 7, isHelpfulByCurrentUser = true) else it
        }) })
        gate.complete(Unit)
        runCurrent()
        assertEquals(2, details.calls)
        assertEquals(7, viewModel.state.value.checkIns.first().helpfulCount)
    }

    @Test fun voteFailureKeepsPreviousValueAndShowsLocalizedErrorFlag() = runTest(dispatcher) {
        val votes = Votes().apply { result = Result.failure(IllegalStateException("secret transport message")) }
        val viewModel = viewModel(votes = votes)
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.ToggleHelpful("first"))
        runCurrent()
        assertTrue(viewModel.state.value.hasVoteError)
        assertEquals(0, viewModel.state.value.checkIns.first().helpfulCount)
        assertEquals(null, viewModel.state.value.pendingVoteId)
    }

    @Test fun writeRefreshDuringAnOlderLoadFetchesThePostWriteSnapshotOnce() = runTest(dispatcher) {
        val details = Details().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(details)
        runCurrent()
        details.result = Result.success(details().copy(overview = details().overview.copy(title = "After write")))
        viewModel.onAction(ShopCheckInListAction.Refresh(1))
        viewModel.onAction(ShopCheckInListAction.Refresh(2))
        runCurrent()
        assertEquals(1, details.calls)
        details.gate!!.complete(Unit)
        runCurrent()
        assertEquals(2, details.calls)
        assertEquals("After write", viewModel.state.value.shopTitle)
        viewModel.onAction(ShopCheckInListAction.Refresh(2))
        runCurrent()
        assertEquals(2, details.calls)
    }

    @Test fun bufferedEffectsRetainTheSessionThatProducedThem() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.Report("first"))
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.SessionChanged(1))
        runCurrent()
        val oldEffect = viewModel.events.first()
        assertEquals(0, oldEffect.sessionGeneration)
        assertEquals(ShopCheckInListEvent.Report("first"), oldEffect.event)
        assertEquals(1, viewModel.state.value.sessionGeneration)
    }

    @Test fun oldNonCooperativeLoadCannotRestoreContentAfterSessionFailure() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val details = Details().apply { this.gate = gate; ignoreCancellation = true }
        var viewer = ShopViewer(true, "viewer")
        val viewModel = viewModel(details, viewer = { Result.success(viewer) })
        runCurrent()
        details.gate = null
        details.result = Result.failure(IllegalStateException("new session offline"))
        viewer = ShopViewer(false, null)
        viewModel.onAction(ShopCheckInListAction.SessionChanged(1))
        runCurrent()
        assertTrue(viewModel.state.value.checkIns.isEmpty())
        assertTrue(viewModel.state.value.hasLoadError)
        assertFalse(viewModel.state.value.hasLoaded)
        gate.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.checkIns.isEmpty())
        assertEquals(1, viewModel.state.value.sessionGeneration)
    }

    @Test fun oldNonCooperativeVoteCannotOverwriteNewAccountState() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val votes = Votes().apply {
            this.gate = gate
            ignoreCancellation = true
            result = Result.success(ShopHelpfulVote(true, 99))
        }
        var viewer = ShopViewer(true, "viewer")
        val viewModel = viewModel(votes = votes, viewer = { Result.success(viewer) })
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.ToggleHelpful("first"))
        runCurrent()
        viewer = ShopViewer(true, "replacement-account")
        viewModel.onAction(ShopCheckInListAction.SessionChanged(1))
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(0, viewModel.state.value.checkIns.first().helpfulCount)
        assertEquals(null, viewModel.state.value.pendingVoteId)
        assertEquals("replacement-account", viewModel.state.value.currentUserId)
    }

    @Test fun photoEffectUsesRepositoryAlbumAndValidatesIndex() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.OpenPhoto("first", 1))
        runCurrent()
        assertEquals(ShopCheckInListEvent.OpenPhoto(listOf("photo-a", "photo-b"), 1), viewModel.events.first().event)
        val events = mutableListOf<ShopCheckInListEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it.event } }
        viewModel.onAction(ShopCheckInListAction.OpenPhoto("first", 2))
        viewModel.onAction(ShopCheckInListAction.OpenPhoto("unknown", 0))
        runCurrent()
        assertTrue(events.isEmpty())
    }

    @Test fun galleryUsesDetailVariantsWithoutLosingExplicitOriginals() = runTest(dispatcher) {
        val details = Details()
        val snapshot = details.result.getOrThrow()
        details.result = Result.success(snapshot.copy(checkIns = snapshot.checkIns.map {
            if (it.id == "first") it.copy(photoDetailUrls = listOf("detail-a", "detail-b")) else it
        }))
        val model = viewModel(details = details)
        runCurrent()
        model.onAction(ShopCheckInListAction.OpenPhoto("first", 1))
        runCurrent()
        assertEquals(ShopCheckInListEvent.OpenPhoto(listOf("detail-a", "detail-b"), 1,
            listOf("photo-a", "photo-b")), model.events.first().event)
    }

    @Test fun resultCancellationIsNotConvertedToLoadOrVoteError() = runTest(dispatcher) {
        val details = Details().apply { result = Result.failure(CancellationException("cancelled")) }
        val votes = Votes().apply { result = Result.failure(CancellationException("cancelled")) }
        val viewModel = viewModel(details, votes)
        runCurrent()
        assertFalse(viewModel.state.value.hasLoadError)
        assertFalse(viewModel.state.value.isLoading)
        details.result = Result.success(details())
        viewModel.onAction(ShopCheckInListAction.Retry)
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.ToggleHelpful("first"))
        runCurrent()
        assertFalse(viewModel.state.value.hasVoteError)
        assertEquals(null, viewModel.state.value.pendingVoteId)
    }

    @Test fun cancelledVoteDoesNotLaunchDeferredRefreshFromItsFinallyBlock() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val details = Details()
        val votes = Votes().apply { this.gate = gate; result = Result.failure(CancellationException("cancelled")) }
        val viewModel = viewModel(details, votes)
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.ToggleHelpful("first"))
        runCurrent()
        viewModel.onAction(ShopCheckInListAction.Retry)
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, details.calls)
        assertEquals(null, viewModel.state.value.pendingVoteId)
        assertFalse(viewModel.state.value.hasVoteError)
    }

    private companion object {
        fun checkIn(id: String, userId: String, rating: Int) = ShopCheckIn(
            id = id, userId = userId, shopId = "shop", note = "visit-$id",
            createdAt = "2026-10-01T12:00:00Z", visitedAt = "2026-10-01T12:00:00Z", reviewId = null,
            photoUrls = listOf("photo-a", "photo-b"), photoThumbnailUrls = emptyList(),
            rating = ShopRating(rating, rating, rating), visibility = ShopCheckInVisibility.Public,
        )

        fun details(): ShopDetails {
            val checkIns = listOf(checkIn("first", "other", 5), checkIn("second", "viewer", 4),
                checkIn("third", "legacy-owner-id", 3))
            return ShopDetails(
                overview = ShopOverview(id = "shop", title = "Coffee", description = null, address = null,
                    latitude = null, longitude = null, rating = 4.0, reviewCount = 3, isOpen = true, photos = emptyList()),
                menu = null, schedules = emptyList(), coffee = ShopCoffeeDetails(), contact = null,
                features = emptyList(), reviews = emptyList(), userCheckIns = listOf(checkIns.last()), checkIns = checkIns,
            )
        }
    }
}
