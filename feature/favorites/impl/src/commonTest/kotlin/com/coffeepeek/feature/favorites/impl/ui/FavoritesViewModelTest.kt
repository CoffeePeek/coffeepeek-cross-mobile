package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
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

    @Test fun dismissingPreviewFailureDoesNotRetryOrRemoveTheRetainedRow() = runTest(dispatcher) {
        val repo = Repo()
        repo.removeResult = Result.failure(IllegalStateException("Offline"))
        val vm = vm(repo)
        runCurrent()
        vm.onAction(FavoritesAction.Remove("a"))
        runCurrent()
        assertTrue(vm.state.value.actionFailed)
        vm.onAction(FavoritesAction.DismissActionFailure)
        runCurrent()
        assertFalse(vm.state.value.actionFailed)
        assertEquals(1, repo.removals)
        assertEquals(listOf("a"), vm.state.value.shops.map { it.id })
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

    private class Roasters : FavoriteRoastersRepository {
        val values = MutableStateFlow<Result<List<FavoriteRoaster>>>(Result.success(listOf(
            FavoriteRoaster("saved-id", "Roaster", "public-slug"))))
        var gate: CompletableDeferred<Unit>? = null
        var result: Result<Unit> = Result.success(Unit)
        var removals = 0
        override fun observe() = values
        override suspend fun remove(roasterId: String): Result<Unit> {
            removals++; gate?.await()
            if (result.isSuccess) values.value = Result.success(emptyList())
            return result
        }
    }

    private fun roasterVm(shops: Repo, roasters: Roasters) = FavoritesViewModel(shops, roasters)
        .also { store.put("favorites", it) }

    @Test fun observesRoastersEvenWhenShopSourceFailsAndRecoversBothOnRetry() = runTest(dispatcher) {
        val shops = Repo().apply { values.value = Result.failure(IllegalStateException("Shop storage unavailable")) }
        val roasters = Roasters(); val vm = roasterVm(shops, roasters); runCurrent()
        assertTrue(vm.state.value.loadFailed)
        assertEquals("saved-id", vm.state.value.roasters.single().id)
        assertFalse(vm.state.value.areRoastersLoading)
        roasters.values.value = Result.failure(IllegalStateException("Roaster storage unavailable")); runCurrent()
        assertTrue(vm.state.value.roastersLoadFailed)
        assertEquals(1, vm.state.value.roasters.size)
        shops.values.value = Result.success(emptyList()); roasters.values.value = Result.success(emptyList())
        vm.onAction(FavoritesAction.Retry); runCurrent()
        assertFalse(vm.state.value.loadFailed); assertFalse(vm.state.value.roastersLoadFailed)
        assertTrue(vm.state.value.roasters.isEmpty())
    }

    @Test fun failedRoasterRemovalRetainsRowAndDuplicateWorkIsGuarded() = runTest(dispatcher) {
        val roasters = Roasters().apply {
            gate = CompletableDeferred(); result = Result.failure(IllegalStateException("Disk full"))
        }
        val vm = roasterVm(Repo(), roasters); runCurrent()
        vm.onAction(FavoritesAction.RemoveRoaster("saved-id")); vm.onAction(FavoritesAction.RemoveRoaster("saved-id"))
        runCurrent(); assertEquals(1, roasters.removals)
        assertEquals(setOf("saved-id"), vm.state.value.removingRoasters)
        roasters.gate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.actionFailed); assertEquals(1, vm.state.value.roasters.size)
        assertTrue(vm.state.value.removingRoasters.isEmpty())
    }

    @Test fun roasterNavigationUsesPublicSlugAndRejectsUnknownOrMissingAddresses() = runTest(dispatcher) {
        val roasters = Roasters(); val vm = roasterVm(Repo(), roasters); runCurrent()
        vm.onAction(FavoritesAction.OpenRoaster("saved-id")); runCurrent()
        assertEquals(FavoritesEvent.OpenRoaster("public-slug"), vm.events.first())
        roasters.values.value = Result.success(listOf(FavoriteRoaster("unaddressed", "No address"))); runCurrent()
        vm.onAction(FavoritesAction.OpenRoaster("unaddressed"))
        vm.onAction(FavoritesAction.OpenRoaster("gone")); vm.onAction(FavoritesAction.Back); runCurrent()
        assertEquals(FavoritesEvent.Back, vm.events.first())
    }

    @Test fun roasterRemovalCancellationIsNotAnErrorAndObservationOwnsSuccess() = runTest(dispatcher) {
        val roasters = Roasters(); val vm = roasterVm(Repo(), roasters); runCurrent()
        vm.onAction(FavoritesAction.RemoveRoaster("saved-id")); runCurrent()
        assertTrue(vm.state.value.roasters.isEmpty())
        roasters.values.value = Result.success(listOf(FavoriteRoaster("saved-id", "Again"))); runCurrent()
        roasters.gate = CompletableDeferred()
        vm.onAction(FavoritesAction.RemoveRoaster("saved-id")); runCurrent()
        store.clear(); runCurrent()
        assertTrue(vm.state.value.removingRoasters.isEmpty()); assertFalse(vm.state.value.actionFailed)
    }
}
