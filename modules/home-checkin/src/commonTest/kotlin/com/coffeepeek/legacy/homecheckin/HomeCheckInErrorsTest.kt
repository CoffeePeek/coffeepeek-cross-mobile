package com.coffeepeek.feature.homecheckin

import com.coffeepeek.feature.homecheckin.data.HomeCheckInRepositoryImpl
import com.coffeepeek.feature.homecheckin.domain.*
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInField.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.*

class HomeCheckInErrorsTest {
    private val draft = HomeCheckInInput("2026-09-20T10:00:00Z")

    @Test
    fun reportsAllInvalidFieldsAndKeepsOptionalFieldsOptional() {
        assertTrue(homeCheckInFieldErrors(draft).isEmpty())
        val errors = homeCheckInFieldErrors(draft.copy(preparedAt = "", doseGrams = "0", brewDurationSeconds = 0, tasteScore = 6))
        assertEquals(setOf(Date, Dose, Duration, Rating), errors.keys)
        assertEquals("Количество кофе должно быть больше 0", errors[Dose])
        assertEquals("Время заваривания должно быть больше 0", errors[Duration])
        assertEquals("Укажите не более двух знаков после запятой", homeCheckInFieldErrors(draft.copy(doseGrams = "18.001"))[Dose])
        assertTrue(homeCheckInFieldErrors(draft.copy(doseGrams = "0.01", brewDurationSeconds = 604800)).isEmpty())
    }

    @Test
    fun timestampValidationMatchesServerOffsetAndPrecisionLimits() {
        assertTrue(Date in homeCheckInFieldErrors(draft.copy(preparedAt = "2026-09-20T10:00:00+14:01")))
        assertTrue(Date in homeCheckInFieldErrors(draft.copy(preparedAt = "2026-09-20T10:00:00.12345678Z")))
        assertTrue(Date in homeCheckInFieldErrors(draft.copy(preparedAt = "9999-09-20T10:00:00Z")))
        assertTrue(homeCheckInFieldErrors(draft.copy(preparedAt = "2026-09-20T10:00:00.1234567+14:00")).isEmpty())
    }

    @Test
    fun serverEnvelopeMapsEveryFieldWithoutExposingEnglish() = runBlocking {
        val failure = failure(HttpStatusCode.BadRequest, """{
            "isSuccess":false,"errorCode":"VALIDATION_FAILED","message":"Validation failed",
            "errors":{"DoseGrams":["DoseGrams must be between 0.01 and 10000 with at most two decimal places."],
            "BrewMethodCode":["Brew method not found in the catalog."],
            "BrewDurationSeconds":["BrewDurationSeconds must be between 1 and 604800."]}}
        """)
        assertEquals(setOf(Dose, Method, Duration), failure.fieldErrors.keys)
        assertEquals("Выберите доступный способ заваривания из списка", failure.fieldErrors[Method])
        assertFalse(failure.message.contains("Validation"))
    }

    @Test
    fun modelBindingProblemDetailsMapsJsonPathsAndNestedPhotos() = runBlocking {
        val failure = failure(HttpStatusCode.BadRequest, """{"title":"One or more validation errors occurred.",
            "errors":{"${'$'}.doseGrams":["The JSON value could not be converted to Decimal. Path..."],
            "request.Photos[0].StorageKey":["The StorageKey field is required."]}}
        """)
        assertEquals(setOf(Dose, Photos), failure.fieldErrors.keys)
        assertFalse(failure.fieldErrors.values.any { "JSON" in it || "StorageKey" in it })
    }

    @Test
    fun legacyMessageOnlyErrorsStillAttachToTheCorrectField() = runBlocking {
        assertEquals(setOf(Method), failure(HttpStatusCode.BadRequest,
            """{"isSuccess":false,"message":"Brew method not found in the catalog."}""").fieldErrors.keys)
        assertEquals(setOf(Bean), failure(HttpStatusCode.NotFound,
            """{"isSuccess":false,"message":"Published coffee product not found."}""").fieldErrors.keys)
        assertEquals(setOf(Photos), failure(HttpStatusCode.Conflict,
            """{"isSuccess":false,"message":"The uploads already belong to a committed check-in."}""").fieldErrors.keys)
        assertEquals(setOf(Duration), failure(HttpStatusCode.OK,
            """{"isSuccess":false,"message":"BrewDurationSeconds must be between 1 and 604800."}""").fieldErrors.keys)
    }

    @Test
    fun infrastructureFailuresHaveSafeSpecificMessages() = runBlocking {
        assertTrue(failure(HttpStatusCode.Unauthorized, "").message.contains("Войдите"))
        assertTrue(failure(HttpStatusCode.Forbidden, "").message.contains("Нет доступа"))
        assertTrue(failure(HttpStatusCode.TooManyRequests, "").message.contains("Подождите"))
        assertTrue(failure(HttpStatusCode.ServiceUnavailable, "<html>proxy stacktrace</html>").message.contains("временно недоступен"))
        val server = failure(HttpStatusCode.InternalServerError,
            """{"message":"SQL password=secret","errors":{"DoseGrams":["SQL exception"]},"stackTrace":"secret"}""")
        assertTrue(server.fieldErrors.isEmpty())
        assertFalse(server.message.contains("secret"))
        val unknown = failure(HttpStatusCode.BadRequest, """{"message":"SomeInternalDto has invalid state"}""")
        assertFalse(unknown.message.contains("InternalDto"))
    }

    @Test
    fun malformedSuccessAndOfflineResponsesNeverExposeExceptionDetails() = runBlocking {
        assertTrue(failure(HttpStatusCode.OK, "not-json").message.contains("ответ сервера"))
        val client = client(MockEngine { throw IOException("Failed to connect https://internal-host/path") })
        try {
            val failure = HomeCheckInRepositoryImpl(client, client, "https://example.com").create(draft).exceptionOrNull()
            assertTrue(failure is HomeCheckInFailure)
            assertTrue(failure.message.orEmpty().contains("Проверьте интернет"))
            assertFalse(failure.message.orEmpty().contains("internal-host"))
        } finally { client.close() }
    }

    @Test
    fun cancellationIsNotPresentedAsAValidationFailure() = runBlocking<Unit> {
        val client = client(MockEngine { throw CancellationException("cancelled") })
        try {
            assertFailsWith<CancellationException> {
                HomeCheckInRepositoryImpl(client, client, "https://example.com").create(draft)
            }
        } finally { client.close() }
    }

    private suspend fun failure(status: HttpStatusCode, body: String): HomeCheckInFailure {
        val client = client(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) })
        return try {
            assertIs<HomeCheckInFailure>(HomeCheckInRepositoryImpl(client, client, "https://example.com").create(draft).exceptionOrNull())
        } finally { client.close() }
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { url("https://example.com") }
    }
}
