package com.coffeepeek.admin.sandbox.backend

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

/**
 * In-memory shelf contract (`coffeepeek-backend/docs/api/shelf-client.md`). Cards are keys from the shared `/api/Photos/` upload fixture;
 * fixture bags use the `sample` image. The shelf list keeps its order, so `position` is always the row's index.
 */
internal class SandboxShelfBackend(private val today: LocalDate, private val uploaded: (String) -> Boolean) {
    companion object {
        const val ROOT = "/api/v1/shelf"
        const val SHELF_LIMIT = 200
        private const val TEXT_LIMIT = 250
        private val products = mapOf(
            "sandbox-ethiopia" to ("22222222-2222-4222-8222-222222222222" to "Sandbox Ethiopia"),
            "sandbox-kenya" to ("22222222-2222-4222-8222-222222222223" to "Sandbox Kenya"),
            "sandbox-brazil" to ("22222222-2222-4222-8222-222222222224" to "Sandbox Brazil"),
        )
        private val details = listOf("coffeeProductId", "coffeeProductSlug", "name", "roasterName", "weightGrams", "grind", "roastedOn")
    }
    private class Rejected(val status: HttpStatusCode) : Exception()

    private val shelf = mutableListOf<JsonObject>()
    private val archive = mutableListOf<JsonObject>()
    private val usedKeys = mutableSetOf<String>()

    fun reset(populated: Boolean) {
        shelf.clear(); archive.clear(); usedKeys.clear()
        if (!populated) return
        shelf += fixture("sandbox-ethiopia", null, 250, "Beans", daysAgo = 3)
        shelf += fixture(null, "Колумбия P!X", 250, "Ground", daysAgo = 9, roaster = "River Roasters")
        shelf += fixture("sandbox-brazil", null, 1000, "Beans", daysAgo = 14)
        shelf += fixture(null, null, null, null, daysAgo = 1)
        archive += finished(fixture("sandbox-kenya", null, 250, "Beans", daysAgo = 40), daysAgo = 17, rating = 4, buyAgain = true)
        archive += finished(fixture(null, "Гондурас Мойзес", 200, "Beans", daysAgo = 70, roaster = "Welder Catherine"), daysAgo = 39, rating = 5, buyAgain = null)
        archive += finished(fixture(null, "Перу Санта Роса", 250, null, daysAgo = 95, roaster = "River Roasters"), daysAgo = 75, rating = null, buyAgain = false)
    }

    /** Owner-only card read: the upload key behind `photos/{photoId}`, or null when the bag or that card no longer exists. */
    fun cardKey(bagId: String, photoId: String): String? =
        find(bagId)?.get("photo")?.jsonObject?.get("id")?.jsonPrimitive?.content?.takeIf { it == photoId }

    fun handle(path: String, method: HttpMethod, parameters: Parameters, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        if (path != ROOT && !path.startsWith("$ROOT/")) return null
        return try {
            when {
                path == ROOT && method == HttpMethod.Get -> ok(JsonArray(shelfRows()))
                path == "$ROOT/archive" && method == HttpMethod.Get -> archivePage(parameters)
                path == "$ROOT/order" && method == HttpMethod.Put -> reorder(input)
                path == "$ROOT/bags" && method == HttpMethod.Post -> create(input)
                path.startsWith("$ROOT/bags/") -> bag(path.removePrefix("$ROOT/bags/"), method, input)
                else -> fail(HttpStatusCode.NotFound)
            }
        } catch (rejected: Rejected) {
            fail(rejected.status)
        }
    }

    private fun bag(route: String, method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val id = route.substringBefore('/')
        val existing = find(id) ?: return fail(HttpStatusCode.NotFound)
        return when (route.removePrefix(id) to method) {
            "" to HttpMethod.Put -> ok(replace(existing, edited(existing, input)))
            "" to HttpMethod.Delete -> { shelf.remove(existing); archive.remove(existing); ok(JsonNull) }
            "/finish" to HttpMethod.Post -> finish(existing, input)
            "/restore" to HttpMethod.Post -> restore(existing, input)
            else -> fail(HttpStatusCode.NotFound)
        }
    }

    private fun create(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (shelf.size >= SHELF_LIMIT) throw Rejected(HttpStatusCode.Conflict)
        val photo = input["photo"].takeUnless { it is JsonNull }?.let(::claim)
        val bean = beanFields(input, null)
        if (photo == null && bean.isEmpty()) throw Rejected(HttpStatusCode.BadRequest)
        val id = UUID.randomUUID().toString()
        val row = buildJsonObject {
            put("id", id); put("status", "OnShelf"); put("photo", photo?.let { card(id, it) } ?: JsonNull)
            detailFields(input, bean).forEach { (key, value) -> put(key, value) }
            put("addedAtUtc", "${today}T12:00:00Z"); put("finishedAtUtc", JsonNull); put("rating", JsonNull); put("buyAgain", JsonNull)
        }
        shelf.add(0, row)
        return ok(output(row))
    }

    private fun edited(existing: JsonObject, input: JsonObject): JsonObject {
        val photo = input["photo"].takeUnless { it == null || it is JsonNull }?.let { card(id(existing), claim(it)) }
        val fields = detailFields(input, beanFields(input, existing))
        return JsonObject(existing - details.toSet() + fields + listOfNotNull(photo?.let { "photo" to it }))
    }

    private fun finish(existing: JsonObject, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (existing !in shelf) throw Rejected(HttpStatusCode.Conflict)
        val rating = input["rating"]?.jsonPrimitive?.intOrNull
        if (rating != null && rating !in 1..5) throw Rejected(HttpStatusCode.BadRequest)
        val row = JsonObject(existing + mapOf("status" to JsonPrimitive("Finished"), "finishedAtUtc" to JsonPrimitive("${today}T12:00:00Z"),
            "rating" to JsonPrimitive(rating), "buyAgain" to JsonPrimitive(input["buyAgain"]?.jsonPrimitive?.booleanOrNull)))
        shelf.remove(existing); archive += row
        return ok(output(row))
    }

    private fun restore(existing: JsonObject, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (existing !in archive || shelf.size >= SHELF_LIMIT) throw Rejected(HttpStatusCode.Conflict)
        val position = (input["position"]?.jsonPrimitive?.intOrNull ?: 0).coerceIn(0, shelf.size)
        val row = JsonObject(existing + mapOf("status" to JsonPrimitive("OnShelf"), "finishedAtUtc" to JsonNull,
            "rating" to JsonNull, "buyAgain" to JsonNull))
        archive.remove(existing); shelf.add(position, row)
        return ok(output(row))
    }

    private fun reorder(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val ids = (input["bagIds"] as? JsonArray ?: throw Rejected(HttpStatusCode.BadRequest)).map { it.jsonPrimitive.content }
        val byId = shelf.associateBy(::id)
        if (ids.size != shelf.size || ids.toSet() != byId.keys) throw Rejected(HttpStatusCode.Conflict)
        val ordered = ids.map(byId::getValue)
        shelf.clear(); shelf += ordered
        return ok(JsonArray(shelfRows()))
    }

    private fun archivePage(parameters: Parameters): Pair<HttpStatusCode, JsonObject> {
        val page = parameters["pageNumber"]?.toIntOrNull()?.coerceIn(1, 10000) ?: 1
        val size = parameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        val query = parameters["query"]?.trim().orEmpty()
        if (query.length > 100) throw Rejected(HttpStatusCode.BadRequest)
        val matching = archive.sortedByDescending { it.getValue("finishedAtUtc").jsonPrimitive.content }.filter { row ->
            query.isEmpty() || listOf("name", "roasterName").any { row[it]?.jsonPrimitive?.contentOrNull?.contains(query, ignoreCase = true) == true }
        }
        return ok(buildJsonObject {
            put("items", JsonArray(matching.drop((page - 1) * size).take(size).map(::output))); put("totalCount", matching.size)
        })
    }

    /** Bean selector rules shared with home check-ins: a published slug, a custom name, or (on PUT) the bag's own product ID. */
    private fun beanFields(input: JsonObject, existing: JsonObject?): Map<String, JsonElement> {
        val slug = input.text("coffeeProductSlug")
        val custom = input.text("customName")?.trim()
        val keptId = input.text("coffeeProductId")
        if (listOfNotNull(slug, custom, keptId).size > 1) throw Rejected(HttpStatusCode.BadRequest)
        return when {
            slug != null -> products[slug]?.let { (id, name) ->
                mapOf("coffeeProductId" to JsonPrimitive(id), "coffeeProductSlug" to JsonPrimitive(slug), "name" to JsonPrimitive(name))
            } ?: throw Rejected(HttpStatusCode.NotFound)
            custom != null -> if (custom.length !in 1..TEXT_LIMIT) throw Rejected(HttpStatusCode.BadRequest) else mapOf("name" to JsonPrimitive(custom))
            keptId != null -> if (existing?.text("coffeeProductId") != keptId) throw Rejected(HttpStatusCode.BadRequest)
                else listOf("coffeeProductId", "coffeeProductSlug", "name").associateWith { existing.getValue(it) }
            else -> emptyMap()
        }
    }

    private fun detailFields(input: JsonObject, bean: Map<String, JsonElement>): Map<String, JsonElement> {
        val roaster = input.text("roasterName")?.trim()?.takeIf { it.isNotEmpty() } ?: "Sandbox Roaster".takeIf { input.text("coffeeProductSlug") != null }
        val weight = input["weightGrams"]?.jsonPrimitive?.intOrNull
        val grind = input.text("grind")
        val roastedOn = input.text("roastedOn")?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: throw Rejected(HttpStatusCode.BadRequest) }
        if ((roaster?.length ?: 0) > TEXT_LIMIT || (weight != null && weight !in 1..10000) || (grind != null && grind !in setOf("Beans", "Ground")) ||
            (roastedOn != null && roastedOn > today.plusDays(1))) throw Rejected(HttpStatusCode.BadRequest)
        return details.associateWith { JsonNull as JsonElement } + bean + mapOf("roasterName" to JsonPrimitive(roaster),
            "weightGrams" to JsonPrimitive(weight), "grind" to JsonPrimitive(grind), "roastedOn" to JsonPrimitive(roastedOn?.toString()))
    }

    private fun claim(photo: JsonElement): String {
        val key = (photo as? JsonObject)?.text("storageKey") ?: throw Rejected(HttpStatusCode.BadRequest)
        if (!uploaded(key)) throw Rejected(HttpStatusCode.BadRequest)
        if (!usedKeys.add(key)) throw Rejected(HttpStatusCode.Conflict)
        return key
    }

    private fun fixture(slug: String?, custom: String?, weight: Int?, grind: String?, daysAgo: Long, roaster: String? = null): JsonObject {
        val id = UUID.randomUUID().toString()
        val input = buildJsonObject {
            slug?.let { put("coffeeProductSlug", it) }; custom?.let { put("customName", it) }; roaster?.let { put("roasterName", it) }
            weight?.let { put("weightGrams", it) }; grind?.let { put("grind", it) }
            put("roastedOn", today.minusDays(daysAgo + 5).toString())
        }
        return buildJsonObject {
            put("id", id); put("status", "OnShelf"); put("photo", card(id, "sample"))
            detailFields(input, beanFields(input, null)).forEach { (key, value) -> put(key, value) }
            put("addedAtUtc", "${today.minusDays(daysAgo)}T09:00:00Z"); put("finishedAtUtc", JsonNull); put("rating", JsonNull); put("buyAgain", JsonNull)
        }
    }

    private fun finished(row: JsonObject, daysAgo: Long, rating: Int?, buyAgain: Boolean?) = JsonObject(row + mapOf(
        "status" to JsonPrimitive("Finished"), "finishedAtUtc" to JsonPrimitive("${today.minusDays(daysAgo)}T18:00:00Z"),
        "rating" to JsonPrimitive(rating), "buyAgain" to JsonPrimitive(buyAgain)))

    private fun card(bagId: String, key: String) = buildJsonObject { put("id", key); put("url", "$ROOT/bags/$bagId/photos/$key") }
    private fun replace(existing: JsonObject, row: JsonObject): JsonObject {
        val list = if (existing in shelf) shelf else archive
        list[list.indexOf(existing)] = row
        return output(row)
    }
    private fun find(id: String) = (shelf + archive).firstOrNull { id(it) == id }
    private fun id(row: JsonObject) = row.getValue("id").jsonPrimitive.content
    private fun shelfRows() = shelf.map(::output)
    private fun output(row: JsonObject) = JsonObject(row + ("position" to JsonPrimitive(shelf.indexOf(row).takeIf { it >= 0 })))
    private fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull
    private fun ok(data: JsonElement) = HttpStatusCode.OK to buildJsonObject { put("isSuccess", true); put("data", data) }
    private fun fail(status: HttpStatusCode) = status to buildJsonObject { put("isSuccess", false); put("data", JsonNull) }
}
