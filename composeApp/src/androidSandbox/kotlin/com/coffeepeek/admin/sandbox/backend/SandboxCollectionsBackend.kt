package com.coffeepeek.admin.sandbox.backend

import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

/** Local-only collection fixture; all identities and images stay inside the Sandbox mock engines. */
internal class SandboxCollectionsBackend(private val today: LocalDate, private val favorites: () -> JsonArray) {
    private val rows = linkedMapOf<String, JsonObject>()
    fun reset() = rows.clear()

    fun handle(path: String, method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        if (path == "/api/v1/favorite-collections") return when (method) {
            HttpMethod.Get -> ok(JsonArray(rows.values.reversed().map(::summary)))
            HttpMethod.Post -> create(input)
            else -> fail(HttpStatusCode.MethodNotAllowed)
        }
        if (!path.startsWith("/api/v1/favorite-collections/")) return null
        val id = path.substringAfterLast('/').replace("-", "")
        val row = rows[id] ?: return fail(HttpStatusCode.NotFound)
        return when (method) {
            HttpMethod.Get -> ok(row)
            HttpMethod.Delete -> { rows.remove(id); HttpStatusCode.NoContent to buildJsonObject {} }
            else -> fail(HttpStatusCode.MethodNotAllowed)
        }
    }

    private fun create(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val title = input["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (title.isBlank() || title.length > 100 || title.any(Char::isISOControl)) return fail(HttpStatusCode.BadRequest)
        val selection = selected(input["items"] as? JsonArray) ?: return fail(HttpStatusCode.BadRequest)
        val id = UUID.randomUUID().toString()
        val key = id.replace("-", "")
        val row = buildJsonObject {
            put("id", id); put("title", title); put("canonicalPath", "/collections/$key")
            put("createdAtUtc", "${today}T10:00:00Z"); put("items", JsonArray(selection))
        }
        rows[key] = row
        return HttpStatusCode.Created to envelope(row)
    }

    private fun selected(items: JsonArray?): List<JsonObject>? {
        if (items == null || items.size !in 1..100) return null
        val available = favorites().map { it.jsonObject }.filter { it["kind"]?.jsonPrimitive?.content in listOf("coffee_shop", "roaster") }
            .associateBy { "${it["kind"]?.jsonPrimitive?.content}:${it["address"]?.jsonObject?.get("slug")?.jsonPrimitive?.content}" }
        val keys = items.map { value ->
            val item = value as? JsonObject ?: return null
            "${item["kind"]?.jsonPrimitive?.contentOrNull}:${item["slug"]?.jsonPrimitive?.contentOrNull}"
        }
        if (keys.distinct().size != keys.size) return null
        return keys.map { available[it] ?: return null }
    }

    private fun summary(row: JsonObject): JsonObject = buildJsonObject {
        listOf("id", "title", "canonicalPath", "createdAtUtc").forEach { put(it, row.getValue(it)) }
        put("itemCount", row.getValue("items").jsonArray.size)
    }
    private fun envelope(data: JsonElement, success: Boolean = true) = buildJsonObject { put("isSuccess", success); put("data", data) }
    private fun ok(data: JsonElement) = HttpStatusCode.OK to envelope(data)
    private fun fail(status: HttpStatusCode) = status to envelope(JsonNull, false)
}
