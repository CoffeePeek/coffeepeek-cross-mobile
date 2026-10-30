package com.coffeepeek.admin.auth

import com.coffeepeek.admin.utils.localizedLoginError
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.api.service.AuthService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the user sees when the sign-in request itself fails (no response at all), through the real client stack and
 * `AuthService`. Documents the legacy mapping; the migrated code must keep or deliberately improve it.
 */
class AuthTransportFailureTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun loginMessage(failure: () -> Throwable): Pair<String?, String> = runBlocking {
        val client = ApiClients("https://api.example", temporary.newFolder().path, false, credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { return null }
                override suspend fun save(tokens: SessionTokens?) {  }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } },
            apiClientFactory = { configure -> HttpClient(MockEngine { throw failure() }) { configure() } },
            uploadClientFactory = { configure -> HttpClient(MockEngine { throw failure() }) { configure() } })
        try {
            val error = AuthService(client.client, client.plainClient).login("person@example.com", "secret1").exceptionOrNull()
            assertTrue(error != null, "A transport failure is a failed result")
            error.message to localizedLoginError(error.message)
        } finally {
            client.client.close(); client.plainClient.close(); client.uploadClient.close(); client.imageClient.close()
        }
    }

    @Test fun transportFailuresReachTheUserAsWordedHere() {
        val network = "Не удалось подключиться. Проверьте интернет-соединение"
        val generic = "Не удалось войти. Проверьте email и пароль"
        val cases = linkedMapOf<String, Pair<() -> Throwable, String>>(
            "I/O failure" to ({ IOException("Sandbox offline scenario") } to generic),
            "unknown host" to ({ UnknownHostException("api.example") } to generic),
            "connect failure" to ({ ConnectException("Failed to connect to api.example/10.0.0.1:443") } to network),
            "socket timeout" to ({ SocketTimeoutException("timeout") } to network),
            "request timeout" to ({ HttpRequestTimeoutException("https://api.example/api/Tokens", 10_000) } to network),
        )
        val seen = cases.mapValues { (_, case) -> loginMessage(case.first) }
        assertEquals(cases.mapValues { it.value.second }, seen.mapValues { it.value.second }, seen.toString())
    }
}
