package com.coffeepeek.feature.shelf.data.repository

import com.coffeepeek.feature.shelf.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.serialization.json.*
import kotlin.test.*

/** Request/response shapes follow the examples of `coffeepeek-backend/docs/api/shelf-client.md`. */
class ShelfRepositoryContractTest {
    private val base = "https://gateway.example.com"
    private val ids = listOf("11111111-1111-4111-8111-111111111111", "11111111-1111-4111-8111-111111111112", "11111111-1111-4111-8111-111111111113")
    private val owners = MutableStateFlow<String?>("user-1")
    private val account = object : ShelfAccount {
        override val owner get() = owners.value
        override val owners = this@ShelfRepositoryContractTest.owners
    }
    private val requests = mutableListOf<String>()

    @Test fun documentedBagIsReadWithAnAbsoluteProtectedPhotoUrl() = runTest {
        val repository = repository { request -> if (request.isGet(ROOT)) ok("[${documented()}]") else null }
        val bag = repository.refresh().getOrThrow().single()
        assertEquals(ShelfBagStatus.OnShelf, bag.status)
        assertEquals(ShelfGrind.Beans, bag.grind)
        assertEquals(ShelfRoastPurpose.Espresso, bag.roastPurpose)
        assertEquals("$base/api/v1/shelf/bags/${ids[0]}/photos/33333333-3333-4333-8333-333333333333", bag.photo?.url)
        assertEquals("Sibaristica", bag.roasterName)
        assertEquals(listOf(bag), repository.shelf.first())
    }

    @Test fun unknownStatusOrAPositionlessShelfBagIsAnInvalidResponse() = runTest {
        val repository = repository { ok("""[${documented().replace("\"OnShelf\"", "\"Lost\"")}]""") }
        assertEquals(ShelfIssue.InvalidResponse, repository.refresh().issue())
        val positionless = repository { ok("""[${documented().replace("\"position\": 0", "\"position\": null")}]""") }
        assertEquals(ShelfIssue.InvalidResponse, positionless.refresh().issue())
    }

    @Test fun aRoastPurposeTheAppDoesNotKnowReadsAsUnknown() = runTest {
        val repository = repository { ok("""[${documented().replace("\"Espresso\"", "\"Omni\"")}]""") }
        assertNull(repository.refresh().getOrThrow().single().roastPurpose)
    }

    @Test fun addUploadsTheCardThenCreatesWithItsKeyAndPutsTheBagFirst() = runTest {
        var created: JsonObject? = null
        val uploaded = mutableListOf<HttpRequestData>()
        val repository = repository(uploader = { uploaded += it; respond("", HttpStatusCode.OK) }) { request ->
            when {
                request.isGet(ROOT) -> ok("[${bag(ids[0], 0)}]")
                request.url.encodedPath == "/api/Photos/shelf-bag" ->
                    ok("""[{"photoId":"p","uploadUrl":"https://storage.example.com/up","storageKey":"shelf-bags/k.jpg"}]""")
                request.url.encodedPath == "$ROOT/bags" -> { created = request.json(); ok(bag(ids[1], 0)) }
                else -> null
            }
        }
        repository.refresh().getOrThrow()
        val details = ShelfBagDetails(ShelfBean.Catalog("nut-n-berry"), weightGrams = 250, grind = ShelfGrind.Beans,
            roastPurpose = ShelfRoastPurpose.Filter)
        repository.add(details, ShelfBagCard(byteArrayOf(1, 2), "image/jpeg", "bag.jpg")).getOrThrow()
        val upload = uploaded.single()
        assertNull(upload.headers[HttpHeaders.Authorization])
        assertEquals("is_permanent=False", upload.headers["x-amz-tagging"])
        assertEquals(JsonPrimitive("shelf-bags/k.jpg"), created!!.getValue("photo").jsonObject["storageKey"])
        assertEquals(JsonPrimitive("nut-n-berry"), created!!["coffeeProductSlug"])
        assertEquals(JsonPrimitive("Filter"), created!!["roastPurpose"])
        assertEquals(listOf(ids[1], ids[0]), repository.shelf.first()!!.map { it.id })
        assertEquals(listOf(0, 1), repository.shelf.first()!!.map { it.position })
    }

    @Test fun addMapsTheContextualStatusesAndRejectsAnEmptyBagLocally() = runTest {
        var status = HttpStatusCode.NotFound
        val repository = repository { request -> if (request.url.encodedPath == "$ROOT/bags") respond("{}", status) else null }
        assertEquals(ShelfIssue.InvalidInput, repository.add(ShelfBagDetails(), null).issue())
        assertTrue(requests.isEmpty())
        assertEquals(ShelfIssue.BeanUnavailable, repository.add(ShelfBagDetails(ShelfBean.Catalog("gone")), null).issue())
        status = HttpStatusCode.Conflict
        assertEquals(ShelfIssue.ShelfFull, repository.add(ShelfBagDetails(ShelfBean.Custom("Своя")), null).issue())
    }

    @Test fun moveShowsTheNewOrderAtOnceAndRollsBackWhenTheServerFails() = runTest {
        val answer = CompletableDeferred<HttpStatusCode>()
        var sent: JsonObject? = null
        val repository = repository { request ->
            when {
                request.isGet(ROOT) -> ok(shelfOf(ids))
                request.url.encodedPath == "$ROOT/order" -> { sent = request.json(); respond("{}", answer.await()) }
                else -> null
            }
        }
        repository.refresh().getOrThrow()
        val move = async { repository.move(ids[2], 0) }
        yield()
        assertEquals(listOf(ids[2], ids[0], ids[1]), repository.shelf.first()!!.map { it.id })
        answer.complete(HttpStatusCode.InternalServerError)
        assertEquals(ShelfIssue.Unavailable, move.await().issue())
        assertEquals(JsonArray(listOf(ids[2], ids[0], ids[1]).map(::JsonPrimitive)), sent!!["bagIds"])
        assertEquals(ids, repository.shelf.first()!!.map { it.id })
    }

    @Test fun aStaleMoveReloadsTheServerOrder() = runTest {
        var reads = 0
        val repository = repository { request ->
            when {
                request.isGet(ROOT) -> ok(shelfOf(if (reads++ == 0) ids else ids.reversed()))
                request.url.encodedPath == "$ROOT/order" -> respond("{}", HttpStatusCode.Conflict)
                else -> null
            }
        }
        repository.refresh().getOrThrow()
        assertEquals(ShelfIssue.Stale, repository.move(ids[0], 2).issue())
        assertEquals(ids.reversed(), repository.shelf.first()!!.map { it.id })
    }

    @Test fun finishRemovesTheBagAtOnceAndPutsItBackOnFailure() = runTest {
        val answer = CompletableDeferred<HttpResponseData?>()
        val repository = repository { request ->
            when {
                request.isGet(ROOT) -> ok(shelfOf(ids))
                request.url.encodedPath == "$ROOT/bags/${ids[1]}/finish" -> answer.await() ?: respond("{}", HttpStatusCode.ServiceUnavailable)
                else -> null
            }
        }
        repository.refresh().getOrThrow()
        assertEquals(ShelfIssue.InvalidInput, repository.finish(ids[1], rating = 6, buyAgain = null).issue())
        val finish = async { repository.finish(ids[1], rating = 4, buyAgain = true) }
        yield()
        assertEquals(listOf(ids[0], ids[2]), repository.shelf.first()!!.map { it.id })
        answer.complete(null)
        assertEquals(ShelfIssue.Unavailable, finish.await().issue())
        assertEquals(ids, repository.shelf.first()!!.map { it.id })
    }

    @Test fun restoreReturnsTheBagToItsFormerPosition() = runTest {
        val repository = repository { request ->
            when {
                request.isGet(ROOT) -> ok(shelfOf(listOf(ids[0], ids[2])))
                request.url.encodedPath == "$ROOT/bags/${ids[1]}/restore" -> {
                    assertEquals(JsonPrimitive(1), request.json()["position"])
                    ok(bag(ids[1], 1))
                }
                else -> null
            }
        }
        repository.refresh().getOrThrow()
        repository.restore(ids[1], 1).getOrThrow()
        assertEquals(ids, repository.shelf.first()!!.map { it.id })
    }

    @Test fun anAccountChangeCancelsTheRequestAndHidesTheOldShelf() = runTest {
        val never = CompletableDeferred<Unit>()
        val repository = repository { request ->
            when {
                request.isGet(ROOT) -> ok(shelfOf(ids))
                request.url.encodedPath == "$ROOT/archive" -> { never.await(); null }
                else -> null
            }
        }
        repository.refresh().getOrThrow()
        val archive = async { repository.archive(1, 20) }
        yield()
        owners.value = "user-2"
        assertEquals(ShelfIssue.AccountChanged, archive.await().issue())
        assertNull(repository.shelf.first())
        owners.value = null
        assertEquals(ShelfIssue.SignedOut, repository.refresh().issue())
    }

    @Test fun archiveSendsPagingAndTrimmedQuery() = runTest {
        val repository = repository { request ->
            assertEquals("2", request.url.parameters["pageNumber"])
            assertEquals("river", request.url.parameters["query"])
            ok("""{"items":[${bag(ids[0], null, "Finished")}],"totalCount":21}""")
        }
        val page = repository.archive(2, 20, "  river ").getOrThrow()
        assertEquals(ShelfBagStatus.Finished, page.items.single().status)
        assertFalse(page.hasMore)
    }

    private fun repository(
        uploader: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { error("No upload expected") },
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData?,
    ) = createShelfRepository(
        HttpClient(MockEngine { request ->
            requests += "${request.method.value} ${request.url.encodedPath}"
            handler(request) ?: respond("{}", HttpStatusCode.NotFound)
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            defaultRequest { url(base); headers.append(HttpHeaders.Authorization, "Bearer fixture") }
            expectSuccess = true
        },
        HttpClient(MockEngine { uploader(it) }) { expectSuccess = true },
        base, account,
    )

    private fun MockRequestHandleScope.ok(data: String) =
        respond("""{"isSuccess":true,"message":"Operation successful","data":$data}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
    private fun HttpRequestData.isGet(path: String) = method == HttpMethod.Get && url.encodedPath == path
    private fun HttpRequestData.json(): JsonObject = Json.parseToJsonElement(when (val body = body) {
        is TextContent -> body.text
        is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
        else -> error("Unexpected body $body")
    }).jsonObject
    private fun Result<*>.issue() = (exceptionOrNull() as? ShelfFailure)?.issue
    private fun shelfOf(order: List<String>) = order.mapIndexed { index, id -> bag(id, index) }.joinToString(",", "[", "]")
    private fun bag(id: String, position: Int?, status: String = "OnShelf") =
        """{"id":"$id","status":"$status","position":${position ?: "null"},"photo":null,"name":"Bag $id","addedAtUtc":"2026-10-20T08:00:00Z"}"""

    /** The ShelfBagDto example of the contract document. */
    private fun documented() = """{
      "id": "${ids[0]}", "status": "OnShelf", "position": 0,
      "photo": { "id": "33333333-3333-4333-8333-333333333333", "url": "/api/v1/shelf/bags/${ids[0]}/photos/33333333-3333-4333-8333-333333333333" },
      "coffeeProductId": "22222222-2222-4222-8222-222222222222", "coffeeProductSlug": "nut-n-berry", "name": "Nut'n'Berry",
      "roasterName": "Sibaristica", "weightGrams": 250, "grind": "Beans", "roastPurpose": "Espresso",
      "roastedOn": "2026-10-12",
      "addedAtUtc": "2026-10-20T08:00:00Z", "finishedAtUtc": null, "rating": null, "buyAgain": null
    }"""

    private companion object { const val ROOT = "/api/v1/shelf" }
}
