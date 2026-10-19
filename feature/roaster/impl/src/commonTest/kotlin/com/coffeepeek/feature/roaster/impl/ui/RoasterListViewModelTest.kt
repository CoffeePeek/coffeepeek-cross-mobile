package com.coffeepeek.feature.roaster.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoasterListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(
        repository: FakeRoasterRepository = FakeRoasterRepository(),
        favorites: FakeFavorites = FakeFavorites(),
        analytics: RecordingListAnalytics = RecordingListAnalytics(),
    ) = RoasterListViewModel(repository, favorites, analytics).also { store.put("list-${nextKey++}", it) }

    private fun TestScope.events(model: RoasterListViewModel): List<RoasterListEvent> =
        mutableListOf<RoasterListEvent>().also { list -> backgroundScope.launch { model.events.collect { list += it } } }

    @Test fun initializeLoadsCatalogAndFavoritesOnceAndIsIdempotent() = runTest(dispatcher) {
        val repository = FakeRoasterRepository()
        val favorites = FakeFavorites().apply { keys.value = setOf("b") }
        val model = model(repository, favorites); runCurrent()
        assertEquals(listOf("a", "b"), model.state.value.items.map { it.slug })
        assertFalse(model.state.value.isLoading)
        assertEquals(setOf("b"), model.state.value.favoriteKeys)
        repeat(3) { model.onAction(RoasterListAction.Initialize) }; runCurrent()
        assertEquals(1, repository.catalogCalls)
        favorites.keys.value = setOf("a", "b"); runCurrent()
        assertEquals(setOf("a", "b"), model.state.value.favoriteKeys)
    }

    @Test fun filtersMatchLegacyRulesAndTagsAreDistinctAndSorted() = runTest(dispatcher) {
        val repository = FakeRoasterRepository().apply { catalog = { Result.success(listOf(
            summary("a", "Alpha Roast", listOf(tag("light", 2), tag("decaf", 1))),
            summary("b", "Beta", listOf(tag("light", 2))),
            summary(null, "No address", listOf(tag("decaf", 1))),
        )) } }
        val favorites = FakeFavorites().apply { keys.value = setOf("b") }
        val model = model(repository, favorites); runCurrent()
        assertEquals(listOf("decaf", "light"), model.state.value.availableTags.map { it.slug })
        model.onAction(RoasterListAction.QueryChanged("  ROAST ")); runCurrent()
        assertEquals(listOf("a"), model.state.value.visibleItems.map { it.slug })
        model.onAction(RoasterListAction.QueryChanged("")); model.onAction(RoasterListAction.ToggleTag("light")); runCurrent()
        assertEquals(listOf("a", "b"), model.state.value.visibleItems.map { it.slug })
        model.onAction(RoasterListAction.ToggleTag("decaf")); runCurrent()
        assertEquals(listOf("a"), model.state.value.visibleItems.map { it.slug })
        model.onAction(RoasterListAction.ClearTags); model.onAction(RoasterListAction.FavoritesOnlyChanged(true)); runCurrent()
        assertEquals(listOf("b"), model.state.value.visibleItems.map { it.slug })
        model.onAction(RoasterListAction.FavoritesOnlyChanged(false)); model.onAction(RoasterListAction.SelectedRoastersChanged(setOf("a"))); runCurrent()
        assertEquals(listOf("a"), model.state.value.visibleItems.map { it.slug })
    }

    @Test fun resultsAreReportedWhenSettledAndOnlyForChangedCriteria() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRoasterRepository().apply { catalog = { gate.await(); Result.success(listOf(summary("a"), summary("b"))) } }
        val analytics = RecordingListAnalytics()
        val model = model(repository, analytics = analytics); runCurrent()
        assertTrue(analytics.calls.isEmpty()) // still loading
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("shown:||false|:[]:2:false"), analytics.calls)
        model.onAction(RoasterListAction.QueryChanged(" B ")); runCurrent()
        model.onAction(RoasterListAction.FavoritesOnlyChanged(true)); runCurrent()
        assertEquals("shown:b||false|:[]:1:false", analytics.calls[1])
        assertEquals("shown:b||true|:[favoritesOnly]:0:false", analytics.calls[2])
    }

    @Test fun failureIsReportedAndRetryRecovers() = runTest(dispatcher) {
        var fail = true
        val repository = FakeRoasterRepository().apply { catalog = {
            if (fail) Result.failure(IllegalStateException("private")) else Result.success(listOf(summary("a")))
        } }
        val analytics = RecordingListAnalytics()
        val model = model(repository, analytics = analytics); runCurrent()
        assertTrue(model.state.value.loadFailed); assertFalse(model.state.value.isLoading)
        assertEquals(listOf("shown:||false|:[]:0:true"), analytics.calls)
        fail = false
        model.onAction(RoasterListAction.Refresh); runCurrent()
        assertFalse(model.state.value.loadFailed)
        assertEquals(listOf("a"), model.state.value.items.map { it.slug })
    }

    @Test fun staleCatalogResultCannotOverwriteNewerRefresh() = runTest(dispatcher) {
        val oldGate = CompletableDeferred<Unit>()
        var call = 0
        val repository = FakeRoasterRepository().apply { catalog = {
            if (++call == 1) { withContext(NonCancellable) { oldGate.await() }; Result.success(listOf(summary("old"))) }
            else Result.success(listOf(summary("new")))
        } }
        val model = model(repository); runCurrent()
        model.onAction(RoasterListAction.Refresh); runCurrent()
        oldGate.complete(Unit); runCurrent()
        assertEquals(listOf("new"), model.state.value.items.map { it.slug })
        assertFalse(model.state.value.isLoading)
    }

    @Test fun openReportsOneBasedVisiblePositionAndIgnoresUnknownSlugs() = runTest(dispatcher) {
        val analytics = RecordingListAnalytics()
        val model = model(analytics = analytics); val events = events(model); runCurrent()
        analytics.calls.clear()
        model.onAction(RoasterListAction.QueryChanged("b")); runCurrent(); analytics.calls.clear()
        model.onAction(RoasterListAction.OpenRoaster("a")) // filtered out
        model.onAction(RoasterListAction.OpenRoaster("")); model.onAction(RoasterListAction.OpenRoaster("b")); runCurrent()
        assertEquals(listOf("open:1"), analytics.calls)
        assertEquals(listOf<RoasterListEvent>(RoasterListEvent.OpenRoaster("b")), events)
    }

    @Test fun detailsLoadLazilyOncePerSlugWithAtMostFourConcurrentRequests() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var running = 0; var peak = 0
        val repository = FakeRoasterRepository().apply {
            catalog = { Result.success((1..8).map { summary("r$it") }) }
            detail = { slug -> running++; peak = maxOf(peak, running); gate.await(); running--; Result.success(details(slug)) }
        }
        val model = model(repository); runCurrent()
        (1..8).forEach { model.onAction(RoasterListAction.LoadDetails("r$it")) }
        repeat(3) { model.onAction(RoasterListAction.LoadDetails("r1")) }
        runCurrent()
        assertEquals(4, peak)
        gate.complete(Unit); runCurrent()
        assertEquals((1..8).map { "r$it" }, repository.detailCalls.sorted().sortedBy { it.drop(1).toInt() })
        assertTrue(model.state.value.items.all { it.details != null })
        model.onAction(RoasterListAction.LoadDetails("r1")); runCurrent()
        assertEquals(8, repository.detailCalls.size) // already loaded
    }

    @Test fun failedDetailsAreIgnoredAndMayBeRetried() = runTest(dispatcher) {
        var fail = true
        val repository = FakeRoasterRepository().apply { detail = { if (fail) Result.failure(IllegalStateException()) else Result.success(details(it)) } }
        val model = model(repository); runCurrent()
        model.onAction(RoasterListAction.LoadDetails("a")); runCurrent()
        assertNull(model.state.value.items.first { it.slug == "a" }.details)
        fail = false
        model.onAction(RoasterListAction.LoadDetails("a")); runCurrent()
        assertEquals("a", model.state.value.items.first { it.slug == "a" }.details?.id)
    }

    @Test fun favoriteToggleUsesSlugKeyPhotoFallbackAndReportsFailure() = runTest(dispatcher) {
        val repository = FakeRoasterRepository().apply { catalog = { Result.success(listOf(summary("a", photo = null), summary("b", photo = "https://m/b"))) } }
        val favorites = FakeFavorites().apply { keys.value = setOf("b") }
        val model = model(repository, favorites); runCurrent()
        model.onAction(RoasterListAction.LoadDetails("a")); runCurrent()
        model.onAction(RoasterListAction.ToggleFavorite("a")); model.onAction(RoasterListAction.ToggleFavorite("b")); runCurrent()
        assertEquals(listOf(
            RoasterFavoriteTarget("a", "Roaster a", "https://m/a-1") to true,
            RoasterFavoriteTarget("b", "Roaster b", "https://m/b") to false,
        ), favorites.changes)
        assertNull(model.state.value.message)
        favorites.failChange = true
        model.onAction(RoasterListAction.ToggleFavorite("a")); runCurrent()
        assertEquals(RoasterMessage.FavoriteChangeFailed, model.state.value.message)
        model.onAction(RoasterListAction.DismissMessage); runCurrent()
        assertNull(model.state.value.message)
        model.onAction(RoasterListAction.ToggleFavorite("missing")); runCurrent()
        assertEquals(3, favorites.changes.size)
    }

    @Test fun storeErrorsAndFlowFailuresBecomeMessages() = runTest(dispatcher) {
        val favorites = FakeFavorites()
        val model = model(favorites = favorites); runCurrent()
        favorites.errors.tryEmit("Лимит"); runCurrent()
        assertEquals(RoasterMessage.FromStore("Лимит"), model.state.value.message)
        val broken = FakeFavorites().apply { keysFlow = flow { throw IllegalStateException("db") } }
        val other = model(favorites = broken); runCurrent()
        assertEquals(RoasterMessage.FavoritesUnavailable, other.state.value.message)
        assertEquals(listOf("a", "b"), other.state.value.items.map { it.slug }) // the catalog still loads
    }
}
