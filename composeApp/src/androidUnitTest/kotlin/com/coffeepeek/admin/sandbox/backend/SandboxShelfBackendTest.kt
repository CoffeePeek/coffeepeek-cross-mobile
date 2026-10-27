package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import java.time.LocalDate
import kotlin.test.*

/** Raw HTTP checks of the Sandbox shelf against `coffeepeek-backend/docs/api/shelf-client.md`. */
class SandboxShelfBackendTest {
    private val png = byteArrayOf(1, 2, 3)
    private fun backend() = SandboxBackend(png, today = LocalDate.parse("2026-10-27"))
    private val base = "${SandboxBackend.BASE_URL}${SandboxShelfBackend.ROOT}"

    @Test fun populatedShelfIsOrderedAndArchiveIsNewestFirstWithSearch() = runTest {
        withShelf { api, _ ->
            val shelf = api.data("GET", base).jsonArray
            assertEquals(listOf(0, 1, 2, 3), shelf.map { it.int("position") })
            assertEquals("Sandbox Ethiopia", shelf[0].text("name"))
            assertEquals("Sandbox Roaster", shelf[0].text("roasterName"))
            val archive = api.data("GET", "$base/archive").jsonObject
            assertEquals(3, archive.int("totalCount"))
            assertEquals(listOf("Sandbox Kenya", "Гондурас Мойзес", "Перу Санта Роса"), archive.getValue("items").jsonArray.map { it.text("name") })
            assertTrue(archive.getValue("items").jsonArray.all { it.jsonObject["position"] is JsonNull })
            val found = api.data("GET", "$base/archive?query=river").jsonObject
            assertEquals(1, found.int("totalCount"))
            assertEquals(1, api.data("GET", "$base/archive?pageNumber=2&pageSize=2").jsonObject.getValue("items").jsonArray.size)
        }
    }

    @Test fun createUploadsCardAtFrontAndServesItOnlyWhileTheBagExists() = runTest {
        withShelf(populated = false) { api, backend ->
            val key = upload(api, backend)
            val bag = api.data("POST", "$base/bags", """{"photo":{"storageKey":"$key","fileName":"bag.jpg","contentType":"image/jpeg","size":3},
                "customName":"Своя пачка","weightGrams":250,"grind":"Ground","roastedOn":"2026-10-20"}""").jsonObject
            assertEquals(0, bag.int("position"))
            assertEquals("OnShelf", bag.text("status"))
            val url = SandboxBackend.BASE_URL + bag.getValue("photo").text("url")
            assertContentEquals(byteArrayOf(9, 9), api.get(url).readRawBytes())
            assertEquals(HttpStatusCode.Conflict, api.call("POST", "$base/bags", """{"photo":{"storageKey":"$key"}}""").status)
            assertEquals(HttpStatusCode.OK, api.call("DELETE", "$base/bags/${bag.text("id")}").status)
            assertEquals(HttpStatusCode.NotFound, api.get(url).status)
            assertTrue(api.data("GET", base).jsonArray.isEmpty())
        }
    }

    @Test fun createRejectsInvalidInputLikeTheServer() = runTest {
        withShelf(populated = false) { api, _ ->
            assertEquals(HttpStatusCode.BadRequest, api.call("POST", "$base/bags", "{}").status)
            assertEquals(HttpStatusCode.BadRequest, api.call("POST", "$base/bags", """{"photo":{"storageKey":"never-uploaded"}}""").status)
            assertEquals(HttpStatusCode.BadRequest, api.call("POST", "$base/bags", """{"coffeeProductSlug":"sandbox-kenya","customName":"x"}""").status)
            assertEquals(HttpStatusCode.NotFound, api.call("POST", "$base/bags", """{"coffeeProductSlug":"unknown"}""").status)
            assertEquals(HttpStatusCode.BadRequest, api.call("POST", "$base/bags", """{"customName":"x","weightGrams":0}""").status)
            assertEquals(HttpStatusCode.BadRequest, api.call("POST", "$base/bags", """{"customName":"x","roastedOn":"2026-10-29"}""").status)
            assertEquals(HttpStatusCode.BadRequest, api.call("POST", "$base/bags", """{"customName":"x","grind":"Fine"}""").status)
        }
    }

    @Test fun putReplacesDetailsKeepsProductByIdAndKeepsCardWhenOmitted() = runTest {
        withShelf { api, _ ->
            val first = api.data("GET", base).jsonArray[0].jsonObject
            val id = first.text("id")
            val kept = api.data("PUT", "$base/bags/$id", """{"coffeeProductId":"${first.text("coffeeProductId")}","weightGrams":500}""").jsonObject
            assertEquals("Sandbox Ethiopia", kept.text("name"))
            assertEquals(500, kept.int("weightGrams"))
            assertTrue(kept["grind"] is JsonNull)
            assertEquals(first["photo"], kept["photo"])
            assertEquals(HttpStatusCode.BadRequest, api.call("PUT", "$base/bags/$id", """{"coffeeProductId":"another"}""").status)
            assertTrue(api.data("PUT", "$base/bags/$id", "{}").jsonObject["name"] is JsonNull)
        }
    }

    @Test fun reorderNeedsTheWholeCurrentShelf() = runTest {
        withShelf { api, _ ->
            val ids = api.data("GET", base).jsonArray.map { it.text("id") }
            val reversed = ids.reversed()
            val body = buildJsonObject { putJsonArray("bagIds") { reversed.forEach { add(it) } } }.toString()
            assertEquals(reversed, api.data("PUT", "$base/order", body).jsonArray.map { it.text("id") })
            val partial = buildJsonObject { putJsonArray("bagIds") { reversed.drop(1).forEach { add(it) } } }.toString()
            assertEquals(HttpStatusCode.Conflict, api.call("PUT", "$base/order", partial).status)
        }
    }

    @Test fun finishThenRestoreReturnsTheBagToItsPosition() = runTest {
        withShelf { api, _ ->
            val id = api.data("GET", base).jsonArray[2].text("id")
            val finished = api.data("POST", "$base/bags/$id/finish", """{"rating":4,"buyAgain":true}""").jsonObject
            assertEquals("Finished", finished.text("status"))
            assertTrue(finished["position"] is JsonNull)
            assertEquals(3, api.data("GET", base).jsonArray.size)
            assertEquals(HttpStatusCode.Conflict, api.call("POST", "$base/bags/$id/finish", "{}").status)
            val restored = api.data("POST", "$base/bags/$id/restore", """{"position":2}""").jsonObject
            assertEquals(2, restored.int("position"))
            assertTrue(restored["rating"] is JsonNull)
            assertEquals(HttpStatusCode.Conflict, api.call("POST", "$base/bags/$id/restore", "{}").status)
            assertEquals(HttpStatusCode.BadRequest, api.call("POST", "$base/bags/$id/finish", """{"rating":6}""").status)
        }
    }

    @Test fun saveErrorScenarioFailsShelfWrites() = runTest {
        withShelf { api, backend ->
            backend.scenario = SandboxScenario.SaveError
            assertEquals(HttpStatusCode.ServiceUnavailable, api.call("POST", "$base/bags", """{"customName":"x"}""").status)
            assertEquals(HttpStatusCode.OK, api.call("GET", base).status)
        }
    }

    private suspend fun withShelf(populated: Boolean = true, block: suspend (HttpClient, SandboxBackend) -> Unit) {
        val backend = backend()
        val api = backend.apiClient {}
        try {
            if (!populated) backend.reset(false)
            block(api, backend)
        } finally {
            api.close(); backend.close()
        }
    }

    private suspend fun upload(api: HttpClient, backend: SandboxBackend): String {
        val reservation = api.data("POST", "${SandboxBackend.BASE_URL}/api/Photos/shelf-bag",
            """[{"sizeBytes":2,"fileName":"bag.jpg","contentType":"image/jpeg"}]""").jsonArray.single()
        val uploader = backend.uploadClient {}
        try {
            uploader.put(reservation.text("uploadUrl")) { setBody(byteArrayOf(9, 9)) }
        } finally { uploader.close() }
        return reservation.text("storageKey")
    }

    private suspend fun HttpClient.call(method: String, url: String, body: String? = null): HttpResponse = request(url) {
        this.method = HttpMethod.parse(method)
        if (body != null) { contentType(ContentType.Application.Json); setBody(body.toByteArray()) }
    }
    private suspend fun HttpClient.data(method: String, url: String, body: String? = null): JsonElement {
        val response = call(method, url, body)
        assertEquals(HttpStatusCode.OK, response.status, "$method $url")
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data")
    }
    private fun JsonElement.text(key: String) = jsonObject.getValue(key).jsonPrimitive.content
    private fun JsonElement.int(key: String) = jsonObject.getValue(key).jsonPrimitive.int
}
