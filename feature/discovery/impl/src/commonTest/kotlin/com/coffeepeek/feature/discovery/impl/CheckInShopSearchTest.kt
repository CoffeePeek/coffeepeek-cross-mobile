package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.api.DiscoveryRecentShops
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.search.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CheckInShopSearchTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    private val location = FakeLocation()
    private val favorites = FakeFavorites()
    private val account = FakeAccount()
    private val analytics = RecordingAnalytics()
    private val here = Coordinates(53.9, 27.56)

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun TestScope.model(recentShops: DiscoveryRecentShops? = null): SearchViewModel {
        val model = SearchViewModel(repository, favorites, FakeCity("brest"), location, account,
            analytics, SearchPurpose.CheckInPicker, recentShops = recentShops)
        store.put("picker", model)
        runCurrent()
        return model
    }

    @Test fun openingAndReceivingTheFirstPositionKeepsTheCityWithoutPermissionRequest() = runTest(dispatcher) {
        val model = model()
        assertFalse(model.state.value.filters.nearbyOnly)
        location.flow.value = here
        runCurrent()
        assertEquals(here, model.state.value.userLocation)
        assertFalse(model.state.value.filters.nearbyOnly)
        assertEquals(0, model.state.value.locationPermissionRequest)
        assertNull(repository.queries.single().origin)
        assertEquals("brest", repository.queries.single().filters.cityId)
        assertTrue(analytics.filters.isEmpty())
    }

    @Test fun cachedPositionIsAvailableWithoutSelectingNearby() = runTest(dispatcher) {
        location.flow.value = here
        val model = model()
        assertTrue(model.state.value.canUseNearby)
        assertFalse(model.state.value.filters.nearbyOnly)
        assertEquals(0, model.state.value.locationPermissionRequest)
        assertEquals("brest", repository.queries.single().filters.cityId)
        assertNull(repository.queries.single().origin)
        model.onAction(SearchAction.ToggleNearby)
        runCurrent()
        assertTrue(model.state.value.filters.nearbyOnly)
        assertEquals(0, model.state.value.locationPermissionRequest)
        assertEquals(here, repository.queries.last().origin)
        assertNull(repository.queries.last().filters.cityId)
    }

    @Test fun furtherPositionsDoNotUndoTheUsersNearbyChoice() = runTest(dispatcher) {
        location.flow.value = here
        val model = model()
        model.onAction(SearchAction.ToggleNearby)
        runCurrent()
        assertTrue(model.state.value.filters.nearbyOnly)
        model.onAction(SearchAction.ToggleNearby)
        runCurrent()
        val requests = repository.queries.size
        location.flow.value = Coordinates(53.91, 27.57)
        runCurrent()
        assertFalse(model.state.value.filters.nearbyOnly)
        assertEquals(requests, repository.queries.size)
        assertEquals("brest", repository.queries.last().filters.cityId)
        assertEquals(2, analytics.filters.size)
    }

    @Test fun deniedOrInvalidLocationKeepsCitySearchWithoutDiscoveryGeoEvents() = runTest(dispatcher) {
        val model = model()
        val cityShops = model.state.value.shops
        model.onAction(SearchAction.ToggleNearby)
        runCurrent()
        assertEquals(1, model.state.value.locationPermissionRequest)
        assertTrue(model.state.value.nearbyRequested)
        location.flow.value = Coordinates(91.0, 27.0)
        model.onAction(SearchAction.LocationDenied)
        runCurrent()
        assertFalse(model.state.value.canUseNearby)
        assertFalse(model.state.value.filters.nearbyOnly)
        assertTrue(model.state.value.locationUnavailable)
        assertSame(cityShops, model.state.value.shops)
        assertEquals("brest", repository.queries.single().filters.cityId)
        assertTrue(analytics.geolocations.isEmpty())
    }

    @Test fun explicitNearbyWaitsForTheGrantedPosition() = runTest(dispatcher) {
        val model = model()
        model.onAction(SearchAction.ToggleNearby)
        runCurrent()
        model.onAction(SearchAction.LocationGranted)
        runCurrent()
        assertEquals(1, model.state.value.locationPermissionRequest)
        assertEquals(1, location.refreshes)
        assertFalse(model.state.value.filters.nearbyOnly)
        location.flow.value = here
        runCurrent()
        assertTrue(model.state.value.filters.nearbyOnly)
        assertEquals(here, repository.queries.last().origin)
        assertNull(repository.queries.last().filters.cityId)
    }

    @Test fun pickerReturnsPublicSlugWithoutAddingDiscoveryOpenAnalytics() = runTest(dispatcher) {
        val model = model()
        val event = async(start = CoroutineStart.UNDISPATCHED) { model.events.first() }
        model.onAction(SearchAction.OpenShop("b"))
        runCurrent()
        assertEquals(SearchEvent.OpenShop("b"), event.await())
        assertTrue(analytics.opened.isEmpty())
        assertEquals(listOf(2), analytics.results)
    }

    @Test fun queryKeepsNearbyAndGuestFavoriteRequiresSignIn() = runTest(dispatcher) {
        account.key.value = null
        location.flow.value = here
        val model = model()
        model.onAction(SearchAction.ToggleNearby)
        runCurrent()
        model.onAction(SearchAction.Query("coffee"))
        runCurrent()
        advanceTimeBy(401)
        runCurrent()
        assertEquals("coffee", repository.queries.last().text)
        assertEquals(here, repository.queries.last().origin)
        val event = async(start = CoroutineStart.UNDISPATCHED) { model.events.first() }
        model.onAction(SearchAction.ToggleFavorite("a"))
        runCurrent()
        assertEquals(SearchEvent.SignInRequired, event.await())
        assertTrue(favorites.changes.isEmpty())
    }

    @Test fun obsoleteResponseDuringDebounceKeepsRowsAndReturningToItsQueryReloadsOnce() = runTest(dispatcher) {
        val stale = CompletableDeferred<Unit>()
        val latest = CompletableDeferred<Unit>()
        var coffeeRequests = 0
        repository.search = { query ->
            if (query.text?.trim() != "coffee") page(listOf(shop("city")))
            else if (++coffeeRequests == 1) {
                stale.await()
                page(listOf(shop("stale")))
            } else {
                latest.await()
                page(listOf(shop("current")))
            }
        }
        val model = model()
        model.onAction(SearchAction.Query("coffee"))
        runCurrent()
        finishDebounce()
        model.onAction(SearchAction.Query("tea"))
        runCurrent()
        stale.complete(Unit)
        runCurrent()
        assertPickerRows(model, "", "city")
        model.onAction(SearchAction.Query(" coffee "))
        runCurrent()
        finishDebounce()
        assertPickerRows(model, "", "city")
        latest.complete(Unit)
        runCurrent()
        assertPickerRows(model, "coffee", "current")
        assertEquals(listOf("", "coffee", "coffee"), repository.queries.map { it.text?.trim() })
    }

    @Test fun newNearbyLocationKeepsCityRowsUntilItAcceptsTheCurrentOrigin() = runTest(dispatcher) {
        val old = CompletableDeferred<Unit>()
        val latest = CompletableDeferred<Unit>()
        val moved = Coordinates(53.91, 27.57)
        repository.search = { query ->
            when (query.origin) {
                here -> withContext(NonCancellable) {
                    old.await()
                    page(listOf(shop("old-nearby")))
                }
                moved -> {
                    latest.await()
                    page(listOf(shop("current-nearby")))
                }
                else -> page(listOf(shop("city")))
            }
        }
        location.flow.value = here
        val model = model()
        model.onAction(SearchAction.ToggleNearby)
        runCurrent()
        location.flow.value = moved
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertPickerRows(model, "", "city")
        assertNull(model.state.value.pickerResultCriteria?.searchQuery()?.origin)
        latest.complete(Unit)
        runCurrent()
        assertPickerRows(model, "", "current-nearby")
        assertEquals(moved, model.state.value.pickerResultCriteria?.origin)
        assertEquals(0, model.state.value.locationPermissionRequest)
        assertEquals(listOf(null, here, moved), repository.queries.map { it.origin })
    }

    @Test fun surroundingWhitespaceKeepsAcceptedResultsAndAllowsPagination() = runTest(dispatcher) {
        repository.search = { query ->
            page(listOf(shop("page-${query.page}")), current = query.page, pages = 2)
        }
        val model = model()
        model.onAction(SearchAction.Query("coffee"))
        runCurrent()
        finishDebounce()
        model.onAction(SearchAction.Query(" coffee "))
        runCurrent()
        finishDebounce()
        val state = model.state.value
        assertTrue(assertNotNull(state.pickerResultCriteria).matchesPickerQuery(state.criteria))
        assertEquals(listOf("", "coffee"), repository.queries.map { it.text?.trim() })
        assertTrue(state.canLoadMore)
        model.onAction(SearchAction.LoadMore)
        runCurrent()
        assertEquals(listOf("page-1", "page-2"), model.state.value.shops.map { it.id })
        assertEquals(2, repository.queries.last().page)
    }

    @Test fun signedInRecentsAreClearedAndLatePreviousAccountResponseIsIgnored() = runTest(dispatcher) {
        val old = CompletableDeferred<List<ShopSummary>>()
        val owners = mutableListOf<String>()
        val model = model(DiscoveryRecentShops {
            val owner = checkNotNull(account.key.value)
            owners += owner
            Result.success(if (owner == "one") withContext(NonCancellable) { old.await() } else listOf(shop(owner)))
        })
        account.key.value = "two"
        runCurrent()
        assertEquals(listOf("two"), model.state.value.pickerRecentShops.map { it.id })
        old.complete(listOf(shop("one")))
        runCurrent()
        assertEquals(listOf("two"), model.state.value.pickerRecentShops.map { it.id })
        account.key.value = null
        runCurrent()
        assertTrue(model.state.value.pickerRecentShops.isEmpty())
        assertEquals(listOf("one", "two"), owners)
    }

    private fun TestScope.finishDebounce() {
        advanceTimeBy(401)
        runCurrent()
    }

    private fun assertPickerRows(model: SearchViewModel, text: String, id: String) {
        assertEquals(listOf(id), model.state.value.shops.map { it.id })
        assertEquals(text, model.state.value.pickerResultCriteria?.text?.trim())
    }
}
