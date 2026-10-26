package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** What `GET /api/v1/app-version` answers. Versions are absolute so they do not depend on the installed Sandbox build. */
internal enum class SandboxUpdatePolicy { UpToDate, Available, Required, Failure }

/** What `GET /api/catalogs/cities` answers: one city (default), several, none, or a server error. */
internal enum class SandboxCities { Single, Several, Empty, Failure }

/** How the account endpoints answer: everything works, or one chosen failure (offline and 5xx come from the scenario chips). */
internal enum class SandboxAuthPolicy { Works, WrongPassword, EmailTaken, RegistrationFailure, RecoveryFailure }

/**
 * What `GET /api/v1/feed` answers. `Single` (default) is one page with the visible public fixtures; the others add 45 generated posts of several authors
 * (some by the blockable friend, some home check-ins) behind a cursor: `Paged` works, `PageFailsOnce` fails the first request for a later page and then recovers,
 * `InvalidCursor` rejects every later page like an expired cursor (the app must restart from the first page), `Empty` has no posts, `Failure` answers 500.
 */
internal enum class SandboxFeed { Single, Paged, PageFailsOnce, InvalidCursor, Empty, Failure }

/** Local fixture contracts for the existing app, not an alternative implementation of its UI. */
internal class SandboxApplicationBackend(private val today: LocalDate, private val visits: MutableList<JsonObject>) {
    companion object {
        const val SELF_ID = "sandbox-author-self"
        const val FRIEND_ID = "sandbox-author-friend"
        const val FRIEND_SLUG = "sandbox-friend"
        const val FRIEND_NAME = "Sandbox Friend"
        const val FRIEND_VISIT_ID = "sandbox-friend-visit"
        const val FRIEND_PHOTO_ID = "sandbox-friend-photo"
        const val RESEND_WINDOW_SECONDS = 60L
    }
    private val shopNames = mapOf("sandbox-cafe" to "Sandbox Cafe", "sandbox-lab" to "Sandbox Lab")
    private val contributionStore = SandboxContributionsBackend(today, shopNames.keys)
    private val contentReports = SandboxContentReportsBackend()
    val submittedReports get() = contentReports.records
    private val notificationStore = SandboxNotificationsBackend(today)
    private val discoveryStore = SandboxDiscoveryBackend({ populated }, { favorites }, ::photo, ::address, ::catalog)
    private val favorites = linkedSetOf("coffee_shop:sandbox-cafe", "roaster:sandbox-roaster", "drink:cappuccino")
    private var username = "Sandbox"
    private var about = "Локальный тестовый профиль. Все действия остаются в Sandbox."
    private var populated = true
    // Other authors' public visits live apart from the account's own rows, so history/calendar never include them.
    private val others = mutableListOf<JsonObject>()
    private val blocked = linkedSetOf<String>()
    private var blocksRevision = 0
    private var friendFollowing = false
    private var deletionRequest: JsonObject? = null
    // Settings fixtures: chosen from the Sandbox controls, kept across scenario changes and cleared by reset().
    @Volatile var updatePolicy = SandboxUpdatePolicy.UpToDate
    @Volatile var cities = SandboxCities.Single
    @Volatile var authPolicy = SandboxAuthPolicy.Works
    @Volatile var feed = SandboxFeed.Single
    var discovery: SandboxDiscovery
        get() = discoveryStore.mode
        set(value) { discoveryStore.mode = value }
    var contributions: SandboxContributions
        get() = contributionStore.mode
        set(value) = contributionStore.select(value, populated)
    private var laterPageFailed = false
    fun reset(populated: Boolean) {
        contentReports.clear()
        updatePolicy = SandboxUpdatePolicy.UpToDate; cities = SandboxCities.Single; authPolicy = SandboxAuthPolicy.Works
        feed = SandboxFeed.Single; laterPageFailed = false; generated = null
        this.populated = populated
        contributionStore.reset(populated)
        notificationStore.reset(populated)
        discoveryStore.reset()
        others.clear(); blocked.clear(); blocksRevision++
        friendFollowing = false; deletionRequest = null
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
        notificationStore.handle(path, method, input)?.let { return it }
        if (path == "/api/v1/me/blocks" && method == HttpMethod.Get) return blockPage(request)
        if (path.startsWith("/api/v1/me/blocks/") && method in listOf(HttpMethod.Put, HttpMethod.Delete)) {
            val id = path.removePrefix("/api/v1/me/blocks/")
            if (id == SELF_ID) return HttpStatusCode.BadRequest to envelope(JsonNull, false)
            if (id != FRIEND_ID) return HttpStatusCode.NotFound to envelope(JsonNull, false)
            val changed = if (method == HttpMethod.Put) blocked.add(id) else blocked.remove(id)
            if (changed) blocksRevision++
            return ok(buildJsonObject { put("authorId", id); put("isBlocked", method == HttpMethod.Put) })
        }
        contributionStore.handle(path, method, request, input)?.let { return it }
        discoveryStore.handle(path, method, request, input)?.let { return it }
        authRoute(path, method, input)?.let { return it }
        if (path == "/api/users/me" && method == HttpMethod.Get) return ok(profile())
        if (path == "/api/users/me/username" && method == HttpMethod.Patch) {
            val requested = input["username"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (requested.length !in 2..64) return HttpStatusCode.BadRequest to failure("Имя должно быть от 2 до 64 символов")
            if (requested.equals(FRIEND_NAME, ignoreCase = true)) return HttpStatusCode.Conflict to failure("Имя уже занято")
            username = requested
            return ok(buildJsonObject { put("username", username); put("address", address("sandbox", "/users/sandbox")) })
        }
        if (path == "/api/users/me/about" && method == HttpMethod.Patch) {
            val requested = input["about"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (requested.length > 600) return HttpStatusCode.BadRequest to failure("Описание слишком длинное")
            about = requested; return ok(JsonNull)
        }
        if (path == "/api/users/me/avatar" && method == HttpMethod.Patch) return ok(JsonNull)
        // DELETE only requests deletion (the account stays active until the e-mailed link is confirmed); the request is kept in RAM.
        if (path == "/api/users/me" && method == HttpMethod.Delete) {
            val request = buildJsonObject {
                put("requestId", UUID.randomUUID().toString()); put("status", "Pending")
                put("expiresAtUtc", Instant.now().plusSeconds(24 * 3600).toString())
                put("resendAvailableAtUtc", Instant.now().plusSeconds(RESEND_WINDOW_SECONDS).toString())
            }
            deletionRequest = request
            return ok(request)
        }
        if (path == "/api/users/me/deletion-request" && method == HttpMethod.Get) {
            return deletionRequest?.let(::ok) ?: (HttpStatusCode.NotFound to envelope(JsonNull, false))
        }
        if (path.startsWith("/api/users/") && path.endsWith("/follow") && method in listOf(HttpMethod.Put, HttpMethod.Delete)) {
            return when (path.removePrefix("/api/users/").removeSuffix("/follow")) {
                FRIEND_SLUG -> {
                    friendFollowing = method == HttpMethod.Put
                    HttpStatusCode.OK to buildJsonObject { put("followersCount", friendFollowers()); put("isFollowing", friendFollowing) }
                }
                "sandbox" -> HttpStatusCode.BadRequest to failure("Нельзя подписаться на себя")
                else -> HttpStatusCode.NotFound to envelope(JsonNull, false)
            }
        }
        if (path.startsWith("/api/users/") && method == HttpMethod.Get) {
            val slug = path.removePrefix("/api/users/").substringBefore('/')
            if (slug != FRIEND_SLUG && slug != "sandbox") return HttpStatusCode.NotFound to envelope(JsonNull, false)
            val friend = slug == FRIEND_SLUG
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
        if (path == "/api/v1/coffee-shops/search" && method == HttpMethod.Post) return ok(buildJsonObject {
            val query = input["q"]?.jsonPrimitive?.contentOrNull
            val filters = input["filters"] as? JsonObject ?: buildJsonObject {}
            val items = JsonArray(shops(query).filter { element ->
                val row = element.jsonObject
                (filters["isOpen"]?.jsonPrimitive?.booleanOrNull != true || row["isOpen"]?.jsonPrimitive?.boolean == true) &&
                    (filters["isNew"]?.jsonPrimitive?.booleanOrNull != true || row["isNew"]?.jsonPrimitive?.boolean == true) &&
                    (filters["visitedOnly"]?.jsonPrimitive?.booleanOrNull != true || row["isVisited"]?.jsonPrimitive?.boolean == true) &&
                    (filters["favoritesOnly"]?.jsonPrimitive?.booleanOrNull != true || row["isFavorite"]?.jsonPrimitive?.boolean == true)
            }.map { searchCard(it.jsonObject, filters["origin"] as? JsonObject) })
            put("items", items); put("totalItems", items.size); put("totalPages", 1); put("currentPage", 1)
        })
        if (path.startsWith("/api/coffeeshops/") && method == HttpMethod.Get) {
            val slug = path.substringAfterLast('/')
            return if (slug in shopNames) ok(shop(slug)) else null
        }
        if (path == "/api/map" && method == HttpMethod.Get) return ok(buildJsonObject { putJsonArray("shops") {
            if (populated) add(buildJsonObject { put("address", address("sandbox-cafe", "/coffee-shops/sandbox-cafe")); put("latitude", 53.9); put("longitude", 27.56); put("title", "Sandbox Cafe") })
        } })
        if (path == "/api/roasters" && method == HttpMethod.Get) return ok(JsonArray(if (populated) roasters() else emptyList()))
        if (path.startsWith("/api/roasters/") && method == HttpMethod.Get) {
            if (!populated) return null
            return roasters().firstOrNull { it["address"]!!.jsonObject["slug"]!!.jsonPrimitive.content == path.substringAfterLast('/') }?.let(::ok)
        }
        if (path == "/api/catalogs/cities") return when (cities) {
            SandboxCities.Failure -> HttpStatusCode.InternalServerError to envelope(JsonNull, false)
            SandboxCities.Empty -> ok(JsonArray(emptyList()))
            SandboxCities.Single -> ok(JsonArray(listOf(city("minsk", "Минск"))))
            SandboxCities.Several -> ok(JsonArray(listOf(city("minsk", "Минск"), city("brest", "Брест"), city("grodno", "Гродно"))))
        }
        if (path == "/api/v1/app-version" && method == HttpMethod.Get) return appVersion()
        if (path == "/api/catalogs/drinks") return ok(drinks())
        if (path == "/api/menu/drinks") return ok(buildJsonObject { put("drinks", drinks()) })
        if (path == "/api/catalogs/brew-methods") return ok(JsonArray(listOf(catalog("hario-v60", "Hario V60"), catalog("aeropress", "AeroPress"))))
        if (path == "/api/catalogs/equipments") return ok(JsonArray(if (populated) listOf(catalog("espresso-machine", "Эспрессо-машина"), catalog("grinder", "Кофемолка")) else emptyList()))
        if (path == "/api/catalogs/beans") return ok(JsonArray(if (populated) listOf(catalog("arabica", "Арабика"), catalog("robusta", "Робуста")) else emptyList()))
        if (path == "/api/catalogs/shop-tags") return ok(JsonArray(if (populated) listOf(catalog("wifi", "Wi-Fi"), catalog("pets", "Можно с животными")) else emptyList()))
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
        if (path == "/api/v1/feed") return feedPage(request)
        if (path == "/api/v1/check-ins" && method == HttpMethod.Post) {
            val shopSlug = (input["coffeeShopSlug"] as? JsonPrimitive)?.contentOrNull?.takeIf(shopNames::containsKey)
                ?: return HttpStatusCode.BadRequest to envelope(JsonNull, false)
            val id = UUID.randomUUID().toString()
            val row = visit(id, shopSlug, input)
            visits += row
            return ok(row)
        }
        if (path.startsWith("/api/v1/check-ins/") && method in listOf(HttpMethod.Put, HttpMethod.Delete, HttpMethod.Post)) {
            val id = path.removePrefix("/api/v1/check-ins/").substringBefore('/')
            val old = visits.firstOrNull { it["id"]?.jsonPrimitive?.content == id }
                ?: others.firstOrNull { it["id"]?.jsonPrimitive?.content == id && visible(it) && (path.endsWith("/helpful") || path.endsWith("/reports")) }
                ?: generatedFeed.firstOrNull { it["id"]?.jsonPrimitive?.content == id && visible(it) && (path.endsWith("/helpful") || path.endsWith("/reports")) }
                ?: return null
            if (path.endsWith("/helpful")) return when (method) {
                HttpMethod.Put, HttpMethod.Delete -> setHelpful(old, method == HttpMethod.Put)
                else -> HttpStatusCode.MethodNotAllowed to envelope(JsonNull, false)
            }
            if (path.endsWith("/reports")) return contentReports.submit(method, id,
                old in visits || old["authorId"]?.jsonPrimitive?.content == SELF_ID, input["text"]?.jsonPrimitive?.content.orEmpty())
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

    private fun feedItem(row: JsonObject) = buildJsonObject { put("publishedAtUtc", row["createdAtUtc"] ?: JsonPrimitive("${today}T07:00:00Z")); put("checkIn", row) }
    /** Page size and cursor are validated like the real endpoint; the cursor is the offset into the visible fixtures. */
    private fun feedPage(request: HttpRequestData): Pair<HttpStatusCode, JsonObject> {
        if (feed == SandboxFeed.Failure) return HttpStatusCode.InternalServerError to failure("Не удалось загрузить ленту")
        fun page(rows: List<JsonObject>, next: String?) = ok(buildJsonObject {
            put("items", JsonArray(rows.map(::feedItem))); put("nextCursor", next?.let(::JsonPrimitive) ?: JsonNull)
        })
        if (feed == SandboxFeed.Empty) return page(emptyList(), null)
        val base = (visits + others.filter(::visible)).filter(::isPublished)
        if (feed == SandboxFeed.Single) return page(base, null)
        val size = request.url.parameters["pageSize"]?.toIntOrNull()?.takeIf { it in 1..100 } ?: 20
        val cursor = request.url.parameters["cursor"]
        val offset = cursor?.let { it.toIntOrNull()?.takeIf { value -> value >= 0 } ?: return HttpStatusCode.BadRequest to failure("Лента изменилась. Обновите её с начала") } ?: 0
        if (cursor != null && feed == SandboxFeed.InvalidCursor) return HttpStatusCode.BadRequest to failure("Cursor expired")
        if (cursor != null && feed == SandboxFeed.PageFailsOnce && !laterPageFailed) {
            laterPageFailed = true
            return HttpStatusCode.ServiceUnavailable to failure("Не удалось загрузить ленту")
        }
        val all = base + generatedFeed.filter(::visible)
        return page(all.drop(offset).take(size), if (offset + size < all.size) (offset + size).toString() else null)
    }

    /** 45 public posts by the blockable friend (every third) and two other authors; every fifth is a home preparation. */
    private var generated: MutableList<JsonObject>? = null
    private val generatedFeed: MutableList<JsonObject> get() = generated ?: buildGeneratedFeed().also { generated = it }
    private fun buildGeneratedFeed(): MutableList<JsonObject> =
        (1..45).map { index ->
            val friend = index % 3 == 0
            val slug = if (friend) FRIEND_SLUG else "sandbox-reader-${index % 3}"
            val authorId = if (friend) FRIEND_ID else "sandbox-author-reader-${index % 3}"
            val home = index % 5 == 0
            buildJsonObject {
                put("id", "sandbox-feed-$index"); put("shopName", "Sandbox Cafe"); put("username", if (friend) FRIEND_NAME else "Sandbox Reader ${index % 3}")
                put("authorId", authorId); put("author", address(slug, "/users/$slug")); put("authorAvatarUrl", "/media/sample")
                put("shop", address("sandbox-cafe", "/coffee-shops/sandbox-cafe"))
                put("createdAtUtc", "${today}T05:${(59 - index).toString().padStart(2, '0')}:00Z"); put("visitedAt", "${today}T05:00:00Z")
                put("text", "Запись ленты номер $index"); put("visibility", "Public"); put("moderationState", "Approved")
                put("helpfulCount", index % 4); put("isHelpfulByCurrentUser", false)
                putJsonObject("rating") { put("coffee", 4); put("service", 4); put("place", 5) }
                if (home) {
                    put("kind", "Home")
                    putJsonObject("home") { put("preparedOn", today.toString()); put("brewMethodCode", "hario-v60"); put("beanName", "Sandbox Bean $index"); put("doseGrams", 15.0); put("brewDurationSeconds", 150); put("tasteScore", 4) }
                }
            }
        }.toMutableList()
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
    private fun friendFollowers() = if (friendFollowing) 1 else 0
    private fun friendProfile() = buildJsonObject {
        put("authorId", FRIEND_ID); put("userName", FRIEND_NAME); put("about", "Другой автор для проверки блокировок.")
        put("avatarUrl", "${SandboxBackend.BASE_URL}/media/sample"); put("checkInCount", 1)
        put("addedShopsCount", 0); put("addedRoastersCount", 0); put("followersCount", friendFollowers()); put("isFollowing", friendFollowing)
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

    private fun setHelpful(visit: JsonObject, helpful: Boolean): Pair<HttpStatusCode, JsonObject> {
        val previous = visit["isHelpfulByCurrentUser"]?.jsonPrimitive?.booleanOrNull == true
        val delta = when {
            previous == helpful -> 0
            helpful -> 1
            else -> -1
        }
        val count = ((visit["helpfulCount"]?.jsonPrimitive?.intOrNull ?: 0) + delta).coerceAtLeast(0)
        val source = when {
            visit in visits -> visits
            visit in others -> others
            else -> generatedFeed
        }
        source[source.indexOf(visit)] = JsonObject(visit + mapOf(
            "isHelpfulByCurrentUser" to JsonPrimitive(helpful), "helpfulCount" to JsonPrimitive(count)))
        return ok(buildJsonObject { put("isHelpful", helpful); put("helpfulCount", count) })
    }

    private fun isPublished(visit: JsonObject) = visit["visibility"]?.jsonPrimitive?.content == "Public" &&
        visit["moderationState"]?.jsonPrimitive?.content == "Approved"

    private fun profile() = buildJsonObject {
        put("authorId", SELF_ID); put("userName", username); put("email", "sandbox@example.invalid"); put("about", about)
        put("avatarUrl", "${SandboxBackend.BASE_URL}/media/sample"); put("checkInCount", visits.size)
        put("addedShopsCount", 0); put("addedRoastersCount", 0); put("followersCount", 0); put("isFollowing", false)
        put("address", address("sandbox", "/users/sandbox")); put("createdAtUtc", "${today}T07:00:00Z")
    }
    private fun searchCard(row: JsonObject, origin: JsonObject?) = buildJsonObject {
        val keys = listOf("address", "name", "city", "addressLine", "coverPhoto", "rating", "checkInCount", "isOpen", "isVisited", "isFavorite", "tags")
        keys.forEach { key -> row[key]?.let { put(key, it) } }
        if (origin != null) row["distanceMeters"]?.let { put("distanceMeters", it) }
    }

    private fun shops(query: String?) = JsonArray(if (!populated) emptyList() else
        shopNames.keys.map(::shop).filter { query.isNullOrBlank() || it["name"].toString().contains(query, true) })
    private fun shop(slug: String) = buildJsonObject {
        val shopVisits = visits.filter { it["shop"]?.jsonObject?.get("slug")?.jsonPrimitive?.contentOrNull == slug }
        val publicVisits = (shopVisits + others.filter { it["shop"]?.jsonObject?.get("slug")?.jsonPrimitive?.contentOrNull == slug })
            .filter(::isPublished)
        put("address", address(slug, "/coffee-shops/$slug")); put("name", shopNames.getValue(slug))
        put("description", "Тестовая кофейня. Данные и действия только локальные.")
        put("city", address("minsk", "/cities/minsk")); put("rating", 4.5); put("checkInCount", publicVisits.size)
        put("isOpen", slug == "sandbox-cafe"); put("isNew", slug == "sandbox-lab"); put("isVisited", shopVisits.isNotEmpty())
        put("isFavorite", "coffee_shop:$slug" in favorites); put("priceRange", "Medium"); put("type", "CoffeeShop")
        put("addressLine", "Тестовая улица, 1"); put("distanceMeters", 250)
        putJsonObject("location") { put("address", "Тестовая улица, 1"); put("latitude", 53.9); put("longitude", 27.56) }
        putJsonArray("photos") { add(photo()) }; put("coverPhoto", photo())
        put("roasters", JsonArray(listOf(catalog("sandbox-roaster", "Sandbox Roaster"))))
        // Counters above stay server aggregates; only the visible cards are filtered.
        put("checkIns", JsonArray(publicVisits.filter(::visible))); put("userCheckIns", JsonArray(shopVisits))
        putJsonObject("menu") { put("currency", "BYN"); put("photos", JsonArray(listOf(photo()))); put("items", drinks()) }
        putJsonArray("schedules") { (1..7).forEach { day -> add(buildJsonObject { put("dayOfWeek", day); putJsonArray("intervals") { add(buildJsonObject { put("openTime", "08:00:00"); put("closeTime", "23:00:00") }) } }) } }
    }
    // Three synthetic roasters: full data (tags, contacts, shops with and without photos), partial data, and bare.
    private fun roasters() = listOf(
        roasterRow("sandbox-roaster", "Sandbox Roaster", about = "Локальный обжарщик для тестирования", withPhotos = true,
            tags = listOf("light" to "Светлая обжарка", "decaf" to "Декаф"), shops = listOf("sandbox-cafe" to true, "sandbox-lab" to false),
            instagram = "@sandbox.roaster", site = "sandbox-roaster.example/about", location = "Минск, Тестовая улица, 5", products = 12),
        roasterRow("sandbox-light", "Sandbox Light Lab", about = "Светлая обжарка под фильтр", withPhotos = false,
            tags = listOf("light" to "Светлая обжарка"), shops = listOf("sandbox-cafe" to true),
            instagram = null, site = "https://light.sandbox.invalid", location = null, products = 4),
        roasterRow("sandbox-decaf", "Sandbox Decaf", about = null, withPhotos = false,
            tags = listOf("decaf" to "Декаф"), shops = emptyList(), instagram = null, site = null, location = null, products = 0),
    )
    private fun roasterRow(slug: String, name: String, about: String?, withPhotos: Boolean, tags: List<Pair<String, String>>,
        shops: List<Pair<String, Boolean>>, instagram: String?, site: String?, location: String?, products: Int) = buildJsonObject {
        put("address", address(slug, "/roasters/$slug")); put("name", name)
        about?.let { put("about", it) }
        put("coffeeShopsCount", shops.size); put("coffeeProductsCount", products); put("availableCoffeeProducts", products / 2)
        if (withPhotos) { put("photoUrl", "${SandboxBackend.BASE_URL}/media/sample"); put("coverPhoto", photo()) }
        putJsonArray("tags") { tags.forEachIndexed { index, (tag, label) ->
            add(buildJsonObject { put("slug", tag); put("name", label); put("sortOrder", index + 1) })
        } }
        location?.let { putJsonObject("location") { put("address", it); put("latitude", 53.9); put("longitude", 27.56) } }
        if (instagram != null || site != null) putJsonObject("contact") { instagram?.let { put("instagramLink", it) }; site?.let { put("siteLink", it) } }
        put("photos", JsonArray(if (withPhotos) listOf(photo(), photo()) else emptyList()))
        putJsonArray("shops") { shops.forEach { (shopSlug, hasPhoto) -> add(roasterShop(shopSlug, hasPhoto)) } }
    }
    // Only some shops carry a cover photo, so the app's shop-photo enrichment is exercised.
    private fun roasterShop(slug: String, withPhoto: Boolean) = buildJsonObject {
        put("address", address(slug, "/coffee-shops/$slug")); put("name", if (slug == "sandbox-cafe") "Sandbox Cafe" else "Sandbox Lab")
        if (withPhoto) put("coverPhoto", photo())
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
                "coffee_shop" -> "Sandbox Cafe"
                "roaster" -> roasters().firstOrNull { it["address"]!!.jsonObject["slug"]!!.jsonPrimitive.content == slug }?.get("name")?.jsonPrimitive?.content ?: "Sandbox Roaster"
                else -> "Капучино"
            }); put("coverPhoto", photo())
            if (kind == "drink") putJsonObject("drink") { put("slug", slug) }
            else put("address", address(slug, if (kind == "roaster") "/roasters/$slug" else "/coffee-shops/$slug"))
        }
    })
    private fun visit(id: String, shopSlug: String, input: JsonObject) = buildJsonObject {
        input.forEach { (key, value) -> if (key != "drinkPhoto") put(key, value) }
        put("id", id); put("shop", address(shopSlug, "/coffee-shops/$shopSlug")); put("shopName", shopNames.getValue(shopSlug))
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
    private fun city(slug: String, name: String) = buildJsonObject {
        put("address", address(slug, "/cities/$slug")); put("name", name)
    }

    private fun appVersion(): Pair<HttpStatusCode, JsonObject> {
        val url = "https://sandbox.coffeepeek.invalid/downloads/coffeepeek.apk"
        fun policy(version: Long, minimum: Long) = ok(buildJsonObject { put("versionCode", version); put("minVersionCode", minimum); put("url", url) })
        return when (updatePolicy) {
            SandboxUpdatePolicy.UpToDate -> policy(1, 0)
            SandboxUpdatePolicy.Available -> policy(999_999_999, 0)
            SandboxUpdatePolicy.Required -> policy(999_999_999, 999_999_999)
            SandboxUpdatePolicy.Failure -> HttpStatusCode.ServiceUnavailable to envelope(JsonNull, false)
        }
    }

    private fun address(slug: String, path: String) = buildJsonObject {
        put("slug", slug); put("canonicalPath", path); put("revision", 0); put("isAlias", false)
    }
    private fun page(items: JsonArray) = buildJsonObject { put("items", items); put("totalCount", items.size); put("currentPage", 1); put("totalPages", 1) }
    // Sign-in, registration and password recovery. Only the e-mail/password path exists: OAuth is blocked like every external service.
    private fun authRoute(path: String, method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject>? = when {
        path == "/api/tokens" && method == HttpMethod.Post ->
            if (authPolicy == SandboxAuthPolicy.WrongPassword) HttpStatusCode.Unauthorized to failure("Invalid credentials")
            else ok(buildJsonObject { put("accessToken", accessToken()); put("refreshToken", "sandbox-refresh-token") })
        path == "/api/tokens" && method == HttpMethod.Delete -> ok(JsonNull)
        path.startsWith("/api/tokens/") -> HttpStatusCode.ServiceUnavailable to failure("OAuth is disabled in offline Sandbox")
        path == "/api/users" && method == HttpMethod.Post ->
            if (authPolicy == SandboxAuthPolicy.RegistrationFailure) HttpStatusCode.BadRequest to failure("Registration rejected") else ok(JsonNull)
        path == "/api/users/exists" && method == HttpMethod.Get -> ok(JsonPrimitive(authPolicy == SandboxAuthPolicy.EmailTaken))
        path == "/api/users/password/forgot" && method == HttpMethod.Post ->
            if (authPolicy == SandboxAuthPolicy.RecoveryFailure) HttpStatusCode.InternalServerError to failure("Recovery unavailable") else ok(JsonNull)
        else -> null
    }

    // A JWT whose subject is the Sandbox account, so the real session code derives the same user id as the local session.
    private fun accessToken(): String {
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        fun part(json: String) = encoder.encodeToString(json.toByteArray())
        return "${part("""{"alg":"none"}""")}.${part("""{"sub":"11111111-1111-4111-8111-111111111111","exp":4102444800}""")}.sandbox"
    }

    private fun envelope(data: JsonElement, success: Boolean = true) = buildJsonObject { put("isSuccess", success); put("data", data) }
    private fun failure(message: String) = buildJsonObject { put("isSuccess", false); put("data", JsonNull); put("message", message) }
    private fun ok(data: JsonElement) = HttpStatusCode.OK to envelope(data)
}
