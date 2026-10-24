package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ApiEnvelopeTest {
    @Serializable private data class Item(val id: String)

    private fun client(status: HttpStatusCode, body: String) = HttpClient(MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    @Test fun aSuccessfulEnvelopeGivesItsData() = runBlocking<Unit> {
        val response = client(HttpStatusCode.OK, """{"isSuccess":true,"data":{"id":"a"}}""").get("/x")
        assertEquals(Item("a"), response.envelopeData<Item> { IllegalArgumentException(it) })
    }

    @Test fun aRejectedEnvelopeOrMissingDataFailsWithTheServerMessage() = runBlocking<Unit> {
        val rejected = client(HttpStatusCode.OK, """{"isSuccess":false,"message":"Нельзя","data":null}""").get("/x")
        assertEquals("Нельзя", assertFailsWith<IllegalArgumentException> { rejected.envelopeData<Item> { IllegalArgumentException(it) } }.message)
        val blank = client(HttpStatusCode.OK, """{"isSuccess":false,"message":"  "}""").get("/x")
        assertNull(assertFailsWith<IllegalArgumentException> { blank.envelopeData<Item> { IllegalArgumentException(it) } }.message)
        val missing = client(HttpStatusCode.OK, """{"isSuccess":true}""").get("/x")
        assertFailsWith<IllegalStateException> { missing.envelopeData<Item> { IllegalArgumentException(it) } }
    }

    @Test fun anErrorResponseGivesItsMessageAndAForeignBodyGivesNull() = runBlocking<Unit> {
        assertEquals("Сервис недоступен", client(HttpStatusCode.ServiceUnavailable, """{"isSuccess":false,"message":"Сервис недоступен"}""").get("/x").errorMessage())
        assertNull(client(HttpStatusCode.BadGateway, "<html>Bad gateway</html>").get("/x").errorMessage())
        assertNull(client(HttpStatusCode.BadRequest, """{"message":""}""").get("/x").errorMessage())
    }
}
