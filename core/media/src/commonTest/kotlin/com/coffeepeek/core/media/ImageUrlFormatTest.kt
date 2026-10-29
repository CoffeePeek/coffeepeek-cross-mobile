package com.coffeepeek.core.media

import io.kamel.image.getDataSourceEnding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ImageUrlFormatTest {
    @Test
    fun decoderSelectionUsesTheUrlPathIncludingImageProxyAndQuery() {
        listOf(
            "https://media.example/roasters/logo.svg",
            "https://media.example/img/_/card/plain/roasters/logo.svg?revision=2",
        ).forEach { url -> assertEquals("svg", getDataSourceEnding(ImageUrl(url))) }
        assertEquals("jpg", getDataSourceEnding(ImageUrl("https://media.example/photo.jpg")))
        assertEquals("xml", getDataSourceEnding(ImageUrl("https://media.example/vector.xml")))
        assertEquals("gif", getDataSourceEnding(ImageUrl("https://media.example/animated.gif")))
    }

    @Test
    fun identicalUrlsInDifferentPermissionScopesRemainDifferentCacheKeys() {
        val url = "https://api.example/check-ins/1/photos/2/variants/card.jpg"
        assertNotEquals(ImageUrl(url, "account-a"), ImageUrl(url, "account-b"))
        assertEquals(ImageUrl(url, "account-a"), ImageUrl(url, "account-a"))
    }
}
