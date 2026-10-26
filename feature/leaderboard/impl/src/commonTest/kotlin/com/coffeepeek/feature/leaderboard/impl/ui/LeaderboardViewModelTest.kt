package com.coffeepeek.feature.leaderboard.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.leaderboard.domain.model.*
import com.coffeepeek.feature.leaderboard.domain.repository.LeaderboardRepository
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardAction
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

private class FakeLeaderboardRepository : LeaderboardRepository {
    val calls = mutableListOf<Triple<LeaderboardPeriod, Int, String?>>()
    var read: suspend (LeaderboardPeriod, Int) -> Result<LeaderboardPage> = { period, number -> Result.success(page(period, number)) }
    override suspend fun load(period: LeaderboardPeriod, page: Int, date: String?): Result<LeaderboardPage> {
        calls += Triple(period, page, date)
        return read(period, page)
    }
}

private fun row(id: String = "a", slug: String? = "maria") = LeaderboardEntry(1, id, "Maria", null, slug, 21, 3, 2, 5)

private fun page(period: LeaderboardPeriod = LeaderboardPeriod.Week, page: Int = 1) = LeaderboardPage(
    period, "2026-10-26", "2026-11-02", "Europe/Minsk", listOf(row()), 21, page, 20, row(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var key = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repository: FakeLeaderboardRepository = FakeLeaderboardRepository(), changes: Flow<Boolean> = flowOf(true)) =
        LeaderboardViewModel(repository, changes).also { store.put("leaderboard:${key++}", it) }

    private fun TestScope.events(vm: LeaderboardViewModel): List<LeaderboardEvent> = mutableListOf<LeaderboardEvent>().also { list ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { list += it } }
    }

    @Test fun initializesOnlyOnceAndIgnoresReselectingTheCurrentPeriod() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        vm.onAction(LeaderboardAction.ObserveContext)
        vm.onAction(LeaderboardAction.SelectPeriod(LeaderboardPeriod.Week))
        runCurrent()
        assertEquals(1, repository.calls.size)
        assertEquals(21, vm.state.value.data?.me?.points)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun delayedCancelledWeekCannotOverwriteMonth() = runTest(dispatcher) {
        val pending = CompletableDeferred<Unit>()
        val repository = FakeLeaderboardRepository().apply { read = { period, number ->
            if (period == LeaderboardPeriod.Week) withContext(NonCancellable) { pending.await() }
            Result.success(page(period, number))
        } }
        val vm = model(repository)
        runCurrent()
        vm.onAction(LeaderboardAction.SelectPeriod(LeaderboardPeriod.Month))
        runCurrent()
        assertEquals(LeaderboardPeriod.Month, vm.state.value.data?.period)
        pending.complete(Unit)
        runCurrent()
        assertEquals(LeaderboardPeriod.Month, vm.state.value.data?.period)
        assertNull(vm.state.value.failedPage)
    }

    @Test fun switchingPeriodClearsOldRowsAndOwnRankWhileTheNewRequestRuns() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        val pending = CompletableDeferred<Result<LeaderboardPage>>()
        repository.read = { _, _ -> pending.await() }
        vm.onAction(LeaderboardAction.SelectPeriod(LeaderboardPeriod.Month))
        runCurrent()
        assertNull(vm.state.value.data)
        assertTrue(vm.state.value.isLoading)
        pending.complete(Result.success(page(LeaderboardPeriod.Month)))
        runCurrent()
    }

    @Test fun duplicatePagingIsGuardedAndDateIsPinnedAndAuthorsAreDeduplicated() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        val pending = CompletableDeferred<Result<LeaderboardPage>>()
        repository.read = { _, _ -> pending.await() }
        repeat(3) { vm.onAction(LeaderboardAction.LoadMore) }
        runCurrent()
        assertEquals(2, repository.calls.size)
        assertEquals("2026-10-26", repository.calls.last().third)
        pending.complete(Result.success(page(page = 2).copy(items = listOf(row(), row("b")))))
        runCurrent()
        assertEquals(listOf("a", "b"), vm.state.value.data?.items?.map { it.authorId })
        vm.onAction(LeaderboardAction.LoadMore)
        runCurrent()
        assertEquals(2, repository.calls.size)
    }

    @Test fun failedNextPageKeepsRowsAndRetriesExactlyThatPage() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        repository.read = { _, _ -> Result.failure(IllegalStateException("offline")) }
        vm.onAction(LeaderboardAction.LoadMore)
        runCurrent()
        assertEquals(1, vm.state.value.data?.items?.size)
        assertEquals(2, vm.state.value.failedPage)
        repository.read = { period, number -> Result.success(page(period, number).copy(items = listOf(row("b")))) }
        vm.onAction(LeaderboardAction.Retry)
        runCurrent()
        assertEquals(listOf(1, 2, 2), repository.calls.map { it.second })
        assertEquals(2, vm.state.value.data?.items?.size)
        assertNull(vm.state.value.failedPage)
    }

    @Test fun refreshStartsAtTheCurrentServerPeriodAndReplacesAllPages() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        vm.onAction(LeaderboardAction.LoadMore)
        runCurrent()
        repository.read = { period, number -> Result.success(page(period, number).copy(startsOn = "2026-11-02")) }
        vm.onAction(LeaderboardAction.Refresh)
        runCurrent()
        assertEquals(1, repository.calls.last().second)
        assertNull(repository.calls.last().third)
        assertEquals("2026-11-02", vm.state.value.data?.startsOn)
    }

    @Test fun aPageFromAnotherCalendarPeriodIsAnErrorAndCannotMixRows() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        repository.read = { period, number -> Result.success(page(period, number).copy(startsOn = "2026-11-02")) }
        vm.onAction(LeaderboardAction.LoadMore)
        runCurrent()
        assertEquals("2026-10-26", vm.state.value.data?.startsOn)
        assertEquals(1, vm.state.value.data?.page)
        assertEquals(2, vm.state.value.failedPage)
    }

    @Test fun privacyContextClearsOwnRankClosesSheetAndWaitsUntilReady() = runTest(dispatcher) {
        val changes = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(true) }
        val repository = FakeLeaderboardRepository()
        val vm = model(repository, changes)
        runCurrent()
        vm.onAction(LeaderboardAction.SetExpanded(true))
        runCurrent()
        changes.emit(false)
        runCurrent()
        assertNull(vm.state.value.data)
        assertFalse(vm.state.value.expanded)
        assertEquals(1, repository.calls.size)
        changes.emit(true)
        runCurrent()
        assertEquals(2, repository.calls.size)
        assertNotNull(vm.state.value.data)
    }

    @Test fun aNewReadyContextInvalidatesResultsEvenWhenReadinessDoesNotChange() = runTest(dispatcher) {
        val changes = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(true) }
        val repository = FakeLeaderboardRepository()
        val vm = model(repository, changes)
        runCurrent()
        val generation = vm.state.value.generation
        changes.emit(true)
        runCurrent()
        assertEquals(2, repository.calls.size)
        assertTrue(vm.state.value.generation > generation)
    }

    @Test fun privacyInvalidationDoesNotWaitForANonCooperativeOldRequest() = runTest(dispatcher) {
        val pending = CompletableDeferred<Unit>()
        val changes = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(true) }
        val repository = FakeLeaderboardRepository().apply { read = { period, number ->
            withContext(NonCancellable) { pending.await() }
            Result.success(page(period, number))
        } }
        val vm = model(repository, changes)
        runCurrent()
        vm.onAction(LeaderboardAction.SetExpanded(true))
        runCurrent()
        changes.emit(false)
        runCurrent()
        assertFalse(vm.state.value.expanded)
        repository.read = { period, number -> Result.success(page(period, number).copy(items = listOf(row("new-account")))) }
        changes.emit(true)
        runCurrent()
        pending.complete(Unit)
        runCurrent()
        assertEquals("new-account", vm.state.value.data?.items?.single()?.authorId)
    }

    @Test fun onlyServerPublicSlugsCanOpenProfilesAndOpeningClosesTheSheet() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository().apply { read = { period, number ->
            Result.success(page(period, number).copy(items = listOf(row(), row("hidden", null))))
        } }
        val vm = model(repository)
        val events = events(vm)
        runCurrent()
        vm.onAction(LeaderboardAction.OpenProfile("unknown"))
        vm.onAction(LeaderboardAction.OpenProfile("hidden"))
        vm.onAction(LeaderboardAction.SetExpanded(true))
        vm.onAction(LeaderboardAction.OpenProfile("a"))
        runCurrent()
        assertEquals(listOf("maria"), events.map { it.slug })
        assertEquals(vm.state.value.generation, events.single().generation)
        assertFalse(vm.state.value.expanded)
    }

    @Test fun initialFailureIsRetryableAndCancellationIsNotRenderedAsFailure() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository().apply { read = { _, _ -> Result.failure(IllegalStateException("offline")) } }
        val vm = model(repository)
        runCurrent()
        assertEquals(1, vm.state.value.failedPage)
        repository.read = { _, _ -> throw CancellationException("cancelled") }
        vm.onAction(LeaderboardAction.Retry)
        runCurrent()
        assertNull(vm.state.value.failedPage)
        assertFalse(vm.state.value.isLoading)
        repository.read = { period, number -> Result.success(page(period, number)) }
        vm.onAction(LeaderboardAction.Refresh)
        runCurrent()
        assertNotNull(vm.state.value.data)
    }
}
