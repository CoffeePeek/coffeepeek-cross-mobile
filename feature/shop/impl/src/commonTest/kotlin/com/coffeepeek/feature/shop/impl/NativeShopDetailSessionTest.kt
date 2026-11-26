package com.coffeepeek.feature.shop.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.api.ShopAnalytics
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.domain.repository.*
import com.coffeepeek.feature.shop.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeShopDetailSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val owners = mutableListOf<NativeShopDetailSession>()
    private var current = true
    private var reads = 0
    private var writes = 0
    private var load: suspend () -> Result<ShopDetails> = { Result.success(details()) }
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() {
        owners.forEach { it.close() }
        store.clear()
        Dispatchers.resetMain()
    }
    private fun owner(): NativeShopDetailSession {
        val repository = object : ShopDetailsRepository {
            override suspend fun getDetails(shopId: String): Result<ShopDetails> {
                reads++
                return load()
            }
        }
        val votes = object : ShopCheckInVoteRepository {
            override suspend fun setHelpful(checkInId: String, helpful: Boolean) = Result.success(ShopHelpfulVote(helpful, 0))
        }
        val favorites = object : ShopFavorites {
            override fun observeIds() = MutableStateFlow(Result.success(emptySet<String>()))
            override suspend fun save(shop: ShopDetails): Result<Unit> {
                writes++
                return Result.success(Unit)
            }
            override suspend fun remove(shopId: String) = Result.success(Unit)
        }
        return createNativeShopDetailSession("shop", repository, votes, favorites, { Result.success(ShopViewer(true, "owner")) },
            { 1 }, { 600 }, 3, false, { current }, ShopAnalytics.None, store).also(owners::add)
    }
    @Test fun linkedCheckInIsDeliveredOnceAfterTheSharedDetailIsReady() = runTest(dispatcher) {
        val owner = owner()
        val events = mutableListOf<ShopDetailEventEnvelope>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { owner.events.collect { events += it } }
        runCurrent()
        val request = NativeShopLinkedCheckIn(true)
        owner.openLinkedCheckIn(request)
        owner.openLinkedCheckIn(request)
        runCurrent()
        assertEquals(1, events.count { it.event is ShopDetailEvent.CreateCheckIn })
        assertEquals(3, events.single().sessionGeneration)
        job.cancel()
    }
    @Test fun quickNativeReturnsRetainTheLoadedSnapshot() = runTest(dispatcher) {
        val owner = owner()
        runCurrent()
        repeat(3) {
            owner.resume()
            runCurrent()
        }
        assertEquals(1, reads)
    }
    @Test fun leaseRevocationRejectsCommandsBeforeTheObserverClosesTheOwner() = runTest(dispatcher) {
        val owner = owner()
        runCurrent()
        current = false
        owner.action(ShopDetailAction.ToggleFavorite)
        owner.retry()
        owner.openLinkedCheckIn(NativeShopLinkedCheckIn(true))
        runCurrent()
        assertEquals(0, writes)
        assertEquals(1, reads)
        assertFalse(owner.isCurrent())
    }
    @Test fun finalCloseCancelsTheDetailReadAndCannotStartAnotherWriter() = runTest(dispatcher) {
        var cancelled = false
        load = { try { awaitCancellation() } finally { cancelled = true } }
        val owner = owner()
        runCurrent()
        owner.close()
        owner.close()
        owner.retry()
        runCurrent()
        assertTrue(cancelled)
        assertFalse(owner.isCurrent())
        assertEquals(1, reads)
    }
    @Test fun replacingTheBrowsingBindingDoesNotReplayTheRoutesConsumedCheckInIntent() = runTest(dispatcher) {
        val request = NativeShopLinkedCheckIn(true)
        val original = owner()
        runCurrent()
        original.openLinkedCheckIn(request)
        runCurrent()
        original.close()
        val replacement = owner()
        runCurrent()
        val events = mutableListOf<ShopDetailEventEnvelope>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { replacement.events.collect { events += it } }
        replacement.openLinkedCheckIn(request)
        runCurrent()
        assertTrue(events.isEmpty())
    }
    private fun details() = ShopDetails(ShopOverview("shop", "Shop", null, null, null, null, null, 0, true, emptyList()),
        null, emptyList(), ShopCoffeeDetails(), null, emptyList(), emptyList(), emptyList())
}
