package com.coffeepeek.feature.contentreport.data

import com.coffeepeek.feature.contentreport.data.repository.createContentReportRepository
import com.coffeepeek.feature.contentreport.domain.model.*
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class ContentReportContractTest {
    private class Fixture {
        var status = HttpStatusCode.Created
        var response = """{"isSuccess":true,"data":{"id":"receipt","extra":true}}"""
        var thrown: Exception? = null
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(ContentType.Application.Json, request.body.contentType)
            bodies += (request.body as TextContent).text
            thrown?.let { throw it }
            respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { expectSuccess = true; install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val repository = createContentReportRepository(client)
    }

    private suspend fun withFixture(block: suspend (Fixture) -> Unit) {
        val fixture = Fixture()
        try { block(fixture) } finally { fixture.client.close() }
    }

    private suspend fun ContentReportRepository.send(kind: ContentReportKind = ContentReportKind.CheckIn, id: String = "publication") =
        submit(ContentReportTarget(kind, id), "  Причина  ")

    @Test fun checkInAndReviewUseTheirOwnExistingEndpointsAndTrimmedJson() = runTest { withFixture { f ->
        f.repository.send().getOrThrow()
        f.repository.send(ContentReportKind.Review).getOrThrow()
        assertEquals(listOf("/api/v1/check-ins/publication/reports", "/api/CoffeeShopReviews/publication/reports"), f.paths)
        assertEquals(listOf("{\"text\":\"Причина\"}", "{\"text\":\"Причина\"}"), f.bodies)
    } }

    @Test fun targetIsEncodedAsOneSegment() = runTest { withFixture { f ->
        f.repository.send(id = "entry/with space").getOrThrow()
        assertEquals("/api/v1/check-ins/entry%2Fwith%20space/reports", f.paths.single())
    } }

    @Test fun invalidInputNeverReachesTransport() = runTest { withFixture { f ->
        for (reason in listOf("   ", "x".repeat(2001))) {
            val error = f.repository.submit(ContentReportTarget(ContentReportKind.CheckIn, "id"), reason).exceptionOrNull()
            assertEquals(ContentReportProblem.InvalidInput, (error as ContentReportFailure).problem)
        }
        assertTrue(f.repository.send(id = " ").isFailure)
        assertTrue(f.paths.isEmpty())
    } }

    @Test fun receivedClientRejectionsRetainTheirBusinessCategoryWithoutRetry() = runTest { withFixture { f ->
        val cases = mapOf(401 to ContentReportProblem.SignInRequired, 403 to ContentReportProblem.Forbidden,
            404 to ContentReportProblem.NotFound, 429 to ContentReportProblem.RateLimited, 400 to ContentReportProblem.Rejected)
        f.response = "not json with private details"
        for ((status, problem) in cases) {
            f.status = HttpStatusCode.fromValue(status)
            assertEquals(problem, (f.repository.send().exceptionOrNull() as ContentReportFailure).problem)
        }
        assertEquals(cases.size, f.paths.size)
    } }

    @Test fun checkInAcceptsAnyNonNullReceiptButReviewRequiresItsStringId() = runTest { withFixture { f ->
        f.response = """{"isSuccess":true,"data":{}}"""
        f.repository.send().getOrThrow()
        assertEquals(ContentReportProblem.Unconfirmed, (f.repository.send(ContentReportKind.Review).exceptionOrNull() as ContentReportFailure).problem)
        f.response = """{"isSuccess":true,"data":{"id":42}}"""
        assertTrue(f.repository.send(ContentReportKind.Review).isFailure)
    } }

    @Test fun pascalEnvelopeAliasesRemainSupported() = runTest { withFixture { f ->
        f.response = """{"IsSuccess":true,"Data":{"id":"receipt"},"Message":null}"""
        f.repository.send(ContentReportKind.Review).getOrThrow()
    } }

    @Test fun malformedMissingOrNullAcknowledgementIsUnconfirmed() = runTest { withFixture { f ->
        for (body in listOf("bad json", "{}", """{"isSuccess":true}""", """{"isSuccess":true,"data":null}""")) {
            f.response = body
            assertEquals(ContentReportProblem.Unconfirmed, (f.repository.send().exceptionOrNull() as ContentReportFailure).problem)
        }
    } }

    @Test fun businessRejectionKeepsOnlyItsEnvelopeMessage() = runTest { withFixture { f ->
        f.response = """{"isSuccess":false,"message":"Уже отправлено"}"""
        val failure = f.repository.send().exceptionOrNull() as ContentReportFailure
        assertEquals(ContentReportProblem.Rejected, failure.problem)
        assertEquals("Уже отправлено", failure.serverMessage)
    } }

    @Test fun unexpectedSuccessAndServerErrorCannotCertifyDeliveryOrRetry() = runTest { withFixture { f ->
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.NoContent, HttpStatusCode.InternalServerError)) {
            f.status = status
            assertEquals(ContentReportProblem.Unconfirmed, (f.repository.send().exceptionOrNull() as ContentReportFailure).problem)
        }
        assertEquals(3, f.paths.size)
    } }

    @Test fun lostTransportHidesPrivateDetailsAndDoesNotRetry() = runTest { withFixture { f ->
        f.thrown = IllegalStateException("private signed URL")
        val failure = f.repository.send().exceptionOrNull() as ContentReportFailure
        assertEquals(ContentReportProblem.Unconfirmed, failure.problem)
        assertNull(failure.serverMessage)
        assertFalse(failure.toString().contains("private signed URL"))
        assertEquals(1, f.paths.size)
    } }

    @Test fun cancellationIsRethrown() = runTest { withFixture { f ->
        f.thrown = CancellationException("cancelled")
        assertFailsWith<CancellationException> { f.repository.send() }
    } }
}
