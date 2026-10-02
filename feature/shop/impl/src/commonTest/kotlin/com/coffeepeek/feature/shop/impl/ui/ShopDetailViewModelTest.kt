package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReview
import com.coffeepeek.feature.shop.domain.model.ShopRoaster
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopReviewVoteRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    private class Favorites : FavoritesRepository {
        var stored = emptyList<FavoriteShop>()
        var saves = 0
        var removes = 0
        var gate: CompletableDeferred<Unit>? = null
        var mutationResult: Result<Unit> = Result.success(Unit)
        var readResult: Result<List<FavoriteShop>>? = null
        var saved: FavoriteShop? = null

        override fun observe(): Flow<Result<List<FavoriteShop>>> = flowOf(Result.success(stored))
        override suspend fun read(): Result<List<FavoriteShop>> = readResult ?: Result.success(stored)
        override suspend fun save(shop: FavoriteShop): Result<Unit> {
            assertEquals("shop-1", shop.id)
            saved = shop
            saves++
            gate?.await()
            return mutationResult
        }
        override suspend fun remove(shopId: String): Result<Unit> {
            assertEquals("shop-1", shopId)
            removes++
            gate?.await()
            return mutationResult
        }
        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }

    private fun viewModel(
        details: DetailsRepository = DetailsRepository(),
        votes: VoteRepository = VoteRepository(),
        favorites: Favorites = Favorites(),
        viewer: ShopViewer = ShopViewer(true, "viewer"),
    ): ShopDetailViewModel = ShopDetailViewModel("shop-1", details, votes, favorites,
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

    @Test fun favoriteToggleUsesLocalRepositoryAndGuardsDuplicateMutation() = runTest(dispatcher) {
        val favorites = Favorites().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(favorites = favorites)
        runCurrent()
        assertTrue(viewModel.state.value.favoriteAvailable)
        assertFalse(viewModel.state.value.isFavorite)
        viewModel.onAction(ShopDetailAction.ToggleFavorite)
        viewModel.onAction(ShopDetailAction.ToggleFavorite)
        runCurrent()
        assertEquals(1, favorites.saves)
        assertEquals("$$", favorites.saved?.priceRange)
        assertEquals(listOf("Wi-Fi"), favorites.saved?.tags)
        assertEquals(listOf("Эспрессо"), favorites.saved?.brewMethods)
        assertEquals(listOf("https://photo/roaster"), favorites.saved?.roasterPhotoUrls)
        assertTrue(viewModel.state.value.isFavoriteLoading)
        favorites.gate!!.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.isFavorite)
        assertFalse(viewModel.state.value.isFavoriteLoading)
        assertEquals(ShopDetailEvent.FavoriteChanged("shop-1", true), viewModel.events.first())
        viewModel.onAction(ShopDetailAction.ToggleFavorite)
        runCurrent()
        assertEquals(1, favorites.removes)
        assertFalse(viewModel.state.value.isFavorite)
    }

    @Test fun favoriteFailureDoesNotChangeMembershipAndUnavailableStorageDisablesMutation() = runTest(dispatcher) {
        val favorites = Favorites().apply { mutationResult = Result.failure(IllegalStateException("secret")) }
        val viewModel = viewModel(favorites = favorites)
        runCurrent()
        viewModel.onAction(ShopDetailAction.ToggleFavorite)
        runCurrent()
        assertEquals(ShopDetailEvent.FavoriteFailed, viewModel.events.first())
        assertFalse(viewModel.state.value.isFavorite)
        favorites.readResult = Result.failure(IllegalStateException("unavailable"))
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertFalse(viewModel.state.value.favoriteAvailable)
        viewModel.onAction(ShopDetailAction.ToggleFavorite)
        runCurrent()
        assertEquals(1, favorites.saves)
    }

    @Test fun shopActionsEmitPlatformAgnosticEvents() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()

        viewModel.onAction(ShopDetailAction.Share)
        viewModel.onAction(ShopDetailAction.SuggestChange)
        viewModel.onAction(ShopDetailAction.OpenRoute)
        runCurrent()

        assertEquals(ShopDetailEvent.ShareShop("shop-1", "Coffee"), viewModel.events.first())
        assertEquals(ShopDetailEvent.SuggestChange("shop-1"), viewModel.events.first())
        assertEquals(ShopDetailEvent.OpenRoute(53.9, 27.5), viewModel.events.first())
    }

    @Test fun guestSuggestChangeRequiresSignIn() = runTest(dispatcher) {
        val viewModel = viewModel(viewer = ShopViewer(false, null))
        runCurrent()

        viewModel.onAction(ShopDetailAction.SuggestChange)
        runCurrent()

        assertEquals(ShopDetailEvent.SignIn, viewModel.events.first())
    }

    @Test fun guestDoesNotSeeAccountCheckInsFromDetailsResponse() = runTest(dispatcher) {
        val repository = DetailsRepository().apply {
            result = Result.success(details().copy(userCheckIns = listOf(
                ShopCheckIn("check-in-1", "someone", "shop-1", "private", "2026-10-02",
                    "2026-10-02", null, emptyList(), emptyList(), null),
            )))
        }
        val viewModel = viewModel(details = repository, viewer = ShopViewer(false, null))
        runCurrent()

        assertTrue(viewModel.state.value.details?.userCheckIns.isNullOrEmpty())
    }
}

private fun details() = ShopDetails(
    overview = ShopOverview("shop-1", "Coffee", null, null, 53.9, 27.5,
        null, 0, true, emptyList(), priceRange = "$$"),
    menu = null,
    schedules = emptyList(),
    coffee = ShopCoffeeDetails(roasters = listOf(ShopRoaster("roaster-1", "Roaster", "https://photo/roaster"))),
    contact = null,
    features = listOf(ShopFeature("Wi-Fi", "wifi", false), ShopFeature("Эспрессо", "espresso", true)),
    reviews = listOf(ShopReview("review-1", null, "author", "shop-1", "Author", "", "",
        ShopRating(5, 5, 5), "", emptyList(), 0, false)),
    userCheckIns = emptyList<ShopCheckIn>(),
)
