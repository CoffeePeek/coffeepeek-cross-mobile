package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.domain.model.MenuGallery
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.domain.repository.ShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.createIosShopMenuGallerySession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopMenuGalleryIosSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun snapshotUsesPreviewsAndViewerReceivesFullUrlsInTheSameOrder() = runTest(dispatcher) {
        val photos = listOf(MenuPhoto("1", "full-1", "preview-1"), MenuPhoto("2", "full-2", "preview-2"))
        val session = createIosShopMenuGallerySession("shop", MenuRepository(photos))
        val opened = mutableListOf<Pair<List<String>, Int>>()
        val observation = session.observeEvents({}, { urls, index -> opened.add(urls to index) })

        try {
            runCurrent()
            assertEquals(listOf("preview-1", "preview-2"), session.currentState().previewUrls)
            session.openPhoto(1)
            runCurrent()
            assertEquals(listOf(listOf("full-1", "full-2") to 1), opened)
        } finally {
            observation.cancel()
            session.close()
        }
    }

    @Test
    fun closingCancelsTheLoadAndTheNativeSubscription() = runTest(dispatcher) {
        val repository = MenuRepository(loadGate = CompletableDeferred())
        val session = createIosShopMenuGallerySession("shop", repository)
        val delivered = mutableListOf<Boolean>()
        val observation = session.observeState { delivered.add(it.isLoading) }

        runCurrent()
        assertEquals(listOf(true), delivered)
        session.close()
        runCurrent()

        assertTrue(repository.loadFinished)
        assertTrue(observation.isCancelled)
        assertEquals(listOf(true), delivered)
    }

    private class MenuRepository(
        private val photos: List<MenuPhoto> = emptyList(),
        private val loadGate: CompletableDeferred<Unit>? = null,
    ) : ShopMenuGalleryRepository {
        var loadFinished = false
            private set

        override suspend fun getMenuGallery(shopId: String): Result<MenuGallery> =
            try {
                loadGate?.await()
                Result.success(MenuGallery("Shop", photos))
            } finally {
                loadFinished = true
            }
    }
}
