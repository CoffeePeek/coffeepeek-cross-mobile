package com.coffeepeek.api

import com.coffeepeek.api.service.ShopApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RequestCancellationTest {
    @Test fun cancellationFromTransportEscapesTheLegacyServiceResult(): Unit = runBlocking {
        val client = HttpClient(MockEngine { throw CancellationException("Screen closed") })
        try { assertFailsWith<CancellationException> { ShopApiService(client).getCities() } }
        finally { client.close() }
    }
}
