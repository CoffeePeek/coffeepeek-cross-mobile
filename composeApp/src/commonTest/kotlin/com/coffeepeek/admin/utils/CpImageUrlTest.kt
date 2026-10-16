package com.coffeepeek.admin.utils

import com.coffeepeek.admin.locator.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CpImageUrlTest {
    @Test
    fun cutoutRouteUsesTheApiOriginWhileLegacyFileNamesKeepTheFileRoute() {
        val route = "/api/v1/check-ins/visit/photos/cup"
        assertEquals(CustomUrl(Constants.BASE_URL.trimEnd('/') + route), cpImageData(route))
        assertEquals(CustomUrl(Constants.BASE_URL + "api/file/cup.png"), cpImageData("cup.png"))
        assertEquals(CustomUrl("https://cdn.example/cup.png"), cpImageData("https://cdn.example/cup.png"))
    }

    @Test
    fun localPngPreviewBytesArePassedToTheDecoderUnchanged() {
        val bytes = byteArrayOf(1, 2, 3)
        assertSame(bytes, cpImageData(bytes))
    }
}
