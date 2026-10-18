package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

/** Local fixture contracts for the existing app, not an alternative implementation of its UI. */
internal class SandboxApplicationBackend(private val today: LocalDate, private val visits: MutableList<JsonObject>) {
    private val favorites = linkedSetOf("coffee_shop:sandbox-cafe", "roaster:sandbox-roaster", "drink:cappuccino")
    private var username = "Sandbox"
    private var about = "Локальный тестовый профиль. Все действия остаются в Sandbox."
    private var populated = true
    fun reset(populated: Boolean) {
        this.populated = populated
        username = "Sandbox"
        about = "Локальный тестовый профиль. Все действия остаются в Sandbox."
        favorites.clear()
        if (populated) favorites += listOf("coffee_shop:sandbox-cafe", "roaster:sandbox-roaster", "drink:cappuccino")
    }

    fun handle(request: HttpRequestData, body: JsonElement?): Pair<HttpStatusCode, JsonObject>? {
        val path = request.url.encodedPath.lowercase()
        val method = request.method
        val input = body as? JsonObject ?: buildJsonObject {}
        if (path == "/api/users/me" && method == HttpMethod.Get) return ok(profile())
        if (path == "/api/users/me/username" && method == HttpMethod.Patch) {
            username = input["username"]?.jsonPrimitive?.content ?: username
            return ok(buildJsonObject { put("username", username); put("address", address("sandbox", "/users/sandbox")) })
        }
        if (path == "/api/users/me/about" && method == HttpMethod.Patch) {
            about = input["about"]?.jsonPrimitive?.content.orEmpty(); return ok(JsonNull)
        }
        if (path == "/api/users/me/avatar" && method == HttpMethod.Patch) return ok(JsonNull)
        if (path == "/api/users/me/deletion-request") return HttpStatusCode.NotFound to envelope(JsonNull, false)
        if (path.startsWith("/api/users/") && method == HttpMethod.Get) {
            if (path.endsWith("/check-ins")) return ok(page(JsonArray(visits)))
            if (path.endsWith("/followers") || path.endsWith("/following")) return HttpStatusCode.OK to buildJsonObject {
                putJsonArray("items") {}; put("totalCount", 0)
                put("page", request.url.parameters["page"]?.toIntOrNull() ?: 1)
                put("pageSize", request.url.parameters["pageSize"]?.toIntOrNull() ?: 20)
            }
            return HttpStatusCode.OK to buildJsonObject { put("data", profile()); put("address", address("sandbox", "/users/sandbox")) }
        }
        if (path.startsWith("/api/v1/profiles/") && path.endsWith("/favorites")) return ok(favoriteRows())
        if (path.startsWith("/api/v1/profiles/") && path.endsWith("/activity")) {
            val year = request.url.parameters["year"]?.toIntOrNull() ?: today.year
            val month = request.url.parameters["month"]?.toIntOrNull() ?: today.monthValue
            return ok(buildJsonObject {
                put("year", year); put("month", month); put("timeZone", request.url.parameters["timeZone"] ?: "UTC")
                put("activeDays", if (populated) 1 else 0); put("totalCheckIns", visits.size)
                putJsonArray("days") { if (populated && year == today.year && month == today.monthValue) add(buildJsonObject {
                    put("date", today.toString()); put("checkInCount", visits.size)
                }) }
            })
        }
        if (path == "/api/v1/favorites" && method == HttpMethod.Get) return ok(favoriteRows())
        if (path.startsWith("/api/v1/favorites/") && method in listOf(HttpMethod.Put, HttpMethod.Delete)) {
            val parts = path.removePrefix("/api/v1/favorites/").split("/", limit = 2)
            if (parts.size != 2 || parts[0] !in listOf("coffee_shop", "roaster", "drink")) return null
            val key = parts.joinToString(":")
            if (method == HttpMethod.Put) favorites += key else favorites -= key
            return HttpStatusCode.NoContent to envelope(JsonNull)
        }
        if (path == "/api/coffeeshops" && method == HttpMethod.Get) return ok(buildJsonObject {
            put("coffeeShops", shops(request.url.parameters["q"])); put("currentPage", 1); put("totalPages", 1)
            put("totalItems", if (populated) 2 else 0)
        })
        if (path == "/api/v1/coffee-shops/search" && method == HttpMethod.Post) return ok(buildJsonObject {
            val query = input["q"]?.jsonPrimitive?.contentOrNull
            val filters = input["filters"] as? JsonObject ?: buildJsonObject {}
            val items = JsonArray(shops(query).filter { element ->
                val row = element.jsonObject
                (filters["isOpen"]?.jsonPrimitive?.booleanOrNull != true || row["isOpen"]?.jsonPrimitive?.boolean == true) &&
                    (filters["isNew"]?.jsonPrimitive?.booleanOrNull != true || row["isNew"]?.jsonPrimitive?.boolean == true) &&
                    (filters["visitedOnly"]?.jsonPrimitive?.booleanOrNull != true || row["isVisited"]?.jsonPrimitive?.boolean == true) &&
                    (filters["favoritesOnly"]?.jsonPrimitive?.booleanOrNull != true || row["isFavorite"]?.jsonPrimitive?.boolean == true)
            })
            put("items", items); put("totalItems", items.size); put("totalPages", 1); put("currentPage", 1)
        })
        if (path.startsWith("/api/coffeeshops/") && method == HttpMethod.Get) return ok(shop(path.substringAfterLast('/')))
        if (path == "/api/map" && method == HttpMethod.Get) return ok(buildJsonObject { putJsonArray("shops") {
            if (populated) add(buildJsonObject { put("address", address("sandbox-cafe", "/coffee-shops/sandbox-cafe")); put("latitude", 53.9); put("longitude", 27.56); put("title", "Sandbox Cafe") })
        } })
        if (path == "/api/roasters" && method == HttpMethod.Get) return ok(JsonArray(if (populated) listOf(roaster()) else emptyList()))
        if (path.startsWith("/api/roasters/") && method == HttpMethod.Get) return ok(roaster())
        if (path == "/api/catalogs/cities") return ok(JsonArray(listOf(buildJsonObject {
            put("address", address("minsk", "/cities/minsk")); put("name", "Минск")
        })))
        if (path == "/api/catalogs/drinks") return ok(drinks())
        if (path == "/api/menu/drinks") return ok(buildJsonObject { put("drinks", drinks()) })
        if (path == "/api/catalogs/brew-methods") return ok(JsonArray(listOf(catalog("hario-v60", "Hario V60"), catalog("aeropress", "AeroPress"))))
        if (path.startsWith("/api/catalogs/") && method == HttpMethod.Get) return ok(JsonArray(emptyList()))
        if (path == "/api/v1/catalogs/coffee-filter-values") return ok(JsonArray(emptyList()))
        if (path == "/api/v1/coffees/search") return ok(buildJsonObject {
            val query = input["q"]?.jsonPrimitive?.content.orEmpty()
            val rows = if (populated && (query.isBlank() || "Sandbox Ethiopia".contains(query, true))) listOf(coffee()) else emptyList()
            put("items", JsonArray(rows)); put("currentPage", 1); put("totalPages", 1); put("totalCount", rows.size)
        })
        if (path.startsWith("/api/v1/coffees/") && method == HttpMethod.Get) return ok(coffee())
        if (path == "/api/v1/feed") return ok(buildJsonObject {
            putJsonArray("items") {
                visits.filter { it["visibility"]?.jsonPrimitive?.content == "Public" && it["moderationState"]?.jsonPrimitive?.content == "Approved" }.forEach { row -> add(buildJsonObject {
                    put("publishedAtUtc", "${today}T07:00:00Z"); put("checkIn", row)
                }) }
            }
            put("nextCursor", JsonNull)
        })
        if (path == "/api/v1/check-ins" && method == HttpMethod.Post) {
            val id = UUID.randomUUID().toString()
            val row = visit(id, input)
            visits += row
            return ok(row)
        }
        if (path.startsWith("/api/v1/check-ins/") && method in listOf(HttpMethod.Put, HttpMethod.Delete, HttpMethod.Post)) {
            val id = path.removePrefix("/api/v1/check-ins/").substringBefore('/')
            val old = visits.firstOrNull { it["id"]?.jsonPrimitive?.content == id } ?: return null
            if (path.endsWith("/helpful") || path.endsWith("/reports")) return ok(buildJsonObject { put("helpfulCount", 1); put("isHelpfulByCurrentUser", method == HttpMethod.Put) })
            if (method == HttpMethod.Delete) { visits.remove(old); return ok(JsonNull) }
            val updated = JsonObject(old + input)
            visits[visits.indexOf(old)] = updated
            return ok(updated)
        }
        if (path.endsWith("/mine") && path.startsWith("/api/moderation")) return ok(page(JsonArray(emptyList())))
        if (method == HttpMethod.Post && path == "/api/shopissuereports") {
            return ok(buildJsonObject { put("id", UUID.randomUUID().toString()); put("status", "Pending") })
        }
        return null // An unimplemented contract is an error, never a fabricated successful write.
    }

    private fun profile() = buildJsonObject {
        put("userName", username); put("email", "sandbox@example.invalid"); put("about", about)
        put("avatarUrl", "${SandboxBackend.BASE_URL}/media/sample"); put("checkInCount", visits.size)
        put("addedShopsCount", 0); put("addedRoastersCount", 0); put("followersCount", 0); put("isFollowing", false)
        put("address", address("sandbox", "/users/sandbox")); put("createdAtUtc", "${today}T07:00:00Z")
    }
    private fun shops(query: String?) = JsonArray(if (!populated) emptyList() else
        listOf("sandbox-cafe", "sandbox-lab").map(::shop).filter { query.isNullOrBlank() || it["name"].toString().contains(query, true) })
    private fun shop(slug: String) = buildJsonObject {
        put("address", address(slug, "/coffee-shops/$slug")); put("name", if (slug == "sandbox-cafe") "Sandbox Cafe" else "Sandbox Lab")
        put("description", "Тестовая кофейня. Данные и действия только локальные.")
        put("city", address("minsk", "/cities/minsk")); put("rating", 4.5); put("checkInCount", visits.size)
        put("isOpen", slug == "sandbox-cafe"); put("isNew", slug == "sandbox-lab"); put("isVisited", slug == "sandbox-cafe" && visits.isNotEmpty())
        put("isFavorite", "coffee_shop:$slug" in favorites); put("priceRange", "Medium"); put("type", "CoffeeShop")
        put("addressLine", "Тестовая улица, 1"); put("distanceMeters", 250)
        putJsonObject("location") { put("address", "Тестовая улица, 1"); put("latitude", 53.9); put("longitude", 27.56) }
        putJsonArray("photos") { add(photo()) }; put("coverPhoto", photo())
        put("roasters", JsonArray(listOf(catalog("sandbox-roaster", "Sandbox Roaster"))))
        put("checkIns", JsonArray(visits)); put("userCheckIns", JsonArray(visits))
        putJsonObject("menu") { put("currency", "BYN"); put("photos", JsonArray(listOf(photo()))); put("items", drinks()) }
        putJsonArray("schedules") { (1..7).forEach { day -> add(buildJsonObject { put("dayOfWeek", day); putJsonArray("intervals") { add(buildJsonObject { put("openTime", "08:00:00"); put("closeTime", "23:00:00") }) } }) } }
    }
    private fun roaster() = buildJsonObject {
        put("address", address("sandbox-roaster", "/roasters/sandbox-roaster")); put("name", "Sandbox Roaster")
        put("about", "Локальный обжарщик для тестирования"); put("coffeeShopsCount", 2); put("coffeeProductsCount", 1)
        put("photoUrl", "${SandboxBackend.BASE_URL}/media/sample"); put("coverPhoto", photo())
        put("photos", JsonArray(listOf(photo()))); putJsonArray("shops") { add(shop("sandbox-cafe")) }
    }
    private fun coffee() = buildJsonObject {
        put("address", address("sandbox-ethiopia", "/coffees/sandbox-ethiopia")); put("name", "Sandbox Ethiopia")
        putJsonObject("roaster") { put("name", "Sandbox Roaster"); put("address", address("sandbox-roaster", "/roasters/sandbox-roaster")) }
        put("coverPhoto", photo()); put("photos", JsonArray(listOf(photo()))); put("description", "Тестовое зерно для локальных сценариев")
    }
    private fun favoriteRows() = JsonArray(favorites.map { key ->
        val (kind, slug) = key.split(':', limit = 2)
        buildJsonObject {
            put("kind", kind); put("createdAtUtc", "${today}T07:00:00Z"); put("name", when (kind) {
                "coffee_shop" -> "Sandbox Cafe"; "roaster" -> "Sandbox Roaster"; else -> "Капучино"
            }); put("coverPhoto", photo())
            if (kind == "drink") putJsonObject("drink") { put("slug", slug) }
            else put("address", address(slug, if (kind == "roaster") "/roasters/$slug" else "/coffee-shops/$slug"))
        }
    })
    private fun visit(id: String, input: JsonObject) = buildJsonObject {
        input.forEach { (key, value) -> put(key, value) }
        put("id", id); put("shop", address("sandbox-cafe", "/coffee-shops/sandbox-cafe")); put("shopName", "Sandbox Cafe")
        put("author", address("sandbox", "/users/sandbox")); put("username", username)
        put("createdAtUtc", "${today}T07:00:00Z"); put("visitedAt", input["visitedAt"] ?: JsonPrimitive("${today}T07:00:00Z"))
        val visibility = input["visibility"]?.jsonPrimitive?.content ?: "Private"
        put("visibility", visibility); put("moderationState", if (visibility == "Public") "Pending" else "NotSubmitted")
        putJsonArray("photos") { (input["photos"] as? JsonArray).orEmpty().forEach { value ->
            val original = value.jsonObject
            val key = original["storageKey"]?.jsonPrimitive?.content ?: return@forEach
            add(JsonObject(original + mapOf("id" to JsonPrimitive(key), "url" to JsonPrimitive("/media/$key"))))
        } }
        (input["drinkPhoto"] as? JsonObject)?.get("storageKey")?.jsonPrimitive?.content?.let { put("drinkPhotoUrl", "/media/$it") }
    }
    private fun drinks() = JsonArray(listOf(buildJsonObject { put("slug", "cappuccino"); put("nameRu", "Капучино"); put("nameEn", "Cappuccino"); put("category", "Coffee"); put("price", 8) }))
    private fun photo() = buildJsonObject { put("id", "sample"); put("url", "/media/sample"); put("fullUrl", "${SandboxBackend.BASE_URL}/media/sample") }
    private fun catalog(slug: String, name: String) = buildJsonObject { put("slug", slug); put("name", name); put("address", address(slug, "/catalog/$slug")) }
    private fun address(slug: String, path: String) = buildJsonObject {
        put("slug", slug); put("canonicalPath", path); put("revision", 0); put("isAlias", false)
    }
    private fun page(items: JsonArray) = buildJsonObject { put("items", items); put("totalCount", items.size); put("currentPage", 1); put("totalPages", 1) }
    private fun envelope(data: JsonElement, success: Boolean = true) = buildJsonObject { put("isSuccess", success); put("data", data) }
    private fun ok(data: JsonElement) = HttpStatusCode.OK to envelope(data)
}
