package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

/** Local fixture contracts for the existing app, not an alternative implementation of its UI. */
internal class SandboxApplicationBackend(private val today: LocalDate, private val visits: MutableList<JsonObject>) {
    companion object {
        const val SELF_ID = "sandbox-author-self"
        const val FRIEND_ID = "sandbox-author-friend"
        const val FRIEND_SLUG = "sandbox-friend"
        const val FRIEND_NAME = "Sandbox Friend"
        const val FRIEND_VISIT_ID = "sandbox-friend-visit"
        const val FRIEND_PHOTO_ID = "sandbox-friend-photo"
    }
    private val favorites = linkedSetOf("coffee_shop:sandbox-cafe", "roaster:sandbox-roaster", "drink:cappuccino")
    private var username = "Sandbox"
    private var about = "Локальный тестовый профиль. Все действия остаются в Sandbox."
    private var populated = true
    // Other authors' public visits live apart from the account's own rows, so history/calendar never include them.
    private val others = mutableListOf<JsonObject>()
    private val blocked = linkedSetOf<String>()
    private var blocksRevision = 0
    fun reset(populated: Boolean) {
        this.populated = populated
        others.clear(); blocked.clear(); blocksRevision++
        if (populated) others += friendVisit()
        username = "Sandbox"
        about = "Локальный тестовый профиль. Все действия остаются в Sandbox."
        favorites.clear()
        if (populated) favorites += listOf("coffee_shop:sandbox-cafe", "roaster:sandbox-roaster", "drink:cappuccino")
    }

    fun handle(request: HttpRequestData, body: JsonElement?): Pair<HttpStatusCode, JsonObject>? {
        val path = request.url.encodedPath.lowercase()
        val method = request.method
        val input = body as? JsonObject ?: buildJsonObject {}
        if (path == "/api/v1/me/blocks" && method == HttpMethod.Get) return blockPage(request)
        if (path.startsWith("/api/v1/me/blocks/") && method in listOf(HttpMethod.Put, HttpMethod.Delete)) {
            val id = path.removePrefix("/api/v1/me/blocks/")
            if (id == SELF_ID) return HttpStatusCode.BadRequest to envelope(JsonNull, false)
            if (id != FRIEND_ID) return HttpStatusCode.NotFound to envelope(JsonNull, false)
            val changed = if (method == HttpMethod.Put) blocked.add(id) else blocked.remove(id)
            if (changed) blocksRevision++
            return ok(buildJsonObject { put("authorId", id); put("isBlocked", method == HttpMethod.Put) })
        }
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
            val friend = path.removePrefix("/api/users/").substringBefore('/') == FRIEND_SLUG
            if (path.endsWith("/check-ins")) return ok(page(JsonArray(if (friend) others.filter(::visible) else visits)))
            if (path.endsWith("/followers") || path.endsWith("/following")) return HttpStatusCode.OK to buildJsonObject {
                putJsonArray("items") {}; put("totalCount", 0)
                put("page", request.url.parameters["page"]?.toIntOrNull() ?: 1)
                put("pageSize", request.url.parameters["pageSize"]?.toIntOrNull() ?: 20)
            }
            return if (friend) HttpStatusCode.OK to buildJsonObject { put("data", friendProfile()); put("address", address(FRIEND_SLUG, "/users/$FRIEND_SLUG")) }
            else HttpStatusCode.OK to buildJsonObject { put("data", profile()); put("address", address("sandbox", "/users/sandbox")) }
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
        if (path == "/api/v1/catalogs/coffee-filter-values") return ok(if (populated) coffeeFilterGroups() else JsonArray(emptyList()))
        if (path == "/api/v1/coffees/search") return ok(buildJsonObject {
            val query = input["q"]?.jsonPrimitive?.content.orEmpty().trim()
            val filters = input["filters"] as? JsonObject ?: buildJsonObject {}
            val rows = if (!populated) emptyList() else coffees().filter { row -> coffeeMatches(row, query, filters) }
            put("items", JsonArray(rows)); put("currentPage", 1); put("totalPages", 1); put("totalCount", rows.size)
        })
        if (path.startsWith("/api/v1/coffees/") && method == HttpMethod.Get) {
            if (!populated) return null
            return coffees().firstOrNull { it["address"]!!.jsonObject["slug"]!!.jsonPrimitive.content == path.substringAfterLast('/') }?.let(::ok)
        }
        if (path == "/api/v1/feed") return ok(buildJsonObject {
            putJsonArray("items") {
                (visits + others.filter(::visible)).filter { it["visibility"]?.jsonPrimitive?.content == "Public" && it["moderationState"]?.jsonPrimitive?.content == "Approved" }.forEach { row -> add(buildJsonObject {
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
            val old = visits.firstOrNull { it["id"]?.jsonPrimitive?.content == id }
                ?: others.firstOrNull { it["id"]?.jsonPrimitive?.content == id && visible(it) && (path.endsWith("/helpful") || path.endsWith("/reports")) }
                ?: return null
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

    /** Photo availability for protected check-in image URLs: a blocked author's media is not served. */
    fun protectedPhotoAvailable(checkInId: String): Boolean =
        (visits + others).firstOrNull { it["id"]?.jsonPrimitive?.content == checkInId }?.let(::visible) == true

    private fun visible(row: JsonObject) = row["authorId"]?.jsonPrimitive?.contentOrNull !in blocked
    private fun blockPage(request: HttpRequestData): Pair<HttpStatusCode, JsonObject> {
        val size = request.url.parameters["pageSize"]?.toIntOrNull()
        val offset = request.url.parameters["cursor"]?.let { it.toIntOrNull() ?: return HttpStatusCode.BadRequest to envelope(JsonNull, false) } ?: 0
        if (size == null || size !in 1..100 || offset < 0) return HttpStatusCode.BadRequest to envelope(JsonNull, false)
        val all = blocked.toList()
        return ok(buildJsonObject {
            putJsonArray("items") { all.drop(offset).take(size).forEach { id ->
                add(buildJsonObject {
                    put("authorId", id); put("userName", FRIEND_NAME); put("avatarUrl", "/media/sample")
                    put("address", address(FRIEND_SLUG, "/users/$FRIEND_SLUG"))
                })
            } }
            put("nextCursor", if (offset + size < all.size) JsonPrimitive((offset + size).toString()) else JsonNull)
            put("revision", "$blocksRevision")
        })
    }
    private fun friendProfile() = buildJsonObject {
        put("authorId", FRIEND_ID); put("userName", FRIEND_NAME); put("about", "Другой автор для проверки блокировок.")
        put("avatarUrl", "${SandboxBackend.BASE_URL}/media/sample"); put("checkInCount", 1)
        put("addedShopsCount", 0); put("addedRoastersCount", 0); put("followersCount", 0); put("isFollowing", false)
        put("isBlockedByViewer", FRIEND_ID in blocked)
        put("address", address(FRIEND_SLUG, "/users/$FRIEND_SLUG")); put("createdAtUtc", "${today}T07:00:00Z")
    }
    private fun friendVisit() = buildJsonObject {
        put("id", FRIEND_VISIT_ID); put("shopName", "Sandbox Cafe"); put("username", FRIEND_NAME)
        put("authorId", FRIEND_ID); put("author", address(FRIEND_SLUG, "/users/$FRIEND_SLUG"))
        put("authorAvatarUrl", "/media/sample"); put("shop", address("sandbox-cafe", "/coffee-shops/sandbox-cafe"))
        put("createdAtUtc", "${today}T06:00:00Z"); put("visitedAt", "${today}T06:00:00Z")
        put("text", "Публичный визит другого автора"); put("visibility", "Public"); put("moderationState", "Approved")
        put("helpfulCount", 2); put("isHelpfulByCurrentUser", false)
        putJsonObject("rating") { put("coffee", 5); put("service", 4); put("place", 5) }
        // Protected delivery shape: an owner-scoped original plus immutable feed/detail variants.
        val original = "/api/v1/check-ins/$FRIEND_VISIT_ID/photos/$FRIEND_PHOTO_ID"
        putJsonArray("photos") { add(buildJsonObject {
            put("url", original); put("sortIndex", 0)
            putJsonArray("variants") { listOf("thumb", "feed", "detail").forEach { purpose ->
                add(buildJsonObject {
                    put("purpose", purpose); put("url", "$original/variants/$purpose/1/$purpose.png")
                    put("contentType", "image/png"); put("width", 600); put("height", 600); put("sizeBytes", 3)
                })
            } }
        }) }
    }

    private fun profile() = buildJsonObject {
        put("authorId", SELF_ID); put("userName", username); put("email", "sandbox@example.invalid"); put("about", about)
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
        // Counters above stay server aggregates; only the visible cards are filtered.
        put("checkIns", JsonArray(visits + others.filter(::visible))); put("userCheckIns", JsonArray(visits))
        putJsonObject("menu") { put("currency", "BYN"); put("photos", JsonArray(listOf(photo()))); put("items", drinks()) }
        putJsonArray("schedules") { (1..7).forEach { day -> add(buildJsonObject { put("dayOfWeek", day); putJsonArray("intervals") { add(buildJsonObject { put("openTime", "08:00:00"); put("closeTime", "23:00:00") }) } }) } }
    }
    private fun roaster() = buildJsonObject {
        put("address", address("sandbox-roaster", "/roasters/sandbox-roaster")); put("name", "Sandbox Roaster")
        put("about", "Локальный обжарщик для тестирования"); put("coffeeShopsCount", 2); put("coffeeProductsCount", 1)
        put("photoUrl", "${SandboxBackend.BASE_URL}/media/sample"); put("coverPhoto", photo())
        put("photos", JsonArray(listOf(photo()))); putJsonArray("shops") { add(shop("sandbox-cafe")) }
    }
    // Three synthetic coffees with distinct tastes/roasts/availability, so filters, offers and "similar" are exercised.
    private fun coffees() = listOf(
        coffeeRow("sandbox-ethiopia", "Sandbox Ethiopia", "ET" to "Эфиопия", listOf("fruity" to "Фруктовый", "floral" to "Цветочный"), "light",
            listOf(coffeeOffer(45.0, 250, "InStock", "Sandbox Seller", "ethiopia"), coffeeOffer(80.0, 500, "PreOrder", "Sandbox Seller", "ethiopia-500"))),
        coffeeRow("sandbox-kenya", "Sandbox Kenya", "KE" to "Кения", listOf("fruity" to "Фруктовый"), "light",
            listOf(coffeeOffer(52.5, 250, "OutOfStock", "Sandbox Seller", "kenya"))),
        coffeeRow("sandbox-brazil", "Sandbox Brazil", "BR" to "Бразилия", listOf("nutty" to "Ореховый"), "medium",
            listOf(coffeeOffer(38.0, 1000, "InStock", "Sandbox Roastery Shop", "brazil"))),
    )
    private fun coffeeOffer(price: Double, grams: Int, availability: String, seller: String, key: String) = buildJsonObject {
        put("price", price); put("currency", "BYN"); put("weightGrams", grams); put("offerKey", key)
        put("availability", availability); put("sellerName", seller); put("sourceUrl", "https://seller.sandbox.invalid/$key")
        put("grind", "Beans"); put("availabilityScope", "Seller"); put("checkedAtUtc", "${today}T06:00:00Z")
    }
    private fun coffeeRow(slug: String, name: String, country: Pair<String, String>, tastes: List<Pair<String, String>>, roast: String, offers: List<JsonObject>) = buildJsonObject {
        put("address", address(slug, "/coffees/$slug")); put("name", name)
        putJsonObject("roaster") { put("name", "Sandbox Roaster"); put("address", address("sandbox-roaster", "/roasters/sandbox-roaster")) }
        put("coverPhoto", photo()); put("photos", JsonArray(listOf(photo()))); put("description", "Тестовое зерно для локальных сценариев")
        putJsonArray("countries") { add(buildJsonObject { put("code", country.first); put("nameRu", country.second) }) }
        putJsonObject("classification") { putJsonArray("tasteGroups") { tastes.forEach { add(it.first) } }; put("roastLevel", roast) }
        put("offers", JsonArray(offers)); put("productKind", "Coffee"); put("productForm", "Beans")
        putJsonArray("tasteDescriptors") { tastes.forEach { add(it.second) } }; put("catalogCheckedAtUtc", "${today}T06:00:00Z")
    }
    private fun coffeeMatches(row: JsonObject, query: String, filters: JsonObject): Boolean {
        val name = row["name"]!!.jsonPrimitive.content
        val classification = row["classification"]!!.jsonObject
        fun selected(code: String) = (filters[code] as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
        val tastes = classification["tasteGroups"]!!.jsonArray.map { it.jsonPrimitive.content }
        val roast = classification["roastLevel"]!!.jsonPrimitive.content
        val inStock = row["offers"]!!.jsonArray.any { it.jsonObject["availability"]?.jsonPrimitive?.content == "InStock" }
        return (query.isBlank() || name.contains(query, true)) &&
            (selected("taste").isEmpty() || selected("taste").any { it in tastes }) &&
            (selected("roast").isEmpty() || roast in selected("roast")) &&
            (filters["availableOnly"]?.jsonPrimitive?.booleanOrNull != true || inStock)
    }
    private fun coffeeFilterGroups() = JsonArray(listOf(
        buildJsonObject { put("code", "taste"); put("name", "Вкус"); putJsonArray("values") {
            add(buildJsonObject { put("code", "fruity"); put("name", "Фруктовый"); put("sortOrder", 1) })
            add(buildJsonObject { put("code", "floral"); put("name", "Цветочный"); put("sortOrder", 2) })
            add(buildJsonObject { put("code", "nutty"); put("name", "Ореховый"); put("sortOrder", 3) })
        } },
        buildJsonObject { put("code", "roast"); put("name", "Обжарка"); putJsonArray("values") {
            add(buildJsonObject { put("code", "light"); put("name", "Светлая"); put("sortOrder", 1) })
            add(buildJsonObject { put("code", "medium"); put("name", "Средняя"); put("sortOrder", 2) })
        } },
    ))
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
        input.forEach { (key, value) -> if (key != "drinkPhoto") put(key, value) }
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
