package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.*
import com.coffeepeek.feature.discovery.api.DiscoveryRecentShops
import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeDiscoverySessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val favorites = FakeFavorites()
    private val account = FakeAccount()
    private val city = FakeCity()
    private val sources = mutableListOf<NativeDiscoverySession>()
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { sources.forEach(NativeDiscoverySession::close); Dispatchers.resetMain() }
    private fun TestScope.source(picker: Boolean = false, recent: DiscoveryRecentShops? = null,
        identity: DiscoveryAccount = account): NativeDiscoverySession {
        val source = createNativeDiscoverySession(repository, favorites, city, FakeLocation(), identity,
            FakeShopDetails(), picker = picker, recentShops = recent).also(sources::add)
        source.presentation(true, 80f, false)
        runCurrent()
        return source
    }

    @Test fun nativeFavoriteUsesMembershipSelectionBeforeAnyDirectWriter() = runTest(dispatcher) {
        favorites.collectionsEnabled = true
        val source = source()
        source.perform("favorite", "a")
        runCurrent()
        assertEquals(listOf("a"), favorites.collectionRequests)
        assertTrue(favorites.changes.isEmpty())
    }

    @Test fun finalCloseCancelsPendingDebounceAndRejectsLateActions() = runTest(dispatcher) {
        val source = source()
        val reads = repository.queries.size
        source.query("new query")
        runCurrent()
        source.close()
        source.close()
        source.query("late")
        source.perform("favorite", "a")
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(reads, repository.queries.size)
        assertTrue(favorites.changes.isEmpty())
        assertTrue(source.current().search.shops.isEmpty())
        assertFalse(source.current().overlay.active)
    }

    @Test fun replacementAccountRevokesTheOldNativeSessionBeforeAnotherAction() = runTest(dispatcher) {
        val source = source()
        account.key.value = "replacement"
        source.perform("favorite", "a")
        runCurrent()
        assertTrue(favorites.changes.isEmpty())
        assertTrue(source.current().search.shops.isEmpty())
        assertFalse(source.current().overlay.active)
    }

    @Test fun stalePermissionReplyCannotResolveANewerSearchRequest() = runTest(dispatcher) {
        val source = source()
        source.perform("nearby", "")
        runCurrent()
        val key = source.current().search.locationPermissionRequest
        assertTrue(key > 0)
        source.permissionResult(NativeDiscoveryPermissionTarget.Search, key + 1, NativeDiscoveryPermissionOutcome.Denied)
        runCurrent()
        assertTrue(source.current().search.nearbyRequested)
        source.permissionResult(NativeDiscoveryPermissionTarget.Search, key, NativeDiscoveryPermissionOutcome.PermanentlyDenied)
        runCurrent()
        assertFalse(source.current().search.nearbyRequested)
        assertTrue(source.current().search.locationSettingsNeeded)
    }

    @Test fun pickerMapDoesNotRequestLocationUntilNearbyIsExplicitlyChosen() = runTest(dispatcher) {
        val source = source(picker = true)
        source.perform("map", "")
        runCurrent()
        assertTrue(source.current().overlay.mapExpanded)
        assertEquals(0, source.current().map.locationPermissionRequest)
        source.perform("nearby", "")
        runCurrent()
        assertFalse(source.current().overlay.mapExpanded)
        assertTrue(source.current().search.locationPermissionRequest > 0)
    }

    @Test fun temporaryCoverageKeepsPickerRecentsAndDoesNotReloadPrivateHistory() = runTest(dispatcher) {
        var reads = 0
        val source = source(picker = true, recent = DiscoveryRecentShops {
            reads++
            Result.success(listOf(shop("recent")))
        })
        assertEquals(listOf("recent"), source.current().search.pickerRecentShops.map { it.id })
        source.presentation(false, 0f, false)
        runCurrent()
        source.presentation(true, 0f, false)
        runCurrent()
        assertEquals(1, reads)
        assertEquals(listOf("recent"), source.current().search.pickerRecentShops.map { it.id })
    }

    @Test fun closeRejectsALatePrivateHistoryResultEvenWhenTheLoaderCannotCancel() = runTest(dispatcher) {
        val complete = CompletableDeferred<Unit>()
        val source = source(picker = true, recent = DiscoveryRecentShops {
            withContext(NonCancellable) {
                complete.await()
                Result.success(listOf(shop("private")))
            }
        })
        source.close()
        complete.complete(Unit)
        runCurrent()
        assertTrue(source.current().search.pickerRecentShops.isEmpty())
        assertTrue(source.current().search.shops.isEmpty())
        assertFalse(source.current().overlay.active)
    }

    @Test fun coldAccountFailureClosesBothGuestAndSignedInNativeOwnersWithoutUnhandledErrors() = runTest(dispatcher) {
        for (key in listOf(null, "signed-in")) {
            val account = object : DiscoveryAccount {
                override fun currentAccountKey() = key
                override val accountKey = flow<String?> { error("Could not restore credentials") }
            }
            val source = source(picker = true, identity = account)
            source.perform("favorite", "a")
            runCurrent()
            assertFalse(source.current().overlay.active)
            assertTrue(source.current().search.shops.isEmpty())
        }
        assertTrue(favorites.changes.isEmpty())
    }

    @Test fun queuedNavigationCannotEscapeAfterTheNativeOwnerWasRevoked() = runTest(dispatcher) {
        val source = source(picker = true)
        source.perform("open", "a")
        runCurrent()
        source.close()
        assertTrue(source.events.toList().isEmpty())
    }

    @Test fun hotAccountFailureHasOneUpstreamAndCancelsPrivateHistoryImmediately() = runTest(dispatcher) {
        val fail = CompletableDeferred<Unit>()
        var subscriptions = 0
        var historyCancelled = false
        val account = object : DiscoveryAccount {
            override fun currentAccountKey() = "signed-in"
            override val accountKey = flow {
                subscriptions++
                emit("signed-in")
                fail.await()
                error("Account observer failed")
            }
        }
        val source = source(picker = true, identity = account, recent = DiscoveryRecentShops {
            try { awaitCancellation() } finally { historyCancelled = true }
        })
        fail.complete(Unit)
        runCurrent()
        assertEquals(1, subscriptions)
        assertTrue(historyCancelled)
        assertFalse(source.current().overlay.active)
        assertTrue(source.current().search.pickerRecentShops.isEmpty())
    }

    @Test fun reviewEligibilityRequiresSettledVisibleDiscovery() = runTest(dispatcher) {
        val source = source()
        assertTrue(source.current().reviewEligible)
        source.perform("scroll", "")
        runCurrent()
        assertFalse(source.current().reviewEligible)
        advanceTimeBy(350)
        runCurrent()
        assertTrue(source.current().reviewEligible)
        source.presentation(false, 80f, false)
        runCurrent()
        assertFalse(source.current().reviewEligible)
    }

    @Test fun nativeFilterApplyPreservesTheLatestCityAndClosesTheDraft() = runTest(dispatcher) {
        val source = source()
        source.perform("filters", "")
        runCurrent()
        source.filterToggle("Tags", "wifi")
        city.select("brest")
        runCurrent()
        source.applyFilters()
        source.applyFilters()
        runCurrent()
        assertEquals("brest", source.current().search.filters.cityId)
        assertEquals(setOf("wifi"), source.current().search.filters.tagIds)
        assertFalse(source.current().search.showFilters)
    }
}
