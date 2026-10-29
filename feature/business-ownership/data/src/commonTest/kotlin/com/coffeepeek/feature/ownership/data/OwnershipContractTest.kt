package com.coffeepeek.feature.ownership.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.ownership.data.repository.createBusinessOwnershipRepository
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

private val headers = headersOf(HttpHeaders.ContentType, "application/json")
private val json = Json { ignoreUnknownKeys = true }
private const val claim = """{"id":"claim","businessType":"Roaster","businessId":"roaster","businessNameSnapshot":"Roaster","businessSlugSnapshot":"roaster-slug","contact":"owner@example.test","evidence":"Official contact","status":"Approved","createdAtUtc":"2026-10-28T12:00:00Z","hasCurrentAccess":false}"""
private fun envelope(data: String) = """{"isSuccess":true,"data":$data}"""
private fun profile(kind: BusinessKind = BusinessKind.Roaster, photos: String = "[]") = """{"id":"business","name":"Original","profileRevision":7,"cityId":"city-id","about":"About","description":"About","location":{"cityId":"city-id","address":"Address","latitude":53.9,"longitude":27.5},"contact":{"siteLink":"https://example.test","instagramLink":"https://instagram.com/example"},"contacts":{"phoneNumber":"123","email":"owner@example.test","siteLink":"https://example.test"},"photos":$photos}"""
private fun photo(id: String, key: String) = """{"id":"$id","storageKey":"$key","fileName":"$id.jpg","fullUrl":"https://media.example/protected/$id?signature=keep","contentType":"image/jpeg","sizeBytes":50}"""
private fun HttpRequestData.jsonBody() = json.parseToJsonElement((body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
private class Account : OwnershipAccount {
    val value = MutableStateFlow<String?>("user")
    override fun current() = value.value
    override fun observe() = value
}
private class Uploader(val account: Account, var changeAccount: Boolean = false) : BusinessPhotoUploader {
    override suspend fun upload(kind: BusinessKind, photos: List<BusinessPhoto>): Result<List<BusinessPhoto>> {
        if (changeAccount) account.value.value = "other-user"
        return Result.success(photos.map { it.copy(bytes = null, storageKey = "new-key") })
    }
}
class OwnershipContractTest {
    @Test fun submitsOnlyServerContractFieldsAndKeepsApprovedSeparateFromAccess() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request -> requests += request
        respond(envelope(claim), headers = headers) }
        val account = Account()
        withRepository(engine, account) { repo ->
            val result = repo.submit(ClaimSubmission(BusinessKind.Roaster, " roast ", " owner ", " proof ")).getOrThrow()
            assertFalse(result.hasCurrentAccess)
            assertEquals(ClaimStatus.Approved, result.status)
            assertEquals("/api/owner/ownership-claims", requests.single().url.encodedPath)
            assertEquals(setOf("businessType", "businessSlug", "contact", "evidence", "evidencePhotoIds"), requests.single().jsonBody().keys)
            assertEquals("roast", requests.single().jsonBody()["businessSlug"]?.jsonPrimitive?.content)
        }
    }
    @Test fun evidenceUsesThePrivateUploadAndOnlyItsPhotoIdEntersTheClaim() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            if (request.url.encodedPath == "/api/Photos/ownership-evidence") {
                assertEquals(HttpMethod.Post, request.method)
                assertTrue(request.body is io.ktor.client.request.forms.MultiPartFormDataContent)
                respond(envelope("""{"photoId":"private-id","storageKey":"ownership-evidence/opaque"}"""), headers = headers)
            } else {
                assertEquals(listOf("private-id"), request.jsonBody()["evidencePhotoIds"]!!.jsonArray.map { it.jsonPrimitive.content })
                respond(envelope(claim), headers = headers)
            }
        }
        val account = Account()
        withRepository(engine, account) { repo ->
            val photo = BusinessPhoto("picked", fileName = "proof.png", contentType = "image/png", size = 2, bytes = byteArrayOf(1, 2))
            val saved = repo.submit(ClaimSubmission(BusinessKind.CoffeeShop, "shop", "contact", "proof", listOf(photo))).getOrThrow()
            assertContentEquals(photo.bytes, saved.evidencePhotos.single().bytes)
            assertEquals(2, requests.size)
        }
    }
    @Test fun invalidEvidenceIsRejectedBeforeUploading() = runTest {
        val engine = MockEngine { error("Invalid evidence must not touch HTTP") }
        val account = Account()
        withRepository(engine, account) { repo ->
            val photo = BusinessPhoto("bad", fileName = "proof.svg", contentType = "image/svg+xml", size = 10, bytes = byteArrayOf(1))
            assertTrue(repo.submit(ClaimSubmission(BusinessKind.Roaster, "roaster", "contact", "proof", listOf(photo))).isFailure)
        }
    }
    @Test fun validatesBeforeSendingAndPreservesPrivatePagingEnvelope() = runTest {
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
            assertEquals("2", request.url.parameters["page"])
            assertEquals(null, request.url.parameters["status"])
            respond(envelope("""{"items":[$claim],"totalCount":41,"page":2,"pageSize":20}"""), headers = headers)
        }
        val account = Account()
        withRepository(engine, account) { repo ->
            assertTrue(repo.submit(ClaimSubmission(BusinessKind.CoffeeShop, "bad/slug", "contact", "proof")).isFailure)
            assertEquals(0, calls)
            val page = repo.claims(2).getOrThrow()
            assertEquals(2, page.page)
            assertTrue(page.hasNext)
            assertEquals(41, page.totalCount)
        }
    }
    @Test fun roasterSaveUsesRevisionAndRetainsCityCoordinatesAndUnchangedPhotos() = runTest {
        var write: HttpRequestData? = null
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Put) { write = request
            respond(envelope("{}"), headers = headers) }
            else respond(envelope(profile()), headers = headers)
        }
        val account = Account()
        withRepository(engine, account) { repo ->
            val original = repo.profile(BusinessKind.Roaster, "business").getOrThrow()
            repo.save(original, ProfileDraft.from(original).copy(about = "Changed")).getOrThrow()
            assertEquals("\"7\"", write!!.headers[HttpHeaders.IfMatch])
            val body = write!!.jsonBody()
            assertEquals("city-id", body["cityId"]?.jsonPrimitive?.content)
            assertEquals(53.9, body["latitude"]?.jsonPrimitive?.double)
            assertEquals("Changed", body["about"]?.jsonPrimitive?.content)
            assertTrue(body["photos"] == null || body["photos"] is JsonNull)
        }
    }
    @Test fun roasterGalleryRetainsPreviousMediaIdProtectedUrlAndStorageKey() = runTest {
        var write: JsonObject? = null
        val existing = """{"id":"old-media-id","storageKey":"old-owner-key","fileName":"old.PNG","fullUrl":"https://media.example/protected/old-media-id?signature=keep"}"""
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Put) { write = request.jsonBody()
            respond(envelope("{}"), headers = headers) }
            else respond(envelope(profile(photos = "[$existing]")), headers = headers)
        }
        val account = Account()
        withRepository(engine, account) { repo ->
            val original = repo.profile(BusinessKind.Roaster, "business").getOrThrow()
            val pending = BusinessPhoto("new", fileName = "new.jpg", bytes = byteArrayOf(1))
            val saved = repo.save(original, ProfileDraft.from(original).copy(photos = original.photos + pending)).getOrThrow()
            assertEquals("old-media-id", saved.photos.first().id)
            assertEquals("image/png", write!!["photos"]!!.jsonArray.first().jsonObject["contentType"]!!.jsonPrimitive.content)
            assertEquals("https://media.example/protected/old-media-id?signature=keep", saved.photos.first().url)
            assertEquals(listOf("old-owner-key", "new-key"), write!!["photos"]!!.jsonArray.map { it.jsonObject["storageKey"]!!.jsonPrimitive.content })
        }
    }
    @Test fun shopGalleryUsesSeparateEndpointsAndFullMediaIdsForOrdering() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = shopGalleryEngine(requests)
        val account = Account()
        withRepository(engine, account) { repo ->
            val original = repo.profile(BusinessKind.CoffeeShop, "business").getOrThrow()
            val pending = BusinessPhoto("new", fileName = "new.jpg", bytes = byteArrayOf(1))
            repo.save(original, ProfileDraft.from(original).copy(photos = listOf(pending, original.photos.first()))).getOrThrow()
            val profileWrite = requests.first { it.method == HttpMethod.Put && !it.url.encodedPath.endsWith("order") }
            assertEquals("\"7\"", profileWrite.headers[HttpHeaders.IfMatch])
            assertTrue(profileWrite.jsonBody()["catalogs"] == null || profileWrite.jsonBody()["catalogs"] is JsonNull)
            assertTrue(profileWrite.jsonBody()["schedules"] == null || profileWrite.jsonBody()["schedules"] is JsonNull)
            assertEquals("removed", requests.first { it.method == HttpMethod.Delete }.jsonBody()["photoIds"]!!.jsonArray.single().jsonPrimitive.content)
            val order = requests.first { it.url.encodedPath.endsWith("/photos/order") }.jsonBody()["photoIds"]!!.jsonArray
            assertEquals(listOf("new-media-id", "kept"), order.map { it.jsonPrimitive.content })
        }
    }
    @Test fun conflictIsNotRetriedAndDoesNotMutateTheDraft() = runTest {
        var writes = 0
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Put) { writes++
            respond("{}", HttpStatusCode.Conflict, headers) }
            else respond(envelope(profile()), headers = headers)
        }
        val account = Account()
        withRepository(engine, account) { repo ->
            val original = repo.profile(BusinessKind.Roaster, "business").getOrThrow()
            val draft = ProfileDraft.from(original).copy(name = "Draft")
            assertEquals(OwnershipProblem.Conflict, (repo.save(original, draft).exceptionOrNull() as OwnershipFailure).problem)
            assertEquals(1, writes)
            assertEquals("Draft", draft.name)
        }
    }
    @Test fun aGalleryFailureAfterProfileWriteRequiresReconciliation() = runTest {
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Delete) respond("{}", HttpStatusCode.ServiceUnavailable, headers)
            else respond(envelope(profile(BusinessKind.CoffeeShop, "[${photo("existing", "old")}]")), headers = headers)
        }
        val account = Account()
        withRepository(engine, account) { repo ->
            val original = repo.profile(BusinessKind.CoffeeShop, "business").getOrThrow()
            assertEquals(OwnershipProblem.PartialSave, (repo.save(original, ProfileDraft.from(original).copy(photos = emptyList())).exceptionOrNull() as OwnershipFailure).problem)
        }
    }
    @Test fun changingAccountDuringUploadPreventsAnyProfileWrite() = runTest {
        var writes = 0
        val engine = MockEngine { request ->
            if (request.method != HttpMethod.Get) writes++
            respond(envelope(profile()), headers = headers)
        }
        val account = Account()
        withRepository(engine, account, changeAccount = true) { repo ->
            val original = repo.profile(BusinessKind.Roaster, "business").getOrThrow()
            val pending = BusinessPhoto("new", fileName = "new.jpg", bytes = byteArrayOf(1))
            assertTrue(repo.save(original, ProfileDraft.from(original).copy(photos = listOf(pending))).isFailure)
            assertEquals(0, writes)
        }
    }
    @Test fun signedOutNeverReadsPrivateDataAndCancellationPropagates() = runTest {
        var calls = 0
        val engine = MockEngine { calls++
        throw CancellationException("cancel fixture") }
        val account = Account()
        withRepository(engine, account) { repo ->
            account.value.value = null
            assertTrue(repo.claims(1).isFailure)
            assertEquals(0, calls)
            account.value.value = "user"
            assertFailsWith<CancellationException> { repo.claims(1) }
        }
    }
}


private suspend fun withRepository(engine: MockEngine, account: Account, changeAccount: Boolean = false,
    block: suspend (BusinessOwnershipRepository) -> Unit) {
    val client = HttpClientFactory(engine).api("https://api.example")
    try { block(createBusinessOwnershipRepository(client, Uploader(account, changeAccount), account)) }
    finally { client.close() }
}

private fun shopGalleryEngine(requests: MutableList<HttpRequestData>): MockEngine {
        val old = "[${photo("kept", "old-key")},${photo("removed", "remove-key")}]"
        val attached = "[${photo("kept", "old-key")},${photo("removed", "remove-key")},${photo("new-media-id", "new-key")}]"
        return MockEngine { request ->
            requests += request
            val payload = if (request.method == HttpMethod.Get && requests.size == 1) profile(BusinessKind.CoffeeShop, old)
                else if (request.url.encodedPath.endsWith("/photos")) profile(BusinessKind.CoffeeShop, attached) else profile(BusinessKind.CoffeeShop, attached)
            respond(envelope(payload), headers = headers)
        }
}
