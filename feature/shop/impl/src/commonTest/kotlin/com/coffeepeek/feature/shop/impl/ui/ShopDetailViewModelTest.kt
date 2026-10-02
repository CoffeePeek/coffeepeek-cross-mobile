package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReview
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopReviewVoteRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class DetailsRepository : ShopDetailsRepository {
        var calls = 0
        var result: Result<ShopDetails> = Result.success(details())
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun getDetails(shopId: String): Result<ShopDetails> {
            assertEquals("shop-1", shopId)
            calls++
            gate?.await()
            return result
        }
    }

    private class VoteRepository : ShopReviewVoteRepository {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        var result: Result<ShopHelpfulVote> = Result.success(ShopHelpfulVote(true, 7))
        override suspend fun setHelpful(reviewId: String, helpful: Boolean): Result<ShopHelpfulVote> {
            assertEquals("review-1", reviewId)
            assertTrue(helpful)
            calls++
            gate?.await()
            return result
        }
    }

    private fun viewModel(
        details: DetailsRepository = DetailsRepository(),
        votes: VoteRepository = VoteRepository(),
        viewer: ShopViewer = ShopViewer(true, "viewer"),
    ): ShopDetailViewModel = ShopDetailViewModel("shop-1", details, votes,
        currentViewer = { Result.success(viewer) }, currentDayOfWeek = { 1 })
        .also { store.put("detail", it) }

    @Test fun loadAndLocalExpansionAreIndependentOfNavigation() = runTest(dispatcher) {
        val repository = DetailsRepository()
        val viewModel = viewModel(details = repository)
        runCurrent()
        assertEquals(1, repository.calls)
        assertEquals("Coffee", viewModel.state.value.details?.overview?.title)
        assertEquals(1, viewModel.state.value.todayDayOfWeek)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.onAction(ShopDetailAction.ToggleSchedule)
        viewModel.onAction(ShopDetailAction.ToggleFeatures)
        runCurrent()
        assertTrue(viewModel.state.value.scheduleExpanded)
        assertTrue(viewModel.state.value.featuresExpanded)
        viewModel.onAction(ShopDetailAction.OpenMap)
        runCurrent()
        assertEquals(ShopDetailEvent.OpenMap(53.9, 27.5), viewModel.events.first())
    }

    @Test fun duplicateLoadsAndVotesAreGuardedWhilePending() = runTest(dispatcher) {
        val details = DetailsRepository().apply { gate = CompletableDeferred() }
        val votes = VoteRepository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(details, votes)
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertEquals(1, details.calls)
        details.gate!!.complete(Unit)
        runCurrent()
        viewModel.onAction(ShopDetailAction.VoteHelpful("review-1"))
        viewModel.onAction(ShopDetailAction.VoteHelpful("review-1"))
        runCurrent()
        assertEquals(1, votes.calls)
        assertEquals(setOf("review-1"), viewModel.state.value.pendingVoteIds)
        votes.gate!!.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.pendingVoteIds.isEmpty())
        assertEquals(7, viewModel.state.value.details?.reviews?.single()?.helpfulCount)
        assertTrue(viewModel.state.value.details?.reviews?.single()?.isHelpfulByCurrentUser == true)
    }

    @Test fun guestVotingRequestsSignInAndLoadFailureCanRetry() = runTest(dispatcher) {
        val repository = DetailsRepository().apply { result = Result.failure(IllegalStateException("hidden")) }
        val votes = VoteRepository()
        val viewModel = viewModel(repository, votes, viewer = ShopViewer(false, null))
        runCurrent()
        assertTrue(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.isLoading)
        repository.result = Result.success(details())
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertFalse(viewModel.state.value.hasError)
        viewModel.onAction(ShopDetailAction.VoteHelpful("review-1"))
        runCurrent()
        assertEquals(ShopDetailEvent.SignIn, viewModel.events.first())
        assertEquals(0, votes.calls)
    }

    @Test fun voteFailureEmitsEffectWithoutOptimisticallyChangingReview() = runTest(dispatcher) {
        val votes = VoteRepository().apply { result = Result.failure(IllegalStateException("hidden")) }
        val viewModel = viewModel(votes = votes)
        runCurrent()
        viewModel.onAction(ShopDetailAction.VoteHelpful("review-1"))
        runCurrent()
        assertEquals(ShopDetailEvent.VoteFailed, viewModel.events.first())
        assertEquals(0, viewModel.state.value.details?.reviews?.single()?.helpfulCount)
        assertTrue(viewModel.state.value.pendingVoteIds.isEmpty())
    }

    @Test fun ownReviewCannotBeVotedFor() = runTest(dispatcher) {
        val votes = VoteRepository()
        val viewModel = viewModel(votes = votes, viewer = ShopViewer(true, "author"))
        runCurrent()
        viewModel.onAction(ShopDetailAction.VoteHelpful("review-1"))
        runCurrent()
        assertEquals(0, votes.calls)
        assertEquals(0, viewModel.state.value.details?.reviews?.single()?.helpfulCount)
    }
}

private fun details() = ShopDetails(
    overview = ShopOverview("shop-1", "Coffee", null, null, 53.9, 27.5,
        null, 0, true, emptyList()),
    menu = null,
    schedules = emptyList(),
    coffee = ShopCoffeeDetails(),
    contact = null,
    features = emptyList(),
    reviews = listOf(ShopReview("review-1", null, "author", "shop-1", "Author", "", "",
        ShopRating(5, 5, 5), "", emptyList(), 0, false)),
    userCheckIns = emptyList<ShopCheckIn>(),
)
