package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the shop search and the map answer. `Seeded` (default) leaves the two core shops and the one-marker map of [SandboxApplicationBackend] untouched; `Paged` serves
 * 47 shops in two cities with every filter, paging, nearby search by distance, map content by bounds and two zones; `Truncated` is `Paged` with a map that cuts the
 * shops and says so; `Empty` has no shops; `SearchFailure` and `MapFailure` answer 500 for the search or for the map.
 */
internal enum class SandboxDiscovery { Seeded, Paged, Truncated, Empty, SearchFailure, MapFailure }

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

/** Local fixtures for `CoffeeShops`, `coffee-shops/search` and `Map`; the favorite flags follow the favorites of the application backend. */
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

    @Volatile var mode = SandboxDiscovery.Seeded

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

    private fun spots(): List<Spot> = if (!populated() || mode == SandboxDiscovery.Empty) emptyList() else core + extras

    fun handle(path: String, method: HttpMethod, request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        if (mode == SandboxDiscovery.Seeded) return null
        return when {
            path == "/api/coffeeshops" && method == HttpMethod.Get ->
                if (mode == SandboxDiscovery.SearchFailure) HttpStatusCode.InternalServerError to failure(SEARCH_FAILURE) else list(request.url.parameters)
            path == "/api/v1/coffee-shops/search" && method == HttpMethod.Post ->
                if (mode == SandboxDiscovery.SearchFailure) HttpStatusCode.InternalServerError to failure(SEARCH_FAILURE) else nearby(input)
            path == "/api/map" && method == HttpMethod.Get ->
                if (mode == SandboxDiscovery.MapFailure) HttpStatusCode.InternalServerError to failure(MAP_FAILURE) else map(request.url.parameters)
            else -> null
        }
    }

    // GET /api/CoffeeShops: the plain search, filtered and paged.
    private fun list(params: Parameters): Pair<HttpStatusCode, JsonObject> {
        val query = Query(
            text = params["q"], city = params["city"], type = params["type"], price = params["priceRange"], minRating = params["minRating"]?.toDoubleOrNull(),
            tags = params.getAll("tags").orEmpty().toSet(), roasters = params.getAll("roasters").orEmpty().toSet(),
            brewMethods = params.getAll("brewMethods").orEmpty().toSet(), equipment = params.getAll("equipments").orEmpty().toSet(),
            beans = params.getAll("beans").orEmpty().toSet(),
        )
        val (items, total, pages, current) = paged(spots().filter { matches(it, query) }, params["page"], params["pageSize"])
        return ok(buildJsonObject {
            put("coffeeShops", JsonArray(items.map { listRow(it, null) }))
            put("totalItems", total); put("totalPages", pages); put("currentPage", current)
        })
    }

    // POST /api/v1/coffee-shops/search: the nearby search, sorted by distance from the origin and limited to the radius.
    private fun nearby(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val filters = input["filters"] as? JsonObject ?: return HttpStatusCode.BadRequest to failure("Некорректный запрос")
        val origin = filters["origin"] as? JsonObject ?: return HttpStatusCode.BadRequest to failure("Укажите точку поиска")
        val originLat = origin["latitude"]?.jsonPrimitive?.doubleOrNull ?: return HttpStatusCode.BadRequest to failure("Укажите точку поиска")
        val originLon = origin["longitude"]?.jsonPrimitive?.doubleOrNull ?: return HttpStatusCode.BadRequest to failure("Укажите точку поиска")
        val radiusMeters = (filters["radiusKm"]?.jsonPrimitive?.intOrNull ?: 3) * 1_000.0
        fun ids(key: String) = (filters[key] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
        val query = Query(
            text = input["q"]?.jsonPrimitive?.contentOrNull, city = filters["city"]?.jsonPrimitive?.contentOrNull, type = filters["type"]?.jsonPrimitive?.contentOrNull,
            price = filters["priceRange"]?.jsonPrimitive?.contentOrNull, minRating = filters["minRating"]?.jsonPrimitive?.doubleOrNull,
            tags = ids("tags"), roasters = ids("roasters"), brewMethods = ids("brewMethods"), equipment = ids("equipments"), beans = ids("beans"),
            open = filters["isOpen"]?.jsonPrimitive?.booleanOrNull, new = filters["isNew"]?.jsonPrimitive?.booleanOrNull,
            visitedOnly = filters["visitedOnly"]?.jsonPrimitive?.booleanOrNull == true, favoritesOnly = filters["favoritesOnly"]?.jsonPrimitive?.booleanOrNull == true,
        )
        val found = spots().filter { matches(it, query) }
            .map { it to distance(originLat, originLon, it.lat, it.lon) }.filter { it.second <= radiusMeters }
            .sortedWith(compareBy<Pair<Spot, Double>> { it.second }.thenBy { it.first.slug })
        val (items, total, pages, current) = paged(found, input["page"]?.jsonPrimitive?.contentOrNull, input["pageSize"]?.jsonPrimitive?.contentOrNull)
        return ok(buildJsonObject {
            put("items", JsonArray(items.map { (spot, meters) -> searchItem(spot, meters) }))
            put("totalItems", total); put("totalPages", pages); put("currentPage", current)
        })
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
        return ok(buildJsonObject {
            put("shops", JsonArray(shown.map { spot ->
                buildJsonObject {
                    put("address", address(spot.slug, "/coffee-shops/${spot.slug}")); put("latitude", spot.lat); put("longitude", spot.lon)
                    put("title", spot.name); put("type", spot.type)
                }
            }))
            put("clusters", JsonArray(emptyList()))
            put("zones", JsonArray(visibleZones.map(::zoneRow)))
            put("isTruncated", truncated)
        })
    }

    private fun matches(spot: Spot, query: Query): Boolean =
        (query.text.isNullOrBlank() || spot.name.contains(query.text.trim(), ignoreCase = true)) &&
            (query.city == null || spot.city == query.city) &&
            (query.type == null || spot.type.equals(query.type, ignoreCase = true)) &&
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

    private fun listRow(spot: Spot, distance: Double?) = buildJsonObject {
        put("address", address(spot.slug, "/coffee-shops/${spot.slug}")); put("name", spot.name)
        put("description", "Тестовая кофейня. Данные и действия только локальные.")
        put("city", address(spot.city, "/cities/${spot.city}")); put("rating", spot.rating); put("checkInCount", 3)
        put("isOpen", spot.open); put("isNew", spot.new); put("isVisited", spot.visited); put("isFavorite", isFavorite(spot))
        put("priceRange", spot.price); put("type", spot.type); put("addressLine", "Тестовая улица, ${spot.slug.substringAfterLast('-').takeWhile(Char::isDigit).ifEmpty { "1" }}")
        distance?.let { put("distanceMeters", it) }
        putJsonObject("location") { put("address", "Тестовая улица"); put("latitude", spot.lat); put("longitude", spot.lon) }
        put("photos", JsonArray(listOf(photo()))); put("coverPhoto", photo())
        put("roasters", JsonArray(spot.roasters.map { catalog(it, names.getValue(it)) }))
        put("tags", JsonArray(spot.tags.map { catalog(it, names.getValue(it)) }))
    }

    private fun searchItem(spot: Spot, distance: Double) = buildJsonObject {
        put("address", address(spot.slug, "/coffee-shops/${spot.slug}")); put("name", spot.name)
        put("addressLine", "Тестовая улица"); put("coverPhoto", photo()); put("rating", spot.rating); put("checkInCount", 3)
        put("isOpen", spot.open); put("isVisited", spot.visited); put("isFavorite", isFavorite(spot)); put("distanceMeters", distance)
        put("tags", JsonArray(spot.tags.map { catalog(it, names.getValue(it)) }))
    }

    private fun zoneRow(zone: Zone) = buildJsonObject {
        put("address", address(zone.slug, "/zones/${zone.slug}")); put("name", zone.name); put("description", "Район ${zone.name}")
        put("color", zone.color); put("latitude", zone.lat); put("longitude", zone.lon); put("radiusMeters", zone.radius)
        put("shopCount", spots().count { distance(zone.lat, zone.lon, it.lat, it.lon) <= zone.radius })
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
