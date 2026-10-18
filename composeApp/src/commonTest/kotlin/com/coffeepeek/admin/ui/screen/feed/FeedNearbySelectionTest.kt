package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FeedNearbySelectionTest {
    private val location = GeoPoint(53.9, 27.56)
    private val movedLocation = GeoPoint(53.91, 27.57)

    @Test
    fun locationBeforeCatalogsKeepsInitialSearchInTheSelectedCity() = runBlocking {
        withFeed {
            vm.onUserLocationChanged(location)
            loadCatalogs()

            val request = shops.requests.receive()
            assertEquals("minsk", request.cityId)
            assertNull(request.origin)
            awaitShops()
            assertFalse(vm.uiState.value.filters.nearbyOnly)
        }
    }

    @Test
    fun locationAfterInitialLoadKeepsDisplayedShopsAndNearbyUnselected() = runBlocking {
        withFeed {
            loadCatalogs()
            shops.requests.receive()
            val displayed = awaitShops().shops

            vm.onUserLocationChanged(location)

            assertFalse(vm.uiState.value.filters.nearbyOnly)
            assertSame(displayed, vm.uiState.value.shops)
            assertEquals(0, vm.uiState.value.activeFilterCount)
        }
    }

    @Test
    fun nearbyCanBeSelectedManuallyAndLocationUpdatesPreserveThatChoice() = runBlocking {
        withFeed {
            loadCatalogs()
            shops.requests.receive()
            awaitShops()
            vm.onUserLocationChanged(location)

            vm.toggleNearbyOnly()
            val selected = shops.requests.receive()
            assertNull(selected.cityId)
            assertEquals(ShopLocation(latitude = location.latitude, longitude = location.longitude), selected.origin)
            awaitShops()
            assertTrue(vm.uiState.value.filters.nearbyOnly)

            vm.onUserLocationChanged(movedLocation)
            val moved = shops.requests.receive()
            assertEquals(ShopLocation(latitude = movedLocation.latitude, longitude = movedLocation.longitude), moved.origin)
            awaitShops()

            vm.toggleNearbyOnly()
            val deselected = shops.requests.receive()
            assertEquals("minsk", deselected.cityId)
            assertNull(deselected.origin)
            val displayed = awaitShops().shops

            vm.onUserLocationChanged(location)
            assertFalse(vm.uiState.value.filters.nearbyOnly)
            assertSame(displayed, vm.uiState.value.shops)
        }
    }

    @Test
    fun checkInPickerCanOptIntoNearbyOnceWithoutOverridingLaterDeselection() = runBlocking {
        withFeed {
            vm.onUserLocationChanged(location, selectNearbyOnFirstLocation = true)
            loadCatalogs()
            val initial = shops.requests.receive()
            assertNull(initial.cityId)
            assertEquals(ShopLocation(latitude = location.latitude, longitude = location.longitude), initial.origin)
            awaitShops()
            assertTrue(vm.uiState.value.filters.nearbyOnly)

            vm.toggleNearbyOnly()
            shops.requests.receive()
            val displayed = awaitShops().shops

            vm.onUserLocationChanged(movedLocation, selectNearbyOnFirstLocation = true)
            assertFalse(vm.uiState.value.filters.nearbyOnly)
            assertSame(displayed, vm.uiState.value.shops)
        }
    }

    private suspend fun withFeed(test: suspend FeedFixture.() -> Unit) {
        val fixture = FeedFixture()
        try {
            withTimeout(5_000) { fixture.test() }
        } finally {
            fixture.vm.close()
        }
    }
}

private class FeedFixture {
    val shops = NearbySelectionShops()
    val vm = FeedViewModel(shops, NearbySelectionFavorites, CityPreference(NearbySelectionSettings), NearbySelectionSession)

    fun loadCatalogs() {
        shops.catalogs.complete(ShopCatalogs(cities = listOf(City("minsk", "Минск"))))
    }

    suspend fun awaitShops(): FeedUiState = vm.uiState.first {
        !it.isLoading && !it.isRefreshing && it.shops.isNotEmpty()
    }
}

private class NearbySelectionShops : ShopRepository {
    val catalogs = CompletableDeferred<ShopCatalogs>()
    val requests = Channel<ShopFilters>(Channel.UNLIMITED)

    override suspend fun getCatalogs() = Result.success(catalogs.await())
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> {
        requests.send(filters)
        val shop = CoffeeShop(
            id = if (filters.origin == null) "city-shop" else "nearby-shop",
            title = "Coffee", rating = null, cityName = "Минск", priceRange = null, photoUrl = null,
        )
        return Result.success(PagedResult(listOf(shop), totalCount = 1, totalPages = 1, currentPage = 1))
    }

    override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> = error("Unused")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("Unused")
    override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("Unused")
    override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("Unused")
    override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("Unused")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("Unused")
}

private object NearbySelectionFavorites : FavoriteRepository {
    override suspend fun getFavoriteIds() = emptySet<String>()
    override suspend fun isFavorite(shopId: String) = false
    override suspend fun getFavorites() = Result.success(emptyList<CoffeeShopDetails>())
    override suspend fun addFavorite(shop: CoffeeShop, address: String?) = Result.success(Unit)
    override suspend fun removeFavorite(shopId: String) = Result.success(Unit)
    override suspend fun clearAll() = Unit
}

private object NearbySelectionSession : SessionRepository {
    override fun peekSession(): Session? = null
    override fun applySession(session: Session?) = Unit
    override fun isActiveSession(session: Session?) = false
    override suspend fun getSession(): Session? = null
    override suspend fun persistSession(session: Session?) = Unit
    override suspend fun saveSession(session: Session?) = Unit
    override suspend fun warmCache() = Unit
    override fun observeSession() = flowOf<Session?>(null)
    override suspend fun isLoggedIn() = false
}

private object NearbySelectionSettings : SettingRepository {
    override suspend fun save(model: Setting) = Unit
    override suspend fun read(key: String): Setting? = null
    override fun readFlow(key: String) = flowOf<Setting?>(null)
    override suspend fun readAll() = emptyList<Setting>()
    override fun readAllFlow() = flowOf(emptyList<Setting>())
    override suspend fun delete(key: String) = Unit
}
