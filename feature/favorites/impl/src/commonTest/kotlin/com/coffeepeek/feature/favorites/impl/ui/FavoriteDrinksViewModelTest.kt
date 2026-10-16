package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.favorites.domain.model.*
import com.coffeepeek.feature.favorites.domain.repository.*
import com.coffeepeek.feature.favorites.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteDrinksViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private class Drinks : FavoriteDrinksRepository {
        val state = MutableStateFlow(Result.success(listOf(FavoriteDrink("drink", "Latte", null, true))))
        var gate: CompletableDeferred<Unit>? = null
        var result: Result<Unit> = Result.success(Unit)
        var removals = 0
        override fun observe() = state
        override suspend fun remove(id: String): Result<Unit> {
            removals++; gate?.await()
            if (result.isSuccess) state.value = Result.success(emptyList())
            return result
        }
    }
    private fun vm(drinks: Drinks): FavoritesViewModel {
        val shops = object : FavoritesRepository {
            override fun observe() = flowOf(Result.success(emptyList<FavoriteShop>()))
            override suspend fun read(): Result<List<FavoriteShop>> = error("Unused")
            override suspend fun save(shop: FavoriteShop): Result<Unit> = error("Unused")
            override suspend fun remove(shopId: String): Result<Unit> = error("Unused")
            override suspend fun clear(): Result<Unit> = error("Unused")
        }
        return FavoritesViewModel(shops, drinkRepository = drinks).also { store.put("drinks", it) }
    }

    @Test fun drinkOnlyAccountLoadsAndAnExternalClearRemovesItsRows() = runTest(dispatcher) {
        val source = Drinks(); val vm = vm(source)
        assertTrue(vm.state.value.areDrinksLoading)
        runCurrent()
        assertEquals("Latte", vm.state.value.drinks.single().title)
        assertFalse(vm.state.value.areDrinksLoading)
        source.state.value = Result.success(emptyList()); runCurrent()
        assertTrue(vm.state.value.drinks.isEmpty())
    }

    @Test fun failedRemovalRetainsTheDrinkAndDuplicateWritesAreGuarded() = runTest(dispatcher) {
        val source = Drinks().apply { result = Result.failure(IllegalStateException("Offline")); gate = CompletableDeferred() }
        val vm = vm(source); runCurrent()
        vm.onAction(FavoritesAction.RemoveDrink("drink")); vm.onAction(FavoritesAction.RemoveDrink("drink")); runCurrent()
        assertEquals(1, source.removals)
        source.gate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.actionFailed)
        assertEquals(1, vm.state.value.drinks.size)
        assertTrue(vm.state.value.removingDrinks.isEmpty())
    }

    @Test fun unknownTargetCannotBeRemovedAndAnAddresslessShopCannotNavigate() = runTest(dispatcher) {
        val source = Drinks().apply { state.value = Result.success(listOf(FavoriteDrink("unknown", "No target", null, false))) }
        val vm = vm(source); runCurrent()
        vm.onAction(FavoritesAction.RemoveDrink("unknown"))
        vm.onAction(FavoritesAction.OpenShop("unknown")); vm.onAction(FavoritesAction.Back); runCurrent()
        assertEquals(0, source.removals)
        assertEquals(FavoritesEvent.Back, vm.events.first())
    }

    @Test fun partialFailureRetainsConfirmedDrinksAndCanRetryWithoutAWrite() = runTest(dispatcher) {
        val source = Drinks(); val vm = vm(source); runCurrent()
        source.state.value = Result.failure(IllegalStateException("Offline")); runCurrent()
        assertTrue(vm.state.value.drinksLoadFailed)
        assertEquals(1, vm.state.value.drinks.size)
        source.state.value = Result.success(emptyList())
        vm.onAction(FavoritesAction.Retry); runCurrent()
        assertFalse(vm.state.value.drinksLoadFailed)
        assertTrue(vm.state.value.drinks.isEmpty())
        assertEquals(0, source.removals)
    }
}
