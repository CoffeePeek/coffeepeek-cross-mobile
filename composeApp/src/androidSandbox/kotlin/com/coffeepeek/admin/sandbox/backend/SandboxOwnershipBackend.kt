package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

/** In-memory owner endpoints for manual Sandbox review; never delegates to a real account/server. */
internal class SandboxOwnershipBackend(private val today: LocalDate) {
    private val profiles = linkedMapOf<String, JsonObject>()
    private val claims = linkedMapOf<String, JsonObject>()
    fun reset(populated: Boolean) {
        profiles.clear()
        claims.clear()
        if (!populated) return
        profiles["coffee-shops/$SHOP"] = profile(SHOP, "Sandbox Cafe", shop = true)
        profiles["roasters/$ROASTER"] = profile(ROASTER, "Sandbox Roaster", shop = false)
        claims["sandbox-approved"] = claim("sandbox-approved", "Roaster", ROASTER, "sandbox-roaster", "Approved", true)
        claims["sandbox-rejected"] = claim("sandbox-rejected", "CoffeeShop", SHOP, "sandbox-cafe", "Rejected", false)
    }
    fun handle(request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        val path = request.url.encodedPath.removePrefix("/api/owner/")
        if (!request.url.encodedPath.startsWith("/api/owner/")) return null
        if (path == "ownership-claims") return claims(request, input)
        if (path.startsWith("ownership-claims/")) return claims[path.substringAfter('/')]
            ?.let { ok(it) } ?: fail(HttpStatusCode.NotFound)
        if (path == "coffee-shops" || path == "roasters") return businesses(path, request)
        val parts = path.split('/')
        val key = parts.take(2).joinToString("/")
        val profile = profiles[key] ?: return fail(HttpStatusCode.NotFound)
        if (parts.size == 2 && request.method == HttpMethod.Get) return ok(profile)
        if (parts.size == 2 && request.method == HttpMethod.Put) return save(key, profile, request, input)
        if (parts.getOrNull(2) == "photos") return gallery(key, profile, request, input)
        return fail(HttpStatusCode.NotFound)
    }
    private fun businesses(kind: String, request: HttpRequestData): Pair<HttpStatusCode, JsonObject> {
        val items = profiles.filterKeys { it.startsWith("$kind/") }.values.map { value ->
            buildJsonObject { put("id", value.getValue("id")); put("name", value.getValue("name")) }
        }
        return ok(if (kind == "coffee-shops") buildJsonObject { put("items", JsonArray(items)) } else page(items, request))
    }
    private fun claims(request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (request.method == HttpMethod.Get) return ok(page(claims.values.toList(), request))
        val slug = input["businessSlug"]?.jsonPrimitive?.content.orEmpty()
        val kind = input["businessType"]?.jsonPrimitive?.content.orEmpty()
        if (kind != "CoffeeShop" && kind != "Roaster") return fail(HttpStatusCode.BadRequest)
        if (slug !in listOf("sandbox-cafe", "sandbox-lab", "sandbox-roaster")) return fail(HttpStatusCode.NotFound)
        if (slug != "sandbox-lab" || claims.values.any { it["businessSlugSnapshot"]?.jsonPrimitive?.content == slug }) return fail(HttpStatusCode.Conflict)
        val id = UUID.randomUUID().toString()
        val value = claim(id, kind, SHOP, slug, "Pending", false).toMutableMap()
        value["contact"] = input["contact"] ?: JsonPrimitive("")
        value["evidence"] = input["evidence"] ?: JsonPrimitive("")
        claims[id] = JsonObject(value)
        return HttpStatusCode.Created to envelope(claims.getValue(id))
    }
    private fun save(key: String, original: JsonObject, request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (request.headers[HttpHeaders.IfMatch] != "\"${original.getValue("profileRevision").jsonPrimitive.long}\"") return fail(HttpStatusCode.Conflict)
        val shop = key.startsWith("coffee-shops/")
        val value = original.toMutableMap()
        value["name"] = input.getValue("name")
        value[if (shop) "description" else "about"] = input[if (shop) "description" else "about"] ?: JsonNull
        value[if (shop) "contacts" else "contact"] = buildJsonObject {
            listOf("phoneNumber", "email", "siteLink", "instagramLink").forEach { put(it, input[it] ?: JsonNull) }
        }
        value["location"] = if (shop) input.getValue("location") else buildJsonObject {
            listOf("address", "latitude", "longitude").forEach { put(it, input[it] ?: JsonNull) }
        }
        (input["photos"] as? JsonArray)?.let { value["photos"] = metadata(it, original["photos"]!!.jsonArray) }
        profiles[key] = revision(value, original)
        return ok(profiles.getValue(key))
    }
    private fun gallery(key: String, original: JsonObject, request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val existing = original.getValue("photos").jsonArray
        val ids = (input["photoIds"] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()
        val selected = when (request.method) {
            HttpMethod.Post -> JsonArray(existing + metadata(input.getValue("photos").jsonArray, existing))
            HttpMethod.Delete -> JsonArray(existing.filterNot { it.jsonObject["id"]?.jsonPrimitive?.content in ids })
            HttpMethod.Put -> {
                val byId = existing.associateBy { it.jsonObject.getValue("id").jsonPrimitive.content }
                if (ids.toSet() != byId.keys || ids.size != byId.size) return fail(HttpStatusCode.BadRequest)
                JsonArray(ids.map(byId::getValue))
            }
            else -> return fail(HttpStatusCode.BadRequest)
        }
        profiles[key] = revision(original.toMutableMap().apply { put("photos", selected) }, original)
        return ok(profiles.getValue(key))
    }
    private fun metadata(input: JsonArray, existing: JsonArray): JsonArray = JsonArray(input.map { photo ->
        val key = photo.jsonObject.getValue("storageKey").jsonPrimitive.content
        existing.firstOrNull { it.jsonObject["storageKey"]?.jsonPrimitive?.content == key } ?: buildJsonObject {
            put("id", UUID.randomUUID().toString())
            photo.jsonObject.forEach { (name, value) -> put(name, value) }
            put("fullUrl", "${SandboxBackend.BASE_URL}/media/$key")
        }
    })
    private fun revision(value: MutableMap<String, JsonElement>, previous: JsonObject): JsonObject {
        value["profileRevision"] = JsonPrimitive(previous.getValue("profileRevision").jsonPrimitive.long + 1)
        return JsonObject(value)
    }
    private fun page(items: List<JsonObject>, request: HttpRequestData) = buildJsonObject {
        val page = request.url.parameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        put("items", JsonArray(items.drop((page - 1) * 20).take(20)))
        put("totalCount", items.size)
        put("page", page)
        put("pageSize", 20)
    }
    private fun profile(id: String, name: String, shop: Boolean) = buildJsonObject {
        put("id", id)
        put("name", name)
        put("profileRevision", 1)
        put("cityId", "sandbox-minsk")
        put(if (shop) "description" else "about", "Локальный профиль бизнеса для проверки Sandbox")
        putJsonObject("location") { put("address", "Минск"); put("latitude", 53.9); put("longitude", 27.5) }
        putJsonObject(if (shop) "contacts" else "contact") { put("siteLink", "https://example.test") }
        put("photos", JsonArray(emptyList()))
    }
    private fun claim(id: String, kind: String, business: String, slug: String, status: String, access: Boolean) = buildJsonObject {
        put("id", id)
        put("businessType", kind)
        put("businessId", business)
        put("businessNameSnapshot", "Sandbox business")
        put("businessSlugSnapshot", slug)
        put("contact", "owner@example.test")
        put("evidence", "Локальное подтверждение полномочий")
        put("status", status)
        put("createdAtUtc", "${today}T10:00:00Z")
        put("hasCurrentAccess", access)
        put("decisionReason", if (status == "Rejected") "Локальный пример отказа" else "Локальный пример решения")
    }
    private fun envelope(data: JsonElement) = buildJsonObject { put("isSuccess", true); put("data", data) }
    private fun ok(data: JsonElement) = HttpStatusCode.OK to envelope(data)
    private fun fail(status: HttpStatusCode) = status to buildJsonObject { put("isSuccess", false); put("data", JsonNull) }
    companion object {
        const val SHOP = "33333333-3333-4333-8333-333333333333"
        const val ROASTER = "44444444-4444-4444-8444-444444444444"
    }
}
