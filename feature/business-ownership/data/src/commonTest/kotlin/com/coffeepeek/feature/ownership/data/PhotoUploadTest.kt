package com.coffeepeek.feature.ownership.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.ownership.data.repository.createBusinessPhotoUploader
import com.coffeepeek.feature.ownership.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoUploadTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val account = object : OwnershipAccount {
        val value = MutableStateFlow<String?>("owner")
        override fun current() = value.value
        override fun observe() = value
    }
    private val photos = listOf(
        BusinessPhoto("a", fileName = "a.jpg", contentType = "image/jpeg", bytes = byteArrayOf(1, 2, 3)),
        BusinessPhoto("b", fileName = "b.png", contentType = "image/png", bytes = byteArrayOf(4, 5)),
    )

    private fun reservation(vararg urls: String) =
        """{"isSuccess":true,"data":[${urls.mapIndexed { i, url -> """{"uploadUrl":"$url","storageKey":"stored/$i"}""" }.joinToString(",")}]}"""

    private fun uploader(api: MockEngine, storage: MockEngine) = createBusinessPhotoUploader(
        HttpClientFactory(api).api("https://api.example"), HttpClient(storage), account,
    )

    @Test fun eachKindReservesItsOwnEndpointAndSendsTheBytesToTheSignedUrlsWithoutCredentials() = runTest {
        for ((kind, path) in listOf(BusinessKind.CoffeeShop to "/api/Photos/shop", BusinessKind.Roaster to "/api/Photos/roaster")) {
            val apiRequests = mutableListOf<HttpRequestData>()
            val stored = mutableListOf<HttpRequestData>()
            val api = MockEngine { request ->
                apiRequests += request
                respond(reservation("https://storage.example.com/one", "https://storage.example.com/two"), headers = jsonHeaders)
            }
            val storage = MockEngine { request -> stored += request; respond("", HttpStatusCode.OK) }
            val result = uploader(api, storage).upload(kind, photos).getOrThrow()

            assertEquals(path, apiRequests.single().url.encodedPath)
            val body = Json.parseToJsonElement((apiRequests.single().body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonArray
            assertEquals(listOf(3, 2), body.map { it.jsonObject["sizeBytes"]!!.jsonPrimitive.content.toInt() })
            assertEquals(listOf("a.jpg", "b.png"), body.map { it.jsonObject["fileName"]!!.jsonPrimitive.content })
            assertEquals(listOf(HttpMethod.Put, HttpMethod.Put), stored.map { it.method })
            assertEquals(listOf("image/jpeg", "image/png"), stored.map { it.body.contentType?.toString() })
            assertEquals(listOf<String?>(null, null), stored.map { it.headers[HttpHeaders.Authorization] })
            assertEquals(listOf("stored/0", "stored/1"), result.map { it.storageKey })
            assertEquals(listOf(3L, 2L), result.map { it.size })
            assertTrue(result.all { it.bytes == null })
            assertContentEquals(byteArrayOf(1, 2, 3), (stored[0].body as OutgoingContent.ByteArrayContent).bytes())
        }
    }

    @Test fun noPhotosMakeNoRequests() = runTest {
        val api = MockEngine { error("Nothing to reserve") }
        val storage = MockEngine { error("Nothing to upload") }
        assertEquals(emptyList(), uploader(api, storage).upload(BusinessKind.Roaster, emptyList()).getOrThrow())
    }

    @Test fun aPrivateStorageAddressIsRejectedBeforeAnyBytesAreSent() = runTest {
        val api = MockEngine { respond(reservation("https://storage.example.com/one", "http://minio:9000/two"), headers = jsonHeaders) }
        val storage = MockEngine { error("Bytes must not leave for a private address") }
        val failure = assertFailsWith<OwnershipFailure> { uploader(api, storage).upload(BusinessKind.Roaster, photos).getOrThrow() }
        assertEquals(OwnershipProblem.Unavailable, failure.problem)
    }

    @Test fun aReservationForAnotherNumberOfPhotosOrWithoutAKeyFails() = runTest {
        val storage = MockEngine { error("No upload without a valid reservation") }
        val short = MockEngine { respond(reservation("https://storage.example.com/one"), headers = jsonHeaders) }
        assertEquals(OwnershipProblem.Unavailable,
            assertFailsWith<OwnershipFailure> { uploader(short, storage).upload(BusinessKind.Roaster, photos).getOrThrow() }.problem)
        val blank = MockEngine {
            respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://storage.example.com/a","storageKey":""},{"uploadUrl":"https://storage.example.com/b","storageKey":"k"}]}""", headers = jsonHeaders)
        }
        assertEquals(OwnershipProblem.Unavailable,
            assertFailsWith<OwnershipFailure> { uploader(blank, storage).upload(BusinessKind.Roaster, photos).getOrThrow() }.problem)
    }

    @Test fun aRejectedReservationOrStoredPhotoFails() = runTest {
        val rejected = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val storage = MockEngine { error("No upload after a rejected reservation") }
        assertEquals(OwnershipProblem.Unavailable,
            assertFailsWith<OwnershipFailure> { uploader(rejected, storage).upload(BusinessKind.CoffeeShop, photos).getOrThrow() }.problem)

        val api = MockEngine { respond(reservation("https://storage.example.com/one", "https://storage.example.com/two"), headers = jsonHeaders) }
        val refusing = MockEngine { respondError(HttpStatusCode.Forbidden) }
        assertEquals(OwnershipProblem.Unavailable,
            assertFailsWith<OwnershipFailure> { uploader(api, refusing).upload(BusinessKind.CoffeeShop, photos).getOrThrow() }.problem)
    }

    @Test fun signingOutOrSwitchingAccountDuringTheUploadFailsAsSignedOut() = runTest {
        val storage = MockEngine { respond("", HttpStatusCode.OK) }
        val switching = MockEngine {
            account.value.value = "other-owner"
            respond(reservation("https://storage.example.com/one", "https://storage.example.com/two"), headers = jsonHeaders)
        }
        assertEquals(OwnershipProblem.SignedOut,
            assertFailsWith<OwnershipFailure> { uploader(switching, storage).upload(BusinessKind.Roaster, photos).getOrThrow() }.problem)

        account.value.value = null
        val untouched = MockEngine { error("A signed-out owner must not reserve photos") }
        assertEquals(OwnershipProblem.SignedOut,
            assertFailsWith<OwnershipFailure> { uploader(untouched, storage).upload(BusinessKind.Roaster, photos).getOrThrow() }.problem)
        assertNull(account.current())
    }
}
