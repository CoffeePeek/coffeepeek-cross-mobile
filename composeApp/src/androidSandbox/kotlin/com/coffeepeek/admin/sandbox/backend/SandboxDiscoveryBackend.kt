package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the shop search and the map answer. `Seeded` (default) leaves the two core shops and the one-marker map of [SandboxApplicationBackend] untouched; `Paged` serves
 * 47 shops in two cities with every filter, paging, nearby search by distance, map content by bounds and two zones; `Truncated` is `Paged` with a map that cuts the
 * shops and says so; `Empty` has no shops; `SearchFailure` and `MapFailure` answer 500 for the search or for the map.
 */
internal enum class SandboxDiscovery { Seeded, Paged, Truncated, Empty, SearchFailure, PageFailure, RefreshFailure, MapFailure, CatalogFailure, CatalogEmpty }

private data class Spot(
    val slug: String, val name: String, val city: String, val type: String, val price: String, val rating: Double,
    val open: Boolean, val new: Boolean, val visited: Boolean, val lat: Double, val lon: Double,
    val tags: Set<String>, val roasters: Set<String>, val brewMethods: Set<String>, val equipment: Set<String>, val beans: Set<String>,
)

private data class Zone(val slug: String, val name: String, val color: String, val lat: Double, val lon: Double, val radius: Double)

private class Query(
    val text: String?, val city: String?, val type: String?, val price: String?, val minRating: Double?,
    val tags: Set<String>, val roasters: Set<String>, val brewMethods: Set<String>, val equipment: Set<String>, val beans: Set<String>,
    val open: Boolean? = null, val new: Boolean? = null, val visitedOnly: Boolean = false, val favoritesOnly: Boolean = false,
)

/** Local fixtures for `coffee-shops/search` and `Map`; the favorite flags follow the favorites of the application backend. */
internal class SandboxDiscoveryBackend(
    private val populated: () -> Boolean,
    private val favorites: () -> Set<String>,
    private val photo: () -> JsonObject,
    private val address: (slug: String, path: String) -> JsonObject,
    private val catalog: (slug: String, name: String) -> JsonObject,
) {
    companion object {
        const val SEARCH_FAILURE = "Не удалось выполнить поиск"
        const val MAP_FAILURE = "Не удалось загрузить карту"
        const val MAP_LIMIT = 25
        const val EXTRA_SHOPS = 45
        val MINSK = 53.9006 to 27.5590
        val BREST = 52.0976 to 23.7341
        private val names = mapOf(
            "wifi" to "Wi-Fi", "pets" to "Можно с животными", "hario-v60" to "Hario V60", "aeropress" to "AeroPress",
            "espresso-machine" to "Эспрессо-машина", "grinder" to "Кофемолка", "arabica" to "Арабика", "robusta" to "Робуста",
            "sandbox-roaster" to "Sandbox Roaster", "sandbox-light" to "Sandbox Light Lab", "sandbox-decaf" to "Sandbox Decaf",
        )
        private val types = listOf("Specialty", "CoffeeBar", "Cafe")
        private val prices = listOf("Cheap", "Moderate", "Expensive")
        private val roasterSlugs = listOf("sandbox-roaster", "sandbox-light", "sandbox-decaf")
    }

    private val refreshAttempts = mutableMapOf<JsonObject, Int>()
    @Volatile var mode = SandboxDiscovery.Seeded
        set(value) {
            if (field != value) {
                refreshAttempts.clear()
            }
            field = value
        }
    private var pageFailureSent = false

    private val core = listOf(
        Spot("sandbox-cafe", "Sandbox Cafe", "minsk", "CoffeeBar", "Moderate", 4.5, open = true, new = false, visited = true, 53.9, 27.56,
            setOf("wifi"), setOf("sandbox-roaster"), setOf("hario-v60"), setOf("espresso-machine"), setOf("arabica")),
        Spot("sandbox-lab", "Sandbox Lab", "minsk", "Specialty", "Moderate", 4.5, open = false, new = true, visited = false, 53.9, 27.56,
            setOf("wifi", "pets"), setOf("sandbox-roaster"), setOf("aeropress"), setOf("grinder"), setOf("robusta")),
    )

    private val extras = (0 until EXTRA_SHOPS).map { i ->
        val (centerLat, centerLon) = if (i % 5 == 4) BREST else MINSK
        Spot(
            slug = "discovery-${i + 1}", name = "Discovery Shop ${i + 1}", city = if (i % 5 == 4) "brest" else "minsk",
            type = types[i % 3], price = prices[(i / 3) % 3], rating = 3.0 + (i % 21) / 10.0,
            open = i % 2 == 0, new = i % 7 == 0, visited = i % 5 == 0,
            lat = centerLat + ((i % 9) - 4) * 0.004, lon = centerLon + ((i / 9) - 2) * 0.006,
            tags = buildSet { add(if (i % 2 == 0) "wifi" else "pets"); if (i % 6 == 0) add("pets") },
            roasters = setOf(roasterSlugs[i % 3]), brewMethods = setOf(if (i % 2 == 0) "hario-v60" else "aeropress"),
            equipment = setOf(if (i % 3 == 0) "grinder" else "espresso-machine"), beans = setOf(if (i % 4 == 0) "robusta" else "arabica"),
        )
    }

    private val zones = listOf(
        Zone("zone-center", "Центр", "#D97706", MINSK.first, MINSK.second, 1_800.0),
        Zone("zone-north", "Север", "#2563EB", 53.93, 27.57, 1_200.0),
    )

    fun reset() { mode = SandboxDiscovery.Seeded }

    private fun spots(): List<Spot> = when {
        !populated() || mode == SandboxDiscovery.Empty -> emptyList()
        mode == SandboxDiscovery.Seeded -> core
        else -> core + extras
    }

    fun handle(path: String, method: HttpMethod, request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        catalogResponse(path, method)?.let { return it }
        return when {
            path == "/api/v1/coffee-shops/search" && method == HttpMethod.Post ->
                searchResponse(input)
            path == "/api/v1/coffee-shops/map" && method == HttpMethod.Post ->
                if (mode == SandboxDiscovery.MapFailure) HttpStatusCode.InternalServerError to failure(MAP_FAILURE) else filteredMap(input)
            path == "/api/map" && method == HttpMethod.Get && mode != SandboxDiscovery.Seeded ->
                if (mode == SandboxDiscovery.MapFailure) HttpStatusCode.InternalServerError to failure(MAP_FAILURE) else map(request.url.parameters)
            else -> null
        }
    }

    private fun catalogResponse(path: String, method: HttpMethod): Pair<HttpStatusCode, JsonObject>? {
        if (method != HttpMethod.Get) return null
        if (mode == SandboxDiscovery.CatalogFailure && path == "/api/catalogs/beans") {
            return HttpStatusCode.InternalServerError to failure("Catalog fixture failure")
        }
        if (mode == SandboxDiscovery.CatalogEmpty &&
            (path.startsWith("/api/catalogs/") && path != "/api/catalogs/cities" || path == "/api/roasters")) {
            return ok(JsonArray(emptyList()))
        }
        return null
    }

    /** Page one rearms the failure; the first attempt at page two fails, and an explicit retry succeeds. */
    private fun searchResponse(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val page = input["page"]?.jsonPrimitive?.intOrNull ?: 1
        if (page == 1) pageFailureSent = false
        if (mode == SandboxDiscovery.SearchFailure) return HttpStatusCode.InternalServerError to failure(SEARCH_FAILURE)
        if (mode == SandboxDiscovery.RefreshFailure && page == 1) {
            val attempt = (refreshAttempts[input] ?: 0) + 1
            refreshAttempts[input] = attempt
            if (attempt == 2) return HttpStatusCode.InternalServerError to failure(SEARCH_FAILURE)
        }
        if (mode == SandboxDiscovery.PageFailure && page == 2 && !pageFailureSent) {
            pageFailureSent = true
            return HttpStatusCode.InternalServerError to failure(SEARCH_FAILURE)
        }
        return search(input)
    }

    private fun search(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        invalidSelection(input)?.let { return HttpStatusCode.BadRequest to failure(it) }
        val (items, total, pages, current) = paged(selected(input), input["page"]?.jsonPrimitive?.contentOrNull, input["pageSize"]?.jsonPrimitive?.contentOrNull)
        return ok(buildJsonObject {
            put("items", JsonArray(items.map { (spot, meters) -> searchItem(spot, meters) }))
            put("totalItems", total); put("totalPages", pages); put("currentPage", current)
        })
    }

    private fun invalidSelection(input: JsonObject): String? {
        val filters = input["filters"] as? JsonObject ?: buildJsonObject {}
        val origin = filters["origin"] as? JsonObject
        val lat = origin?.get("latitude")?.jsonPrimitive?.doubleOrNull
        val lon = origin?.get("longitude")?.jsonPrimitive?.doubleOrNull
        val radius = filters["radiusKm"]?.jsonPrimitive?.doubleOrNull
        if (origin != null && (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0)) {
            return "Укажите точку поиска"
        }
        if (radius != null && (origin == null || radius <= 0 || radius > 100) || input["sort"]?.jsonPrimitive?.contentOrNull == "distance_asc" && origin == null) {
            return "Укажите точку поиска и радиус"
        }
        return null
    }

    private fun selected(input: JsonObject): List<Pair<Spot, Double?>> {
        val filters = input["filters"] as? JsonObject ?: buildJsonObject {}
        val origin = filters["origin"] as? JsonObject
        return searchHits(searchQuery(input, filters), origin?.get("latitude")?.jsonPrimitive?.doubleOrNull,
            origin?.get("longitude")?.jsonPrimitive?.doubleOrNull, filters["radiusKm"]?.jsonPrimitive?.doubleOrNull,
            input["sort"]?.jsonPrimitive?.contentOrNull)
    }

    private fun filteredMap(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        invalidSelection(input)?.let { return HttpStatusCode.BadRequest to failure(it) }
        val bounds = input["bounds"] as? JsonObject
        val minLat = bounds?.get("minLat")?.jsonPrimitive?.doubleOrNull
        val maxLat = bounds?.get("maxLat")?.jsonPrimitive?.doubleOrNull
        val minLon = bounds?.get("minLon")?.jsonPrimitive?.doubleOrNull
        val maxLon = bounds?.get("maxLon")?.jsonPrimitive?.doubleOrNull
        val zoom = input["zoom"]?.jsonPrimitive?.intOrNull
        if (minLat == null || maxLat == null || minLon == null || maxLon == null || zoom == null || zoom !in 0..22 ||
            !minLat.isFinite() || !maxLat.isFinite() || !minLon.isFinite() || !maxLon.isFinite() ||
            minLat !in -90.0..90.0 || maxLat !in minLat..90.0 || minLon !in -180.0..180.0 || maxLon !in minLon..180.0)
            return HttpStatusCode.BadRequest to failure("Укажите границы карты и масштаб")
        val inside = selected(input).map { it.first }.filter { it.lat in minLat..maxLat && it.lon in minLon..maxLon }
        val truncated = mode == SandboxDiscovery.Truncated && inside.size > MAP_LIMIT
        val shown = if (truncated) inside.take(MAP_LIMIT) else inside
        val visibleZones = zones.filter { zone -> inside.any { distance(zone.lat, zone.lon, it.lat, it.lon) <= zone.radius } }
        return ok(mapRows(shown, zoom, visibleZones, truncated))
    }

    private fun searchQuery(input: JsonObject, filters: JsonObject): Query {
        fun ids(key: String) = (filters[key] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
        return Query(
            text = input["q"]?.jsonPrimitive?.contentOrNull, city = filters["city"]?.jsonPrimitive?.contentOrNull, type = filters["type"]?.jsonPrimitive?.contentOrNull,
            price = filters["priceRange"]?.jsonPrimitive?.contentOrNull, minRating = filters["minRating"]?.jsonPrimitive?.doubleOrNull,
            tags = ids("tags"), roasters = ids("roasters"), brewMethods = ids("brewMethods"), equipment = ids("equipments"), beans = ids("beans"),
            open = filters["isOpen"]?.jsonPrimitive?.booleanOrNull, new = filters["isNew"]?.jsonPrimitive?.booleanOrNull,
            visitedOnly = filters["visitedOnly"]?.jsonPrimitive?.booleanOrNull == true, favoritesOnly = filters["favoritesOnly"]?.jsonPrimitive?.booleanOrNull == true,
        )
    }

    private fun searchHits(query: Query, lat: Double?, lon: Double?, radius: Double?, sort: String?): List<Pair<Spot, Double?>> {
        val found = spots().filter { matches(it, query) }.map { spot ->
            spot to if (lat != null && lon != null) distance(lat, lon, spot.lat, spot.lon) else null
        }.filter { radius == null || it.second?.let { meters -> meters <= radius * 1_000 } == true }
        return when (sort ?: if (!query.text.isNullOrBlank()) "relevance" else if (lat != null) "distance_asc" else "name_asc") {
            "distance_asc" -> found.sortedWith(compareBy<Pair<Spot, Double?>> { it.second }.thenBy { it.first.slug })
            "rating_desc" -> found.sortedByDescending { it.first.rating }
            else -> found.sortedBy { it.first.name }
        }
    }

    // GET /api/Map: the shops and zones inside the bounds; the truncated fixture keeps only a few shops and says so.
    private fun map(params: Parameters): Pair<HttpStatusCode, JsonObject> {
        val minLat = params["minLat"]?.toDoubleOrNull(); val maxLat = params["maxLat"]?.toDoubleOrNull()
        val minLon = params["minLon"]?.toDoubleOrNull(); val maxLon = params["maxLon"]?.toDoubleOrNull()
        if (minLat == null || maxLat == null || minLon == null || maxLon == null) return HttpStatusCode.BadRequest to failure("Укажите границы карты")
        val query = Query(
            text = null, city = params["city"], type = params["type"], price = params["priceRange"], minRating = params["minRating"]?.toDoubleOrNull(),
            tags = params.getAll("tags").orEmpty().toSet(), roasters = params.getAll("roasters").orEmpty().toSet(),
            brewMethods = params.getAll("brewMethods").orEmpty().toSet(), equipment = params.getAll("equipments").orEmpty().toSet(),
            beans = params.getAll("beans").orEmpty().toSet(),
        )
        val inside = spots().filter { it.lat in minLat..maxLat && it.lon in minLon..maxLon && matches(it, query) }
        val truncated = mode == SandboxDiscovery.Truncated && inside.size > MAP_LIMIT
        val shown = if (truncated) inside.take(MAP_LIMIT) else inside
        val visibleZones = if (spots().isEmpty()) emptyList() else zones.filter { it.lat in minLat..maxLat && it.lon in minLon..maxLon }
        return ok(mapRows(shown, params["zoom"]?.toIntOrNull() ?: 22, visibleZones, truncated))
    }

    private fun mapRows(shown: List<Spot>, zoom: Int, visibleZones: List<Zone>, truncated: Boolean): JsonObject {
        // Local groups exercise transitions; production grouping remains owned by the server.
        val zoned = if (zoom <= 13) shown.groupBy { spot ->
            visibleZones.firstOrNull { distance(it.lat, it.lon, spot.lat, spot.lon) <= it.radius }
        } else mapOf(null to shown)
        val unzoned = zoned[null].orEmpty()
        val cell = 360.0 / 2.0.pow(zoom.coerceIn(0, 22))
        val groups = if (zoom > 13) unzoned.map(::listOf) else unzoned.groupBy { floor(it.lat / cell) to floor(it.lon / cell) }.values
        return buildJsonObject {
            put("shops", JsonArray(groups.filter { it.size == 1 }.map { mapShop(it.single()) }))
            put("clusters", JsonArray(groups.filter { it.size > 1 }.map(::mapCluster)))
            put("zones", JsonArray(zoned.mapNotNull { (zone, members) -> zone?.let { zoneRow(it, members.size) } }))
            put("isTruncated", truncated)
        }
    }

    private fun mapShop(spot: Spot) = buildJsonObject {
        put("address", address(spot.slug, "/coffee-shops/${spot.slug}"))
        put("latitude", spot.lat)
        put("longitude", spot.lon)
        put("title", spot.name)
        put("type", spot.type)
    }

    private fun mapCluster(group: List<Spot>) = buildJsonObject {
        put("id", group.map { it.slug }.sorted().joinToString(","))
        put("latitude", group.map { it.lat }.average())
        put("longitude", group.map { it.lon }.average())
        put("count", group.size)
        put("bounds", buildJsonObject {
            put("minLatitude", group.minOf { it.lat })
            put("minLongitude", group.minOf { it.lon })
            put("maxLatitude", group.maxOf { it.lat })
            put("maxLongitude", group.maxOf { it.lon })
        })
    }

    private fun matches(spot: Spot, query: Query): Boolean =
        (query.text.isNullOrBlank() || spot.name.contains(query.text.trim(), ignoreCase = true)) &&
            (query.city == null || spot.city == query.city) &&
            (query.type == null || spot.type.equals(query.type.replace("-", ""), ignoreCase = true)) &&
            (query.price == null || spot.price.equals(query.price, ignoreCase = true)) &&
            (query.minRating == null || spot.rating >= query.minRating) &&
            (query.tags.isEmpty() || spot.tags.any(query.tags::contains)) &&
            (query.roasters.isEmpty() || spot.roasters.any(query.roasters::contains)) &&
            (query.brewMethods.isEmpty() || spot.brewMethods.any(query.brewMethods::contains)) &&
            (query.equipment.isEmpty() || spot.equipment.any(query.equipment::contains)) &&
            (query.beans.isEmpty() || spot.beans.any(query.beans::contains)) &&
            (query.open != true || spot.open) && (query.new != true || spot.new) &&
            (!query.visitedOnly || spot.visited) && (!query.favoritesOnly || isFavorite(spot))

    private fun isFavorite(spot: Spot) = "coffee_shop:${spot.slug}" in favorites()

    private data class Page<T>(val items: List<T>, val total: Int, val pages: Int, val current: Int)

    private fun <T> paged(all: List<T>, page: String?, pageSize: String?): Page<T> {
        val size = pageSize?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        val current = page?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pages = (all.size + size - 1) / size
        return Page(all.drop((current - 1) * size).take(size), all.size, pages, current)
    }

    private fun searchItem(spot: Spot, distance: Double?) = buildJsonObject {
        put("address", address(spot.slug, "/coffee-shops/${spot.slug}")); put("name", spot.name)
        put("addressLine", "Тестовая улица"); put("coverPhoto", photo()); put("rating", spot.rating); put("checkInCount", 3)
        put("isOpen", spot.open); put("isVisited", spot.visited); put("isFavorite", isFavorite(spot))
        distance?.let { put("distanceMeters", it) }
        put("tags", JsonArray(spot.tags.map { catalog(it, names.getValue(it)) }))
    }

    private fun zoneRow(zone: Zone, count: Int) = buildJsonObject {
        put("address", address(zone.slug, "/zones/${zone.slug}")); put("name", zone.name); put("description", "Район ${zone.name}")
        put("color", zone.color); put("latitude", zone.lat); put("longitude", zone.lon); put("radiusMeters", zone.radius)
        put("shopCount", count)
        put("polygon", JsonArray((0 until 6).map { index ->
            val angle = 2 * PI * index / 6
            buildJsonObject {
                put("latitude", zone.lat + zone.radius / 111_320.0 * sin(angle))
                put("longitude", zone.lon + zone.radius / (111_320.0 * cos(zone.lat * PI / 180)) * cos(angle))
            }
        }))
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1) * PI / 180; val dLon = (lon2 - lon1) * PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * 6_371_000.0 * asin(sqrt(a))
    }

    private fun ok(data: JsonElement) = HttpStatusCode.OK to buildJsonObject { put("isSuccess", true); put("data", data) }
    private fun failure(message: String) = buildJsonObject { put("isSuccess", false); put("data", JsonNull); put("message", message) }
}
