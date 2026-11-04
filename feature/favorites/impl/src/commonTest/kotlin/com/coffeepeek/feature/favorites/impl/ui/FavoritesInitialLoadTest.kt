package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.favorites.domain.model.*
import com.coffeepeek.feature.favorites.domain.repository.*
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesInitialLoadTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val shops = MutableSharedFlow<Result<List<FavoriteShop>>>(extraBufferCapacity = 1)
    private val roasters = MutableSharedFlow<Result<List<FavoriteRoaster>>>(extraBufferCapacity = 1)
    private val drinks = MutableSharedFlow<Result<List<FavoriteDrink>>>(extraBufferCapacity = 1)

    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { store.clear(); Dispatchers.resetMain() }

    @Test fun fastDrinksAndRoastersCannotBecomeAnchorsBeforeTheShopResult() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        drinks.emit(Result.success(listOf(FavoriteDrink("drink", "Coffee", null, true))))
        runCurrent()
        assertTrue(vm.state.value.showInitialSkeleton)
        roasters.emit(Result.success(listOf(FavoriteRoaster("roaster", "Roaster"))))
        runCurrent()
        assertTrue(vm.state.value.showInitialSkeleton)
        shops.emit(Result.success(listOf(FavoriteShop("shop", "Cafe"))))
        runCurrent()
        assertTrue(vm.state.value.hasSettledInitialLoad)
        assertFalse(vm.state.value.showInitialSkeleton)
        assertEquals("shop", vm.state.value.shops.first().id)
    }

    @Test fun anEmptyShopResultDoesNotFlashAnEmptyStateWhileOtherSectionsArePending() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        shops.emit(Result.success(emptyList()))
        runCurrent()
        assertTrue(vm.state.value.isEmpty)
        assertTrue(vm.state.value.showInitialSkeleton)
        roasters.emit(Result.success(emptyList()))
        drinks.emit(Result.success(emptyList()))
        runCurrent()
        assertTrue(vm.state.value.hasSettledInitialLoad)
        assertFalse(vm.state.value.showInitialSkeleton)
        assertTrue(vm.state.value.isEmpty)
    }

    @Test fun failedSectionsSettleWithoutHidingAnotherSectionsSuccessfulFavorites() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        roasters.emit(Result.success(listOf(FavoriteRoaster("roaster", "Roaster"))))
        drinks.emit(Result.failure(IllegalStateException("Drinks unavailable")))
        runCurrent()
        assertTrue(vm.state.value.showInitialSkeleton)
        shops.emit(Result.failure(IllegalStateException("Shops unavailable")))
        runCurrent()
        assertFalse(vm.state.value.showInitialSkeleton)
        assertTrue(vm.state.value.hasSettledInitialLoad)
        assertTrue(vm.state.value.loadFailed)
        assertEquals("roaster", vm.state.value.roasters.single().id)
    }

    @Test fun retryWithCachedShopsDoesNotReplaceTheVisibleListWithAnInitialSkeleton() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        ready()
        runCurrent()
        vm.onAction(FavoritesAction.Retry)
        runCurrent()
        assertTrue(vm.state.value.hasSettledInitialLoad)
        assertTrue(vm.state.value.areRoastersLoading)
        assertFalse(vm.state.value.showInitialSkeleton)
        assertEquals("shop", vm.state.value.shops.single().id)
    }

    @Test fun detailNavigationDoesNotRearmInitialLoadingOnTheExistingDestination() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        ready()
        runCurrent()
        vm.onAction(FavoritesAction.OpenShop("shop"))
        runCurrent()
        assertEquals(FavoritesEvent.OpenShop("shop"), vm.events.first())
        assertTrue(vm.state.value.hasSettledInitialLoad)
        assertFalse(vm.state.value.showInitialSkeleton)
    }

    @Test fun aNewDestinationMustSettleItsOwnInitialSources() = runTest(dispatcher) {
        val previous = model()
        runCurrent()
        ready()
        runCurrent()
        assertTrue(previous.state.value.hasSettledInitialLoad)
        val fresh = model()
        assertFalse(fresh.state.value.hasSettledInitialLoad)
        assertTrue(fresh.state.value.showInitialSkeleton)
    }

    private suspend fun ready() {
        shops.emit(Result.success(listOf(FavoriteShop("shop", "Cafe"))))
        roasters.emit(Result.success(emptyList()))
        drinks.emit(Result.success(emptyList()))
    }

    private fun model() = FavoritesViewModel(Shops(shops), Roasters(roasters), Drinks(drinks)).also { store.put("favorites", it) }

    private class Shops(private val values: MutableSharedFlow<Result<List<FavoriteShop>>>) : FavoritesRepository {
        override fun observe() = values
        override suspend fun read(): Result<List<FavoriteShop>> = error("Unexpected read")
        override suspend fun save(shop: FavoriteShop): Result<Unit> = error("Unexpected save")
        override suspend fun remove(shopId: String): Result<Unit> = error("Unexpected remove")
        override suspend fun clear(): Result<Unit> = error("Unexpected clear")
    }

    private class Roasters(private val values: MutableSharedFlow<Result<List<FavoriteRoaster>>>) : FavoriteRoastersRepository {
        override fun observe() = values
        override suspend fun remove(roasterId: String): Result<Unit> = error("Unexpected remove")
    }

    private class Drinks(private val values: MutableSharedFlow<Result<List<FavoriteDrink>>>) : FavoriteDrinksRepository {
        override fun observe() = values
        override suspend fun remove(id: String): Result<Unit> = error("Unexpected remove")
    }
}
