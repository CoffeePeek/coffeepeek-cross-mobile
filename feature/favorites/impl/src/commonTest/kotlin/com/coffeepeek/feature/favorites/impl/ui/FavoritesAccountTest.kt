package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.favorites.domain.model.*
import com.coffeepeek.feature.favorites.domain.repository.*
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesAccountTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val shops = Values<FavoriteShop>()
    private val roasters = Values<FavoriteRoaster>()
    private val drinks = Values<FavoriteDrink>()
    private val shopRepository = Shops(shops)

    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { store.clear(); Dispatchers.resetMain() }

    @Test fun warmingAnAccountStartsAllSourcesWithoutPublishingAnEmptySuccess() = runTest(dispatcher) {
        val account = Account(null)
        val vm = model(account)
        runCurrent()
        assertEquals(0, shops.observations)
        account.publish("first")
        runCurrent()
        assertEquals(listOf(1, 1, 1), listOf(shops.observations, roasters.observations, drinks.observations))
        assertTrue(vm.state.value.showInitialSkeleton)
        shops.emit(listOf(FavoriteShop("shop", "Hydrated")))
        runCurrent()
        assertTrue(vm.state.value.showInitialSkeleton)
        roasters.emit(emptyList())
        drinks.emit(emptyList())
        runCurrent()
        assertFalse(vm.state.value.showInitialSkeleton)
    }

    @Test fun ownerReplacementErasesEverySectionAndWaitsForTheNewOwnersResults() = runTest(dispatcher) {
        val account = Account("first")
        val vm = model(account)
        runCurrent()
        emitOwner("First")
        runCurrent()
        account.publish("second")
        runCurrent()
        assertTrue(vm.state.value.isEmpty)
        assertTrue(vm.state.value.showInitialSkeleton)
        assertFalse(vm.state.value.hasSettledInitialLoad)
        assertEquals(listOf(2, 2, 2), listOf(shops.observations, roasters.observations, drinks.observations))
        emitOwner("Second")
        runCurrent()
        assertEquals("Second", vm.state.value.shops.single().title)
        assertEquals("Second", vm.state.value.roasters.single().title)
        assertEquals("Second", vm.state.value.drinks.single().title)
    }

    @Test fun lateOldResultAndWriteAreRejectedEvenBeforeTheOwnerFlowCallbackRuns() = runTest(dispatcher) {
        val account = Account("first")
        val vm = model(account)
        runCurrent()
        emitOwner("First")
        runCurrent()
        account.current = "second"
        shops.emit(listOf(FavoriteShop("shop", "Late old result")))
        vm.onAction(FavoritesAction.Remove("shop"))
        runCurrent()
        assertEquals("First", vm.state.value.shops.single().title)
        assertEquals(0, shopRepository.removals)
        account.publish("second")
        runCurrent()
        assertTrue(vm.state.value.isEmpty)
        assertTrue(vm.state.value.showInitialSkeleton)
    }

    @Test fun aSameOwnerIdentityEmissionDoesNotClearCachedCardsOrRestartSources() = runTest(dispatcher) {
        val account = Account("first")
        val vm = model(account)
        runCurrent()
        emitOwner("First")
        runCurrent()
        account.publish("first")
        runCurrent()
        assertEquals(listOf(1, 1, 1), listOf(shops.observations, roasters.observations, drinks.observations))
        assertEquals("First", vm.state.value.shops.single().title)
        assertTrue(vm.state.value.hasSettledInitialLoad)
        assertFalse(vm.state.value.showInitialSkeleton)
    }

    @Test fun signOutClearsAllSectionsAndRetryDoesNotObserveAnAnonymousAccount() = runTest(dispatcher) {
        val account = Account("first")
        val vm = model(account)
        runCurrent()
        emitOwner("First")
        runCurrent()
        account.publish(null)
        runCurrent()
        assertTrue(vm.state.value.isEmpty)
        assertTrue(vm.state.value.hasSettledInitialLoad)
        vm.onAction(FavoritesAction.Retry)
        shops.emit(listOf(FavoriteShop("shop", "Late cached first owner")))
        runCurrent()
        assertTrue(vm.state.value.isEmpty)
        assertEquals(1, shops.observations)
    }

    @Test fun anOldRemovalCompletionCannotClearTheNextOwnersPendingAction() = runTest(dispatcher) {
        val account = Account("first")
        val vm = readyModel(account)
        val old = CompletableDeferred<Result<Unit>>()
        val current = CompletableDeferred<Result<Unit>>()
        shopRepository.writes.addAll(listOf(old, current))
        vm.act(FavoritesAction.Remove("shop"))
        account.publish("second")
        runCurrent()
        emitOwner("Second")
        runCurrent()
        vm.act(FavoritesAction.Remove("shop"))
        old.complete(Result.failure(IllegalStateException("Old request failed")))
        runCurrent()
        assertEquals(setOf("shop"), vm.state.value.removing)
        assertFalse(vm.state.value.actionFailed)
        current.complete(Result.success(Unit))
        runCurrent()
        assertTrue(vm.state.value.removing.isEmpty())
    }

    @Test fun sameOwnerRetryDoesNotStrandAPendingHeartOrDiscardItsFailure() = runTest(dispatcher) {
        val vm = readyModel(Account("first"))
        val pending = CompletableDeferred<Result<Unit>>()
        shopRepository.writes.add(pending)
        vm.onAction(FavoritesAction.Remove("shop"))
        runCurrent()
        vm.onAction(FavoritesAction.Retry)
        runCurrent()
        assertEquals(setOf("shop"), vm.state.value.removing)
        pending.complete(Result.failure(IllegalStateException("Current write failed")))
        runCurrent()
        assertTrue(vm.state.value.removing.isEmpty())
        assertTrue(vm.state.value.actionFailed)
    }

    private suspend fun TestScope.readyModel(account: FavoritesAccount): FavoritesViewModel {
        val vm = model(account)
        runCurrent()
        emitOwner("First")
        runCurrent()
        return vm
    }

    private fun FavoritesViewModel.act(action: FavoritesAction) {
        onAction(action)
        dispatcher.scheduler.runCurrent()
    }

    private fun model(account: FavoritesAccount) = FavoritesViewModel(shopRepository, Roasters(roasters), Drinks(drinks), account)
        .also { store.put("favorites-account", it) }

    private suspend fun emitOwner(title: String) {
        shops.emit(listOf(FavoriteShop("shop", title)))
        roasters.emit(listOf(FavoriteRoaster("roaster", title)))
        drinks.emit(listOf(FavoriteDrink("drink", title, null, true)))
    }

    private class Account(initial: String?) : FavoritesAccount {
        var current = initial
        private val values = MutableSharedFlow<String?>(replay = 1, extraBufferCapacity = 1).apply { tryEmit(initial) }
        override val identities = values
        override fun currentId() = current
        fun publish(value: String?) {
            current = value
            check(values.tryEmit(value))
        }
    }

    private class Values<T> {
        private val values = MutableSharedFlow<Result<List<T>>>(extraBufferCapacity = 1)
        var observations = 0
        fun observe() = values.also { observations++ }
        suspend fun emit(rows: List<T>) { values.emit(Result.success(rows)) }
    }

    private class Shops(private val values: Values<FavoriteShop>) : FavoritesRepository {
        var removals = 0
        val writes = ArrayDeque<CompletableDeferred<Result<Unit>>>()
        override fun observe() = values.observe()
        override suspend fun read(): Result<List<FavoriteShop>> = error("Unexpected read")
        override suspend fun save(shop: FavoriteShop): Result<Unit> = error("Unexpected save")
        override suspend fun clear(): Result<Unit> = error("Unexpected clear")
        override suspend fun remove(shopId: String): Result<Unit> {
            removals++
            return writes.removeFirstOrNull()?.await() ?: Result.success(Unit)
        }
    }

    private class Roasters(private val values: Values<FavoriteRoaster>) : FavoriteRoastersRepository {
        override fun observe() = values.observe()
        override suspend fun remove(roasterId: String): Result<Unit> = error("Unexpected remove")
    }

    private class Drinks(private val values: Values<FavoriteDrink>) : FavoriteDrinksRepository {
        override fun observe() = values.observe()
        override suspend fun remove(id: String): Result<Unit> = error("Unexpected remove")
    }
}
