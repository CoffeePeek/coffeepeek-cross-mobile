package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.api.ShopDetailContext
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
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopFavorites
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
            val response = result
            gate?.await()
            return response
        }
    }

    private class CheckInVotes : ShopCheckInVoteRepository {
        val calls = mutableListOf<Pair<String, Boolean>>()
        var gate: CompletableDeferred<Unit>? = null
        var result: Result<ShopHelpfulVote> = Result.success(ShopHelpfulVote(true, 8))
        override suspend fun setHelpful(checkInId: String, helpful: Boolean): Result<ShopHelpfulVote> {
            calls += checkInId to helpful
            gate?.await()
            return result
        }
    }

    private class Favorites : ShopFavorites {
        val snapshots = MutableStateFlow(Result.success(emptySet<String>()))
        var saves = 0
        var removes = 0
        var gate: CompletableDeferred<Unit>? = null
        var mutationResult: Result<Unit> = Result.success(Unit)
        var saved: ShopDetails? = null

        override fun observeIds(): Flow<Result<Set<String>>> = snapshots
        override suspend fun save(shop: ShopDetails): Result<Unit> {
            assertEquals("shop-1", shop.overview.id)
            saved = shop
            saves++
            gate?.await()
            if (mutationResult.isSuccess) snapshots.value = Result.success(
                snapshots.value.getOrThrow() + shop.overview.id,
            )
            return mutationResult
        }
        override suspend fun remove(shopId: String): Result<Unit> {
            assertEquals("shop-1", shopId)
            removes++
            gate?.await()
            if (mutationResult.isSuccess) snapshots.value = Result.success(
                snapshots.value.getOrThrow() - shopId,
            )
            return mutationResult
        }
    }

    private fun viewModel(
        details: DetailsRepository = DetailsRepository(),
        favorites: Favorites = Favorites(),
        viewer: ShopViewer = ShopViewer(true, "viewer"),
        checkInVotes: CheckInVotes = CheckInVotes(),
        viewerProvider: suspend () -> Result<ShopViewer> = { Result.success(viewer) },
        dayProvider: () -> Int = { 1 },
        minuteProvider: () -> Int? = { null },
    ): ShopDetailViewModel = ShopDetailViewModel(
        shopId = "shop-1", detailsRepository = details,
        checkInVoteRepository = checkInVotes, favoritesRepository = favorites,
        currentViewer = viewerProvider, currentDayOfWeek = dayProvider, currentMinuteOfDay = minuteProvider)
        .also { store.put("detail", it) }

    @Test fun checkInGalleryKeepsExplicitOriginalAndRejectsGuestHiddenRows() = runTest(dispatcher) {
        val repository = DetailsRepository()
        val row = ShopCheckIn("visit", "author", "shop-1", "", "", "", null,
            listOf("/signed-original"), listOf("/feed"), null, photoDetailUrls = listOf("/detail"))
        repository.result = Result.success(details().copy(checkIns = listOf(row, row.copy(id = "hidden"))))
        val model = viewModel(details = repository, viewer = ShopViewer(false, null))
        val events = mutableListOf<ShopDetailEvent>()
        backgroundScope.launch { model.events.collect { events += it.event } }
        runCurrent()
        model.onAction(ShopDetailAction.OpenCheckInPhoto("hidden", 0))
        model.onAction(ShopDetailAction.OpenCheckInPhoto("visit", 1))
        runCurrent()
        assertTrue(events.isEmpty())
        model.onAction(ShopDetailAction.OpenCheckInPhoto("visit", 0))
        runCurrent()
        assertEquals<List<ShopDetailEvent>>(listOf(ShopDetailEvent.OpenPhoto(listOf("/detail"), 0, listOf("/signed-original"))), events)
    }

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
        assertEquals(ShopDetailEvent.OpenMap(53.9, 27.5, "Coffee"), viewModel.events.first().event)
    }

    @Test fun publicAuthorUsesCanonicalVisibleRowAndDoesNotExposeHiddenGuestAuthors() = runTest(dispatcher) {
        val repository = DetailsRepository().apply { result = Result.success(details().copy(checkIns = listOf(
            checkIn().copy(id = "visible", authorSlug = "canonical", username = "Author", authorAvatarUrl = "issued-avatar"),
            checkIn().copy(id = "hidden", authorSlug = "hidden-author"),
        ))) }
        val model = viewModel(details = repository, viewer = ShopViewer(false, null))
        val events = mutableListOf<ShopDetailEvent>()
        backgroundScope.launch { model.events.collect { events += it.event } }
        runCurrent()
        model.onAction(ShopDetailAction.OpenAuthor("hidden"))
        model.onAction(ShopDetailAction.OpenAuthor("missing"))
        model.onAction(ShopDetailAction.OpenAuthor("visible")); runCurrent()
        assertEquals(listOf<ShopDetailEvent>(ShopDetailEvent.OpenAuthor("canonical", "Author", "issued-avatar")), events)
        repository.result = Result.success(details().copy(checkIns = listOf(checkIn().copy(id = "visible"))))
        model.onAction(ShopDetailAction.Retry); runCurrent()
        model.onAction(ShopDetailAction.OpenAuthor("visible")); runCurrent()
        assertEquals(1, events.size)
    }

    @Test fun duplicateLoadsAreGuardedWhilePending() = runTest(dispatcher) {
        val details = DetailsRepository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(details)
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertEquals(1, details.calls)
        details.gate!!.complete(Unit)
        runCurrent()
    }

    @Test fun localClockIsUpdatedOnReturnAndScheduleExpansionWithoutAnotherFirstLoad() = runTest(dispatcher) {
        var day = 1
        var minute = 600
        val repository = DetailsRepository()
        val viewModel = viewModel(repository, dayProvider = { day }, minuteProvider = { minute })
        runCurrent()
        assertEquals(600, viewModel.state.value.todayMinuteOfDay)
        day = 2
        minute = 540
        viewModel.onAction(ShopDetailAction.Resume)
        runCurrent()
        assertEquals(1, repository.calls)
        assertEquals(2, viewModel.state.value.todayDayOfWeek)
        assertEquals(540, viewModel.state.value.todayMinuteOfDay)
        minute = 720
        viewModel.onAction(ShopDetailAction.ToggleSchedule)
        runCurrent()
        assertEquals(720, viewModel.state.value.todayMinuteOfDay)
        assertTrue(viewModel.state.value.scheduleExpanded)
    }

    @Test fun featuresStayExpandedOnSameSnapshotButCollapseWhenTheListChanges() = runTest(dispatcher) {
        val repository = DetailsRepository()
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onAction(ShopDetailAction.ToggleFeatures)
        runCurrent()
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertTrue(viewModel.state.value.featuresExpanded)
        repository.result = Result.success(repository.result.getOrThrow().copy(
            features = listOf(ShopFeature("Выпечка", "bakery", false))))
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertFalse(viewModel.state.value.featuresExpanded)
    }

    @Test fun loadFailureCanRetry() = runTest(dispatcher) {
        val repository = DetailsRepository().apply { result = Result.failure(IllegalStateException("hidden")) }
        val viewModel = viewModel(repository, viewer = ShopViewer(false, null))
        runCurrent()
        assertTrue(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.isLoading)
        repository.result = Result.success(details())
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertFalse(viewModel.state.value.hasError)
    }

    @Test fun refreshFailureKeepsLoadedContentAndCanRetry() = runTest(dispatcher) {
        val repository = DetailsRepository()
        val viewModel = viewModel(details = repository)
        runCurrent()
        val loaded = viewModel.state.value.details
        repository.result = Result.failure(IllegalStateException("refresh failed"))
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertEquals(loaded, viewModel.state.value.details)
        assertTrue(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.isLoading)
        repository.result = Result.success(details())
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertFalse(viewModel.state.value.hasError)
        assertEquals(3, repository.calls)
    }

    @Test fun sessionFailureDoesNotRestorePreviousAccountsPrivateDetails() = runTest(dispatcher) {
        val repository = DetailsRepository().apply { result = Result.success(details().copy(userCheckIns = listOf(checkIn()))) }
        val viewModel = viewModel(details = repository)
        runCurrent()
        assertTrue(viewModel.state.value.details!!.userCheckIns.isNotEmpty())
        repository.result = Result.failure(IllegalStateException("session refresh failed"))
        viewModel.onAction(ShopDetailAction.SessionChanged(1))
        runCurrent()
        assertEquals(null, viewModel.state.value.details)
        assertTrue(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.isLoggedIn)
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
        assertEquals(details(), favorites.saved)
        assertTrue(viewModel.state.value.isFavoriteLoading)
        favorites.gate!!.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.isFavorite)
        assertFalse(viewModel.state.value.isFavoriteLoading)
        assertEquals(ShopDetailEvent.FavoriteChanged("shop-1", true), viewModel.events.first().event)
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
        assertEquals(ShopDetailEvent.FavoriteFailed, viewModel.events.first().event)
        assertFalse(viewModel.state.value.isFavorite)
        favorites.snapshots.value = Result.failure(IllegalStateException("unavailable"))
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertFalse(viewModel.state.value.favoriteAvailable)
        viewModel.onAction(ShopDetailAction.ToggleFavorite)
        runCurrent()
        assertEquals(1, favorites.saves)
    }

    @Test fun externalFavoriteChangesAndLogoutClearAreObservedWithoutReloadingDetails() = runTest(dispatcher) {
        val favorites = Favorites()
        val repository = DetailsRepository()
        val viewModel = viewModel(details = repository, favorites = favorites)
        runCurrent()
        favorites.snapshots.value = Result.success(setOf(details().overview.id))
        runCurrent()
        assertTrue(viewModel.state.value.isFavorite)
        favorites.snapshots.value = Result.success(emptySet())
        runCurrent()
        assertFalse(viewModel.state.value.isFavorite)
        assertEquals(1, repository.calls)
        assertEquals(0, favorites.saves)
        assertEquals(0, favorites.removes)
        store.clear()
        runCurrent()
        assertEquals(0, favorites.snapshots.subscriptionCount.value)
    }

    @Test fun observationFailureDisablesWritesAndRecoveryRestoresAuthoritativeMembership() = runTest(dispatcher) {
        val favorites = Favorites()
        val viewModel = viewModel(favorites = favorites)
        runCurrent()
        favorites.snapshots.value = Result.success(setOf(details().overview.id))
        runCurrent()
        favorites.snapshots.value = Result.failure(IllegalStateException("read failure"))
        runCurrent()
        assertTrue(viewModel.state.value.isFavorite)
        assertFalse(viewModel.state.value.favoriteAvailable)
        viewModel.onAction(ShopDetailAction.ToggleFavorite)
        runCurrent()
        assertEquals(0, favorites.removes)
        favorites.snapshots.value = Result.success(emptySet())
        runCurrent()
        assertFalse(viewModel.state.value.isFavorite)
        assertTrue(viewModel.state.value.favoriteAvailable)
    }

    @Test fun detailResponseCannotOverwriteFavoriteChangesObservedDuringLoading() = runTest(dispatcher) {
        val favorites = Favorites()
        val repository = DetailsRepository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(details = repository, favorites = favorites)
        runCurrent()
        favorites.snapshots.value = Result.success(setOf(details().overview.id))
        runCurrent()
        repository.gate!!.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.isFavorite)
        assertEquals(1, favorites.snapshots.subscriptionCount.value)
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        assertEquals(1, favorites.snapshots.subscriptionCount.value)
    }

    @Test fun shopActionsEmitPlatformAgnosticEvents() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()

        viewModel.onAction(ShopDetailAction.Share)
        viewModel.onAction(ShopDetailAction.SuggestChange)
        viewModel.onAction(ShopDetailAction.OpenRoute)
        runCurrent()

        assertEquals(ShopDetailEvent.ShareShop("/shops/coffee", "Coffee"), viewModel.events.first().event)
        assertEquals(ShopDetailEvent.SuggestChange(ShopDetailContext("shop-1", "shop-1", "Coffee")), viewModel.events.first().event)
        assertEquals(ShopDetailEvent.OpenRoute(53.9, 27.5), viewModel.events.first().event)
    }

    @Test fun guestSuggestChangeRequiresSignIn() = runTest(dispatcher) {
        val viewModel = viewModel(viewer = ShopViewer(false, null))
        runCurrent()

        viewModel.onAction(ShopDetailAction.SuggestChange)
        runCurrent()

        assertEquals(ShopDetailEvent.SignIn, viewModel.events.first().event)
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

    @Test fun checkInVoteUsesVisitRepositoryAndGuardsAllVotesUntilCompletion() = runTest(dispatcher) {
        val visits = CheckInVotes().apply { gate = CompletableDeferred() }
        val repository = DetailsRepository().apply {
            result = Result.success(details().copy(checkIns = listOf(checkIn(), checkIn().copy(id = "visit-2"))))
        }
        val viewModel = viewModel(details = repository, checkInVotes = visits)
        runCurrent()
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit-2"))
        runCurrent()
        assertEquals(listOf("visit" to true), visits.calls)
        assertEquals("visit", viewModel.state.value.pendingCheckInVoteId)
        visits.gate!!.complete(Unit)
        runCurrent()
        val voted = viewModel.state.value.details!!.checkIns.first()
        assertTrue(voted.isHelpfulByCurrentUser)
        assertEquals(8, voted.helpfulCount)
        assertEquals(null, viewModel.state.value.pendingCheckInVoteId)
        visits.result = Result.success(ShopHelpfulVote(false, 7))
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
        runCurrent()
        assertEquals("visit" to false, visits.calls.last())
        assertFalse(viewModel.state.value.details!!.checkIns.first().isHelpfulByCurrentUser)
    }

    @Test fun voteFailureKeepsCheckInAndResetsPendingState() = runTest(dispatcher) {
        val visits = CheckInVotes().apply { result = Result.failure(IllegalStateException("rejected")) }
        val repository = DetailsRepository().apply { result = Result.success(details().copy(checkIns = listOf(checkIn()))) }
        val viewModel = viewModel(details = repository, checkInVotes = visits)
        runCurrent()
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
        runCurrent()
        assertEquals(ShopDetailEvent.VoteFailed, viewModel.events.first().event)
        assertEquals(checkIn(), viewModel.state.value.details!!.checkIns.single())
        assertEquals(null, viewModel.state.value.pendingCheckInVoteId)
    }

    @Test fun ownCheckInGuardsUseAuthorAndPersonalVisitIdsWhenIdentityIsMissing() = runTest(dispatcher) {
        for (viewer in listOf(ShopViewer(true, "author"), ShopViewer(true, null))) {
            val visits = CheckInVotes()
            val repository = DetailsRepository().apply { result = Result.success(details().copy(
                checkIns = listOf(checkIn()), userCheckIns = listOf(checkIn()),
            )) }
            val viewModel = viewModel(details = repository, checkInVotes = visits, viewer = viewer)
            runCurrent()
            viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
            viewModel.onAction(ShopDetailAction.ReportCheckIn("visit"))
            runCurrent()
            assertTrue(visits.calls.isEmpty())
            // Back must be the next event: owner report emitted nothing.
            viewModel.onAction(ShopDetailAction.Back)
            runCurrent()
            assertEquals(ShopDetailEvent.Back, viewModel.events.first().event)
        }
    }

    @Test fun checkInNavigationDoesNotDependOnReviewEligibility() = runTest(dispatcher) {
        val repository = DetailsRepository().apply { result = Result.success(details().copy(checkIns = listOf(checkIn()))) }
        val viewModel = viewModel(details = repository)
        runCurrent()
        viewModel.onAction(ShopDetailAction.OpenCheckIn)
        viewModel.onAction(ShopDetailAction.OpenCheckIns)
        viewModel.onAction(ShopDetailAction.ReportCheckIn("visit"))
        runCurrent()
        assertEquals(ShopDetailEvent.CreateCheckIn(ShopDetailContext("shop-1", "shop-1", "Coffee")), viewModel.events.first().event)
        assertEquals(ShopDetailEvent.OpenCheckIns("shop-1"), viewModel.events.first().event)
        assertEquals(ShopDetailEvent.ReportCheckIn("visit"), viewModel.events.first().event)
    }

    @Test fun guestCheckInMutationsRequestSignInWithoutCallingRepository() = runTest(dispatcher) {
        val visits = CheckInVotes()
        val repository = DetailsRepository().apply { result = Result.success(details().copy(checkIns = listOf(checkIn()))) }
        val viewModel = viewModel(details = repository, checkInVotes = visits, viewer = ShopViewer(false, null))
        runCurrent()
        viewModel.onAction(ShopDetailAction.OpenCheckIn)
        viewModel.onAction(ShopDetailAction.ReportCheckIn("visit"))
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
        runCurrent()
        repeat(3) { assertEquals(ShopDetailEvent.SignIn, viewModel.events.first().event) }
        assertTrue(visits.calls.isEmpty())
    }

    @Test fun resumeSkipsInitialDuplicateAndRefreshesWhenReturning() = runTest(dispatcher) {
        val repository = DetailsRepository()
        val viewModel = viewModel(details = repository)
        viewModel.onAction(ShopDetailAction.Resume)
        runCurrent()
        assertEquals(1, repository.calls)
        repository.result = Result.success(details().copy(overview = details().overview.copy(title = "Updated")))
        viewModel.onAction(ShopDetailAction.Resume)
        runCurrent()
        assertEquals(2, repository.calls)
        assertEquals("Updated", viewModel.state.value.details!!.overview.title)
    }

    @Test fun sessionChangeCancelsPreviousLoadAndClearsPrivateDetailsBeforeRefresh() = runTest(dispatcher) {
        val repository = DetailsRepository().apply { result = Result.success(details().copy(userCheckIns = listOf(checkIn()))) }
        var viewer = ShopViewer(true, "viewer")
        val viewModel = viewModel(details = repository, viewerProvider = { Result.success(viewer) })
        runCurrent()
        assertEquals(1, viewModel.state.value.details!!.userCheckIns.size)
        val oldGate = CompletableDeferred<Unit>()
        repository.gate = oldGate
        viewModel.onAction(ShopDetailAction.Retry)
        runCurrent()
        viewer = ShopViewer(false, null)
        repository.gate = CompletableDeferred()
        viewModel.onAction(ShopDetailAction.SessionChanged(1))
        runCurrent()
        assertEquals(null, viewModel.state.value.details)
        assertFalse(viewModel.state.value.isLoggedIn)
        assertTrue(viewModel.state.value.isLoading)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.details!!.userCheckIns.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        oldGate.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.details!!.userCheckIns.isEmpty())
    }

    @Test fun refreshSignalsDuringVoteAreCoalescedAndPreserveTheServerVote() = runTest(dispatcher) {
        val votes = CheckInVotes().apply { gate = CompletableDeferred() }
        val repository = DetailsRepository().apply { result = Result.success(details().copy(checkIns = listOf(checkIn()))) }
        val viewModel = viewModel(details = repository, checkInVotes = votes)
        runCurrent()
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
        runCurrent()
        viewModel.onAction(ShopDetailAction.Refresh(1))
        viewModel.onAction(ShopDetailAction.Refresh(2))
        runCurrent()
        assertEquals(1, repository.calls)
        repository.result = Result.success(details().copy(checkIns = listOf(
            checkIn().copy(helpfulCount = 8, isHelpfulByCurrentUser = true))))
        votes.gate!!.complete(Unit)
        runCurrent()
        assertEquals(2, repository.calls)
        assertEquals(8, viewModel.state.value.details!!.checkIns.single().helpfulCount)
        assertTrue(viewModel.state.value.details!!.checkIns.single().isHelpfulByCurrentUser)
        viewModel.onAction(ShopDetailAction.Refresh(2))
        runCurrent()
        assertEquals(2, repository.calls)
    }

    @Test fun postWriteSignalDuringLoadRequestsANewerSnapshotAfterTheOldReply() = runTest(dispatcher) {
        val repository = DetailsRepository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(repository)
        runCurrent()
        repository.result = Result.success(details().copy(overview = details().overview.copy(title = "After write")))
        viewModel.onAction(ShopDetailAction.Refresh(1))
        viewModel.onAction(ShopDetailAction.Refresh(2))
        runCurrent()
        assertEquals(1, repository.calls)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertEquals(2, repository.calls)
        assertEquals("After write", viewModel.state.value.details!!.overview.title)
    }

    @Test fun sessionGenerationIsIdempotentAndAnOlderSignalCannotRestoreTheAccount() = runTest(dispatcher) {
        val repository = DetailsRepository()
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onAction(ShopDetailAction.SessionChanged(2))
        runCurrent()
        viewModel.onAction(ShopDetailAction.SessionChanged(2))
        viewModel.onAction(ShopDetailAction.SessionChanged(1))
        runCurrent()
        assertEquals(2, repository.calls)
        assertEquals(2, viewModel.state.value.sessionGeneration)
    }

    @Test fun formContextKeepsCallerIdentitySeparateFromTheLoadedPublicSlug() = runTest(dispatcher) {
        val repository = DetailsRepository().apply {
            result = Result.success(details().copy(overview = details().overview.copy(id = "public-coffee-slug")))
        }
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onAction(ShopDetailAction.OpenCheckIn)
        runCurrent()
        val envelope = viewModel.events.first()
        assertEquals(0, envelope.sessionGeneration)
        assertEquals(ShopDetailEvent.CreateCheckIn(ShopDetailContext("shop-1", "public-coffee-slug", "Coffee")), envelope.event)
    }

    @Test fun missingCanonicalPathDoesNotInventAShareLinkFromTheId() = runTest(dispatcher) {
        val repository = DetailsRepository().apply {
            result = Result.success(details().copy(overview = details().overview.copy(canonicalPath = null)))
        }
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onAction(ShopDetailAction.Share)
        viewModel.onAction(ShopDetailAction.Back)
        runCurrent()
        assertEquals(ShopDetailEvent.Back, viewModel.events.first().event)
    }

    @Test fun voteReplyFromPreviousSessionCannotMutateNewSessionOrEmitFailure() = runTest(dispatcher) {
        val visits = CheckInVotes().apply { gate = CompletableDeferred() }
        val repository = DetailsRepository().apply { result = Result.success(details().copy(checkIns = listOf(checkIn()))) }
        var viewer = ShopViewer(true, "viewer")
        val viewModel = viewModel(details = repository, checkInVotes = visits, viewerProvider = { Result.success(viewer) })
        runCurrent()
        viewModel.onAction(ShopDetailAction.VoteCheckInHelpful("visit"))
        runCurrent()
        viewer = ShopViewer(false, null)
        viewModel.onAction(ShopDetailAction.SessionChanged(1))
        runCurrent()
        visits.gate!!.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.details!!.checkIns.single().isHelpfulByCurrentUser)
        assertEquals(null, viewModel.state.value.pendingCheckInVoteId)
    }
}

private fun checkIn() = ShopCheckIn("visit", "author", "shop-1", "Coffee", "2026-10-01", "2026-10-01",
    null, emptyList(), emptyList(), ShopRating(4, 4, 4))

private fun details() = ShopDetails(
    overview = ShopOverview("shop-1", "Coffee", null, null, 53.9, 27.5,
        null, 0, true, emptyList(), priceRange = "$", canonicalPath = "/shops/coffee"),
    menu = null,
    schedules = emptyList(),
    coffee = ShopCoffeeDetails(roasters = listOf(ShopRoaster("roaster-1", "Roaster", "https://photo/roaster"))),
    contact = null,
    features = listOf(ShopFeature("Wi-Fi", "wifi", false), ShopFeature("Эспрессо", "espresso", true)),
    reviews = listOf(ShopReview("review-1", null, "author", "shop-1", "Author", "", "",
        ShopRating(5, 5, 5), "", emptyList(), 0, false)),
    userCheckIns = emptyList<ShopCheckIn>(),
)
