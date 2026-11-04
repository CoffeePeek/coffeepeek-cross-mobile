package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the behaviour of the legacy discovery search before it moves: the request mapping, the debounce, paging, the load race, the error text, the guest gates
 * of the quick filters, local filtering, the optimistic favorite and the city change. They run on the real view model with in-memory repositories.
 */
class FeedViewModelParityTest {
    @Test
    fun explicitRetryLoadsTheFailedPageAndKeepsEarlierResults() {
        var failed = false
        parity(handler = { filters ->
            if (filters.page == 2 && !failed) {
                failed = true
                Result.failure(IllegalStateException("offline"))
            } else pageOf(listOf(shop("page-${filters.page}")), filters.page, pages = 2)
        }) {
            requests.receive()
            awaitShops()
            vm.loadMore()
            assertEquals(2, requests.receive().page)
            vm.uiState.first { it.loadMoreFailed }
            vm.retryLoadMore()
            assertEquals(2, requests.receive().page)
            val loaded = vm.uiState.first { it.currentPage == 2 }
            assertEquals(listOf("page-1", "page-2"), loaded.shops.map { it.id })
            assertFalse(loaded.loadMoreFailed)
            assertNull(loaded.error)
        }
    }

    @Test
    fun refreshClearsTheFailedPaginationAndStartsAtPageOne() = parity(handler = { filters ->
        if (filters.page == 1) pageOf(listOf(shop("first")), pages = 2)
        else Result.failure(IllegalStateException("offline"))
    }) {
        requests.receive()
        awaitShops()
        vm.loadMore()
        requests.receive()
        vm.uiState.first { it.loadMoreFailed }
        vm.refresh()
        assertEquals(1, requests.receive().page)
        val loaded = awaitShops()
        assertFalse(loaded.loadMoreFailed)
        assertTrue(loaded.canLoadMore)
    }

    @Test
    fun aFailedNextPageDoesNotAutomaticallyRetry() = parity(handler = { filters ->
        if (filters.page == 1) pageOf(listOf(shop("first")), pages = 2)
        else Result.failure(IllegalStateException("offline"))
    }) {
        requests.receive()
        awaitShops()
        vm.loadMore()
        assertEquals(2, requests.receive().page)
        vm.uiState.first { !it.isLoadingMore }
        repeat(3) { vm.loadMore() }
        settle()
        assertNull(requests.tryReceive().getOrNull())
        assertEquals(listOf("first"), vm.uiState.value.shops.map { it.id })
        assertEquals(1, vm.uiState.value.currentPage)
    }

    @Test
    fun theFirstSearchWaitsForTheCatalogsAndUsesTheSavedCity() = parity(savedCity = "brest") {
        val first = requests.receive()
        assertEquals("brest", first.cityId); assertEquals(1, first.page); assertEquals(20, first.pageSize); assertNull(first.query)
        awaitShops()
        assertEquals(listOf("Минск", "Брест"), vm.uiState.value.cities.map { it.name })
    }

    @Test
    fun aSavedCityThatIsGoneFallsBackToTheFirstCity() = parity(savedCity = "removed") {
        assertEquals("minsk", requests.receive().cityId)
    }

    @Test
    fun theQueryIsDebouncedTrimmedAndSentOnce() = parity {
        requests.receive(); settle()
        vm.onQueryChange("c"); vm.onQueryChange("co"); vm.onQueryChange("  cof  ")
        val sent = requests.receive()
        assertEquals("cof", sent.query)
        assertNull(requests.tryReceive().getOrNull())
        assertEquals("  cof  ", vm.uiState.value.query)
    }

    @Test
    fun aBlankQueryIsSentAsNoQuery() = parity {
        requests.receive(); settle()
        vm.onQueryChange("x"); assertEquals("x", requests.receive().query)
        vm.onQueryChange("   "); assertNull(requests.receive().query)
    }

    @Test
    fun everyFilterReachesTheRequestAndNearbyDropsTheCity() = parity {
        requests.receive(); awaitShops()
        vm.applyFilters(
            FeedFiltersUi(
                cityId = "brest", coffeeFocus = "specialty", priceRange = 2, minRating = 4.5, openOnly = true, newOnly = true,
                roasterIds = setOf("r1"), beanIds = setOf("b1"), equipmentIds = setOf("e1"), brewMethodIds = setOf("m1"), tagIds = setOf("t1"),
            ),
        )
        val sent = requests.receive()
        assertEquals("brest", sent.cityId); assertEquals("specialty", sent.coffeeFocus); assertEquals(2, sent.priceRange); assertEquals(4.5, sent.minRating)
        assertTrue(sent.openOnly); assertTrue(sent.newOnly); assertFalse(sent.visitedOnly); assertFalse(sent.favoritesOnly); assertNull(sent.origin)
        assertEquals(listOf("r1"), sent.roasterIds); assertEquals(listOf("b1"), sent.beanIds); assertEquals(listOf("e1"), sent.equipmentIds)
        assertEquals(listOf("m1"), sent.brewMethodIds); assertEquals(listOf("t1"), sent.tagIds)
        awaitShops()

        vm.onUserLocationChanged(GeoPoint(53.9, 27.56))
        vm.applyFilters(vm.uiState.value.filters.copy(nearbyOnly = true))
        val nearby = requests.receive()
        assertNull(nearby.cityId); assertEquals(53.9, nearby.origin?.latitude); assertEquals(27.56, nearby.origin?.longitude)
    }

    @Test
    fun pagingAppendsPagesAndStopsAtTheLastPage() = parity(handler = { filters -> pageOf((1..20).map { shop("p${filters.page}-$it") }, filters.page, pages = 2) }) {
        requests.receive(); awaitShops()
        assertTrue(vm.uiState.value.hasMore)
        vm.loadMore()
        assertEquals(2, requests.receive().page)
        val all = vm.uiState.first { it.shops.size == 40 && !it.isLoadingMore }
        assertFalse(all.hasMore); assertEquals(2, all.currentPage)
        vm.loadMore()
        assertNull(requests.tryReceive().getOrNull())
    }

    @Test
    fun loadMoreIsIgnoredWhileAPageIsLoading() {
        val gate = CompletableDeferred<Unit>()
        parity(handler = { filters ->
            if (filters.page == 2) gate.await()
            pageOf(listOf(shop("p${filters.page}")), filters.page, pages = 3)
        }) {
            requests.receive(); awaitShops()
            vm.loadMore(); assertEquals(2, requests.receive().page)
            vm.loadMore(); vm.loadMore()
            assertNull(requests.tryReceive().getOrNull())
            gate.complete(Unit)
            vm.uiState.first { it.shops.size == 2 && !it.isLoadingMore }
        }
    }

    @Test
    fun aNewResetLoadCancelsTheOlderOne() {
        val older = CompletableDeferred<Unit>()
        parity(handler = { filters ->
            if (filters.minRating == 3.0) older.await()
            pageOf(listOf(shop(if (filters.minRating == 3.0) "old" else "new")))
        }) {
            requests.receive(); awaitShops()
            vm.setMinRating(3.0); requests.receive()
            vm.setMinRating(4.0); requests.receive()
            vm.uiState.first { it.shops.map(CoffeeShop::id) == listOf("new") && !it.isLoading && !it.isRefreshing }
            older.complete(Unit)
            kotlinx.coroutines.delay(100)
            assertEquals(listOf("new"), vm.uiState.value.shops.map { it.id })
        }
    }

    @Test
    fun aFailedRefreshKeepsTheShopsAndShowsTheFixedText() {
        var fail = false
        parity(handler = { if (fail) Result.failure(IllegalStateException("offline")) else pageOf(listOf(shop("a"))) }) {
            requests.receive(); awaitShops()
            fail = true
            vm.refresh(); requests.receive()
            val state = vm.uiState.first { it.error != null }
            assertEquals("Не удалось загрузить кофейни", state.error)
            assertEquals(listOf("a"), state.shops.map { it.id }); assertFalse(state.isLoading); assertFalse(state.isRefreshing)
            assertTrue(state.showsRefreshError)
            fail = false
            vm.refresh()
            assertEquals(1, requests.receive().page)
            val recovered = awaitShops()
            assertFalse(recovered.showsRefreshError)
            assertNull(recovered.error)
        }
    }

    @Test
    fun aFailedFirstLoadShowsTheErrorWithoutShops() = parity(handler = { Result.failure(IllegalStateException("offline")) }) {
        requests.receive()
        val state = vm.uiState.first { it.error != null }
        assertTrue(state.shops.isEmpty()); assertFalse(state.isLoading)
        assertFalse(state.showsRefreshError)
    }

    @Test
    fun aGuestGetsTheSignInScreenForVisitedAndFavoritesAndNothingChanges() = parity(loggedIn = false) {
        requests.receive(); awaitShops()
        for (toggle in listOf(vm::toggleVisitedOnly, vm::toggleFavoritesOnly)) {
            val event = scope.async(start = CoroutineStart.UNDISPATCHED) { Navigator.navigationEvents.first() }
            toggle()
            assertEquals(Navigator.NavEvent.NavigateTo(Navigator.Screen.Auth), withTimeout(2_000) { event.await() })
        }
        assertFalse(vm.uiState.value.filters.visitedOnly); assertFalse(vm.uiState.value.filters.favoritesOnly)
        assertNull(requests.tryReceive().getOrNull())
    }

    @Test
    fun everyQuickFilterReloadsTheServerSearchWithoutLocation() = parity {
        requests.receive()
        awaitShops()
        vm.toggleOpenOnly()
        assertTrue(requests.receive().openOnly)
        vm.toggleNewOnly()
        assertTrue(requests.receive().newOnly)
        vm.toggleVisitedOnly()
        assertTrue(requests.receive().visitedOnly)
        vm.toggleFavoritesOnly()
        val filters = requests.receive()
        assertTrue(filters.favoritesOnly)
        assertNull(filters.origin)
    }

    @Test
    fun clearingTheFiltersKeepsTheCityDropsTheQueryAndReloads() = parity {
        requests.receive(); settle()
        vm.onQueryChange("abc"); requests.receive()
        vm.setCoffeeFocus("cafe"); requests.receive()
        vm.clearFilters()
        val sent = requests.receive()
        assertEquals("minsk", sent.cityId); assertNull(sent.query); assertNull(sent.coffeeFocus)
        assertEquals(0, vm.uiState.value.activeFilterCount); assertEquals("", vm.uiState.value.query)
    }

    @Test
    fun cancellingTheSearchWithFiltersClearsThemOtherwiseOnlyTheQuery() = parity {
        requests.receive(); settle()
        vm.activateSearch(); vm.setCoffeeFocus("cafe"); requests.receive()
        vm.cancelSearch()
        assertEquals(0, vm.uiState.value.activeFilterCount); assertFalse(vm.uiState.value.isSearchActive)
        requests.receive()
        vm.activateSearch(); vm.onQueryChange("tea"); requests.receive()
        vm.cancelSearch()
        assertEquals("", vm.uiState.value.query); assertFalse(vm.uiState.value.isSearchActive)
    }

    @Test
    fun aChangeOfTheSavedCityReloadsTheList() = parity {
        requests.receive(); awaitShops()
        preference.select("brest")
        val sent = requests.receive()
        assertEquals("brest", sent.cityId)
        assertEquals("brest", vm.uiState.first { it.filters.cityId == "brest" }.filters.cityId)
    }

    @Test
    fun theFavoriteIsOptimisticOnePerShopAndRestoredOnFailure() {
        val gate = CompletableDeferred<Result<Unit>>()
        parity(addFavorite = { gate.await() }) {
            requests.receive(); awaitShops()
            val target = vm.uiState.value.shops.first()
            vm.toggleFavorite(target); vm.toggleFavorite(target)
            vm.uiState.first { state -> state.shops.first().isFavorite && target.id in state.favoriteUpdates }
            gate.complete(Result.failure(IllegalStateException("offline")))
            vm.uiState.first { state -> !state.shops.first().isFavorite && state.favoriteUpdates.isEmpty() }
            assertEquals(1, favorites.added.size)
        }
    }

    @Test
    fun aSuccessfulFavoriteStaysAndCallsTheRepositoryOnce() = parity {
        requests.receive(); awaitShops()
        val target = vm.uiState.value.shops.first()
        vm.toggleFavorite(target)
        vm.uiState.first { state -> state.shops.first().isFavorite && state.favoriteUpdates.isEmpty() }
        assertEquals(listOf(target.id), favorites.added)
        vm.toggleFavorite(vm.uiState.value.shops.first())
        vm.uiState.first { state -> !state.shops.first().isFavorite && state.favoriteUpdates.isEmpty() }
        assertEquals(listOf(target.id), favorites.removed)
    }

    @Test
    fun aGuestFavoriteGoesToSignInAndChangesNothing() = parity(loggedIn = false) {
        requests.receive(); awaitShops()
        val event = scope.async(start = CoroutineStart.UNDISPATCHED) { Navigator.navigationEvents.first() }
        vm.toggleFavorite(vm.uiState.value.shops.first())
        assertEquals(Navigator.NavEvent.NavigateTo(Navigator.Screen.Auth), withTimeout(2_000) { event.await() })
        assertFalse(vm.uiState.value.shops.first().isFavorite); assertTrue(favorites.added.isEmpty())
    }

    @Test
    fun aLocationOutsideTheValidRangeIsIgnored() = parity {
        requests.receive(); awaitShops()
        vm.onUserLocationChanged(GeoPoint(91.0, 27.56)); vm.onUserLocationChanged(GeoPoint(53.9, 181.0)); vm.onUserLocationChanged(GeoPoint(Double.NaN, 27.0))
        vm.toggleNearbyOnly()
        assertFalse(vm.uiState.value.filters.nearbyOnly)
        assertNull(requests.tryReceive().getOrNull())
    }

    private fun parity(
        loggedIn: Boolean = true,
        savedCity: String? = null,
        handler: suspend (ShopFilters) -> Result<PagedResult<CoffeeShop>> = { pageOf(listOf(shop("a"), shop("b"))) },
        addFavorite: suspend () -> Result<Unit> = { Result.success(Unit) },
        test: suspend ParityFixture.() -> Unit,
    ) = runBlocking {
        val fixture = ParityFixture(loggedIn, savedCity, handler, addFavorite).also { it.scope = this }
        try { withTimeout(10_000) { fixture.test() } } finally { fixture.vm.close() }
    }
}

private fun shop(id: String, favorite: Boolean = false, visited: Boolean = false, open: Boolean = false, new: Boolean = false) = CoffeeShop(
    id = id, title = "Shop $id", rating = null, cityName = "Минск", priceRange = null, photoUrl = null,
    isFavorite = favorite, isVisited = visited, isOpen = open, isNew = new,
)

private fun pageOf(shops: List<CoffeeShop>, current: Int = 1, pages: Int = 1): Result<PagedResult<CoffeeShop>> =
    Result.success(PagedResult(shops, totalCount = shops.size * pages, totalPages = pages, currentPage = current))

private class ParityFixture(
    loggedIn: Boolean, savedCity: String?, handler: suspend (ShopFilters) -> Result<PagedResult<CoffeeShop>>, addFavorite: suspend () -> Result<Unit>,
) {
    lateinit var scope: CoroutineScope
    val requests = Channel<ShopFilters>(Channel.UNLIMITED)
    val favorites = ParityFavorites(addFavorite)
    val preference = CityPreference(ParitySettings(savedCity))
    val vm = FeedViewModel(ParityShops(requests, handler), favorites, preference, ParitySession(loggedIn))

    suspend fun awaitShops(): FeedUiState = vm.uiState.first { !it.isLoading && !it.isRefreshing && it.shops.isNotEmpty() }

    /** The view model drops the first debounced query (the initial blank one); a query typed earlier than the first window would be swallowed. */
    suspend fun settle() { awaitShops(); kotlinx.coroutines.delay(500) }
}

private class ParityShops(
    private val requests: Channel<ShopFilters>, private val handler: suspend (ShopFilters) -> Result<PagedResult<CoffeeShop>>,
) : ShopRepository {
    override suspend fun getCatalogs() = Result.success(ShopCatalogs(cities = listOf(City("minsk", "Минск"), City("brest", "Брест"))))
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> { requests.send(filters); return handler(filters) }
    override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> = error("Unused")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("Unused")
    override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("Unused")
    override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("Unused")
    override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("Unused")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("Unused")
}

private class ParityFavorites(private val add: suspend () -> Result<Unit>) : FavoriteRepository {
    val added = mutableListOf<String>()
    val removed = mutableListOf<String>()
    override suspend fun getFavoriteIds() = emptySet<String>()
    override suspend fun isFavorite(shopId: String) = false
    override suspend fun getFavorites() = Result.success(emptyList<CoffeeShopDetails>())
    override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> { added += shop.id; return add() }
    override suspend fun removeFavorite(shopId: String): Result<Unit> { removed += shopId; return Result.success(Unit) }
    override suspend fun clearAll() = Unit
}

private class ParitySession(private val loggedIn: Boolean) : SessionRepository {
    override fun peekSession(): Session? = null
    override fun applySession(session: Session?) = Unit
    override fun isActiveSession(session: Session?) = loggedIn
    override suspend fun getSession(): Session? = null
    override suspend fun persistSession(session: Session?) = Unit
    override suspend fun saveSession(session: Session?) = Unit
    override suspend fun warmCache() = Unit
    override fun observeSession() = flowOf<Session?>(null)
    override suspend fun isLoggedIn() = loggedIn
}

private class ParitySettings(private val saved: String?) : SettingRepository {
    override suspend fun save(model: Setting) = Unit
    override suspend fun read(key: String): Setting? = saved?.takeIf { key == "selected_city_id" }?.let { Setting(key, it) }
    override fun readFlow(key: String) = flowOf<Setting?>(null)
    override suspend fun readAll() = emptyList<Setting>()
    override fun readAllFlow() = flowOf(emptyList<Setting>())
    override suspend fun delete(key: String) = Unit
}
