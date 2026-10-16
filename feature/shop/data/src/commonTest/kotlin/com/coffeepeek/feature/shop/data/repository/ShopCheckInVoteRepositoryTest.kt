package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShopCheckInVoteRepositoryTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun usesCanonicalVisitEndpointAndBodylessPutDelete() = runBlocking {
        val methods = mutableListOf<HttpMethod>()
        val engine = MockEngine { request ->
            assertEquals("/api/v1/check-ins/visit/helpful", request.url.encodedPath)
            assertTrue(request.body is OutgoingContent.NoContent)
            methods += request.method
            respond("""{"isSuccess":true,"data":{"isHelpful":${request.method == HttpMethod.Put},"helpfulCount":8}}""", headers = headers)
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopCheckInVoteRepository(client)
            assertTrue(repository.setHelpful("visit", true).getOrThrow().isHelpful)
            assertEquals(false, repository.setHelpful("visit", false).getOrThrow().isHelpful)
            assertEquals(listOf(HttpMethod.Put, HttpMethod.Delete), methods)
        } finally { client.close(); engine.close() }
    }

    @Test fun rejectsHttpEnvelopeAndIncompleteVoteResponses() = runBlocking {
        for ((body, status) in listOf(
            """{"isSuccess":true,"data":{"isHelpful":true,"helpfulCount":1}}""" to HttpStatusCode.Forbidden,
            """{"isSuccess":false,"data":{"isHelpful":true,"helpfulCount":1}}""" to HttpStatusCode.OK,
            """{"isSuccess":true,"data":null}""" to HttpStatusCode.OK,
            """{"isSuccess":true,"data":{}}""" to HttpStatusCode.OK,
            """{"isSuccess":true,"data":{"isHelpful":true,"helpfulCount":-1}}""" to HttpStatusCode.OK,
            "malformed" to HttpStatusCode.OK,
        )) {
            val engine = MockEngine { respond(body, status, headers) }
            val client = HttpClientFactory(engine).api("https://example.com")
            try { assertTrue(createShopCheckInVoteRepository(client).setHelpful("visit", true).isFailure) }
            finally { client.close(); engine.close() }
        }
    }

    @Test fun validatesIdBeforeSendingAndPropagatesCancellation() = runBlocking<Unit> {
        var calls = 0
        val engine = MockEngine { calls++; throw CancellationException("cancel") }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopCheckInVoteRepository(client)
            for (id in listOf("", "bad/id", "bad?id", "bad#id")) assertTrue(repository.setHelpful(id, true).isFailure)
            assertEquals(0, calls)
            assertFailsWith<CancellationException> { repository.setHelpful("visit", true) }
            assertEquals(1, calls)
        } finally { client.close(); engine.close() }
    }
}
