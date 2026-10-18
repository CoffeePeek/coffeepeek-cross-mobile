package com.coffeepeek.admin.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PhotoViewerImagesTest {
    private val original = "https://gateway.example/api/v1/check-ins/visit/photos/photo"

    @Test fun pendingPreviewRequiresTheExplicitOriginalAction() {
        val images = PhotoViewerImages(listOf(""), listOf(original))
        assertNull(images.displayUrl(0, 0, null, true))
        assertEquals(original, images.originalForAction(0))
        assertEquals(original, images.displayUrl(0, 0, 0, true))
    }

    @Test fun incomingVisiblePageKeepsItsPreviewAndNeverLoadsItsOriginal() {
        val originals = List(3) { "$original-$it" }
        val previews = originals.map { "$it/variants/source/1/detail.png" }
        val images = PhotoViewerImages(previews)
        // During a drag both halves are visible, including after the snap candidate flips.
        assertEquals(previews[1], images.displayUrl(1, 0, 0, true))
        assertEquals(previews[0], images.displayUrl(0, 1, 0, true))
        assertEquals(originals[0], images.displayUrl(0, 0, 0, true))
        assertEquals(previews[1], images.displayUrl(1, 1, 0, true))
        assertNull(images.displayUrl(1, 0, 1, false))
        assertNull(images.displayUrl(2, 0, 2, true))
    }

    @Test fun legacyAdjacentImagesRenderOnlyWhenTheyEnterTheViewport() {
        val urls = listOf("https://cdn.example/first.jpg", "https://cdn.example/second.jpg")
        val images = PhotoViewerImages(urls)
        assertEquals(urls[1], images.displayUrl(1, 0, null, true))
        assertNull(images.displayUrl(1, 0, null, false))
        assertNull(images.originalForAction(1))
    }

    @Test fun variantsStillDeriveTheirExplicitOriginalAndIndicesMustStayAligned() {
        val images = PhotoViewerImages(listOf("$original/variants/source/1/detail.png"))
        assertEquals(original, images.originalForAction(0))
        assertNull(images.originalForAction(1))
        assertFailsWith<IllegalArgumentException> { PhotoViewerImages(listOf(""), emptyList()) }
    }
}
