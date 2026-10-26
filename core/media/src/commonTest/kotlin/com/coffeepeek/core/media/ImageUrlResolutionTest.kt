package com.coffeepeek.core.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ImageUrlResolutionTest {
    @Test
    fun photoRouteUsesTheApiOriginWhileLegacyFileNamesKeepTheFileRoute() {
        val route = "/api/v1/check-ins/visit/photos/photo"
        assertEquals(ImageUrl(API_BASE_URL.trimEnd('/') + route), resolveImageData(route, API_BASE_URL))
        assertEquals(ImageUrl(API_BASE_URL + "api/file/photo.png"), resolveImageData("photo.png", API_BASE_URL))
        assertEquals(ImageUrl("https://cdn.example/photo.png"), resolveImageData("https://cdn.example/photo.png", API_BASE_URL))
    }

    @Test
    fun localPngPreviewBytesArePassedToTheDecoderUnchanged() {
        val bytes = byteArrayOf(1, 2, 3)
        assertSame(bytes, resolveImageData(bytes, API_BASE_URL))
    }
}
private const val API_BASE_URL = "https://api.example/"
