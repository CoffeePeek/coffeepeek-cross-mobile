@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.media

import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosPhotoViewerTest {
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
}
