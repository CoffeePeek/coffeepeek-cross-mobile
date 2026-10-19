package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomePhotoViewerImagesTest {
    @Test fun pendingGalleryKeepsDistinctOriginalsInTheirPhotoOrder() {
        val first = "https://gateway.example/api/v1/check-ins/home/photos/first"
        val second = "https://gateway.example/api/v1/check-ins/home/photos/second"
        val thumbnail = "$second/variants/source/1/thumb.png"
        val images = homePhotoViewerImages(listOf(
            HomeCheckInPhoto("first", first, thumbnailUrl = "", detailUrl = ""),
            HomeCheckInPhoto("second", second, thumbnailUrl = thumbnail),
        ))
        assertEquals(listOf("", thumbnail), images.previews)
        assertEquals(listOf(first, second), images.originals)
        assertEquals(first, images.displayUrl(0, 0, true))
        assertEquals(first, images.highQualityUrl(0, 0))
        assertEquals(thumbnail, images.displayUrl(1, 1, true))
        assertEquals(second, images.highQualityUrl(1, 1))
        assertNull(images.highQualityUrl(1, 0))
    }

    @Test fun legacyHomePhotoDisplaysWithoutASecondQualityRequest() {
        val original = "https://gateway.example/api/v1/check-ins/home/photos/legacy"
        val images = homePhotoViewerImages(listOf(HomeCheckInPhoto("legacy", original)))
        assertEquals(original, images.displayUrl(0, 0, true))
        assertNull(images.highQualityUrl(0, 0))
    }

}
