package com.coffeepeek.core.network

import kotlin.test.Test
import kotlin.test.assertFailsWith

class PublicUploadUrlTest {
    @Test fun acceptsPublicPresignedUrl() {
        requirePublicUploadUrl("https://uploads.example.com/photo?signature=value")
    }

    @Test fun blocksLocalAndPrivateHosts() {
        listOf("http://localhost/photo", "http://minio/photo", "https://site.internal/photo",
            "http://10.0.0.1/photo", "http://127.0.0.2/photo", "http://169.254.169.254/photo",
            "http://172.16.1.2/photo", "http://192.168.1.1/photo",
            "file:///tmp/photo").forEach { url ->
            assertFailsWith<IllegalArgumentException> { requirePublicUploadUrl(url) }
        }
    }
}
