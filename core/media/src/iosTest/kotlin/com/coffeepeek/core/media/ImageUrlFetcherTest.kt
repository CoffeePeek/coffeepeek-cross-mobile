package com.coffeepeek.core.media

import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfigBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.toByteArray
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImageUrlFetcherTest {
    @Test
    fun responseRemainsOpenUntilSuspendedConsumerFinishesReading() = runTest {
        val body = ByteChannel(autoFlush = true)
        val client = HttpClient(MockEngine { respond(body) })
        val consumerEntered = CompletableDeferred<Unit>()
        val resumeConsumer = CompletableDeferred<Unit>()
        val expected = ByteArray(8_192) { (it % 251).toByte() }
        val config = ResourceConfigBuilder(coroutineContext).build()
        val result = async {
            ImageUrlFetcher(client).fetch(ImageUrl(IMAGE_URL), config)
                .map { resource ->
                    val channel = assertIs<Resource.Success<ByteReadChannel>>(resource).value
                    consumerEntered.complete(Unit)
                    resumeConsumer.await()
                    channel.toByteArray()
                }
                .single()
        }
        try {
            consumerEntered.await()
            assertFalse(body.isClosedForRead)
            assertFalse(result.isCompleted)
            val producer = launch {
                body.writeFully(expected)
                body.close()
            }
            resumeConsumer.complete(Unit)
            assertContentEquals(expected, result.await())
            producer.join()
        } finally {
            resumeConsumer.complete(Unit)
            result.cancelAndJoin()
            body.cancel()
            client.close()
        }
    }

    @Test
    fun cancellingConsumerCancelsTheStreamingResponse() = runTest {
        val body = ByteChannel(autoFlush = true)
        val client = HttpClient(MockEngine { respond(body) })
        val consumerEntered = CompletableDeferred<Unit>()
        val config = ResourceConfigBuilder(coroutineContext).build()
        val collection = launch {
            ImageUrlFetcher(client).fetch(ImageUrl(IMAGE_URL), config).collect { resource ->
                val channel = assertIs<Resource.Success<ByteReadChannel>>(resource).value
                consumerEntered.complete(Unit)
                channel.toByteArray()
            }
        }
        try {
            consumerEntered.await()
            collection.cancelAndJoin()
            assertTrue(body.isClosedForRead)
        } finally {
            collection.cancelAndJoin()
            body.cancel()
            client.close()
        }
    }

    @Test
    fun transportFailurePropagatesToTheCollector() = runTest {
        val client = HttpClient(MockEngine { throw IllegalStateException("transport failed") })
        try {
            val config = ResourceConfigBuilder(coroutineContext).build()
            assertFailsWith<IllegalStateException> {
                ImageUrlFetcher(client).fetch(ImageUrl(IMAGE_URL), config).collect()
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun configuredHttpValidationStillRejectsErrorResponses() = runTest {
        val client = HttpClient(MockEngine { respond("missing", HttpStatusCode.NotFound) }) {
            expectSuccess = true
        }
        try {
            val config = ResourceConfigBuilder(coroutineContext).build()
            assertFailsWith<ClientRequestException> {
                ImageUrlFetcher(client).fetch(ImageUrl(IMAGE_URL), config).collect()
            }
        } finally {
            client.close()
        }
    }

    private companion object {
        const val IMAGE_URL = "https://example.test/photo.jpg"
    }
}
