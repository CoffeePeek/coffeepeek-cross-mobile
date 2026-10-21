package com.coffeepeek.feature.shop.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ShopFileUrlResolverTest {
    @Test fun resolvesIssuedCheckInApiUrlsWithoutStorageKeyFallback() {
        val files = ShopFileUrlResolver("https://files.example.com/")
        assertEquals("https://files.example.com/api/v1/check-ins/visit/photos/photo",
            files.resolveApiUrl("/api/v1/check-ins/visit/photos/photo"))
        assertEquals("https://cdn.example.com/photo.jpg", files.resolveApiUrl("https://cdn.example.com/photo.jpg"))
        for (url in listOf("", "javascript:alert(1)", "//other.example/photo", "../secret")) {
            assertNull(files.resolveApiUrl(url))
        }
    }

    @Test fun resolvesAbsoluteUrlOrStorageKeyWithoutUnsafeSchemes() {
        val files = ShopFileUrlResolver("https://files.example.com/")
        assertEquals("https://cdn.example.com/photo.jpg", files.resolve("ignored", "https://cdn.example.com/photo.jpg"))
        assertEquals("https://files.example.com/api/file/reviews/photo.jpg", files.resolve("reviews/photo.jpg", null))
        assertNull(files.resolve(null, "javascript:alert(1)"))
        assertNull(files.resolve("../secret", null))
        assertNull(files.resolve("/absolute/path", null))
        assertFailsWith<IllegalArgumentException> { ShopFileUrlResolver("file:///tmp") }
    }
}
