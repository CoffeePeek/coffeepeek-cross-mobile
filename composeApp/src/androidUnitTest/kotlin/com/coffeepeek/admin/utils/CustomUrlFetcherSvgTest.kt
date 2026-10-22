package com.coffeepeek.admin.utils

import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfigBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CustomUrlFetcherSvgTest {
    @Test
    fun nonBitmapBytesReachTheirOwnDecoderWithoutAndroidBitmapConversion() = runTest {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M0 0h24v24H0z"/></svg>""".encodeToByteArray()
        val client = HttpClient(MockEngine { respond(svg, headers = headersOf(HttpHeaders.ContentType, "image/svg+xml")) })
        try {
            listOf("svg", "xml", "gif").forEach { extension ->
                val resource = CustomUrlFetcher(client).fetch(CustomUrl("https://media.example/img/_/card/plain/asset.$extension"),
                    ResourceConfigBuilder(coroutineContext).build()).single()
                assertContentEquals(svg, assertIs<Resource.Success<ByteReadChannel>>(resource).value.toByteArray())
            }
        } finally { client.close() }
    }

    @Test
    fun missingSvgStillFailsInsteadOfDisplayingAnErrorDocument() = runTest {
        val client = HttpClient(MockEngine { respond("missing", HttpStatusCode.NotFound) })
        try {
            assertFailsWith<IllegalStateException> {
                CustomUrlFetcher(client).fetch(CustomUrl("https://media.example/logo.svg"),
                    ResourceConfigBuilder(coroutineContext).build()).single()
            }
        } finally { client.close() }
    }
}
