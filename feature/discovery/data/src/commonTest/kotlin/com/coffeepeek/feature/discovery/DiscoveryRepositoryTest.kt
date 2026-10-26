package com.coffeepeek.feature.discovery

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.sync.withLock
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
    /** The catalogs are read in parallel, so the recording must not race. */
    private val recording = kotlinx.coroutines.sync.Mutex()
    private var reply: (HttpRequestData) -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to """{"isSuccess":true,"data":null}""" }
    private var favorites: suspend () -> Set<String> = { emptySet() }
    private var roasters: suspend () -> Result<List<CatalogEntry>> = { Result.success(emptyList()) }
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repository(): DiscoveryRepository {
        val client = HttpClientFactory(MockEngine { request ->
            recording.withLock { requests += request }
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

    // ---- ordinary search uses the same POST without geolocation ----

    @Test fun ordinarySearchPostsTextCityFiltersAndPaginationWithoutGeolocation() = runBlocking {
        reply = { ok("""{"items":[],"currentPage":2,"totalItems":45,"totalPages":3}""") }
        val filters = DiscoveryFilters(
            cityId = "minsk", kind = ShopKind.Specialty, priceLevel = PriceLevel.Moderate, minRating = 4.5,
            openOnly = true, newOnly = true, visitedOnly = true, favoritesOnly = true,
            roasterIds = setOf("r1"), beanIds = setOf("b1"), equipmentIds = setOf("e1"), brewMethodIds = setOf("m1"), tagIds = setOf("t1", "t2"),
        )
        val page = repository().search(ShopQuery("  cof  ", filters, page = 2, pageSize = 20)).getOrThrow()
        val request = requests.single()
        assertEquals("/api/v1/coffee-shops/search", request.url.encodedPath)
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.parameters.isEmpty())
        val sent = body(request)
        assertEquals("cof", sent["q"]!!.jsonPrimitive.content)
        assertEquals(2, sent["page"]!!.jsonPrimitive.int)
        assertEquals(20, sent["pageSize"]!!.jsonPrimitive.int)
        assertOrdinaryFilters(sent)
        assertEquals(45, page.totalCount)
        assertEquals(3, page.totalPages)
        assertEquals(2, page.currentPage)
        assertTrue(page.hasMore)
    }

    private fun assertOrdinaryFilters(sent: JsonObject) {
        assertFalse("sort" in sent)
        val filters = sent["filters"]!!.jsonObject
        assertFalse("origin" in filters)
        assertFalse("radiusKm" in filters)
        assertEquals("minsk", filters["city"]!!.jsonPrimitive.content)
        assertEquals("specialty", filters["type"]!!.jsonPrimitive.content)
        assertEquals("moderate", filters["priceRange"]!!.jsonPrimitive.content)
        assertEquals(4.5, filters["minRating"]!!.jsonPrimitive.double)
        for ((key, ids) in mapOf("roasters" to listOf("r1"), "beans" to listOf("b1"), "equipments" to listOf("e1"), "brewMethods" to listOf("m1"), "tags" to listOf("t1", "t2"))) {
            assertEquals(ids, filters[key]!!.jsonArray.map { it.jsonPrimitive.content })
        }
        for (key in listOf("isOpen", "isNew", "visitedOnly", "favoritesOnly")) {
            assertTrue(filters[key]!!.jsonPrimitive.boolean, key)
        }
    }

    @Test fun blankTextDoesNotSendQueryOriginRadiusOrDistanceSort() = runBlocking {
        reply = { ok("""{"items":[]}""") }
        repository().search(ShopQuery("   ")).getOrThrow()
        val sent = body(requests.single())
        assertFalse("q" in sent)
        assertFalse("sort" in sent)
        assertFalse("origin" in sent["filters"]!!.jsonObject)
        assertFalse("radiusKm" in sent["filters"]!!.jsonObject)
    }

    @Test fun ordinaryCardsReadThePostResponseAndAccountOrServerFavorites() = runBlocking {
        favorites = { setOf("cafe") }
        reply = { ok("""{"items":[
            {"address":{"slug":"cafe"},"name":"Cafe","rating":4.5,"checkInCount":"12","isOpen":true,"isVisited":true,
             "addressLine":"Street 1","coverPhoto":{"fullUrl":"https://img/full","urls":{"card":"https://img/card"}},
             "tags":[{"name":"Wi-Fi"},{"name":"Pets"},{"name":"Quiet"},{"name":"Garden"}]},
            {"address":{"slug":"bare"},"name":"Bare","rating":0,"isFavorite":true}],"totalItems":2,"totalPages":1}""") }
        val (full, bare) = repository().search(ShopQuery()).getOrThrow().items
        assertEquals("cafe", full.id)
        assertEquals(4.5, full.rating)
        assertEquals(12, full.checkInCount)
        assertEquals("Street 1", full.address)
        assertEquals("https://img/card", full.photoUrl)
        assertTrue(full.isOpen && full.isVisited && full.isFavorite)
        assertEquals(listOf("Wi-Fi", "Pets", "Quiet"), full.tags)
        assertNull(full.coordinates)
        assertNull(full.priceLevel)
        assertNull(bare.rating)
        assertTrue(bare.isFavorite)
    }

    @Test fun failingFavoritesSourceKeepsTheServersFavoriteFlag() = runBlocking {
        favorites = { error("offline") }
        reply = { ok("""{"items":[{"address":{"slug":"a"},"name":"A","isFavorite":true}]}""") }
        assertTrue(repository().search(ShopQuery()).getOrThrow().items.single().isFavorite)
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
        assertEquals("cafe", sentFilters["type"]!!.jsonPrimitive.content); assertEquals("expensive", sentFilters["priceRange"]!!.jsonPrimitive.content)
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
        ok("""{"shops":[$shop],"clusters":[{"id":"c","latitude":1,"longitude":1,"count":3,"bounds":{"minLatitude":0,"minLongitude":0,"maxLatitude":2,"maxLongitude":2}}],"zones":[$zone],"isTruncated":$truncated}""")

    private val bounds = MapBounds(53.8, 27.5, 54.0, 27.7)

    @Test fun theMapRequestsOneFinerZoomAndRetainsServerClustersAndFilters() = runBlocking {
        reply = { mapAnswer("""{"address":{"slug":"s1"},"latitude":53.9,"longitude":27.56,"title":"Cafe","type":"Specialty","primaryZone":{"slug":"z"}}""",
            """{"address":{"slug":"zone"},"name":"Centre","latitude":53.9,"longitude":27.56,"radiusMeters":800,"shopCount":7}""", truncated = true) }
        val filters = DiscoveryFilters(cityId = "minsk", priceLevel = PriceLevel.Cheap, minRating = 4.0, roasterIds = setOf("r"), tagIds = setOf("t"))
        val content = repository().mapContent(bounds, 14.7f, filters).getOrThrow()
        val request = requests.single()
        assertEquals("15", request.url.parameters["zoom"])
        assertEquals("/api/Map", request.url.encodedPath)
        assertEquals("53.8", request.url.parameters["minLat"])
        assertEquals("minsk", request.url.parameters["city"])
        assertEquals("Cheap", request.url.parameters["priceRange"])
        assertEquals(listOf("r"), params(request, "roasters"))
        assertEquals(listOf("s1"), content.markers.map { it.id })
        assertEquals(ShopKind.Specialty, content.markers.single().kind)
        assertEquals("z", content.markers.single().zoneId)
        assertEquals(listOf("zone"), content.zones.map { it.id })
        assertEquals(MapCluster("c", Coordinates(1.0, 1.0), 3, MapBounds(0.0, 0.0, 2.0, 2.0)), content.clusters.single())
        assertTrue(content.isTruncated)
    }

    @Test fun mapDetailLevelUnfoldsCityClustersEarlierAndKeepsTheServerZoomLimits() = runBlocking {
        reply = { ok("""{"shops":[],"clusters":[],"zones":[]}""") }
        val cases = listOf(-2f to 0, 10f to 11, 12.9f to 13, 13f to 14, 22f to 22, 30f to 22)
        val repository = repository()
        for ((cameraZoom, requestZoom) in cases) {
            repository.mapContent(bounds, cameraZoom, DiscoveryFilters()).getOrThrow()
            assertEquals(requestZoom.toString(), requests.last().url.parameters["zoom"])
        }
    }

    @Test fun clusterOnlyPascalCaseAnswersKeepBoundsCountsAndFallbackIdentity() = runBlocking {
        reply = { ok("""{"Clusters":[{"Latitude":1,"Longitude":1,"Count":120,"Bounds":{"MinLatitude":0,"MinLongitude":0,"MaxLatitude":2,"MaxLongitude":2}}]}""") }
        val content = repository().mapContent(bounds, 12f, DiscoveryFilters()).getOrThrow()
        assertTrue(content.markers.isEmpty())
        assertEquals("1.0:1.0:120", content.clusters.single().id)
        assertEquals(120, content.clusters.single().count)
        assertEquals(MapBounds(0.0, 0.0, 2.0, 2.0), content.clusters.single().bounds)
        assertEquals(1, requests.size)
    }

    @Test fun invalidClustersAreDroppedAndAnAnswerWithoutClustersStillWorks() = runBlocking {
        val cluster = """{"latitude":1,"longitude":1,"count":0,"bounds":{"minLatitude":0,"minLongitude":0,"maxLatitude":2,"maxLongitude":2}}"""
        for (invalid in listOf(cluster, cluster.replace("\"count\":0", "\"count\":2").replace("\"latitude\":1", "\"latitude\":91"))) {
            reply = { ok("""{"clusters":[$invalid]}""") }
            assertTrue(repository().mapContent(bounds, 12f, DiscoveryFilters()).getOrThrow().clusters.isEmpty())
        }
        reply = { ok("""{"shops":[],"zones":[]}""") }
        assertEquals(MapContent(), repository().mapContent(bounds, 22f, DiscoveryFilters()).getOrThrow())
    }

    @Test fun theMapKeepsOnlyTheFocusedKindAcceptsPascalCaseAndNamesAnUntitledShopLikeProd() = runBlocking {
        reply = { ok("""{"Shops":[{"address":{"slug":"a"},"Latitude":1.0,"Longitude":2.0,"Title":" ","Type":"Cafe"},{"address":{"slug":"b"},"Latitude":3.0,"Longitude":4.0,"Title":"B","Type":"Specialty"}],"Zones":[],"IsTruncated":true}""") }
        val content = repository().mapContent(bounds, 22f, DiscoveryFilters(kind = ShopKind.Cafe)).getOrThrow()
        assertEquals(listOf("a"), content.markers.map { it.id }); assertEquals("Кофейня", content.markers.single().title) /* a blank title is named as in prod */; assertTrue(content.isTruncated)
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
