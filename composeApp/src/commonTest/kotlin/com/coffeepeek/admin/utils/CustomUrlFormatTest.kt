package com.coffeepeek.admin.utils

import io.kamel.image.getDataSourceEnding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CustomUrlFormatTest {
    @Test
    fun decoderSelectionUsesTheUrlPathIncludingImageProxyAndQuery() {
        listOf(
            "https://media.example/roasters/logo.svg",
            "https://media.example/img/_/card/plain/roasters/logo.svg?revision=2",
        ).forEach { url -> assertEquals("svg", getDataSourceEnding(CustomUrl(url))) }
        assertEquals("jpg", getDataSourceEnding(CustomUrl("https://media.example/photo.jpg")))
        assertEquals("xml", getDataSourceEnding(CustomUrl("https://media.example/vector.xml")))
        assertEquals("gif", getDataSourceEnding(CustomUrl("https://media.example/animated.gif")))
    }

    @Test
    fun identicalUrlsInDifferentPermissionScopesRemainDifferentCacheKeys() {
        val url = "https://api.example/check-ins/1/photos/2/variants/card.jpg"
        assertNotEquals(CustomUrl(url, "account-a"), CustomUrl(url, "account-b"))
        assertEquals(CustomUrl(url, "account-a"), CustomUrl(url, "account-a"))
    }
}
