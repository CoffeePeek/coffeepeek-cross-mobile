package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomePhotoViewerImagesTest {
    @Test fun pendingGalleryKeepsDistinctOriginalsInTheirPhotoOrder() {
        val first = "https://gateway.example/api/v1/check-ins/home/photos/first"
        val second = "https://gateway.example/api/v1/check-ins/home/photos/second"
        val detail = "$second/variants/source/1/detail.png"
        val images = homePhotoViewerImages(listOf(
            HomeCheckInPhoto("first", first, thumbnailUrl = "", detailUrl = ""),
            HomeCheckInPhoto("second", second, detailUrl = detail),
        ))
        assertEquals(listOf("", detail), images.previews)
        assertEquals(listOf(first, second), images.originals)
        assertNull(images.displayUrl(0, 0, null, true))
        assertEquals(first, images.originalForAction(0))
        assertEquals(first, images.displayUrl(0, 0, 0, true))
        assertEquals(second, images.originalForAction(1))
    }

    @Test fun pendingDrinkKeepsAnExplicitOriginalEvenWithoutADetailDescriptor() {
        val original = "https://gateway.example/api/v1/check-ins/home/photos/drink"
        for (detail in listOf(null, "")) {
            val images = homeDrinkPhotoViewerImages(original, detail)
            assertEquals(listOf(""), images.previews)
            assertNull(images.displayUrl(0, 0, null, true))
            assertEquals(original, images.originalForAction(0))
            assertEquals(original, images.displayUrl(0, 0, 0, true))
        }
    }

    @Test fun legacyHomePhotoStillDisplaysWithoutASecondOriginalAction() {
        val original = "https://gateway.example/api/v1/check-ins/home/photos/legacy"
        val images = homePhotoViewerImages(listOf(HomeCheckInPhoto("legacy", original)))
        assertEquals(original, images.displayUrl(0, 0, null, true))
        assertNull(images.originalForAction(0))
    }
}
