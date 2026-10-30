package com.coffeepeek.admin.network

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.api.service.AuthService
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDeviceMetadataTest {
    @Test fun everySessionCreationAndRefreshSendsTheActualDeviceAndOs() = runBlocking {
        val client = ApiClients("https://gateway.example",
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.absolutePath,
            debug = false, credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { return null }
                override suspend fun save(tokens: SessionTokens?) {  }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } })
        val requests = mutableListOf<Pair<String, List<String>?>>()
        val captured = IllegalStateException("Request captured before the network")
        client.plainClient.plugin(HttpSend).intercept { request ->
            requests += request.url.build().encodedPath to request.headers.getAll(HttpHeaders.UserAgent)
            throw captured
        }
        try {
            val auth = AuthService(client.client, client.plainClient)
            for (result in listOf(
                auth.login("fixture@example.com", "fixture-password"),
                auth.googleLogin("fixture-google-token"),
                auth.appleLogin("fixture-challenge", "fixture-apple-token"),
                auth.refresh("fixture-refresh-token"),
            )) assertEquals(captured, result.exceptionOrNull())

            assertEquals(listOf("/api/Tokens", "/api/Tokens/google/login",
                "/api/Tokens/apple/login", "/api/Tokens"), requests.map { it.first })
            for ((_, agents) in requests) {
                assertEquals(1, agents?.size)
                val agent = agents!!.single()
                assertTrue(agent.startsWith("CoffeePeek ("))
                assertTrue(agent.contains(Build.MODEL))
                assertTrue(agent.contains(Build.MANUFACTURER, ignoreCase = true))
                assertTrue(agent.contains("Android ${Build.VERSION.RELEASE}"))
            }
        } finally {
            client.client.close()
            client.plainClient.close()
            client.imageClient.close()
            client.uploadClient.close()
        }
    }
}
