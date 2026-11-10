package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.feature.favorites.data.repository.createAccountFavorites
import com.coffeepeek.core.network.configureApiTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoadedServerFavoritesCacheTest {
    private val dispatcher = StandardTestDispatcher()

    @Test fun concurrentNormalizedSubscriptionsShareOneRpcAndSubsequentReadsUseTheCache() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var sends = 0
        val client = client { sends++; gate.await() }
        try {
            val sessions = CheckInTestSessions()
            val source = createAccountFavorites(client, sessions)
            val subscriptions = List(3) { async { observeLoadedServerFavorites(source, sessions).first().getOrThrow() } }
            runCurrent()
            assertEquals(1, sends)
            gate.complete(Unit)
            assertTrue(subscriptions.awaitAll().all { it.isEmpty() })
            assertTrue(observeLoadedServerFavorites(source, sessions).first().getOrThrow().isEmpty())
            assertEquals(1, sends)
        } finally { client.close() }
    }

    private fun client(beforeResponse: suspend () -> Unit) = HttpClient(MockEngine) {
        engine {
            dispatcher = this@LoadedServerFavoritesCacheTest.dispatcher
            addHandler {
                beforeResponse()
                respond("""{"isSuccess":true,"data":[]}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        configureApiTransport("https://fixture.invalid")
    }
}
