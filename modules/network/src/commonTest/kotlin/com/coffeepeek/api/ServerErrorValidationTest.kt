package com.coffeepeek.api

import com.coffeepeek.api.utils.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerErrorValidationTest {
    @Test
    fun serverErrorWithNonJsonBodyIsSafeForDisplay() = runBlocking {
        for (status in listOf(HttpStatusCode.InternalServerError, HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable)) {
            val client = HttpClient(MockEngine { respond("<html>Server error</html>", status) }) {
                installServerErrorValidation()
            }
            try {
                val error = assertFailsWith<ApiException> { client.get("https://example.test/api/shops") }
                assertEquals("Сервис временно недоступен. Попробуйте чуть позже.", error.message)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun clientErrorsRemainAvailableForExistingBusinessHandling() = runBlocking {
        val client = HttpClient(MockEngine { respond("bad request", HttpStatusCode.BadRequest) }) {
            installServerErrorValidation()
        }
        try {
            assertEquals(HttpStatusCode.BadRequest, client.get("https://example.test/api/shops").status)
        } finally {
            client.close()
        }
    }
}
