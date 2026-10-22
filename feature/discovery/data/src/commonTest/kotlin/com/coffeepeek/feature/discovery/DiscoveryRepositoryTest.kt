package com.coffeepeek.feature.discovery

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class DiscoveryRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var reply: (HttpRequestData) -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to """{"isSuccess":true,"data":null}""" }
    private var favorites: suspend () -> Set<String> = { emptySet() }
    private var roasters: suspend () -> Result<List<CatalogEntry>> = { Result.success(emptyList()) }
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repository(): DiscoveryRepository {
        val client = HttpClientFactory(MockEngine { request ->
            requests += request
            val (status, body) = reply(request)
            respond(body, status, json)
        }).api("https://api.example")
        return createDiscoveryRepository(client, { favorites() }, { roasters() })
    }

    private fun ok(data: String) = HttpStatusCode.OK to """{"isSuccess":true,"data":$data}"""
    private fun body(request: HttpRequestData) = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
    private fun failure(result: Result<*>) = result.exceptionOrNull() as DiscoveryFailure
    private fun params(request: HttpRequestData, name: String) = request.url.parameters.getAll(name).orEmpty()
    private val origin = Coordinates(53.9, 27.56)

    // ---- the plain search ----

    @Test fun thePlainSearchSendsEveryFilterItUnderstandsAndNeverTheSwitches() = runBlocking {
        reply = { ok("""{"coffeeShops":[],"currentPage":2,"totalItems":45,"totalPages":3}""") }
        val filters = DiscoveryFilters(
            cityId = "minsk", kind = ShopKind.Specialty, openOnly = true, newOnly = true, visitedOnly = true, favoritesOnly = true, priceLevel = PriceLevel.Moderate, minRating = 4.5,
            roasterIds = setOf("r1"), beanIds = setOf("b1"), equipmentIds = setOf("e1"), brewMethodIds = setOf("m1"), tagIds = setOf("t1", "t2"),
        )
        val page = repository().search(ShopQuery("  cof  ", filters, page = 2, pageSize = 20)).getOrThrow()
        val request = requests.single()
        assertEquals("/api/CoffeeShops", request.url.encodedPath); assertEquals(HttpMethod.Get, request.method)
        assertEquals("cof", request.url.parameters["q"]); assertEquals("minsk", request.url.parameters["city"]); assertEquals("Specialty", request.url.parameters["type"])
        assertEquals("Moderate", request.url.parameters["priceRange"]); assertEquals("4.5", request.url.parameters["minRating"])
        assertEquals("2", request.url.parameters["page"]); assertEquals("20", request.url.parameters["pageSize"])
        assertEquals(listOf("t1", "t2"), params(request, "tags")); assertEquals(listOf("r1"), params(request, "roasters")); assertEquals(listOf("b1"), params(request, "beans"))
        assertEquals(listOf("e1"), params(request, "equipments")); assertEquals(listOf("m1"), params(request, "brewMethods"))
        for (name in listOf("isOpen", "isNew", "visitedOnly", "favoritesOnly", "open", "new")) assertNull(request.url.parameters[name], name)
        assertEquals(45, page.totalCount); assertEquals(3, page.totalPages); assertEquals(2, page.currentPage); assertTrue(page.hasMore)
    }

    @Test fun aBlankTextAndAnEmptyFilterSendNothingExtra() = runBlocking {
        reply = { ok("""{"coffeeShops":[]}""") }
        repository().search(ShopQuery("   ")).getOrThrow()
        assertEquals(setOf("page", "pageSize"), requests.single().url.parameters.names())
    }

    @Test fun aRowIsMappedWithTheAccountsFavoritesAndTheBackendTags() = runBlocking {
        favorites = { setOf("cafe") }
        reply = { ok("""{"coffeeShops":[
            {"address":{"slug":"cafe"},"name":"Cafe","rating":4.5,"checkInCount":"12","isOpen":true,"isNew":true,"isVisited":true,"isFavorite":false,
             "priceRange":"$$","type":"Specialty","location":{"address":"Street 1","latitude":53.9,"longitude":27.56},
             "photos":[{"fullUrl":"https://img/full","urls":{"card":"https://img/card"}}],
             "tags":["Wi-Fi",{"name":"Pets"},{"title":" Quiet "}],"shopTags":["Wi-Fi","Garden"],
             "roasters":[{"photoUrl":"https://logo/1"},{"photoUrl":"https://logo/1"},{"photoUrl":" "}],"brewMethods":[{"name":"V60"}],"beans":[{"name":"Arabica"}]},
            {"address":{"slug":"bare"},"name":"Bare","rating":0,"isFavorite":true,"coffeeFocus":"3","photos":[{"fullUrl":"https://img/only"}],
             "brewMethods":[{"name":"AeroPress"},{"name":" "}],"beans":[{"name":"Robusta"},{"name":"Blend"},{"name":"Extra"}]}],"totalItems":2,"totalPages":1}""") }
        val (full, bare) = repository().search(ShopQuery()).getOrThrow().items
        assertEquals("cafe", full.id); assertEquals(4.5, full.rating); assertEquals(12, full.checkInCount); assertEquals("Street 1", full.address)
        assertEquals(PriceLevel.Moderate, full.priceLevel); assertEquals("https://img/card", full.photoUrl); assertEquals(ShopKind.Specialty, full.kind)
        assertTrue(full.isOpen && full.isNew && full.isVisited && full.isFavorite)
        assertEquals(listOf("Wi-Fi", "Pets", "Quiet"), full.tags); assertEquals(listOf("V60"), full.brewMethods); assertEquals(listOf("https://logo/1"), full.roasterLogos)
        assertEquals(Coordinates(53.9, 27.56), full.coordinates)
        assertEquals("bare", bare.id); assertNull(bare.rating); assertFalse(bare.isFavorite); assertEquals("https://img/only", bare.photoUrl); assertEquals(ShopKind.Cafe, bare.kind)
        assertEquals(listOf("AeroPress", "Robusta", "Blend"), bare.tags); assertNull(bare.coordinates); assertNull(bare.priceLevel)
    }

    @Test fun aFailingFavoritesSourceCountsAsNoFavorites() = runBlocking {
        favorites = { error("offline") }
        reply = { ok("""{"coffeeShops":[{"address":{"slug":"a"},"name":"A","isFavorite":true}]}""") }
        assertFalse(repository().search(ShopQuery()).getOrThrow().items.single().isFavorite)
    }

    // ---- the nearby search ----

    @Test fun theNearbySearchPostsTheOriginAndTheSwitchesAndLeavesTheTextOutWhenThereIsNone() = runBlocking {
        reply = { ok("""{"items":[],"totalItems":0,"totalPages":0,"currentPage":1}""") }
        val filters = DiscoveryFilters(kind = ShopKind.Cafe, openOnly = true, visitedOnly = true, priceLevel = PriceLevel.Luxury, minRating = 3.0, tagIds = setOf("wifi"), roasterIds = setOf("r"))
        repository().search(ShopQuery(filters = filters, origin = origin, page = 3, pageSize = 10)).getOrThrow()
        val request = requests.single()
        assertEquals("/api/v1/coffee-shops/search", request.url.encodedPath); assertEquals(HttpMethod.Post, request.method)
        val sent = body(request)
        assertEquals(3, sent["page"]!!.jsonPrimitive.int); assertEquals(10, sent["pageSize"]!!.jsonPrimitive.int); assertEquals("distance_asc", sent["sort"]!!.jsonPrimitive.content)
        assertFalse("q" in sent)
        val sentFilters = sent["filters"]!!.jsonObject
        assertEquals(53.9, sentFilters["origin"]!!.jsonObject["latitude"]!!.jsonPrimitive.double); assertEquals(3, sentFilters["radiusKm"]!!.jsonPrimitive.int)
        assertTrue(sentFilters["isOpen"]!!.jsonPrimitive.boolean); assertTrue(sentFilters["visitedOnly"]!!.jsonPrimitive.boolean); assertFalse(sentFilters["favoritesOnly"]!!.jsonPrimitive.boolean)
        assertEquals("Cafe", sentFilters["type"]!!.jsonPrimitive.content); assertEquals("Luxury", sentFilters["priceRange"]!!.jsonPrimitive.content)
        assertEquals(listOf("wifi"), sentFilters["tags"]!!.jsonArray.map { it.jsonPrimitive.content }); assertEquals(3.0, sentFilters["minRating"]!!.jsonPrimitive.double)
    }

    @Test fun theNearbyTextAndTheCityAreSentAndAnUnsetSwitchIsNull() = runBlocking {
        reply = { ok("""{"items":[]}""") }
        repository().search(ShopQuery(" tea ", DiscoveryFilters(cityId = "brest"), origin)).getOrThrow()
        val sent = body(requests.single())
        assertEquals("tea", sent["q"]!!.jsonPrimitive.content)
        val sentFilters = sent["filters"]!!.jsonObject
        assertEquals("brest", sentFilters["city"]!!.jsonPrimitive.content); assertTrue(sentFilters["isOpen"] == null || sentFilters["isOpen"] is JsonNull)
    }

    @Test fun aNearbyRowCarriesWhatWasAskedForWhereTheServerIsSilent() = runBlocking {
        favorites = { setOf("kept") }
        reply = { ok("""{"items":[
            {"address":{"slug":"near"},"name":"Near","addressLine":"Street 2","rating":4.2,"checkInCount":3,"isOpen":true,"distanceMeters":120.5,
             "coverPhoto":{"fullUrl":"https://img/f","urls":{"card":"https://img/c"}},"tags":[{"name":"Wi-Fi"},{"name":"A"},{"name":"B"},{"name":"C"}]},
            {"address":{"slug":"kept"},"name":"Kept","isFavorite":false,"isVisited":true},
            {"address":{"slug":"liked"},"name":"Liked","isFavorite":true,"rating":0}],"totalItems":3,"totalPages":1,"currentPage":1}""") }
        val items = repository().search(ShopQuery(filters = DiscoveryFilters(kind = ShopKind.Specialty, newOnly = true, visitedOnly = true), origin = origin)).getOrThrow().items
        val near = items[0]
        assertEquals("Street 2", near.address); assertEquals(120.5, near.distanceMeters); assertEquals("https://img/c", near.photoUrl); assertEquals(listOf("Wi-Fi", "A", "B"), near.tags)
        assertEquals(ShopKind.Specialty, near.kind); assertTrue(near.isNew); assertTrue(near.isVisited); assertFalse(near.isFavorite)
        assertTrue(items[1].isFavorite); assertTrue(items[2].isFavorite); assertNull(items[2].rating)
        assertEquals(ShopKind.CoffeeBar, repository().search(ShopQuery(origin = origin)).getOrThrow().items.first().kind)
    }

    @Test fun anInvalidOriginOrPageSendsNothing() = runBlocking {
        for (query in listOf(ShopQuery(origin = Coordinates(91.0, 0.0)), ShopQuery(page = 0), ShopQuery(pageSize = 0), ShopQuery(pageSize = 101), ShopQuery(origin = origin, pageSize = 500))) {
            assertEquals(FailureKind.Other, failure(repository().search(query)).kind)
        }
        assertTrue(requests.isEmpty())
    }

    // ---- failures ----

    @Test fun aServerAnswerBecomesATypedFailureWithItsOwnMessage() = runBlocking {
        reply = { HttpStatusCode.Unauthorized to """{"isSuccess":false,"message":"Войдите"}""" }
        failure(repository().search(ShopQuery(filters = DiscoveryFilters(favoritesOnly = true), origin = origin))).let {
            assertEquals(FailureKind.SignInRequired, it.kind); assertEquals("Войдите", it.serverMessage)
        }
        reply = { HttpStatusCode.BadRequest to """{"isSuccess":false,"message":"Неверный запрос"}""" }
        failure(repository().search(ShopQuery())).let { assertEquals(FailureKind.Other, it.kind); assertEquals("Неверный запрос", it.serverMessage) }
        reply = { HttpStatusCode.OK to """{"isSuccess":false,"message":"  "}""" }
        failure(repository().search(ShopQuery())).let { assertEquals(FailureKind.Other, it.kind); assertNull(it.serverMessage) }
        reply = { HttpStatusCode.NotFound to "not json" }
        failure(repository().search(ShopQuery())).let { assertEquals(FailureKind.Other, it.kind); assertNull(it.serverMessage) }
    }

    // ---- the map ----

    private fun mapAnswer(shop: String, zone: String, truncated: Boolean = false) =
        ok("""{"shops":[$shop],"clusters":[{"id":"c","latitude":1,"longitude":1,"count":3}],"zones":[$zone],"isTruncated":$truncated}""")

    private val bounds = MapBounds(53.8, 27.5, 54.0, 27.7)

    @Test fun theMapAsksTwiceAndTakesTheShopsFromTheShopZoomAndTheZonesFromTheVisibleZoom() = runBlocking {
        reply = { request ->
            if (request.url.parameters["zoom"] == "22") mapAnswer("""{"address":{"slug":"s1"},"latitude":53.9,"longitude":27.56,"title":"Cafe","type":"Specialty","primaryZone":{"slug":"z"}}""", """{"address":{"slug":"fine"},"latitude":1,"longitude":1,"radiusMeters":1,"shopCount":1}""", truncated = true)
            else mapAnswer("{}".let { """{"address":{"slug":"x"},"latitude":0,"longitude":0}""" }, """{"address":{"slug":"zone"},"name":"Центр","description":"d","color":"#ABC","latitude":53.9,"longitude":27.56,"radiusMeters":800,"shopCount":7,"polygon":[{"latitude":1.0,"longitude":2.0}]}""")
        }
        val filters = DiscoveryFilters(cityId = "minsk", priceLevel = PriceLevel.Cheap, minRating = 4.0, roasterIds = setOf("r"), tagIds = setOf("t"))
        val content = repository().mapContent(bounds, 14.7f, filters).getOrThrow()
        assertEquals(setOf("22", "14"), requests.map { it.url.parameters["zoom"] }.toSet())
        for (request in requests) {
            assertEquals("/api/Map", request.url.encodedPath); assertEquals("53.8", request.url.parameters["minLat"]); assertEquals("27.7", request.url.parameters["maxLon"])
            assertEquals("minsk", request.url.parameters["city"]); assertEquals("Cheap", request.url.parameters["priceRange"]); assertEquals(listOf("r"), params(request, "roasters"))
        }
        assertEquals(listOf("s1"), content.markers.map { it.id }); assertEquals(Coordinates(53.9, 27.56), content.markers.single().coordinates)
        assertEquals(ShopKind.Specialty, content.markers.single().kind); assertEquals("z", content.markers.single().zoneId); assertTrue(content.isTruncated)
        val zone = content.zones.single()
        assertEquals("zone", zone.id); assertEquals("Центр", zone.name); assertEquals("#ABC", zone.color); assertEquals(7, zone.shopCount); assertEquals(listOf(Coordinates(1.0, 2.0)), zone.polygon)
    }

    @Test fun atTheShopZoomTheMapAsksOnceAndAFailedZoneRequestFallsBackToTheShopAnswer() = runBlocking {
        reply = { mapAnswer("""{"address":{"slug":"s"},"latitude":1,"longitude":2}""", """{"address":{"slug":"own"},"latitude":1,"longitude":2,"radiusMeters":5,"shopCount":1}""") }
        val atShopZoom = repository().mapContent(bounds, 22f, DiscoveryFilters()).getOrThrow()
        assertEquals(1, requests.size); assertEquals(listOf("own"), atShopZoom.zones.map { it.id })
        requests.clear()
        reply = { request -> if (request.url.parameters["zoom"] == "22") mapAnswer("""{"address":{"slug":"s"},"latitude":1,"longitude":2}""", """{"address":{"slug":"own"},"latitude":1,"longitude":2,"radiusMeters":5,"shopCount":1}""") else HttpStatusCode.InternalServerError to "{}" }
        assertEquals(listOf("own"), repository().mapContent(bounds, 12f, DiscoveryFilters()).getOrThrow().zones.map { it.id })
        assertEquals(2, requests.size)
    }

    @Test fun theMapKeepsOnlyTheFocusedKindAcceptsPascalCaseAndNamesNoShop() = runBlocking {
        reply = { ok("""{"Shops":[{"address":{"slug":"a"},"Latitude":1.0,"Longitude":2.0,"Title":" ","Type":"Cafe"},{"address":{"slug":"b"},"Latitude":3.0,"Longitude":4.0,"Title":"B","Type":"Specialty"}],"Zones":[],"IsTruncated":true}""") }
        val content = repository().mapContent(bounds, 22f, DiscoveryFilters(kind = ShopKind.Cafe)).getOrThrow()
        assertEquals(listOf("a"), content.markers.map { it.id }); assertEquals("", content.markers.single().title); assertTrue(content.isTruncated)
        assertEquals("Cafe", requests.single().url.parameters["type"])
    }

    @Test fun aFailedShopRequestFailsTheMapWithTheServerMessage() = runBlocking {
        reply = { HttpStatusCode.BadRequest to """{"isSuccess":false,"message":"Слишком большая область"}""" }
        assertEquals("Слишком большая область", failure(repository().mapContent(bounds, 22f, DiscoveryFilters())).serverMessage)
    }

    // ---- the catalogs ----

    private fun catalogReply(request: HttpRequestData): Pair<HttpStatusCode, String> = when (request.url.encodedPath) {
        "/api/Catalogs/cities" -> ok("""[{"address":{"slug":"minsk"},"name":"Минск"},{"address":{"slug":"brest"},"name":"Брест"}]""")
        "/api/Catalogs/beans" -> ok("""[{"name":"Арабика","address":{"slug":"arabica"}}]""")
        "/api/Catalogs/equipments" -> ok("""[{"name":"Кофемолка","slug":"grinder"}]""")
        "/api/Catalogs/brew-methods" -> ok("""[{"name":"V60","address":{"slug":"v60"},"slug":"ignored"}]""")
        "/api/Catalogs/shop-tags" -> ok("""[{"name":"Wi-Fi","address":{"slug":"wifi"}},{"slug":"nameless"}]""")
        else -> HttpStatusCode.NotFound to "{}"
    }

    @Test fun theCatalogsComeFromSixSourcesAndAreKeptAfterTheFirstSuccess() = runBlocking {
        roasters = { Result.success(listOf(CatalogEntry("roast", "Roaster", "https://logo"))) }
        reply = ::catalogReply
        val repository = repository()
        val catalogs = repository.catalogs().getOrThrow()
        assertEquals(listOf(CityEntry("minsk", "Минск"), CityEntry("brest", "Брест")), catalogs.cities)
        assertEquals(listOf(CatalogEntry("arabica", "Арабика")), catalogs.beans); assertEquals(listOf(CatalogEntry("grinder", "Кофемолка")), catalogs.equipment)
        assertEquals(listOf(CatalogEntry("v60", "V60")), catalogs.brewMethods); assertEquals(listOf(CatalogEntry("wifi", "Wi-Fi"), CatalogEntry("nameless", "")), catalogs.tags)
        assertEquals(listOf(CatalogEntry("roast", "Roaster", "https://logo")), catalogs.roasters)
        assertEquals(5, requests.size)
        assertSame(catalogs, repository.catalogs().getOrThrow()); assertEquals(5, requests.size)
    }

    @Test fun aFailedCatalogIsNotKeptAndTheNextCallReadsAgain() = runBlocking {
        var failing = true
        roasters = { if (failing) Result.failure(IllegalStateException("offline")) else Result.success(emptyList()) }
        reply = ::catalogReply
        val repository = repository()
        assertTrue(repository.catalogs().isFailure)
        failing = false
        assertEquals(2, repository.catalogs().getOrThrow().cities.size)
    }

    @Test fun aFailedCatalogSourceFailsTheWholeCatalogWithItsMessage() = runBlocking {
        reply = { request -> if (request.url.encodedPath.endsWith("beans")) HttpStatusCode.BadRequest to """{"isSuccess":false,"message":"Каталог недоступен"}""" else catalogReply(request) }
        assertEquals("Каталог недоступен", failure(repository().catalogs()).serverMessage)
    }

    @Test fun parallelCallsShareOneLoad() = runBlocking {
        reply = ::catalogReply
        val repository = repository()
        val results = listOf(async { repository.catalogs() }, async { repository.catalogs() }, async { repository.catalogs() }).map { it.await().getOrThrow() }
        assertEquals(1, results.distinct().size); assertEquals(5, requests.size)
    }
}
