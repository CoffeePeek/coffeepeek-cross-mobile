package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun vm(repo: Repo) = FavoritesViewModel(repo).also { store.put("favorites", it) }

    private class Repo : FavoritesRepository {
        val values = MutableStateFlow<Result<List<FavoriteShop>>>(Result.success(listOf(FavoriteShop("a", "A"))))
        var removeResult: Result<Unit> = Result.success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        var removals = 0
        override fun observe() = values
        override suspend fun read() = values.value
        override suspend fun save(shop: FavoriteShop): Result<Unit> = error("not used")
        override suspend fun clear(): Result<Unit> = error("not used")
        override suspend fun remove(shopId: String): Result<Unit> {
            removals++; gate?.await()
            if (removeResult.isSuccess) values.value = Result.success(values.value.getOrThrow().filterNot { it.id == shopId })
            return removeResult
        }
    }

    @Test fun startsLoadingThenObservesExternalChanges() = runTest(dispatcher) {
        val repo = Repo(); val vm = vm(repo)
        assertTrue(vm.state.value.isLoading)
        runCurrent()
        assertFalse(vm.state.value.isLoading)
        repo.values.value = Result.success(emptyList()); runCurrent()
        assertTrue(vm.state.value.shops.isEmpty())
    }

    @Test fun failedRemovalKeepsCardAndClearsPendingAction() = runTest(dispatcher) {
        val repo = Repo(); repo.removeResult = Result.failure(IllegalStateException("disk full"))
        val vm = vm(repo); runCurrent()
        vm.onAction(FavoritesAction.Remove("a")); runCurrent()
        assertEquals(listOf("a"), vm.state.value.shops.map { it.id })
        assertTrue(vm.state.value.actionFailed)
        assertTrue(vm.state.value.removing.isEmpty())
    }

    @Test fun duplicateRemovalIsBlockedAndSuccessComesFromObservation() = runTest(dispatcher) {
        val repo = Repo(); repo.gate = CompletableDeferred()
        val vm = vm(repo); runCurrent()
        vm.onAction(FavoritesAction.Remove("a")); vm.onAction(FavoritesAction.Remove("a")); runCurrent()
        assertEquals(1, repo.removals)
        assertEquals(setOf("a"), vm.state.value.removing)
        repo.gate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.shops.isEmpty())
        assertFalse(vm.state.value.actionFailed)
    }

    @Test fun readFailureKeepsExistingListAndRetryRecovers() = runTest(dispatcher) {
        val repo = Repo(); val vm = vm(repo); runCurrent()
        repo.values.value = Result.failure(IllegalStateException("read failed")); runCurrent()
        assertTrue(vm.state.value.loadFailed)
        assertEquals(1, vm.state.value.shops.size)
        repo.values.value = Result.success(emptyList()); vm.onAction(FavoritesAction.Retry); runCurrent()
        assertFalse(vm.state.value.loadFailed)
        assertFalse(vm.state.value.isLoading)
        assertTrue(vm.state.value.shops.isEmpty())
    }

    @Test fun lifecycleCancellationDoesNotBecomeActionError() = runTest(dispatcher) {
        val repo = Repo(); repo.gate = CompletableDeferred()
        val vm = vm(repo); runCurrent(); vm.onAction(FavoritesAction.Remove("a")); runCurrent()
        store.clear(); runCurrent()
        assertTrue(vm.state.value.removing.isEmpty())
        assertFalse(vm.state.value.actionFailed)
        assertEquals(1, vm.state.value.shops.size)
    }

    @Test fun navigationIntentsBecomeOneOffEvents() = runTest(dispatcher) {
        val vm = vm(Repo()); runCurrent()
        vm.onAction(FavoritesAction.OpenShop("a"))
        vm.onAction(FavoritesAction.Back)
        runCurrent()
        assertEquals(FavoritesEvent.OpenShop("a"), vm.events.first())
        assertEquals(FavoritesEvent.Back, vm.events.first())
    }

    @Test fun pendingRemovalDoesNotBlockBackNavigation() = runTest(dispatcher) {
        val repo = Repo(); repo.gate = CompletableDeferred()
        val vm = vm(repo); runCurrent()
        vm.onAction(FavoritesAction.Remove("a")); runCurrent()
        assertEquals(setOf("a"), vm.state.value.removing)

        vm.onAction(FavoritesAction.Back); runCurrent()
        assertEquals(FavoritesEvent.Back, vm.events.first())
    }
}
