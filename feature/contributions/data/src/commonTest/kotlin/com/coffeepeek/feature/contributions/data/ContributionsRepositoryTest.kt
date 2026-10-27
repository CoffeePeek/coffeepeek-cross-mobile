package com.coffeepeek.feature.contributions.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.contributions.data.repository.createContributionsRepository
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class ContributionsRepositoryTest {
    private val api = mutableListOf<HttpRequestData>()
    private val puts = mutableListOf<HttpRequestData>()
    private var reply: (HttpRequestData) -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to """{"isSuccess":true,"data":null}""" }
    private var uploadUrl: (String) -> String = { "https://uploads.example.com/$it" }
    private var uploadStatus = HttpStatusCode.OK
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repository(offset: Int = 0): ContributionsRepository {
        val apiClient = HttpClientFactory(MockEngine { request ->
            api += request
            val path = request.url.encodedPath
            if (path.startsWith("/api/Photos/") && request.method == HttpMethod.Post) {
                val count = Json.parseToJsonElement((request.body as TextContent).text).jsonArray.size
                val targets = (1..count).joinToString(",") { """{"photoId":"p$it","uploadUrl":"${uploadUrl("k$it")}","storageKey":"${path.substringAfterLast('/')}/k$it"}""" }
                respond("""{"isSuccess":true,"data":[$targets]}""", HttpStatusCode.OK, json)
            } else {
                val (status, body) = reply(request)
                respond(body, status, json)
            }
        }).api("https://api.example")
        val uploadClient = HttpClientFactory(MockEngine { request -> puts += request; respond("", uploadStatus) }).upload()
        return createContributionsRepository(apiClient, uploadClient) { offset }
    }

    private fun body(request: HttpRequestData) = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
    private fun requests(path: String) = api.filter { it.url.encodedPath == path }
    private fun photo(name: String) = PendingPhoto(name, "image/jpeg", byteArrayOf(1, 2, 3))
    private fun failure(result: Result<*>) = result.exceptionOrNull() as ContributionFailure

    // ---- lists ----

    @Test fun shopListUsesTheModerationEndpointAndMapsPublishedAndWaitingShops() = runBlocking {
        reply = { HttpStatusCode.OK to """{"isSuccess":true,"data":{"items":[
            {"id":"a","name":"Cafe","address":"Street 1","moderationStatus":"Approved","publishedShop":{"slug":"cafe","canonicalPath":"/coffee-shops/cafe"}},
            {"id":"b","name":"Waiting","moderationStatus":"Approved","publishedShop":{"slug":" "}},
            {"id":"c","name":"No","moderationStatus":"Rejected","rejectedReason":"Нет адреса"}],
            "totalItems":45,"totalPages":3,"currentPage":2}}""" }
        val page = repository().shops(ModerationStatus.Approved, page = 2, pageSize = 20).getOrThrow()
        val request = api.single()
        assertEquals("/api/ModerationShops/mine", request.url.encodedPath)
        assertEquals(mapOf("page" to "2", "pageSize" to "20", "status" to "Approved"), request.url.parameters.names().associateWith { request.url.parameters[it] })
        assertEquals(listOf("cafe", null, null), page.items.map { it.publishedShopSlug })
        assertEquals(listOf(ModerationStatus.Approved, ModerationStatus.Approved, ModerationStatus.Rejected), page.items.map { it.status })
        assertEquals("Нет адреса", page.items[2].rejectedReason); assertNull(page.items[1].address)
        assertEquals(45, page.totalCount); assertTrue(page.hasMore)
    }

    @Test fun pageAndSizeAreKeptInsideTheServerLimits() = runBlocking {
        reply = { HttpStatusCode.OK to """{"isSuccess":true,"data":{"items":[],"totalItems":0,"totalPages":0,"currentPage":1}}""" }
        repository().roasters(ModerationStatus.Pending, page = 0, pageSize = 500).getOrThrow()
        val request = api.single()
        assertEquals("/api/ModerationRoasters/mine", request.url.encodedPath)
        assertEquals("1", request.url.parameters["page"]); assertEquals("100", request.url.parameters["pageSize"])
    }

    @Test fun changeListSendsTheFiltersAndMapsPayloads() = runBlocking {
        reply = { HttpStatusCode.OK to """{"isSuccess":true,"data":{"items":[
            {"id":"r","shop":{"slug":"cafe"},"section":"Menu","status":"Pending","createdAtUtc":"2026-10-21T07:00:00Z",
             "payload":{"menu":{"items":[{"slug":"latte","availability":"Present","price":6.5},{"slug":"tea","availability":"Strange"}],
                         "retainedPhotoIds":["m1"],"newPhotos":[{"fileName":"a.jpg","contentType":"image/jpeg","storageKey":"k","size":3}]}}}],
            "totalItems":1,"totalPages":1,"currentPage":1}}""" }
        val page = repository().changes(ModerationStatus.Pending, 1, 20, shopSlug = "cafe", section = ChangeSection.Menu).getOrThrow()
        val params = api.single().url.parameters
        assertEquals("/api/ShopChangeRequests/mine", api.single().url.encodedPath)
        assertEquals(listOf("Pending", "cafe", "Menu"), listOf(params["status"], params["shop"], params["section"]))
        val request = page.items.single()
        assertEquals("cafe", request.shopSlug); assertEquals(ChangeSection.Menu, request.section)
        val menu = request.payload.menu!!
        assertEquals(listOf(MenuEntry("latte", Availability.Present, 6.5), MenuEntry("tea", Availability.Unknown)), menu.items)
        assertEquals(listOf("m1"), menu.retainedPhotoIds); assertEquals("k", menu.uploaded.single().storageKey)
        assertNull(request.payload.description)
    }

    @Test fun changeListWithoutFiltersSendsOnlyPaging() = runBlocking {
        reply = { HttpStatusCode.OK to """{"isSuccess":true,"data":{"items":[]}}""" }
        repository().changes(null, 1, 20).getOrThrow()
        assertEquals(setOf("page", "pageSize"), api.single().url.parameters.names())
    }

    // ---- failures ----

    @Test fun statusCodesBecomeTypedFailuresAndKeepTheServerMessage() = runBlocking {
        val cases = mapOf(
            HttpStatusCode.Unauthorized to FailureKind.SignInRequired, HttpStatusCode.Forbidden to FailureKind.Forbidden,
            HttpStatusCode.NotFound to FailureKind.NotFound, HttpStatusCode.BadRequest to FailureKind.Other, HttpStatusCode.InternalServerError to FailureKind.Other,
        )
        cases.forEach { (status, kind) ->
            reply = { status to """{"isSuccess":false,"message":"Сообщение сервера"}""" }
            val failure = failure(repository().shops(ModerationStatus.Pending, 1, 20))
            assertEquals(kind, failure.kind, "$status"); assertEquals("Сообщение сервера", failure.serverMessage)
        }
        reply = { HttpStatusCode.BadGateway to "<html>gateway</html>" }
        assertNull(failure(repository().change("x")).serverMessage)
        reply = { HttpStatusCode.OK to """{"isSuccess":false,"message":"  "}""" }
        assertNull(failure(repository().change("x")).serverMessage)
    }

    @Test fun anUnknownModerationStatusFailsTheListInsteadOfGuessing() = runBlocking {
        reply = { HttpStatusCode.OK to """{"isSuccess":true,"data":{"items":[{"id":"a","moderationStatus":"Archived"}]}}""" }
        assertTrue(repository().shops(ModerationStatus.Pending, 1, 20).isFailure)
    }

    // ---- shop submission ----

    @Test fun shopUploadsItsPhotosFirstThenSendsOneRequestWithTheWeekInUtc() = runBlocking {
        val week = com.coffeepeek.feature.contributions.domain.rules.unifiedWeek("09:00", "18:00", closedDays = setOf(0))
        repository(offset = 180).submitShop(
            NewShop(
                name = "  Кофейня ", address = " Улица, 1 ", cityId = "minsk", latitude = 53.9, longitude = 27.5, description = "  ",
                priceRange = PriceRange.Expensive, contacts = Contacts(instagram = "@shop", phone = " "),
                equipmentIds = listOf("grinder"), beanIds = listOf("arabica"),
                photos = listOf(photo("a.jpg"), photo("b.jpg")), menuPhotos = listOf(photo("m.jpg")), week = week,
            ),
        ).getOrThrow()
        // Both uploads run before the shop request, in either order.
        assertEquals(setOf("/api/Photos/shop", "/api/Photos/menu"), api.take(2).map { it.url.encodedPath }.toSet())
        assertEquals("/api/ModerationShops", api.last().url.encodedPath)
        assertEquals(3, puts.size)
        assertEquals("https://uploads.example.com/k1", puts.first { it.method == HttpMethod.Put }.url.toString())
        assertEquals(1, requests("/api/ModerationShops").size)
        val sent = body(requests("/api/ModerationShops").single())
        assertEquals("Кофейня", sent.getValue("name").jsonPrimitive.content); assertEquals("Улица, 1", sent.getValue("address").jsonPrimitive.content)
        assertEquals("minsk", sent.getValue("city").jsonPrimitive.content); assertEquals("Expensive", sent.getValue("priceRange").jsonPrimitive.content)
        assertTrue(sent["description"].let { it == null || it is JsonNull })
        val contact = sent.getValue("shopContact").jsonObject
        assertEquals("@shop", contact.getValue("instagramLink").jsonPrimitive.content)
        assertTrue(contact["phoneNumber"].let { it == null || it is JsonNull })
        assertEquals(listOf("shop/k1", "shop/k2"), sent.getValue("shopPhotos").jsonArray.map { it.jsonObject.getValue("storageKey").jsonPrimitive.content })
        assertEquals(listOf("menu/k1"), sent.getValue("menuPhotos").jsonArray.map { it.jsonObject.getValue("storageKey").jsonPrimitive.content })
        assertEquals(listOf("grinder"), sent.getValue("equipments").jsonArray.map { it.jsonPrimitive.content })
        assertTrue(sent["roasters"].let { it == null || it is JsonNull })
        // 09:00-18:00 local with +03:00 is 06:00-15:00 UTC on the same days; Sunday is closed and has no intervals.
        val schedules = sent.getValue("schedules").jsonArray.map { it.jsonObject }
        assertEquals(listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"), schedules.map { it.getValue("dayOfWeek").jsonPrimitive.content })
        assertTrue(schedules.first().getValue("isClosed").jsonPrimitive.boolean)
        assertTrue(schedules.first()["intervals"].let { it == null || it is JsonNull })
        val monday = schedules[1].getValue("intervals").jsonArray.single().jsonObject
        assertEquals("06:00", monday.getValue("openTime").jsonPrimitive.content); assertEquals("15:00", monday.getValue("closeTime").jsonPrimitive.content)
    }

    @Test fun shopWithoutContactsOrPhotosSendsNeitherPart() = runBlocking {
        repository().submitShop(NewShop("Кофейня", "Улица, 1", "minsk")).getOrThrow()
        assertTrue(api.none { it.url.encodedPath.startsWith("/api/Photos/") })
        val sent = body(api.single())
        listOf("shopContact", "schedules", "shopPhotos", "menuPhotos", "equipments", "beans", "roasters", "brewMethods", "priceRange").forEach { key ->
            assertTrue(sent[key].let { it == null || it is JsonNull }, key)
        }
    }

    @Test fun anInvalidShopIsRefusedBeforeAnyRequest() = runBlocking {
        val result = repository().submitShop(NewShop("Кофейня", "Улица, 1", cityId = " ", photos = listOf(photo("a.jpg"))))
        assertEquals(FailureKind.Invalid, failure(result).kind)
        assertTrue(api.isEmpty() && puts.isEmpty())
    }

    @Test fun aFailedUploadSendsNothingAndAnUnsafeUploadAddressIsRejected() = runBlocking {
        uploadStatus = HttpStatusCode.Forbidden
        assertEquals(FailureKind.Upload, failure(repository().submitShop(NewShop("Кофейня", "Улица", "minsk", photos = listOf(photo("a.jpg"))))).kind)
        assertTrue(requests("/api/ModerationShops").isEmpty())

        uploadStatus = HttpStatusCode.OK; puts.clear(); api.clear()
        uploadUrl = { "http://127.0.0.1/$it" }
        assertEquals(FailureKind.Upload, failure(repository().submitShop(NewShop("Кофейня", "Улица", "minsk", photos = listOf(photo("a.jpg"))))).kind)
        assertTrue(puts.isEmpty() && requests("/api/ModerationShops").isEmpty())
    }

    @Test fun aRejectedShopKeepsTheServerMessage() = runBlocking {
        reply = { HttpStatusCode.BadRequest to """{"isSuccess":false,"message":"Кофейня уже существует"}""" }
        val failure = failure(repository().submitShop(NewShop("Кофейня", "Улица", "minsk")))
        assertEquals("Кофейня уже существует", failure.serverMessage)
    }

    // ---- roaster submission ----

    @Test fun roasterSendsTrimmedValuesAndReturnsTheServerResult() = runBlocking {
        reply = { HttpStatusCode.OK to """{"isSuccess":true,"message":"Отправлено","data":{"roasterId":"r1","status":"Pending","isAddressValidated":true}}""" }
        val result = repository().submitRoaster(
            NewRoaster(" Roast ", about = "  ", cityId = "minsk", address = " Улица, 1 ", instagramLink = "https://instagram.com/roast", siteLink = " ", photos = listOf(photo("p.jpg"))),
        ).getOrThrow()
        assertEquals(RoasterSubmitted("r1", "Pending", true, "Отправлено"), result)
        assertEquals("/api/Photos/roaster", api.first().url.encodedPath)
        val sent = body(requests("/api/ModerationRoasters").single())
        assertEquals("Roast", sent.getValue("name").jsonPrimitive.content); assertEquals("Улица, 1", sent.getValue("address").jsonPrimitive.content)
        assertTrue(sent["about"].let { it == null || it is JsonNull }); assertTrue(sent["siteLink"].let { it == null || it is JsonNull })
        assertEquals(listOf("roaster/k1"), sent.getValue("photos").jsonArray.map { it.jsonObject.getValue("storageKey").jsonPrimitive.content })
    }

    @Test fun roasterWithACityButNoAddressIsRefusedBeforeAnyRequest() = runBlocking {
        assertEquals(FailureKind.Invalid, failure(repository().submitRoaster(NewRoaster("Roast", cityId = "minsk"))).kind)
        assertTrue(api.isEmpty())
    }

    // ---- change requests ----

    private val created = """{"isSuccess":true,"data":{"id":"request","shop":{"slug":"cafe"},"section":"Description","status":"Pending","createdAtUtc":"2026-10-21T07:00:00Z","payload":{}}}"""

    @Test fun everyDraftSendsItsOwnSectionAndOnlyItsOwnPayloadKey() = runBlocking {
        reply = { HttpStatusCode.OK to created }
        val repository = repository()
        val cases = listOf(
            ChangeDraft.Description("cafe", " Текст ") to ("Description" to "description"),
            ChangeDraft.ContactsChange("cafe", Contacts(phone = "+375291112233", email = " ")) to ("Contacts" to "contacts"),
            ChangeDraft.Selection("cafe", ChangeSection.Tags, listOf("wifi")) to ("Tags" to "tags"),
            ChangeDraft.Selection("cafe", ChangeSection.Roasters, listOf("roast")) to ("Roasters" to "roasters"),
            ChangeDraft.Selection("cafe", ChangeSection.Equipment, listOf("grinder")) to ("Equipment" to "equipments"),
            ChangeDraft.Selection("cafe", ChangeSection.BrewMethods, listOf("v60")) to ("BrewMethods" to "brewMethods"),
            ChangeDraft.Photos("cafe", listOf("p1"), emptyList()) to ("Photos" to "photos"),
            ChangeDraft.Menu("cafe", listOf(MenuEntry("latte", Availability.Absent)), emptyList(), emptyList()) to ("Menu" to "menu"),
        )
        cases.forEachIndexed { index, (draft, expected) ->
            repository.submitChange(draft).getOrThrow()
            val request = requests("/api/ShopChangeRequests")[index]
            assertEquals(HttpMethod.Post, request.method)
            val sent = body(request)
            assertEquals("cafe", sent.getValue("shop").jsonPrimitive.content)
            assertEquals(expected.first, sent.getValue("section").jsonPrimitive.content)
            assertEquals(setOf(expected.second), sent.getValue("payload").jsonObject.filterValues { it !is JsonNull }.keys, expected.first)
        }
        val contacts = body(requests("/api/ShopChangeRequests")[1]).getValue("payload").jsonObject.getValue("contacts").jsonObject
        assertEquals("+375291112233", contacts.getValue("phoneNumber").jsonPrimitive.content)
        assertTrue(contacts["email"].let { it == null || it is JsonNull })
        assertEquals("Текст", body(requests("/api/ShopChangeRequests")[0]).getValue("payload").jsonObject.getValue("description").jsonPrimitive.content)
    }

    @Test fun aBlankDescriptionIsSentAsNoTextAsTheLegacyFormDid() = runBlocking {
        reply = { HttpStatusCode.OK to created }
        repository().submitChange(ChangeDraft.Description("cafe", "   ")).getOrThrow()
        val payload = body(api.single()).getValue("payload").jsonObject
        assertTrue(payload["description"].let { it == null || it is JsonNull })
    }

    @Test fun anEditUsesPutOnTheRequestWithoutTheShop() = runBlocking {
        reply = { HttpStatusCode.OK to created }
        repository().editChange("request", ChangeDraft.Description("cafe", "Новый")).getOrThrow()
        val request = api.single()
        assertEquals(HttpMethod.Put, request.method); assertEquals("/api/ShopChangeRequests/request", request.url.encodedPath)
        assertFalse("shop" in body(request)); assertEquals("Description", body(request).getValue("section").jsonPrimitive.content)
    }

    @Test fun photoChangeSendsEarlierUploadsBeforeTheNewOnesAndMenuUsesItsOwnEndpoint() = runBlocking {
        reply = { HttpStatusCode.OK to created }
        val earlier = UploadedPhoto("old.jpg", "image/jpeg", "old/key", 3)
        val repository = repository()
        repository.submitChange(ChangeDraft.Photos("cafe", listOf("p1", "p2"), listOf(photo("a.jpg"), photo("b.jpg")), listOf(earlier))).getOrThrow()
        val gallery = body(requests("/api/ShopChangeRequests").single()).getValue("payload").jsonObject.getValue("photos").jsonObject
        assertEquals(listOf("p1", "p2"), gallery.getValue("retainedPhotoIds").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("old/key", "shop/k1", "shop/k2"), gallery.getValue("newPhotos").jsonArray.map { it.jsonObject.getValue("storageKey").jsonPrimitive.content })

        api.clear()
        repository.submitChange(ChangeDraft.Menu("cafe", listOf(MenuEntry("latte", Availability.Present, 6.5)), listOf("m1"), listOf(photo("m.jpg")))).getOrThrow()
        assertEquals("/api/Photos/menu", api.first().url.encodedPath)
        val menu = body(requests("/api/ShopChangeRequests").single()).getValue("payload").jsonObject.getValue("menu").jsonObject
        assertEquals("Present", menu.getValue("items").jsonArray.single().jsonObject.getValue("availability").jsonPrimitive.content)
        assertEquals(listOf("menu/k1"), menu.getValue("newPhotos").jsonArray.map { it.jsonObject.getValue("storageKey").jsonPrimitive.content })
    }

    @Test fun aDraftOverTheLimitIsRefusedBeforeAnyRequestAndAFailedUploadSendsNothing() = runBlocking {
        val repository = repository()
        val tooLong = ChangeDraft.Description("cafe", "x".repeat(1001))
        assertEquals(FailureKind.Invalid, failure(repository.submitChange(tooLong)).kind)
        assertEquals(FailureKind.Invalid, failure(repository.editChange("request", ChangeDraft.ContactsChange("cafe", Contacts(email = "nope")))).kind)
        assertTrue(api.isEmpty())

        uploadStatus = HttpStatusCode.InternalServerError
        assertEquals(FailureKind.Upload, failure(repository.submitChange(ChangeDraft.Photos("cafe", emptyList(), listOf(photo("a.jpg"))))).kind)
        assertTrue(requests("/api/ShopChangeRequests").isEmpty())
    }

    @Test fun anUnknownShopOrRequestIsNotFound() = runBlocking {
        reply = { HttpStatusCode.NotFound to """{"isSuccess":false,"message":"Кофейня не найдена"}""" }
        val failure = failure(repository().submitChange(ChangeDraft.Description("unknown", "x")))
        assertEquals(FailureKind.NotFound, failure.kind); assertEquals("Кофейня не найдена", failure.serverMessage)
        assertEquals(FailureKind.NotFound, failure(repository().change("missing")).kind)
    }
}
