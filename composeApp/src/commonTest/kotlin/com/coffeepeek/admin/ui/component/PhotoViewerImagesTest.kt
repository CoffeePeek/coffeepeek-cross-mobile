package com.coffeepeek.admin.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PhotoViewerImagesTest {
    private val original = "https://gateway.example/api/v1/check-ins/visit/photos/photo"

    @Test fun currentPhotoFallsBackToOriginalWhenPreviewIsUnavailable() {
        val images = PhotoViewerImages(listOf(""), listOf(original))
        assertEquals(original, images.displayUrl(0, 0, true))
        assertEquals(original, images.highQualityUrl(0, 0))
        assertNull(images.displayUrl(0, 1, true))
        assertNull(images.highQualityUrl(0, 1))
    }

    @Test fun incomingVisiblePageKeepsItsPreviewAndNeverLoadsItsOriginal() {
        val originals = List(3) { "$original-$it" }
        val previews = originals.map { "$it/variants/source/1/detail.png" }
        val images = PhotoViewerImages(previews)
        // During a drag both halves are visible, including after the snap candidate flips.
        assertEquals(previews[1], images.displayUrl(1, 0, true))
        assertEquals(previews[0], images.displayUrl(0, 1, true))
        assertNull(images.highQualityUrl(1, 0))
        assertNull(images.highQualityUrl(0, 1))
        assertEquals(previews[0], images.displayUrl(0, 0, true))
        assertNull(images.highQualityUrl(0, 0))
        assertNull(images.highQualityUrl(1, 1))
        assertNull(images.displayUrl(1, 0, false))
        assertNull(images.displayUrl(2, 0, true))
    }

    @Test fun legacyAdjacentImagesRenderOnlyWhenTheyEnterTheViewport() {
        val urls = listOf("https://cdn.example/first.jpg", "https://cdn.example/second.jpg")
        val images = PhotoViewerImages(urls)
        assertEquals(urls[1], images.displayUrl(1, 0, true))
        assertNull(images.displayUrl(1, 0, false))
        assertNull(images.highQualityUrl(1, 1))
    }

    @Test fun prefetchStaysWithinOneNeighborAndNeverFallsBackToOriginals() {
        val images = PhotoViewerImages(
            listOf("first", "", "third", "fourth", "fifth"),
            List(5) { "$original-$it" },
        )
        assertEquals(listOf("fourth"), images.adjacentPreviewUrls(2))
        assertEquals(listOf("third", "fifth"), images.adjacentPreviewUrls(3))
        assertEquals(emptyList(), images.adjacentPreviewUrls(0))
        assertEquals(listOf("fourth"), images.adjacentPreviewUrls(4))
    }

    @Test fun readyDetailNeverDownloadsOriginalEvenWhenAnExplicitOriginalIsAvailable() {
        val detail = "$original/variants/source/1/detail.png"
        for (images in listOf(PhotoViewerImages(listOf(detail)), PhotoViewerImages(listOf(detail), listOf(original)))) {
            assertEquals(detail, images.displayUrl(0, 0, true))
            assertNull(images.highQualityUrl(0, 0))
            assertEquals(listOf(original), images.originals)
        }
    }

    @Test fun missingOriginalDoesNotRequestAnEmptyUrlAndIndicesMustStayAligned() {
        val images = PhotoViewerImages(listOf(""), listOf(""))
        assertNull(images.displayUrl(0, 0, true))
        assertNull(images.highQualityUrl(0, 0))
        assertNull(images.highQualityUrl(1, 1))
        assertFailsWith<IllegalArgumentException> { PhotoViewerImages(listOf(""), emptyList()) }
    }
}
