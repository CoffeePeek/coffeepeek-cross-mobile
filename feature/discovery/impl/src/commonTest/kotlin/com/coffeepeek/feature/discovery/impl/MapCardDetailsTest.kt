package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.api.MapShopDetails
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapCardDetailsTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val details = FakeShopDetails()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun loadedCardContentRemainsAvailableWhileAnotherCardIsSelected() = runTest(dispatcher) {
        val model = model()
        focus(model, "a")
        focus(model, "b")
        assertEquals("b", model.state.value.selectedShop?.id)
        assertEquals(sampleDetails("a"), model.state.value.cardDetails["a"])
        assertEquals(sampleDetails("b"), model.state.value.cardDetails["b"])
        focus(model, "a")
        assertEquals(listOf("a", "b"), details.requests)
        assertFalse(model.state.value.isLoadingDetails)
        assertEquals(sampleDetails("a"), model.state.value.details)
        assertEquals(sampleDetails("b"), model.state.value.cardDetails["b"])
    }

    @Test fun returningToACachedCardDropsALateResponseFromTheAbandonedCard() = runTest(dispatcher) {
        val delayed = CompletableDeferred<Result<MapShopDetails>>()
        details.answer = { id ->
            if (id == "b") withContext(NonCancellable) { delayed.await() }
            else Result.success(sampleDetails(id))
        }
        val model = model()
        focus(model, "a")
        focus(model, "b")
        assertTrue(model.state.value.isLoadingDetails)
        assertEquals(sampleDetails("a"), model.state.value.cardDetails["a"])
        focus(model, "a")
        delayed.complete(Result.success(sampleDetails("b")))
        runCurrent()
        assertEquals(sampleDetails("a"), model.state.value.details)
        assertEquals(setOf("a"), model.state.value.cardDetails.keys)
        assertFalse(model.state.value.isLoadingDetails)
        assertEquals(listOf("a", "b"), details.requests)
    }

    @Test fun cachedCardsSurviveChangingTheSharedQuery() = runTest(dispatcher) {
        val model = model()
        focus(model, "a")
        model.onAction(MapAction.SyncCriteria(DiscoveryCriteria("coffee")))
        advanceUntilIdle()
        assertNull(model.state.value.selectedShop)
        assertNull(model.state.value.details)
        assertEquals(sampleDetails("a"), model.state.value.cardDetails["a"])
        focus(model, "a")
        assertEquals(listOf("a"), details.requests)
        assertFalse(model.state.value.isLoadingDetails)
        assertEquals(sampleDetails("a"), model.state.value.details)
    }

    private fun TestScope.model(): MapViewModel =
        MapViewModel(FakeRepository(), FakeCity(), FakeLocation(), details, RecordingAnalytics(), FakeAccount(), FakeFavorites()).also {
            store.put("map", it)
            runCurrent()
        }

    private fun TestScope.focus(model: MapViewModel, id: String) {
        val marker = marker(id)
        model.onAction(MapAction.Focus(MapFocus(marker.id, marker.title, marker.coordinates)))
        runCurrent()
    }
}
