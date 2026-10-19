package com.coffeepeek.admin.utils

import com.coffeepeek.admin.locator.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CpImageUrlTest {
    @Test
    fun photoRouteUsesTheApiOriginWhileLegacyFileNamesKeepTheFileRoute() {
        val route = "/api/v1/check-ins/visit/photos/photo"
        assertEquals(CustomUrl(Constants.BASE_URL.trimEnd('/') + route), cpImageData(route))
        assertEquals(CustomUrl(Constants.BASE_URL + "api/file/photo.png"), cpImageData("photo.png"))
        assertEquals(CustomUrl("https://cdn.example/photo.png"), cpImageData("https://cdn.example/photo.png"))
    }

    @Test
    fun localPngPreviewBytesArePassedToTheDecoderUnchanged() {
        val bytes = byteArrayOf(1, 2, 3)
        assertSame(bytes, cpImageData(bytes))
    }
}
