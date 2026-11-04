package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import com.coffeepeek.feature.shelf.data.repository.ShelfAccount
import com.coffeepeek.feature.shelf.data.repository.createShelfProfileRepository
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID
import kotlin.test.*

/** Fake HTTP privacy contracts only: synthetic owner headers do not certify production authentication. */
class SandboxShelfProfileContractTest {
    @Test fun actualRepositoryLoadsAllPopulatedRowsIncludingThePhotoOnlyBag() = runTest {
        Fixture().use { f -> f.withRepository { repository ->
            repository.setVisibility(true).getOrThrow()
            val shelf = assertNotNull(repository.publicShelf("sandbox").getOrThrow())
            assertEquals(7, shelf.totalCount)
            assertEquals(7, shelf.items.size)
            assertEquals("", shelf.items.last().name)
            assertNotNull(shelf.items.last().photoUrl)
        } }
    }

    @Test fun visibilityDefaultsOffAndRejectsGuestsAndUnknownOwners() = runTest {
        Fixture().use { f ->
            assertFalse(f.preference().getValue("isVisible").jsonPrimitive.boolean)
            assertEquals(JsonNull, f.data("GET", PUBLIC))
            assertEquals(HttpStatusCode.Unauthorized, f.call("GET", PREFERENCE).status)
            assertEquals(HttpStatusCode.Unauthorized, f.call("PUT", PREFERENCE, "{\"isVisible\":true}").status)
            assertEquals(HttpStatusCode.Unauthorized, f.call("GET", PREFERENCE, owner = "unknown-owner").status)
            assertEquals(HttpStatusCode.NotFound, f.call("GET", "/api/v1/profiles/unknown/shelf").status)
        }
    }

    @Test fun optInExposesOnlyOrderedCurrentShelfFieldsAndUuidPhotoAliases() = runTest {
        Fixture().use { f ->
            val rows = f.rows()
            val ids = rows.map { it.text("id") }.reversed()
            f.data("PUT", "$ROOT/order", buildJsonObject { put("bagIds", JsonArray(ids.map(::JsonPrimitive))) }.toString())
            f.enable(true)
            val view = f.data("GET", PUBLIC).jsonObject
            val items = view.getValue("items").jsonArray
            assertEquals(ids, items.map { it.text("id") })
            assertEquals(rows.size, view.getValue("totalCount").jsonPrimitive.int)
            assertTrue(items.all { it.jsonObject.keys == setOf("id", "name", "roasterName", "coffeeSlug", "photoUrl") })
            val url = items.first().text("photoUrl")
            assertNotNull(UUID.fromString(url.substringAfterLast('/')))
            assertFalse(url.contains("bag-card-"))
            assertContentEquals(PHOTO, f.call("GET", url).readRawBytes())
        }
    }

    @Test fun optingOutRevokesPreviouslyReturnedPublicPhotoUrls() = runTest {
        Fixture().use { f ->
            f.enable(true)
            val url = f.firstPublicPhoto()
            assertEquals(HttpStatusCode.OK, f.call("GET", url).status)
            f.enable(false)
            assertEquals(JsonNull, f.data("GET", PUBLIC))
            val response = f.call("GET", url)
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("nosniff", response.headers["X-Content-Type-Options"])
        }
    }

    @Test fun finishingABagRevokesItsPublicPhotoAndRestoreMakesItCurrentAgain() = runTest {
        Fixture().use { f ->
            f.enable(true)
            val row = f.rows().first()
            val url = f.firstPublicPhoto()
            f.data("POST", "$ROOT/bags/${row.text("id")}/finish", "{}")
            assertEquals(HttpStatusCode.NotFound, f.call("GET", url).status)
            val items = f.data("GET", PUBLIC).jsonObject.getValue("items").jsonArray
            assertTrue(items.none { it.text("id") == row.text("id") })
            f.data("POST", "$ROOT/bags/${row.text("id")}/restore", "{\"position\":0}")
            assertContentEquals(PHOTO, f.call("GET", url).readRawBytes())
        }
    }

    @Test fun replacingABagsPhotoInvalidatesItsOldAliasWithoutChangingPrivateFixtureIds() = runTest {
        Fixture().use { f ->
            f.enable(true)
            val row = f.rows().first()
            val old = f.firstPublicPhoto()
            val key = f.upload()
            f.data("PUT", "$ROOT/bags/${row.text("id")}", """{"photo":{"storageKey":"$key"},"customName":"New front"}""")
            val current = f.firstPublicPhoto()
            assertNotEquals(old, current)
            assertEquals(HttpStatusCode.NotFound, f.call("GET", old).status)
            assertContentEquals(byteArrayOf(9, 9), f.call("GET", current).readRawBytes())
            val privatePhoto = f.rows().first().jsonObject.getValue("photo").jsonObject
            assertEquals(key, privatePhoto.text("id"))
        }
    }

    @Test fun putRequiresAnActualBooleanAndInvalidRequestsPreserveThePriorChoice() = runTest {
        Fixture().use { f ->
            f.enable(true)
            listOf("{}", "{\"isVisible\":null}", "{\"isVisible\":\"true\"}", "{\"isVisible\":1}", "{\"isVisible\":[]}").forEach { input ->
                assertEquals(HttpStatusCode.BadRequest, f.call("PUT", PREFERENCE, input, SELF).status)
            }
            assertTrue(f.preference().getValue("isVisible").jsonPrimitive.boolean)
        }
    }

    @Test fun ownerChoicesAreIsolatedAndASecondOwnerNeverReadsTheSelfShelf() = runTest {
        Fixture().use { f ->
            f.enable(true)
            assertFalse(f.preference(FRIEND).getValue("isVisible").jsonPrimitive.boolean)
            f.enable(true, FRIEND)
            val friend = f.data("GET", FRIEND_PUBLIC).jsonObject
            assertEquals(0, friend.getValue("totalCount").jsonPrimitive.int)
            assertTrue(friend.getValue("items").jsonArray.isEmpty())
            assertEquals(HttpStatusCode.NotFound, f.call("GET", f.firstPublicPhoto().replace("/sandbox/", "/sandbox-friend/")).status)
            f.enable(false, FRIEND)
            assertTrue(f.preference("sandbox").getValue("isVisible").jsonPrimitive.boolean)
            assertEquals(JsonNull, f.data("GET", FRIEND_PUBLIC))
        }
    }

    @Test fun blockedAuthorsLoseTheirPublicShelfRoute() = runTest {
        Fixture().use { f ->
            f.enable(true, FRIEND)
            assertEquals(HttpStatusCode.OK, f.call("GET", FRIEND_PUBLIC).status)
            f.data("PUT", "/api/v1/me/blocks/$FRIEND")
            assertEquals(HttpStatusCode.NotFound, f.call("GET", FRIEND_PUBLIC).status)
            f.data("DELETE", "/api/v1/me/blocks/$FRIEND")
            assertEquals(HttpStatusCode.OK, f.call("GET", FRIEND_PUBLIC).status)
        }
    }

    @Test fun mediaRechecksAuthorAvailabilityEvenAfterThePublicLinkWasIssued() {
        val shelf = SandboxShelfBackend(LocalDate.parse("2026-11-04")) { true }
        shelf.reset(true)
        var available = true
        val profile = SandboxShelfProfileBackend(shelf) { available }
        val request = HttpRequestBuilder().apply {
            url(SandboxBackend.BASE_URL + PREFERENCE)
            method = HttpMethod.Put
            headers.append(SandboxSavedCollectionsBackend.OWNER, SELF)
        }.build()
        assertEquals(HttpStatusCode.OK, profile.handle(request, buildJsonObject { put("isVisible", true) })?.first)
        val path = shelf.publicItems("sandbox").first().text("photoUrl")
        assertNotNull(profile.cardKey(path))
        available = false
        assertNull(profile.cardKey(path))
    }

    private class Fixture : AutoCloseable {
        val backend = SandboxBackend(PHOTO, today = LocalDate.parse("2026-11-04"))
        private val api: HttpClient = backend.apiClient {}

        suspend fun call(method: String, path: String, input: String? = null, owner: String? = null): HttpResponse =
            api.request(SandboxBackend.BASE_URL + path) {
                this.method = HttpMethod.parse(method)
                owner?.let { header(SandboxSavedCollectionsBackend.OWNER, it) }
                if (input != null) {
                    contentType(ContentType.Application.Json)
                    setBody(input.toByteArray())
                }
            }

        suspend fun data(method: String, path: String, input: String? = null, owner: String? = null): JsonElement {
            val response = call(method, path, input, owner)
            assertEquals(HttpStatusCode.OK, response.status, "$method $path")
            return Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data")
        }

        suspend fun preference(owner: String = SELF) = data("GET", PREFERENCE, owner = owner).jsonObject
        suspend fun enable(value: Boolean, owner: String = SELF) = data("PUT", PREFERENCE, "{\"isVisible\":$value}", owner)
        suspend fun rows() = data("GET", ROOT).jsonArray
        suspend fun firstPublicPhoto() = data("GET", PUBLIC).jsonObject.getValue("items").jsonArray.first().text("photoUrl")

        suspend fun upload(): String {
            val row = data("POST", "/api/Photos/shelf-bag", """[{"sizeBytes":2,"fileName":"bag.jpg","contentType":"image/jpeg"}]""").jsonArray.single()
            backend.uploadClient {}.use { client -> client.put(row.text("uploadUrl")) { setBody(byteArrayOf(9, 9)) } }
            return row.text("storageKey")
        }

        suspend fun withRepository(block: suspend (ShelfProfileRepository) -> Unit) {
            val ownerClient = jsonClient()
            val publicClient = jsonClient()
            val account = object : ShelfAccount {
                override val owner = SELF
                override val owners = MutableStateFlow<String?>(SELF)
            }
            try {
                block(createShelfProfileRepository(ownerClient, publicClient, SandboxBackend.BASE_URL, account) { request, owner ->
                    request.header(SandboxSavedCollectionsBackend.OWNER, owner)
                })
            } finally {
                ownerClient.close()
                publicClient.close()
            }
        }

        private fun jsonClient() = backend.gateway.config { }

        override fun close() {
            api.close()
            backend.close()
        }
    }

    companion object {
        private const val SELF = SandboxSavedCollectionsBackend.LOCAL_ACCOUNT
        private const val FRIEND = SandboxApplicationBackend.FRIEND_ID
        private const val ROOT = SandboxShelfBackend.ROOT
        private const val PREFERENCE = SandboxShelfProfileBackend.VISIBILITY
        private const val PUBLIC = "/api/v1/profiles/sandbox/shelf"
        private const val FRIEND_PUBLIC = "/api/v1/profiles/sandbox-friend/shelf"
        private val PHOTO = byteArrayOf(1, 2, 3)
        private fun JsonElement.text(key: String) = jsonObject.getValue(key).jsonPrimitive.content
    }
}
