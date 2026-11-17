@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.media

import com.coffeepeek.admin.session.ReadOnlySessions
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.installImageAccess
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class IosPhotoViewerTest {
    @Test fun protectedCataloguePhotosClearWhenAccessEnds() = runTest(timeout = 10.seconds) {
        val requests = mutableListOf<String>()
        val publicClient = HttpClient(MockEngine { error("Protected photos require their own client") })
        val protectedClient = HttpClient(MockEngine { request ->
            requests += request.url.encodedPath
            respond(Base64.decode(PIXEL_PNG), headers = headersOf(HttpHeaders.ContentType, "image/png"))
        })
        val media = IosCatalogueMedia(ImageUrlFetcher(publicClient, protectedClient), "https://fixture.invalid")
        val sessions = object : ReadOnlySessions() {
            override fun observeSession() = flowOf(null)
        }
        try {
            installImageAccess(sessions, publicClient)
            PHOTO_PATHS.forEach { path -> media.assertAuthorizedFrameAndClear(path) }
            assertEquals(PHOTO_PATHS, requests)
        } finally {
            protectedClient.close()
            publicClient.close()
        }
    }

    @Test fun retainsRequestedIndexAndFetchesOnlyTheSelectedOriginal() = runTest {
        val urls = mutableListOf<String>()
        val images = CoffeePeekPhotoViewerImages(listOf("thumb-0", "thumb-1", "thumb-2"), listOf("full-0", "full-1", "full-2"))
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewer = IosPhotoViewer(images, 1) { url ->
                urls += url
                flowOf(null)
            }
            assertEquals(1, viewer.initialIndex)
            assertEquals(3, viewer.photoCount)
            viewer.loadPreview(page = 0, selected = 1) {}
            viewer.loadOriginal(page = 0, selected = 1) {}
            viewer.loadOriginal(page = 1, selected = 1) {}
            runCurrent()
            assertEquals(listOf("thumb-0", "full-1"), urls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun cancellingAnObservationStopsItsFrameStream() = runTest {
        var stopped = false
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewer = IosPhotoViewer(CoffeePeekPhotoViewerImages(listOf("thumb")), 0) {
                flow {
                    try {
                        awaitCancellation()
                    } finally {
                        stopped = true
                    }
                }
            }
            val observation = viewer.loadPreview(page = 0, selected = 0) {}
            runCurrent()
            observation.cancel()
            runCurrent()
            assertTrue(stopped)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private suspend fun IosCatalogueMedia.assertAuthorizedFrameAndClear(path: String) {
        var current = true
        val frames = observeImage(path) { current }.onEach { image ->
            if (current) {
                assertNotNull(image)
                current = false
            }
        }.take(2).toList()
        assertNull(frames[1])
    }

    private companion object {
        val PHOTO_PATHS = listOf(
            "/api/v1/check-ins/check-in/photos/photo/variants/source/1/feed.png",
            "/api/v1/shelf/bags/bag/photos/card",
        )
        const val PIXEL_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGD4DwABBAEAX+XDSwAAAABJRU5ErkJggg=="
    }
}
