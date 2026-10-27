package com.coffeepeek.data.repository

import com.coffeepeek.api.service.ShopApiService
import com.coffeepeek.api.service.ShopChangeRequestApiService
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.RoasterRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

/** Parity of what the legacy submissions send: the payload of each change section, the photo uploads and the shop request. */
class ContributionSubmissionContractTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val requests = mutableListOf<HttpRequestData>()
    private val photos = RecordingPhotos()

    private val changeResponse = """{"isSuccess":true,"data":{"id":"request","section":"Description","payload":{},"status":"Pending","createdAtUtc":"2026-10-21T00:00:00Z"}}"""

    private fun client(response: String = changeResponse, status: HttpStatusCode = HttpStatusCode.OK) =
        HttpClient(MockEngine { request -> requests += request; respond(response, status, headers) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun changes() = ShopChangeRequestRepositoryImpl(ShopChangeRequestApiService(client()), photos)
    private fun body(index: Int = 0) = Json.parseToJsonElement((requests[index].body as TextContent).text).jsonObject
    private fun payload(index: Int = 0) = body(index).getValue("payload").jsonObject
    private fun pending(name: String) = PendingPhotoUpload(name, "image/jpeg", byteArrayOf(1, 2))
    private fun draft(section: ShopChangeSection, block: ShopChangeDraft.() -> ShopChangeDraft = { this }) =
        ShopChangeDraft(shopId = "shop", section = section).block()

    @Test fun everySectionSendsOnlyItsOwnPayloadKeyUnderTheSectionName() = runBlocking {
        val repository = changes()
        val cases = listOf(
            draft(ShopChangeSection.Description) { copy(description = "Текст") } to ("Description" to "description"),
            draft(ShopChangeSection.Contacts) { copy(contacts = ShopChangeContacts(phoneNumber = "+375291112233")) } to ("Contacts" to "contacts"),
            draft(ShopChangeSection.Tags) { copy(tagIds = listOf("wifi")) } to ("Tags" to "tags"),
            draft(ShopChangeSection.Roasters) { copy(roasterIds = listOf("roast")) } to ("Roasters" to "roasters"),
            draft(ShopChangeSection.Equipment) { copy(equipmentIds = listOf("grinder")) } to ("Equipment" to "equipments"),
            draft(ShopChangeSection.BrewMethods) { copy(brewMethodIds = listOf("v60")) } to ("BrewMethods" to "brewMethods"),
        )
        cases.forEachIndexed { index, (input, expected) ->
            repository.create(input).getOrThrow()
            assertEquals(HttpMethod.Post, requests[index].method)
            assertEquals("/api/ShopChangeRequests", requests[index].url.encodedPath)
            assertEquals("shop", body(index).getValue("shop").jsonPrimitive.content)
            assertEquals(expected.first, body(index).getValue("section").jsonPrimitive.content)
            val present = payload(index).filterValues { it !is JsonNull }.keys
            assertEquals(setOf(expected.second), present, "section ${expected.first}")
        }
        assertEquals(0, photos.shopCalls + photos.menuCalls)
    }

    @Test fun anEmptyDescriptionClearsItAndAnEditUsesPutOnTheRequest() = runBlocking {
        val repository = changes()
        repository.update("request-1", draft(ShopChangeSection.Description) { copy(description = "") }).getOrThrow()
        assertEquals(HttpMethod.Put, requests.single().method)
        assertEquals("/api/ShopChangeRequests/request-1", requests.single().url.encodedPath)
        assertFalse("shop" in body())
        assertEquals("", payload().getValue("description").jsonPrimitive.content)
    }

    @Test fun photoChangeKeepsRetainedIdsAndSendsEarlierUploadsBeforeNewOnes() = runBlocking {
        val earlier = UploadedPhotoMeta("old.jpg", "image/jpeg", "shop/old", 3)
        changes().create(draft(ShopChangeSection.Photos) {
            copy(retainedPhotoIds = listOf("p1", "p2"), alreadyUploadedPhotos = listOf(earlier), newPhotos = listOf(pending("a.jpg"), pending("b.jpg")))
        }).getOrThrow()
        val gallery = payload().getValue("photos").jsonObject
        assertEquals(listOf("p1", "p2"), gallery.getValue("retainedPhotoIds").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("shop/old", "shop/a.jpg", "shop/b.jpg"), gallery.getValue("newPhotos").jsonArray.map { it.jsonObject.getValue("storageKey").jsonPrimitive.content })
        assertEquals(1, photos.shopCalls)
    }

    @Test fun menuChangeUploadsAtMostFourNewPhotosAndKeepsDrinkRows() = runBlocking {
        changes().create(draft(ShopChangeSection.Menu) {
            copy(
                menuItems = listOf(ShopChangeMenuItem("latte", MenuItemAvailability.Present, price = 6.5), ShopChangeMenuItem("tea", MenuItemAvailability.Absent)),
                retainedMenuPhotoIds = listOf("m1"),
                newMenuPhotos = List(6) { pending("m$it.jpg") },
            )
        }).getOrThrow()
        val menu = payload().getValue("menu").jsonObject
        assertEquals(4, menu.getValue("newPhotos").jsonArray.size)
        assertEquals(listOf("m1"), menu.getValue("retainedPhotoIds").jsonArray.map { it.jsonPrimitive.content })
        val items = menu.getValue("items").jsonArray.map { it.jsonObject }
        assertEquals(listOf("latte", "tea"), items.map { it.getValue("slug").jsonPrimitive.content })
        assertEquals(listOf("Present", "Absent"), items.map { it.getValue("availability").jsonPrimitive.content })
        assertEquals(6.5, items[0].getValue("price").jsonPrimitive.double)
        assertEquals(4, photos.lastMenuCount)
    }

    @Test fun aFailedUploadFailsTheSubmissionBeforeAnyRequestIsSent() = runBlocking {
        photos.fail = true
        val result = changes().create(draft(ShopChangeSection.Photos) { copy(newPhotos = listOf(pending("a.jpg"))) })
        assertTrue(result.isFailure)
        assertTrue(requests.isEmpty())
    }

    @Test fun shopSubmissionUploadsPhotosThenSendsOneRequestWithoutEmptyOptionalParts() = runBlocking {
        val repository = ShopRepositoryImpl(ShopApiService(client(response = """{"isSuccess":true,"data":null}""")), photos, NoFavorites, FileUrlResolver("https://api.example"), NoRoasters)
        repository.createShop(
            CreateShopInput(
                name = "Кофейня", address = "Улица, 1", cityId = "minsk", priceRange = 3,
                photos = listOf(pending("a.jpg"), pending("b.jpg")), menuPhotos = List(5) { pending("m$it.jpg") },
                schedules = (0..6).map { ShopSchedule(it, isClosed = it == 0, intervals = if (it == 0) emptyList() else listOf(ScheduleInterval("09:00", "18:00"))) },
            ),
        ).getOrThrow()
        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals("/api/ModerationShops", requests.single().url.encodedPath)
        val sent = body()
        assertEquals("minsk", sent.getValue("city").jsonPrimitive.content)
        assertEquals("Expensive", sent.getValue("priceRange").jsonPrimitive.content)
        assertEquals(listOf("shop/a.jpg", "shop/b.jpg"), sent.getValue("shopPhotos").jsonArray.map { it.jsonObject.getValue("storageKey").jsonPrimitive.content })
        assertEquals(4, sent.getValue("menuPhotos").jsonArray.size)
        assertTrue(sent["shopContact"] == null || sent["shopContact"] is JsonNull)
        assertTrue(sent["description"] == null || sent["description"] is JsonNull)
        assertEquals(7, sent.getValue("schedules").jsonArray.size)
    }

    @Test fun shopSubmissionSendsTheContactObjectWhenAnyContactIsFilled() = runBlocking {
        val repository = ShopRepositoryImpl(ShopApiService(client(response = """{"isSuccess":true,"data":null}""")), photos, NoFavorites, FileUrlResolver("https://api.example"), NoRoasters)
        repository.createShop(CreateShopInput(name = "Кофейня", address = "Улица, 1", cityId = "minsk", instagram = "https://instagram.com/shop", phone = " ")).getOrThrow()
        val contact = body().getValue("shopContact").jsonObject
        assertEquals("https://instagram.com/shop", contact.getValue("instagramLink").jsonPrimitive.content)
        assertTrue(contact["phoneNumber"] == null || contact["phoneNumber"] is JsonNull)
    }

    @Test fun aRejectedShopSubmissionKeepsTheServerMessage() = runBlocking {
        val failing = client(response = """{"isSuccess":false,"message":"Кофейня уже существует"}""", status = HttpStatusCode.BadRequest)
        val result = ShopRepositoryImpl(ShopApiService(failing), photos, NoFavorites, FileUrlResolver("https://api.example"), NoRoasters)
            .createShop(CreateShopInput(name = "Кофейня", address = "Улица, 1", cityId = "minsk"))
        assertEquals("Кофейня уже существует", result.exceptionOrNull()?.message)
    }
}

private class RecordingPhotos : PhotoRepository {
    var fail = false
    var shopCalls = 0
    var menuCalls = 0
    var lastMenuCount = 0
    private fun uploaded(prefix: String, photos: List<PendingPhotoUpload>) =
        if (fail) Result.failure(IllegalStateException("Upload failed")) else Result.success(photos.map { UploadedPhotoMeta(it.fileName, it.contentType, "$prefix/${it.fileName}", it.bytes.size.toLong()) })
    override suspend fun uploadShopPhotos(photos: List<PendingPhotoUpload>) = uploaded("shop", photos).also { shopCalls++ }
    override suspend fun uploadMenuPhotos(photos: List<PendingPhotoUpload>) = uploaded("menu", photos).also { menuCalls++; lastMenuCount = photos.size }
    override suspend fun uploadAvatar(photo: PendingPhotoUpload): Result<UploadedPhotoMeta> = error("Unused")
    override suspend fun uploadReviewPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> = error("Unused")
    override suspend fun uploadCheckInPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> = error("Unused")
    override suspend fun uploadRoasterPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> = error("Unused")
}

private object NoFavorites : FavoriteRepository {
    override suspend fun getFavoriteIds(): Set<String> = error("Unused")
    override suspend fun isFavorite(shopId: String): Boolean = error("Unused")
    override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = error("Unused")
    override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("Unused")
    override suspend fun removeFavorite(shopId: String): Result<Unit> = error("Unused")
    override suspend fun clearAll(): Unit = error("Unused")
}

private object NoRoasters : RoasterRepository {
    override suspend fun getRoasters(): Result<List<RoasterSummary>> = error("Unused")
    override suspend fun getRoaster(id: String): Result<RoasterDetails> = error("Unused")
    override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("Unused")
    override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("Unused")
}
