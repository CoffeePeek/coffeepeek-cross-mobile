package com.coffeepeek.feature.coffee.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.coffee.domain.model.*
import com.coffeepeek.feature.coffee.impl.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repository: TestCoffeeRepository = TestCoffeeRepository(), slug: String = "coffee") =
        CoffeeDetailViewModel(slug, repository).also { store.put("detail-${nextKey++}", it) }

    @Test fun initialReadsAreIndependentAndInitializationIsIdempotent() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { filters = { gate.await(); Result.success(groups()) } }
        val model = model(repository, "alias"); runCurrent()
        assertEquals(detail(), model.state.value.details)
        assertFalse(model.state.value.isLoading)
        assertTrue(model.state.value.filterGroups.isEmpty())
        repeat(3) { model.onAction(CoffeeDetailAction.Initialize) }; runCurrent()
        assertEquals(listOf("alias"), repository.details)
        assertEquals(1, repository.filterCalls)
        assertTrue(repository.searches.isEmpty())
        gate.complete(Unit); runCurrent(); assertEquals(groups(), model.state.value.filterGroups)
    }

    @Test fun similarUsesTasteSetExcludesCanonicalCoffeeAndKeepsSix() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply {
            detail = { Result.success(detail(coffee("canonical", listOf("fruit", "nut", "fruit")))) }
            search = { Result.success(page("canonical", "a", "b", "c", "d", "e", "f", "g")) }
        }
        val model = model(repository, "alias"); runCurrent()
        assertEquals(SearchCall("", CoffeeFilters(mapOf("taste" to setOf("fruit", "nut"))), 1), repository.searches.single())
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), model.state.value.similar.map { it.slug })
        assertFalse(model.state.value.similarLoading)
    }

    @Test fun noTastesMeansNoSimilarRequestEvenOnRetry() = runTest(dispatcher) {
        val repository = TestCoffeeRepository()
        val model = model(repository); runCurrent()
        model.onAction(CoffeeDetailAction.RetrySimilar); runCurrent()
        assertTrue(repository.searches.isEmpty())
        assertTrue(model.state.value.similar.isEmpty())
        assertFalse(model.state.value.similarLoading)
    }

    @Test fun detailFailureIsLocalizedByUiRatherThanLeakingAnExceptionMessage() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { detail = { Result.failure(IllegalStateException("private body")) } }
        val model = model(repository); runCurrent()
        assertTrue(model.state.value.loadFailed)
        assertFalse(model.state.value.isLoading)
        assertNull(model.state.value.details)
        assertTrue(repository.searches.isEmpty())
        repository.detail = { Result.success(detail()) }
        model.onAction(CoffeeDetailAction.Retry); runCurrent()
        assertEquals(detail(), model.state.value.details)
        assertFalse(model.state.value.loadFailed)
        assertEquals(1, repository.filterCalls)
    }

    @Test fun optionalLabelsFailureDoesNotHideDetailsOrTriggerListSearch() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { filters = { throw IllegalStateException("private body") } }
        val model = model(repository); runCurrent()
        assertEquals(detail(), model.state.value.details)
        assertFalse(model.state.value.loadFailed)
        assertTrue(model.state.value.filterGroups.isEmpty())
        assertTrue(repository.searches.isEmpty())
    }

    @Test fun similarFailureCanBeRetriedWithoutReloadingDetails() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply {
            detail = { Result.success(detail(coffee(tastes = listOf("fruit")))) }
            search = { Result.failure(IllegalStateException("private body")) }
        }
        val model = model(repository); runCurrent()
        assertTrue(model.state.value.similarFailed)
        assertFalse(model.state.value.loadFailed)
        assertNotNull(model.state.value.details)
        repository.search = { Result.success(page("other")) }
        model.onAction(CoffeeDetailAction.RetrySimilar); runCurrent()
        assertEquals(listOf("other"), model.state.value.similar.map { it.slug })
        assertFalse(model.state.value.similarFailed)
        assertEquals(listOf("coffee"), repository.details)
    }

    @Test fun lateDetailCannotReplaceNewerRetry() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { detail = {
            withContext(NonCancellable) { gate.await() }; Result.success(detail(coffee("old")))
        } }
        val model = model(repository); runCurrent()
        repository.detail = { Result.success(detail(coffee("new"))) }
        model.onAction(CoffeeDetailAction.Retry); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals("new", model.state.value.details?.coffee?.slug)
        assertFalse(model.state.value.loadFailed)
    }

    @Test fun oldSimilarResultCannotSurviveDetailReload() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply {
            detail = { Result.success(detail(coffee("old", listOf("fruit")))) }
            search = { withContext(NonCancellable) { gate.await() }; Result.success(page("stale")) }
        }
        val model = model(repository); runCurrent()
        repository.detail = { Result.success(detail(coffee("new", listOf("nut")))) }
        repository.search = { Result.success(page("fresh")) }
        model.onAction(CoffeeDetailAction.Retry); runCurrent()
        assertEquals(listOf("fresh"), model.state.value.similar.map { it.slug })
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("fresh"), model.state.value.similar.map { it.slug })
        assertFalse(model.state.value.similarFailed)
    }

    @Test fun lateSimilarFailureCannotClearNewRequestsSpinner() = runTest(dispatcher) {
        val oldGate = CompletableDeferred<Unit>()
        val newGate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply {
            detail = { Result.success(detail(coffee(tastes = listOf("fruit")))) }
            search = { withContext(NonCancellable) { oldGate.await() }; Result.failure(IllegalStateException("old")) }
        }
        val model = model(repository); runCurrent()
        repository.search = { newGate.await(); Result.success(page("fresh")) }
        model.onAction(CoffeeDetailAction.RetrySimilar); runCurrent()
        oldGate.complete(Unit); runCurrent()
        assertTrue(model.state.value.similarLoading)
        assertFalse(model.state.value.similarFailed)
        newGate.complete(Unit); runCurrent()
        assertEquals(listOf("fresh"), model.state.value.similar.map { it.slug })
    }

    @Test fun invalidSlugNeverReachesDetailRepository() = runTest(dispatcher) {
        for (slug in listOf(" ", "x".repeat(201))) {
            val repository = TestCoffeeRepository()
            val model = model(repository, slug); runCurrent()
            assertTrue(model.state.value.loadFailed)
            assertFalse(model.state.value.isLoading)
            assertTrue(repository.details.isEmpty())
        }
    }

    @Test fun cancellationInResultDoesNotBecomeDetailError() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { detail = { Result.failure(CancellationException("cancelled")) } }
        val model = model(repository); runCurrent()
        assertFalse(model.state.value.loadFailed)
        assertFalse(model.state.value.isLoading)
        repository.detail = { Result.success(detail()) }
        model.onAction(CoffeeDetailAction.Retry); runCurrent()
        assertEquals(detail(), model.state.value.details)
    }

    @Test fun thrownSimilarCancellationDoesNotBecomeAnError() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply {
            detail = { Result.success(detail(coffee(tastes = listOf("fruit")))) }
            search = { throw CancellationException("cancelled") }
        }
        val model = model(repository); runCurrent()
        assertNotNull(model.state.value.details)
        assertFalse(model.state.value.similarFailed)
        assertFalse(model.state.value.similarLoading)
    }

    @Test fun clearingLifecyclePreventsLateDetailAndLabels() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply {
            detail = { withContext(NonCancellable) { gate.await() }; Result.success(detail()) }
            filters = { withContext(NonCancellable) { gate.await() }; Result.success(groups()) }
        }
        val model = model(repository); runCurrent()
        store.clear(); gate.complete(Unit); runCurrent()
        assertNull(model.state.value.details)
        assertTrue(model.state.value.filterGroups.isEmpty())
        assertTrue(repository.searches.isEmpty())
    }

    @Test fun navigationAndExternalEffectsUseOnlyLoadedValidTargets() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply {
            detail = { Result.success(detail(coffee(tastes = listOf("fruit"), offers = listOf(
                CoffeeOffer(null, null, null), CoffeeOffer(10.0, "BYN", 200, sourceUrl = "https://seller.invalid/product"),
            )))) }
            search = { Result.success(page("similar")) }
        }
        val model = model(repository); runCurrent()
        val events = mutableListOf<CoffeeDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.onAction(CoffeeDetailAction.OpenSimilar("unknown"))
        model.onAction(CoffeeDetailAction.OpenSeller(-1))
        model.onAction(CoffeeDetailAction.OpenSeller(0))
        model.onAction(CoffeeDetailAction.OpenSeller(99))
        model.onAction(CoffeeDetailAction.OpenRoaster)
        model.onAction(CoffeeDetailAction.OpenSeller(1))
        model.onAction(CoffeeDetailAction.OpenSimilar("similar"))
        model.onAction(CoffeeDetailAction.Share)
        model.onAction(CoffeeDetailAction.Back); runCurrent()
        assertEquals(listOf(CoffeeDetailEvent.OpenRoaster("roast"), CoffeeDetailEvent.OpenSeller("https://seller.invalid/product"),
            CoffeeDetailEvent.OpenCoffee("similar"), CoffeeDetailEvent.Share("Coffee coffee", "/coffees/coffee"), CoffeeDetailEvent.Back), events)
    }

    @Test fun unavailableTargetsDoNotEmitEffectsButBackStillWorks() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { detail = { Result.failure(IllegalStateException()) } }
        val model = model(repository); runCurrent()
        val events = mutableListOf<CoffeeDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.onAction(CoffeeDetailAction.Share)
        model.onAction(CoffeeDetailAction.OpenRoaster)
        model.onAction(CoffeeDetailAction.OpenSeller(0))
        model.onAction(CoffeeDetailAction.OpenSimilar("coffee"))
        model.onAction(CoffeeDetailAction.Back); runCurrent()
        assertEquals(listOf<CoffeeDetailEvent>(CoffeeDetailEvent.Back), events)
    }

    @Test fun photosAndDescriptionAreControlledStateAndRespectMissingContent() = runTest(dispatcher) {
        val model = model(); runCurrent()
        model.onAction(CoffeeDetailAction.OpenPhotos)
        model.onAction(CoffeeDetailAction.ToggleDescription); runCurrent()
        assertTrue(model.state.value.arePhotosOpen)
        assertTrue(model.state.value.isDescriptionExpanded)
        model.onAction(CoffeeDetailAction.ClosePhotos)
        model.onAction(CoffeeDetailAction.ToggleDescription); runCurrent()
        assertFalse(model.state.value.arePhotosOpen)
        assertFalse(model.state.value.isDescriptionExpanded)
        val empty = model(TestCoffeeRepository().apply { detail = { Result.success(detail(photos = emptyList(), description = null, roaster = null)) } })
        runCurrent()
        empty.onAction(CoffeeDetailAction.OpenPhotos)
        empty.onAction(CoffeeDetailAction.ToggleDescription); runCurrent()
        assertFalse(empty.state.value.arePhotosOpen)
        assertFalse(empty.state.value.isDescriptionExpanded)
    }
}
