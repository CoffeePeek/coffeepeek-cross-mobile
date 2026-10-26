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
    val calls = mutableListOf<LeaderboardPeriod>()
    var read: suspend (LeaderboardPeriod) -> Result<LeaderboardPage> = { period -> Result.success(page(period)) }
    override suspend fun load(period: LeaderboardPeriod): Result<LeaderboardPage> {
        calls += period
        return read(period)
    }
}

private fun row(id: String = "a", slug: String? = "maria") = LeaderboardEntry(1, id, "Maria", null, slug, 21, 3, 2, 5)

private fun page(period: LeaderboardPeriod = LeaderboardPeriod.Week) = LeaderboardPage(
    period, "2026-10-26", "2026-11-02", "Europe/Minsk", listOf(row()), 40, 1, 30, row(),
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
        val repository = FakeLeaderboardRepository().apply { read = { period ->
            if (period == LeaderboardPeriod.Week) withContext(NonCancellable) { pending.await() }
            Result.success(page(period))
        } }
        val vm = model(repository)
        runCurrent()
        vm.onAction(LeaderboardAction.SelectPeriod(LeaderboardPeriod.Month))
        runCurrent()
        assertEquals(LeaderboardPeriod.Month, vm.state.value.data?.period)
        pending.complete(Unit)
        runCurrent()
        assertEquals(LeaderboardPeriod.Month, vm.state.value.data?.period)
        assertFalse(vm.state.value.loadFailed)
    }

    @Test fun switchingPeriodClearsOldRowsAndOwnRankWhileTheNewRequestRuns() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        val pending = CompletableDeferred<Result<LeaderboardPage>>()
        repository.read = { _ -> pending.await() }
        vm.onAction(LeaderboardAction.SelectPeriod(LeaderboardPeriod.Month))
        runCurrent()
        assertNull(vm.state.value.data)
        assertTrue(vm.state.value.isLoading)
        pending.complete(Result.success(page(LeaderboardPeriod.Month)))
        runCurrent()
    }

    @Test fun limitsResultsToThirtyWhileKeepingOwnRankOutsideTheTop() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository().apply { read = { period ->
            Result.success(page(period).copy(items = (1..40).map { row("author-$it").copy(rank = it) },
                me = row("self").copy(rank = 41)))
        } }
        val vm = model(repository)
        runCurrent()
        assertEquals(30, vm.state.value.data?.items?.size)
        assertEquals("author-30", vm.state.value.data?.items?.last()?.authorId)
        assertEquals(41, vm.state.value.data?.me?.rank)
    }

    @Test fun failedRefreshKeepsLastResultAndRetriesTheCurrentPeriod() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository()
        val vm = model(repository)
        runCurrent()
        repository.read = { Result.failure(IllegalStateException("offline")) }
        vm.onAction(LeaderboardAction.Refresh)
        runCurrent()
        assertTrue(vm.state.value.loadFailed)
        assertEquals("a", vm.state.value.data?.items?.single()?.authorId)
        repository.read = { period -> Result.success(page(period).copy(items = listOf(row("updated")))) }
        vm.onAction(LeaderboardAction.Retry)
        runCurrent()
        assertFalse(vm.state.value.loadFailed)
        assertEquals("updated", vm.state.value.data?.items?.single()?.authorId)
        assertEquals(listOf(LeaderboardPeriod.Week, LeaderboardPeriod.Week, LeaderboardPeriod.Week), repository.calls)
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
        val repository = FakeLeaderboardRepository().apply { read = { period ->
            withContext(NonCancellable) { pending.await() }
            Result.success(page(period))
        } }
        val vm = model(repository, changes)
        runCurrent()
        vm.onAction(LeaderboardAction.SetExpanded(true))
        runCurrent()
        changes.emit(false)
        runCurrent()
        assertFalse(vm.state.value.expanded)
        repository.read = { period -> Result.success(page(period).copy(items = listOf(row("new-account")))) }
        changes.emit(true)
        runCurrent()
        pending.complete(Unit)
        runCurrent()
        assertEquals("new-account", vm.state.value.data?.items?.single()?.authorId)
    }

    @Test fun onlyServerPublicSlugsCanOpenProfilesAndOpeningClosesTheSheet() = runTest(dispatcher) {
        val repository = FakeLeaderboardRepository().apply { read = { period ->
            Result.success(page(period).copy(items = listOf(row(), row("hidden", null))))
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
        val repository = FakeLeaderboardRepository().apply { read = { _ -> Result.failure(IllegalStateException("offline")) } }
        val vm = model(repository)
        runCurrent()
        assertTrue(vm.state.value.loadFailed)
        repository.read = { _ -> throw CancellationException("cancelled") }
        vm.onAction(LeaderboardAction.Retry)
        runCurrent()
        assertFalse(vm.state.value.loadFailed)
        assertFalse(vm.state.value.isLoading)
        repository.read = { period -> Result.success(page(period)) }
        vm.onAction(LeaderboardAction.Refresh)
        runCurrent()
        assertNotNull(vm.state.value.data)
    }
}
