package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCatalogs
import com.coffeepeek.feature.discovery.impl.search.CatalogLoadState
import com.coffeepeek.feature.discovery.impl.search.CatalogKind
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchCatalogRecoveryTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private fun model() = SearchViewModel(repository, FakeFavorites(), FakeCity(), FakeLocation(), FakeAccount(), RecordingAnalytics())
        .also { store.put("search", it) }

    @Test fun failureDoesNotBlockSearchAndExplicitRetryRetainsSelections() = runTest(dispatcher) {
        repository.catalogs = { Result.failure(IllegalStateException()) }
        val vm = model()
        runCurrent()
        assertEquals(CatalogLoadState.Failed, vm.state.value.catalogLoadState)
        assertEquals(1, repository.queries.size)
        vm.onAction(SearchAction.ToggleCatalog(CatalogKind.Bean, "arabica"))
        runCurrent()
        repository.catalogs = { Result.success(DiscoveryCatalogs(beans = listOf(CatalogEntry("arabica", "Арабика")))) }
        vm.onAction(SearchAction.RetryCatalogs)
        runCurrent()
        assertEquals(CatalogLoadState.Ready, vm.state.value.catalogLoadState)
        assertEquals(setOf("arabica"), vm.state.value.filters.beanIds)
        assertEquals(2, repository.queries.size)
    }

    @Test fun emptySuccessIsNotTreatedAsFailureOrRetriedOnEveryOpen() = runTest(dispatcher) {
        repository.catalogs = { Result.success(DiscoveryCatalogs()) }
        val vm = model()
        runCurrent()
        assertEquals(CatalogLoadState.Ready, vm.state.value.catalogLoadState)
        assertTrue(vm.state.value.hasNoCatalogChoices)
        vm.onAction(SearchAction.ToggleFilters)
        runCurrent()
        assertEquals(1, repository.catalogCalls)
    }

    @Test fun repeatedRetryWhileLoadingUsesOneRequest() = runTest(dispatcher) {
        val pending = CompletableDeferred<Result<DiscoveryCatalogs>>()
        repository.catalogs = { pending.await() }
        val vm = model()
        runCurrent()
        vm.onAction(SearchAction.RetryCatalogs)
        vm.onAction(SearchAction.ToggleFilters)
        runCurrent()
        assertEquals(CatalogLoadState.Loading, vm.state.value.catalogLoadState)
        assertEquals(1, repository.catalogCalls)
        pending.complete(Result.success(DiscoveryCatalogs()))
        runCurrent()
        assertEquals(CatalogLoadState.Ready, vm.state.value.catalogLoadState)
    }

    @Test fun refreshFailureRetainsKnownOptionsAndCity() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        val before = vm.state.value.catalogs
        repository.catalogs = { throw IllegalStateException() }
        vm.onAction(SearchAction.RetryCatalogs)
        runCurrent()
        assertEquals(CatalogLoadState.Failed, vm.state.value.catalogLoadState)
        assertEquals(before, vm.state.value.catalogs)
        assertEquals("minsk", vm.state.value.filters.cityId)
        assertEquals(1, repository.queries.size)
    }

    @Test fun wrappedCancellationDoesNotBecomeCatalogFailureOrStartSearch() = runTest(dispatcher) {
        repository.catalogs = { Result.failure(CancellationException()) }
        val vm = model()
        runCurrent()
        assertNotEquals(CatalogLoadState.Failed, vm.state.value.catalogLoadState)
        assertTrue(repository.queries.isEmpty())
    }
}
