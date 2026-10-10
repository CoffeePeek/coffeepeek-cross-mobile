package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.MenuGallery
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.domain.repository.ShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopMenuGalleryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Repository : ShopMenuGalleryRepository {
        var calls = 0
        var result: Result<MenuGallery> = Result.success(MenuGallery("Shop",
            listOf(MenuPhoto("first", "https://photo/1"), MenuPhoto("second", "https://photo/2"))))
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun getMenuGallery(shopId: String): Result<MenuGallery> {
            assertEquals("shop-1", shopId)
            calls++
            gate?.await()
            return result
        }
    }

    private fun viewModel(repository: Repository) = ShopMenuGalleryViewModel("shop-1", repository)
        .also { store.put("gallery", it) }

    @Test fun initialLoadAndPhotoEventUseLoadedOrder() = runTest(dispatcher) {
        val repository = Repository()
        val viewModel = viewModel(repository)
        runCurrent()
        assertEquals(1, repository.calls)
        assertEquals("Shop", viewModel.state.value.shopTitle)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.onAction(ShopMenuGalleryAction.OpenPhoto(1))
        runCurrent()
        assertEquals(ShopMenuGalleryEvent.OpenPhoto(listOf("https://photo/1", "https://photo/2"), 1),
            viewModel.events.first())
        viewModel.onAction(ShopMenuGalleryAction.OpenPhoto(9))
        runCurrent()
        viewModel.onAction(ShopMenuGalleryAction.Back)
        runCurrent()
        assertEquals(ShopMenuGalleryEvent.Back, viewModel.events.first())
    }

    @Test fun duplicateLoadsAreBlockedWhilePending() = runTest(dispatcher) {
        val repository = Repository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(repository)
        viewModel.onAction(ShopMenuGalleryAction.Load)
        runCurrent()
        assertEquals(1, repository.calls)
        assertTrue(viewModel.state.value.isLoading)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun failureHasGenericStateAndRetryRecovers() = runTest(dispatcher) {
        val repository = Repository().apply { result = Result.failure(IllegalStateException("secret")) }
        val viewModel = viewModel(repository)
        runCurrent()
        assertTrue(viewModel.state.value.hasError)
        assertTrue(viewModel.state.value.photos.isEmpty())
        repository.result = Result.success(MenuGallery("Recovered", emptyList()))
        viewModel.onAction(ShopMenuGalleryAction.Load)
        runCurrent()
        assertFalse(viewModel.state.value.hasError)
        assertEquals("Recovered", viewModel.state.value.shopTitle)
        assertEquals(2, repository.calls)
    }

    @Test fun cancellingPendingLoadClearsLoading() = runTest(dispatcher) {
        val repository = Repository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(repository)
        runCurrent()
        assertTrue(viewModel.state.value.isLoading)
        store.clear()
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.hasError)
    }
}
