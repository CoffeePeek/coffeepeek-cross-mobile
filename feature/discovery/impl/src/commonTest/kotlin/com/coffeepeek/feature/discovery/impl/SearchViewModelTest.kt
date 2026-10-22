package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.api.DiscoveryGate
import com.coffeepeek.feature.discovery.api.FilterAction
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.impl.search.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val repository = FakeRepository()
    private val favorites = FakeFavorites()
    private val location = FakeLocation()
    private val analytics = RecordingAnalytics()
    private val account = FakeAccount()
    private var city = FakeCity()
    private val here = Coordinates(53.9, 27.56)

    private fun TestScope.model(): SearchViewModel =
        SearchViewModel(repository, favorites, city, location, account, analytics).also { store.put("search", it); runCurrent() }

    private fun SearchViewModel.send(vararg actions: SearchAction, scope: TestScope) { actions.forEach { onAction(it); scope.runCurrent() } }
    private val SearchViewModel.now get() = state.value

    // ---- start ----

    @Test fun theFirstSearchWaitsForTheCatalogsAndUsesTheResolvedCity() = runTest(dispatcher) {
        city = FakeCity("brest")
        val gate = CompletableDeferred<Result<DiscoveryCatalogs>>()
        repository.catalogs = { gate.await() }
        val vm = model()
        assertTrue(vm.now.isLoading); assertTrue(repository.queries.isEmpty())
        gate.complete(Result.success(DiscoveryCatalogs(cities = listOf(minskCity, brestCity)))); runCurrent()
        assertEquals("brest", repository.queries.single().filters.cityId); assertEquals(1, repository.queries.single().page); assertEquals(20, repository.queries.single().pageSize)
        assertEquals(listOf("a", "b"), vm.now.shops.map { it.id }); assertFalse(vm.now.isLoading); assertEquals(brestCity, vm.now.selectedCity)
    }

    @Test fun aSavedCityThatIsGoneFallsBackToTheFirstCity() = runTest(dispatcher) {
        city = FakeCity("removed")
        model()
        assertEquals("minsk", repository.queries.single().filters.cityId)
    }

    @Test fun aFailedCatalogStillLetsTheFirstSearchStartAndTheFilterSheetAsksAgain() = runTest(dispatcher) {
        repository.catalogs = { Result.failure(DiscoveryFailure(FailureKind.Other)) }
        val vm = model()
        assertEquals(1, repository.queries.size); assertNull(repository.queries.single().filters.cityId); assertTrue(vm.now.catalogs.cities.isEmpty())
        repository.catalogs = { Result.success(DiscoveryCatalogs(cities = listOf(minskCity))) }
        vm.send(SearchAction.ToggleFilters, scope = this)
        assertTrue(vm.now.showFilters); assertEquals(2, repository.catalogCalls); assertEquals(1, vm.now.catalogs.cities.size)
        assertEquals(2, repository.queries.size); assertEquals("minsk", repository.queries.last().filters.cityId)
        assertEquals(1, analytics.filtersOpened)
    }

    // ---- the query ----

    @Test fun theQueryIsDebouncedTrimmedAndSentOnceEvenInTheFirstWindow() = runTest(dispatcher) {
        val vm = model()
        vm.send(SearchAction.Query("c"), SearchAction.Query("co"), SearchAction.Query("  cof  "), scope = this)
        advanceTimeBy(399); runCurrent(); assertEquals(1, repository.queries.size)
        advanceTimeBy(2); runCurrent()
        assertEquals(2, repository.queries.size); assertEquals("  cof  ", repository.queries.last().text); assertEquals("  cof  ", vm.now.query)
        assertEquals("cof|", analytics.searches.last().substringBefore("city").substringBefore("isOpen").trimEnd(',').let { it.take(4) })
    }

    @Test fun aBlankQueryAndAWhitespaceOnlyChangeDoNotReload() = runTest(dispatcher) {
        val vm = model()
        vm.send(SearchAction.Query("   "), scope = this); advanceTimeBy(500); runCurrent()
        assertEquals(1, repository.queries.size)
        vm.send(SearchAction.Query("tea"), scope = this); advanceTimeBy(500); runCurrent()
        assertEquals(2, repository.queries.size)
        vm.send(SearchAction.Query("tea  "), scope = this); advanceTimeBy(500); runCurrent()
        assertEquals(2, repository.queries.size)
        vm.send(SearchAction.Query(""), scope = this); advanceTimeBy(500); runCurrent()
        assertEquals(3, repository.queries.size); assertTrue(searchText(repository.queries.last()) == null)
    }

    private fun searchText(query: ShopQuery) = query.text?.trim()?.takeIf(String::isNotEmpty)

    // ---- the filters ----

    @Test fun everyFilterReachesTheQueryAndNearbyDropsTheCityAndCarriesTheOrigin() = runTest(dispatcher) {
        val vm = model()
        val filters = DiscoveryFilters(
            cityId = "brest", kind = ShopKind.Specialty, openOnly = true, newOnly = true, priceLevel = PriceLevel.Moderate, minRating = 4.5,
            roasterIds = setOf("r"), beanIds = setOf("b"), equipmentIds = setOf("e"), brewMethodIds = setOf("m"), tagIds = setOf("t"),
        )
        vm.send(SearchAction.ApplyFilters(filters), scope = this)
        assertEquals(filters, repository.queries.last().filters); assertNull(repository.queries.last().origin)
        location.flow.value = here; runCurrent()
        vm.send(SearchAction.ToggleNearby, scope = this)
        val nearby = repository.queries.last()
        assertNull(nearby.filters.cityId); assertEquals(here, nearby.origin); assertTrue(nearby.filters.nearbyOnly); assertEquals(1, location.refreshes)
        vm.send(SearchAction.ToggleNearby, scope = this)
        assertEquals("brest", repository.queries.last().filters.cityId); assertNull(repository.queries.last().origin); assertEquals(1, location.refreshes)
    }

    @Test fun catalogSelectionsToggleAndEachChangeReloads() = runTest(dispatcher) {
        val vm = model()
        for (kind in CatalogKind.entries) vm.send(SearchAction.ToggleCatalog(kind, "x"), scope = this)
        val f = vm.now.filters
        assertEquals(setOf("x"), f.roasterIds); assertEquals(setOf("x"), f.beanIds); assertEquals(setOf("x"), f.equipmentIds); assertEquals(setOf("x"), f.brewMethodIds); assertEquals(setOf("x"), f.tagIds)
        assertEquals(6, repository.queries.size); assertEquals(5, vm.now.activeFilterCount)
        vm.send(SearchAction.ToggleCatalog(CatalogKind.Tag, "x"), scope = this)
        assertTrue(vm.now.filters.tagIds.isEmpty())
        vm.send(SearchAction.SetKind(ShopKind.Cafe), SearchAction.SetPrice(PriceLevel.Cheap), SearchAction.SetMinRating(4.0), scope = this)
        assertEquals(ShopKind.Cafe, repository.queries.last().filters.kind); assertEquals(PriceLevel.Cheap, repository.queries.last().filters.priceLevel); assertEquals(4.0, repository.queries.last().filters.minRating)
    }

    @Test fun aGuestGetsTheSignInEventAndTheGateForVisitedAndFavoritesAndNothingChanges() = runTest(dispatcher) {
        account.key.value = null
        val vm = model()
        val events = async(start = CoroutineStart.UNDISPATCHED) { listOf(vm.events.first(), vm.events.first()) }
        vm.send(SearchAction.ToggleVisited, SearchAction.ToggleFavorites, scope = this)
        assertEquals(listOf(SearchEvent.SignInRequired, SearchEvent.SignInRequired), events.await())
        assertEquals(listOf(DiscoveryGate.VisitedFilter, DiscoveryGate.FavoritesFilter), analytics.gates)
        assertFalse(vm.now.filters.visitedOnly); assertFalse(vm.now.filters.favoritesOnly); assertEquals(1, repository.queries.size)
    }

    @Test fun theQuickSwitchesFilterTheLoadedPageAndReloadOnlyWithNearby() = runTest(dispatcher) {
        repository.search = { page(listOf(shop("a", favorite = true, open = true), shop("b", visited = true), shop("c", new = true))) }
        favorites.flow.value = setOf("a")
        val vm = model()
        vm.send(SearchAction.ToggleFavorites, scope = this)
        assertEquals(listOf("a"), vm.now.visibleShops.map { it.id }); assertEquals(1, repository.queries.size)
        vm.send(SearchAction.ToggleFavorites, SearchAction.ToggleVisited, scope = this)
        assertEquals(listOf("b"), vm.now.visibleShops.map { it.id })
        vm.send(SearchAction.ToggleNew, SearchAction.ToggleOpen, scope = this)
        assertTrue(vm.now.visibleShops.isEmpty()); assertEquals(1, repository.queries.size)
        location.flow.value = here; runCurrent()
        vm.send(SearchAction.ToggleNearby, scope = this); assertEquals(2, repository.queries.size)
        vm.send(SearchAction.ToggleOpen, scope = this)
        assertEquals(3, repository.queries.size); assertFalse(repository.queries.last().filters.openOnly)
    }

    @Test fun clearingTheFiltersKeepsTheCityDropsTheQueryAndReloadsOnce() = runTest(dispatcher) {
        val vm = model()
        vm.send(SearchAction.Query("abc"), scope = this); advanceTimeBy(500); runCurrent()
        vm.send(SearchAction.SetKind(ShopKind.Cafe), scope = this)
        val before = repository.queries.size
        vm.send(SearchAction.ClearFilters, scope = this); advanceTimeBy(500); runCurrent()
        assertEquals(before + 1, repository.queries.size)
        val sent = repository.queries.last()
        assertEquals("minsk", sent.filters.cityId); assertNull(sent.filters.kind); assertEquals("", sent.text)
        assertEquals(0, vm.now.activeFilterCount); assertEquals("", vm.now.query)
        assertEquals(FilterAction.Reset, analytics.filters.last().third)
    }

    @Test fun cancellingTheSearchClearsTheFiltersOrOtherwiseTheQuery() = runTest(dispatcher) {
        val vm = model()
        vm.send(SearchAction.ActivateSearch, SearchAction.SetKind(ShopKind.Cafe), SearchAction.Query("x"), scope = this)
        vm.send(SearchAction.CancelSearch, scope = this)
        assertEquals(0, vm.now.activeFilterCount); assertEquals("", vm.now.query); assertFalse(vm.now.searchActive)
        vm.send(SearchAction.ActivateSearch, SearchAction.Query("tea"), scope = this)
        assertTrue(vm.now.searchActive)
        vm.send(SearchAction.CancelSearch, scope = this)
        assertEquals("", vm.now.query); assertFalse(vm.now.searchActive)
    }

    @Test fun aChangeOfTheSavedCityReloadsTheList() = runTest(dispatcher) {
        val vm = model()
        city.flow.value = "brest"; runCurrent()
        assertEquals("brest", repository.queries.last().filters.cityId); assertEquals(2, repository.queries.size); assertEquals("brest", vm.now.filters.cityId)
    }

    // ---- the list ----

    @Test fun pagingAppendsPagesStopsAtTheLastPageAndIgnoresLoadMoreWhileLoading() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.search = { q -> if (q.page == 2) gate.await(); page((1..20).map { shop("p${q.page}-$it") }, q.page, pages = 2) }
        val vm = model()
        assertTrue(vm.now.hasMore)
        vm.send(SearchAction.LoadMore, SearchAction.LoadMore, SearchAction.LoadMore, scope = this)
        assertEquals(2, repository.queries.size); assertEquals(2, repository.queries.last().page); assertTrue(vm.now.isLoadingMore)
        gate.complete(Unit); runCurrent()
        assertEquals(40, vm.now.shops.size); assertFalse(vm.now.hasMore); assertEquals(2, vm.now.currentPage)
        vm.send(SearchAction.LoadMore, scope = this)
        assertEquals(2, repository.queries.size)
    }

    @Test fun aNewResetLoadCancelsTheOlderOne() = runTest(dispatcher) {
        val old = CompletableDeferred<Unit>()
        repository.search = { q -> if (q.filters.minRating == 3.0) old.await(); page(listOf(shop(if (q.filters.minRating == 3.0) "old" else "new"))) }
        val vm = model()
        vm.send(SearchAction.SetMinRating(3.0), SearchAction.SetMinRating(4.0), scope = this)
        old.complete(Unit); runCurrent()
        assertEquals(listOf("new"), vm.now.shops.map { it.id })
    }

    @Test fun aFailedRefreshKeepsTheShopsAndTheServerMessageOrADefaultIsKept() = runTest(dispatcher) {
        val vm = model()
        repository.search = { Result.failure(DiscoveryFailure(FailureKind.Other, "Сервер занят")) }
        vm.send(SearchAction.Refresh, scope = this)
        assertEquals(SearchError("Сервер занят"), vm.now.error); assertEquals(2, vm.now.shops.size); assertFalse(vm.now.isRefreshing)
        repository.search = { Result.failure(IllegalStateException("offline")) }
        vm.send(SearchAction.Refresh, scope = this)
        assertEquals(SearchError(null), vm.now.error)
        assertEquals(2, analytics.failures.size)
    }

    @Test fun aFailedFirstLoadShowsTheErrorWithoutShops() = runTest(dispatcher) {
        repository.search = { Result.failure(DiscoveryFailure(FailureKind.Other)) }
        val vm = model()
        assertEquals(SearchError(null), vm.now.error); assertTrue(vm.now.shops.isEmpty()); assertFalse(vm.now.isLoading)
    }

    // ---- favorites ----

    @Test fun theFavoritesOfTheAccountDecideTheFlagsAcrossPages() = runTest(dispatcher) {
        favorites.flow.value = setOf("b"); val vm = model()
        assertEquals(listOf(false, true), vm.now.shops.map { it.isFavorite })
        favorites.flow.value = setOf("a"); runCurrent()
        assertEquals(listOf(true, false), vm.now.shops.map { it.isFavorite })
    }

    @Test fun theFavoriteIsOptimisticOnePerShopAndRestoredOnFailure() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Unit>>()
        favorites.set = { _, _ -> gate.await() }
        val vm = model()
        vm.send(SearchAction.ToggleFavorite("a"), SearchAction.ToggleFavorite("a"), scope = this)
        assertTrue(vm.now.shops.first().isFavorite); assertEquals(setOf("a"), vm.now.favoriteUpdates)
        gate.complete(Result.failure(IllegalStateException("offline"))); runCurrent()
        assertFalse(vm.now.shops.first().isFavorite); assertTrue(vm.now.favoriteUpdates.isEmpty()); assertEquals(listOf("a" to true), favorites.changes)
    }

    @Test fun aSuccessfulFavoriteStaysAndAnotherToggleRemovesIt() = runTest(dispatcher) {
        val vm = model()
        vm.send(SearchAction.ToggleFavorite("a"), scope = this)
        assertTrue(vm.now.shops.first().isFavorite)
        vm.send(SearchAction.ToggleFavorite("a"), scope = this)
        assertFalse(vm.now.shops.first().isFavorite); assertEquals(listOf("a" to true, "a" to false), favorites.changes)
    }

    @Test fun aFailedFavoriteDoesNotUndoWhatTheAccountsFavoritesAlreadySay() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Unit>>()
        favorites.set = { _, _ -> gate.await() }
        val vm = model()
        vm.send(SearchAction.ToggleFavorite("a"), scope = this)
        favorites.flow.value = setOf("a"); runCurrent()
        gate.complete(Result.failure(IllegalStateException("offline"))); runCurrent()
        assertTrue(vm.now.shops.first().isFavorite)
    }

    @Test fun aGuestFavoriteGoesToSignInAndChangesNothing() = runTest(dispatcher) {
        account.key.value = null
        val vm = model()
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.events.first() }
        vm.send(SearchAction.ToggleFavorite("a"), scope = this)
        assertEquals(SearchEvent.SignInRequired, event.await())
        assertFalse(vm.now.shops.first().isFavorite); assertTrue(favorites.changes.isEmpty())
    }

    // ---- location and account ----

    @Test fun anInvalidLocationIsIgnoredAndNearbyNeedsAPosition() = runTest(dispatcher) {
        val vm = model()
        location.flow.value = Coordinates(91.0, 27.0); runCurrent()
        vm.send(SearchAction.ToggleNearby, scope = this)
        assertFalse(vm.now.canUseNearby); assertFalse(vm.now.filters.nearbyOnly); assertEquals(1, repository.queries.size)
    }

    @Test fun aNewPositionWhileNearbyIsOnClearsTheListAndReloads() = runTest(dispatcher) {
        val vm = model()
        location.flow.value = here; runCurrent()
        vm.send(SearchAction.ToggleNearby, scope = this)
        val moved = Coordinates(53.91, 27.57)
        location.flow.value = moved; runCurrent()
        assertEquals(moved, repository.queries.last().origin); assertEquals(3, repository.queries.size)
    }

    @Test fun aPositionAndADeniedPermissionAreReportedWithTheDiscoveryPlacement() = runTest(dispatcher) {
        val vm = model()
        location.flow.value = here; runCurrent()
        vm.send(SearchAction.LocationDenied, SearchAction.LocationGranted, scope = this)
        assertEquals(listOf(Triple("discovery", false, GeolocationOutcome.Available), Triple("discovery", false, GeolocationOutcome.Unavailable)), analytics.geolocations)
        assertEquals(1, location.refreshes)
    }

    @Test fun aNewPositionWithoutNearbyKeepsTheShownShops() = runTest(dispatcher) {
        val vm = model()
        val shown = vm.now.shops
        location.flow.value = here; runCurrent()
        assertTrue(vm.now.canUseNearby); assertSame(shown, vm.now.shops); assertEquals(1, repository.queries.size)
    }

    @Test fun aChangeOfAccountDropsVisitedAndFavoritesAndReloadsAndALateAnswerIsIgnored() = runTest(dispatcher) {
        val late = CompletableDeferred<Unit>()
        val vm = model()
        vm.send(SearchAction.ToggleVisited, SearchAction.ToggleFavorites, scope = this)
        assertTrue(vm.now.filters.visitedOnly)
        var calls = 0
        repository.search = { val n = ++calls; if (n == 1) withContext(NonCancellable) { late.await() }; page(listOf(shop(if (n == 1) "old" else "new"))) }
        vm.send(SearchAction.Refresh, scope = this)
        account.key.value = "two"; runCurrent()
        assertFalse(vm.now.filters.visitedOnly); assertFalse(vm.now.filters.favoritesOnly)
        late.complete(Unit); runCurrent()
        assertEquals(listOf("new"), vm.now.shops.map { it.id })
    }

    // ---- analytics and events ----

    @Test fun theSearchIsReportedWithItsKeysAndOutcomeAndOpeningAShopReportsItsPosition() = runTest(dispatcher) {
        val vm = model()
        assertEquals("|city,isOpen,isNew,visitedOnly,favoritesOnly|false|minsk", analytics.searches.single()); assertEquals(listOf(2), analytics.results)
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.events.first() }
        vm.send(SearchAction.OpenShop("b"), scope = this)
        assertEquals(SearchEvent.OpenShop("b"), event.await()); assertEquals<List<Int?>>(listOf(2), analytics.opened)
    }

    @Test fun filterChangesReportTheKeysThatChanged() = runTest(dispatcher) {
        val vm = model()
        vm.send(SearchAction.SetKind(ShopKind.Cafe), scope = this)
        val (keys, changed, action) = analytics.filters.single()
        assertTrue("type" in keys && "city" in keys); assertEquals(listOf("type"), changed); assertEquals(FilterAction.Apply, action)
        vm.send(SearchAction.ToggleNew, scope = this)
        assertEquals(listOf("isNew"), analytics.filters.last().second)
    }

    @Test fun theAnalyticsValuesFollowTheLegacyKeys() {
        assertEquals(setOf("isOpen", "isNew", "visitedOnly", "favoritesOnly"), DiscoveryFilters().analyticsValues().keys)
        val all = DiscoveryFilters(
            cityId = "minsk", kind = ShopKind.CoffeeBar, openOnly = true, nearbyOnly = true, priceLevel = PriceLevel.Expensive, minRating = 4.0,
            roasterIds = setOf("r"), beanIds = setOf("b"), equipmentIds = setOf("e"), brewMethodIds = setOf("m"), tagIds = setOf("t"),
        ).analyticsValues()
        assertEquals(setOf("type", "isOpen", "isNew", "visitedOnly", "favoritesOnly", "nearby", "priceRange", "minRating", "roasters", "beans", "equipments", "brewMethods", "tags"), all.keys)
        assertEquals("coffee_bar", all["type"]); assertEquals(3, all["priceRange"])
        assertEquals("minsk", DiscoveryFilters(cityId = "minsk").analyticsValues()["city"])
        assertEquals(listOf("isNew", "type"), changedFilterKeys(DiscoveryFilters().analyticsValues(), DiscoveryFilters(newOnly = true, kind = ShopKind.Cafe).analyticsValues()))
    }
}
