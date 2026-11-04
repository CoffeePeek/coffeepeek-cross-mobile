package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

/** RAM-only mutable lists. Synthetic owner headers are added only by the Sandbox composition guard. */
internal class SandboxSavedCollectionsBackend(private val today: LocalDate, private val available: () -> JsonArray,
    private val uploaded: (String, Long) -> Boolean, private val initialFavorites: (String) -> JsonArray,
    private val favoritesChanged: (String, Set<String>, Set<String>) -> Unit) {
    private data class Row(val owner: String, val id: String, val title: String, val description: String,
        val visibility: String, val items: List<JsonObject>, val revision: Long = 1, val cover: JsonObject? = null,
        val isDefault: Boolean = false)
    private data class Reservation(val owner: String, val key: String, val size: Long, val mime: String)
    private val rows = linkedMapOf<String, Row>()
    private val featured = mutableMapOf<String, String>()
    private val reservations = mutableMapOf<String, Reservation>()

    fun reset() {
        rows.clear()
        featured.clear()
        reservations.clear()
    }

    fun handle(request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        val path = request.url.encodedPath
        val owner = request.headers[OWNER]
        publicRead(path, request.method)?.let { return it }
        if (!path.startsWith(ROOT)) return null
        if (owner.isNullOrBlank()) return fail(HttpStatusCode.Unauthorized)
        val before = mutationTargets(owner, request)
        val result = ownedRequest(owner, request, input)
        if (result.first.isSuccess() && request.method != HttpMethod.Get)
            favoritesChanged(owner, before + mutationTargets(owner, request, result.second), membershipKeys(owner))
        return result
    }

    private fun ownedRequest(owner: String, request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val path = request.url.encodedPath
        if (path == "$ROOT/default" && request.method == HttpMethod.Put) return ensureDefault(owner)
        if (path == "$ROOT/featured") return featured(owner, request.method, input)
        if (path.startsWith("$ROOT/membership/")) return if (request.method == HttpMethod.Put) membership(owner, path, input)
            else fail(HttpStatusCode.MethodNotAllowed)
        if (path == ROOT) return if (request.method == HttpMethod.Post) create(owner, input)
            else ok(summaries(owner, request.url.parameters["kind"], request.url.parameters["slug"]))
        val parts = path.removePrefix("$ROOT/").split('/')
        val row = rows[key(parts.first())]?.takeIf { it.owner == owner } ?: return fail(HttpStatusCode.NotFound)
        return owned(row, request.method, parts, input)
    }

    private fun ensureDefault(owner: String) = ok(json(defaultRow(owner)))

    private fun defaultRow(owner: String): Row {
        val existing = rows.values.firstOrNull { it.owner == owner && it.isDefault }
            ?: rows.values.firstOrNull { it.owner == owner && it.title == "Избранное" }
        if (existing?.isDefault == true) return existing
        val row = existing?.copy(isDefault = true)
            ?: Row(owner, key(UUID.randomUUID().toString()), "Избранное", "", "private", emptyList(), isDefault = true)
        val known = membershipKeys(owner)
        val legacy = if (row.visibility == "private") initialFavorites(owner).map { it.jsonObject }
            .filter { value(it, "kind") in listOf("coffee_shop", "roaster") && itemKey(it) !in known } else emptyList()
        val canonical = row.copy(items = row.items + legacy)
        rows[row.id] = canonical
        return canonical
    }

    fun setLegacyFavorite(owner: String, kind: String, slug: String, selected: Boolean): Boolean {
        val target = "$kind:$slug"
        val item = (available().map { it.jsonObject } + rows.values.filter { it.owner == owner }.flatMap { it.items })
            .firstOrNull { itemKey(it) == target } ?: return false
        if (selected) {
            val row = defaultRow(owner)
            if (row.visibility == "private" && row.items.none { itemKey(it) == target })
                rows[row.id] = row.copy(items = row.items + item, revision = row.revision + 1)
        } else rows.values.filter { it.owner == owner && it.items.any { item -> itemKey(item) == target } }.toList().forEach { row ->
            rows[row.id] = row.copy(items = row.items.filterNot { itemKey(it) == target }, revision = row.revision + 1)
        }
        favoritesChanged(owner, setOf(target), if (selected) setOf(target) else emptySet())
        return true
    }

    private fun membershipKeys(owner: String) = rows.values.filter { it.owner == owner }.flatMap { it.items }.map(::itemKey).toSet()

    private fun mutationTargets(owner: String, request: HttpRequestData, response: JsonObject? = null): Set<String> {
        val path = request.url.encodedPath
        if (path.startsWith("$ROOT/membership/")) return setOf(path.removePrefix("$ROOT/membership/").replace('/', ':'))
        if (path == ROOT && request.method == HttpMethod.Post) {
            val items = (response?.get("data") as? JsonObject)?.get("items") as? JsonArray
            return items.orEmpty().map { itemKey(it.jsonObject) }.toSet()
        }
        if (request.method != HttpMethod.Delete && !(path.endsWith("/items") && request.method == HttpMethod.Put)) return emptySet()
        val id = path.removePrefix("$ROOT/").substringBefore('/')
        return rows[key(id)]?.takeIf { it.owner == owner }?.items.orEmpty().map(::itemKey).toSet()
    }

    private fun publicRead(path: String, method: HttpMethod): Pair<HttpStatusCode, JsonObject>? {
        if (method != HttpMethod.Get) return null
        if (path.startsWith("/api/v1/profiles/") && path.endsWith("/collection")) {
            val slug = path.removePrefix("/api/v1/profiles/").substringBefore('/')
            val owner = if (slug == "sandbox") featured.keys.firstOrNull { it == "sandbox" || it == LOCAL_ACCOUNT } else slug
            return ok(owner?.let { featured[it] }?.let(rows::get)?.takeIf { it.visibility == "public" }?.let(::json) ?: JsonNull)
        }
        if (!path.startsWith(PUBLIC)) return null
        val id = path.removePrefix("$PUBLIC/").takeIf { '/' !in it } ?: return null
        val row = rows[key(id)] ?: return null
        return if (row.visibility == "public") ok(json(row)) else fail(HttpStatusCode.NotFound)
    }

    private fun owned(row: Row, method: HttpMethod, parts: List<String>, input: JsonObject): Pair<HttpStatusCode, JsonObject> = when {
        parts.size == 2 && parts[1] == "items" && method == HttpMethod.Put -> replace(row, input)
        parts.size != 1 -> fail(HttpStatusCode.NotFound)
        method == HttpMethod.Get -> ok(json(row))
        method == HttpMethod.Put -> update(row, input)
        method == HttpMethod.Delete -> {
            rows.remove(row.id)
            if (featured[row.owner] == row.id) featured.remove(row.owner)
            HttpStatusCode.NoContent to buildJsonObject {}
        }
        else -> fail(HttpStatusCode.MethodNotAllowed)
    }

    private fun create(owner: String, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (!validMetadata(input)) return fail(HttpStatusCode.BadRequest)
        val items = selection(input["items"] as? JsonArray ?: JsonArray(emptyList())) ?: return fail(HttpStatusCode.BadRequest)
        val cover = cover(owner, input) ?: return fail(HttpStatusCode.BadRequest)
        val row = Row(owner, key(UUID.randomUUID().toString()), value(input, "title").trim(), value(input, "description"),
            value(input, "visibility"), items, cover = cover.takeUnless { it.isEmpty() })
        rows[row.id] = row
        return HttpStatusCode.Created to envelope(json(row))
    }

    private fun update(row: Row, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (input["expectedRevision"]?.jsonPrimitive?.longOrNull != row.revision) return fail(HttpStatusCode.Conflict)
        if (!validMetadata(input)) return fail(HttpStatusCode.BadRequest)
        val nextCover = cover(row.owner, input) ?: return fail(HttpStatusCode.BadRequest)
        val next = row.copy(title = value(input, "title").trim(), description = value(input, "description"),
            visibility = value(input, "visibility"), revision = row.revision + 1,
            cover = if (input["removeCover"]?.jsonPrimitive?.booleanOrNull == true) null
                else nextCover.takeUnless { it.isEmpty() } ?: row.cover)
        rows[row.id] = next
        if (next.visibility != "public" && featured[row.owner] == row.id) featured.remove(row.owner)
        return ok(json(next))
    }

    private fun replace(row: Row, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (input["expectedRevision"]?.jsonPrimitive?.longOrNull != row.revision) return fail(HttpStatusCode.Conflict)
        val items = selection(input["items"] as? JsonArray, row.items) ?: return fail(HttpStatusCode.BadRequest)
        val next = row.copy(items = items, revision = row.revision + 1)
        rows[row.id] = next
        return ok(json(next))
    }

    private fun membership(owner: String, path: String, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val target = path.removePrefix("$ROOT/membership/").split('/')
        if (target.size != 2 || target[0] !in listOf("coffee_shop", "roaster")) return fail(HttpStatusCode.BadRequest)
        val ids = membershipSelection(owner, target.joinToString(":"), input) ?: return fail(HttpStatusCode.BadRequest)
        if (ids.any { rows[it]?.owner != owner } || ids.distinct().size != ids.size) return fail(HttpStatusCode.NotFound)
        val item = (available().map { it.jsonObject } + rows.values.filter { it.owner == owner }.flatMap { it.items })
            .firstOrNull { itemKey(it) == target.joinToString(":") } ?: return fail(HttpStatusCode.NotFound)
        rows.values.filter { it.owner == owner }.toList().forEach { row ->
            val kept = row.items.filterNot { itemKey(it) == itemKey(item) }
            val items = if (row.id in ids) row.items.takeIf { it.any { saved -> itemKey(saved) == itemKey(item) } } ?: (kept + item) else kept
            if (items != row.items) rows[row.id] = row.copy(items = items, revision = row.revision + 1)
        }
        return ok(summaries(owner, target[0], target[1]))
    }

    private fun membershipSelection(owner: String, target: String, input: JsonObject): List<String>? {
        val added = input["addedCollectionIds"] as? JsonArray
        val removed = input["removedCollectionIds"] as? JsonArray
        if ((added == null) != (removed == null)) return null
        if (added == null || removed == null) return (input["collectionIds"] as? JsonArray)?.map { key(it.jsonPrimitive.content) }
        val additions = added.map { key(it.jsonPrimitive.content) }.toSet()
        val removals = removed.map { key(it.jsonPrimitive.content) }.toSet()
        if (additions.intersect(removals).isNotEmpty() || (additions + removals).any { rows[it]?.owner != owner }) return null
        val current = rows.values.filter { it.owner == owner && it.items.any { place -> itemKey(place) == target } }.map { it.id }.toSet()
        return ((current + additions) - removals).toList()
    }

    private fun featured(owner: String, method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (method == HttpMethod.Put) {
            val id = input["collectionId"]?.jsonPrimitive?.contentOrNull?.let(::key)
            if (id != null && rows[id]?.let { it.owner == owner && it.visibility == "public" } != true) return fail(HttpStatusCode.NotFound)
            if (id == null) featured.remove(owner) else featured[owner] = id
        } else if (method != HttpMethod.Get) return fail(HttpStatusCode.MethodNotAllowed)
        return ok(buildJsonObject { put("collectionId", featured[owner]?.let(::JsonPrimitive) ?: JsonNull) })
    }

    fun reserve(owner: String?, input: JsonArray): Pair<HttpStatusCode, JsonObject> {
        if (owner.isNullOrBlank()) return fail(HttpStatusCode.Unauthorized)
        if (input.size != 1) return fail(HttpStatusCode.BadRequest)
        val row = input.single().jsonObject
        val size = row["sizeBytes"]?.jsonPrimitive?.longOrNull ?: return fail(HttpStatusCode.BadRequest)
        val mime = value(row, "contentType")
        if (size !in 1L..1_048_576L || mime !in listOf("image/jpeg", "image/png", "image/webp")) return fail(HttpStatusCode.BadRequest)
        val id = key(UUID.randomUUID().toString())
        val key = "collection-covers/$id"
        reservations[key] = Reservation(owner, key, size, mime)
        return ok(JsonArray(listOf(reservationJson(id, key))))
    }

    private fun reservationJson(id: String, key: String) = buildJsonObject {
        put("photoId", id)
        put("uploadUrl", "${SandboxBackend.BASE_URL}/upload/$key")
        put("storageKey", key)
    }

    private fun cover(owner: String, input: JsonObject): JsonObject? {
        val upload = input["coverUpload"] as? JsonObject ?: return buildJsonObject {}
        val key = value(upload, "storageKey")
        val reservation = reservations[key]?.takeIf { it.owner == owner } ?: return null
        if (upload["size"]?.jsonPrimitive?.longOrNull != reservation.size || value(upload, "contentType") != reservation.mime ||
            !uploaded(key, reservation.size)) return null
        return buildJsonObject {
            put("id", key.substringAfterLast('/'))
            put("storageKey", key)
            put("contentType", reservation.mime)
        }
    }

    fun coverKey(path: String, owner: String?): String? {
        val public = path.startsWith("$PUBLIC/")
        val root = if (public) PUBLIC else ROOT
        val parts = path.removePrefix("$root/").split('/')
        if (parts.size != 3 || parts[1] != "cover") return null
        val row = rows[key(parts[0])] ?: return null
        if (if (public) row.visibility != "public" else row.owner != owner) return null
        return row.cover?.takeIf { value(it, "id") == key(parts[2]) }?.let { value(it, "storageKey") }
    }

    private fun selection(items: JsonArray?, kept: List<JsonObject> = emptyList()): List<JsonObject>? {
        if (items == null) return null
        val source = (available().map { it.jsonObject } + kept).associateBy(::itemKey)
        val keys = items.map { value ->
            val item = value as? JsonObject ?: return null
            "${value(item, "kind")}:${value(item, "slug")}".takeIf { value(item, "kind") in listOf("coffee_shop", "roaster") } ?: return null
        }
        if (keys.distinct().size != keys.size) return null
        return keys.map { source[it] ?: return null }
    }

    private fun summaries(owner: String, kind: String?, slug: String?) = JsonArray(rows.values.reversed().filter { it.owner == owner }
        .map { row -> JsonObject(json(row) + ("containsItem" to JsonPrimitive(row.items.any { itemKey(it) == "$kind:$slug" }))) })

    private fun json(row: Row): JsonObject = buildJsonObject {
        put("id", row.id)
        put("title", row.title)
        put("description", row.description)
        put("visibility", row.visibility)
        put("mode", "list")
        put("revision", row.revision)
        put("itemCount", row.items.size)
        put("items", JsonArray(row.items))
        put("createdAtUtc", "${today}T10:00:00Z")
        put("isFeatured", featured[row.owner] == row.id)
        put("isDefault", row.isDefault)
        put("canonicalPath", if (row.visibility == "public") JsonPrimitive("/collections/${row.id}") else JsonNull)
        put("coverPhoto", row.cover?.let { JsonObject(it.filterKeys { key -> key == "id" }) } ?: JsonNull)
    }

    private fun validMetadata(input: JsonObject): Boolean = value(input, "title").trim().let { it.isNotBlank() && it.length <= 100 && it.none(Char::isISOControl) } &&
        value(input, "description").let { it.length <= 1000 && it.none { char -> char.isISOControl() && char !in "\n\r\t" } } &&
        value(input, "visibility") in listOf("public", "private")

    private fun itemKey(item: JsonObject) = "${value(item, "kind")}:${(item["address"] as? JsonObject)?.let { value(it, "slug") }}"
    private fun key(id: String) = id.replace("-", "").lowercase()
    private fun value(input: JsonObject, key: String) = input[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun envelope(data: JsonElement, success: Boolean = true) = buildJsonObject { put("isSuccess", success); put("data", data) }
    private fun ok(data: JsonElement) = HttpStatusCode.OK to envelope(data)
    private fun fail(status: HttpStatusCode) = status to envelope(JsonNull, false)

    companion object {
        const val OWNER = "X-CoffeePeek-Sandbox-Owner"
        const val ROOT = "/api/v1/saved-collections"
        const val PUBLIC = "/api/v1/favorite-collections"
        const val LOCAL_ACCOUNT = "11111111-1111-4111-8111-111111111111"
    }
}
