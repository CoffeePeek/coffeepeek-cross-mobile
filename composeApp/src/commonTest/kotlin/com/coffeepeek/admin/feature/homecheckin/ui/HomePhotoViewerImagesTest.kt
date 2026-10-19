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
        assertEquals(first, images.displayUrl(0, 0, true))
        assertEquals(first, images.highQualityUrl(0, 0))
        assertEquals(detail, images.displayUrl(1, 1, true))
        assertNull(images.highQualityUrl(1, 1))
        assertNull(images.highQualityUrl(1, 0))
    }

    @Test fun pendingDrinkAutomaticallyShowsOriginalWithoutADetailDescriptor() {
        val original = "https://gateway.example/api/v1/check-ins/home/photos/drink"
        for (detail in listOf(null, "")) {
            val images = homeDrinkPhotoViewerImages(original, detail)
            assertEquals(listOf(""), images.previews)
            assertEquals(original, images.displayUrl(0, 0, true))
            assertEquals(original, images.highQualityUrl(0, 0))
        }
    }

    @Test fun legacyHomePhotoDisplaysWithoutASecondQualityRequest() {
        val original = "https://gateway.example/api/v1/check-ins/home/photos/legacy"
        val images = homePhotoViewerImages(listOf(HomeCheckInPhoto("legacy", original)))
        assertEquals(original, images.displayUrl(0, 0, true))
        assertNull(images.highQualityUrl(0, 0))
    }

    @Test fun readyDrinkDetailDoesNotDownloadTheOriginalAgain() {
        val original = "https://gateway.example/api/v1/check-ins/home/photos/drink"
        val detail = "$original/variants/source/1/detail.png"
        val images = homeDrinkPhotoViewerImages(original, detail)
        assertEquals(detail, images.displayUrl(0, 0, true))
        assertNull(images.highQualityUrl(0, 0))
    }
}
