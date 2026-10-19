package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import com.coffeepeek.core.network.HttpClientFactory
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

internal enum class SandboxScenario { Ready, Slow, Offline, ServerError, SaveError, UploadError }

/** Stateful HTTP fixture only. Both engines have no network delegate, even for unknown URLs. */
internal class SandboxBackend(private val samplePhoto: ByteArray,
    private val today: LocalDate = LocalDate.now()) {
    companion object { const val BASE_URL = "https://sandbox.coffeepeek.invalid" }
    @Volatile var scenario = SandboxScenario.Ready
    private val lock = Any()
    private val rows = linkedMapOf<String, JsonObject>()
    private val shopRows = mutableListOf<JsonObject>()
    private val images = mutableMapOf<String, ByteArray>()
    private val reservations = mutableSetOf<String>()
    private val uploaded = mutableSetOf<String>()
    val gateway: HttpClient = HttpClientFactory(MockEngine { request ->
        val mode = scenario
        if (mode == SandboxScenario.Slow) delay(900)
        if (mode == SandboxScenario.Offline) throw IOException("Sandbox offline scenario")
        synchronized(lock) { handle(request, mode) }.let { (status, payload) ->
            respond(payload.toString(), status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
    }).api(BASE_URL)
    val uploader: HttpClient = HttpClientFactory(MockEngine { request ->
        val accepted = synchronized(lock) {
            val key = request.url.encodedPath.removePrefix("/upload/")
            val bytes = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()
            if (scenario == SandboxScenario.UploadError || request.url.host != "sandbox.coffeepeek.invalid" ||
                request.method != HttpMethod.Put || key !in reservations || bytes == null || bytes.isEmpty() ||
                request.headers[HttpHeaders.Authorization] != null || request.headers[HttpHeaders.Cookie] != null) false
            else { images["/media/$key"] = bytes.copyOf(); uploaded += key; true }
        }
        respond("", if (accepted) HttpStatusCode.OK else HttpStatusCode.BadRequest)
    }).upload()

    init { reset(populated = true) }

    fun reset(populated: Boolean) = synchronized(lock) {
        rows.clear(); shopRows.clear(); images.clear(); reservations.clear(); uploaded.clear()
        images["/media/sample"] = samplePhoto.copyOf()
        scenario = SandboxScenario.Ready
        if (populated) repeat(3) { index ->
            val id = UUID.randomUUID().toString()
            val day = today.minusDays(if (index < 2) 0 else 1)
            rows[id] = buildJsonObject {
                put("id", id); put("preparedOn", day.toString()); put("preparedAtUtc", "${day}T${(8 + index).toString().padStart(2, '0')}:00:00Z")
                put("createdAtUtc", "${day}T08:00:00Z"); put("beanName", "Sandbox Ethiopia ${index + 1}")
                put("brewMethodCode", "hario-v60"); put("doseGrams", 18); put("brewDurationSeconds", 180)
                put("tasteScore", 4); put("text", "Тестовая запись. Не отправляется на сервер.")
                put("visibility", "Private"); put("moderationState", "NotSubmitted")
                putJsonArray("photos") { if (index == 0) add(buildJsonObject { put("id", "sample"); put("url", "/media/sample") }) }
            }
        }
        if (populated) shopRows += buildJsonObject {
            put("id", UUID.randomUUID().toString()); put("shopName", "Sandbox Cafe"); put("username", "Sandbox")
            putJsonObject("shop") { put("slug", "sandbox-cafe"); put("canonicalPath", "/coffee-shop/sandbox-cafe") }
            put("createdAt", "${today}T07:00:00Z"); put("visitedAt", "${today}T07:00:00Z")
            put("note", "Локальное тестовое посещение"); put("visibility", "Private"); put("moderationState", "NotSubmitted")
            putJsonObject("rating") { put("coffee", 4); put("service", 5); put("place", 4) }
            putJsonArray("photos") { add(buildJsonObject { put("url", "/media/sample"); put("sortIndex", 0) }) }
        }
    }

    fun photo(url: String): ByteArray? = synchronized(lock) {
        val parsed = runCatching { Url(url) }.getOrNull() ?: return@synchronized null
        if (parsed.host != "sandbox.coffeepeek.invalid") null else images[parsed.encodedPath]?.copyOf()
    }

    private fun handle(request: HttpRequestData, mode: SandboxScenario): Pair<HttpStatusCode, JsonObject> {
        if (request.url.host != "sandbox.coffeepeek.invalid") return fail(HttpStatusCode.Forbidden)
        if (mode == SandboxScenario.ServerError) return fail(HttpStatusCode.InternalServerError)
        val path = request.url.encodedPath
        val home = "/api/v1/check-ins/home"
        if (mode == SandboxScenario.SaveError && (path == home || path.startsWith("$home/")) &&
            request.method in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete)) return fail(HttpStatusCode.ServiceUnavailable)
        if (path == "/api/v1/check-ins/mine" && request.method == HttpMethod.Get) {
            val filtered = shopRows.filter { row ->
                val date = java.time.Instant.parse(row.getValue("visitedAt").jsonPrimitive.content)
                (request.url.parameters["from"]?.let { date >= java.time.Instant.parse(it) } != false) &&
                    (request.url.parameters["to"]?.let { date < java.time.Instant.parse(it) } != false)
            }
            val page = request.url.parameters["pageNumber"]?.toIntOrNull() ?: 1
            val size = request.url.parameters["pageSize"]?.toIntOrNull() ?: 20
            if (page !in 1..10000 || size !in 1..100) return fail(HttpStatusCode.BadRequest)
            return ok(buildJsonObject { put("totalCount", filtered.size); put("items", JsonArray(filtered.drop((page - 1) * size).take(size))) })
        }
        if (path == "/api/Photos/check-in" && request.method == HttpMethod.Post) {
            val body = body(request) as? JsonArray ?: return fail(HttpStatusCode.BadRequest)
            return ok(JsonArray(body.map {
                val key = UUID.randomUUID().toString(); reservations += key
                buildJsonObject { put("storageKey", key); put("uploadUrl", "$BASE_URL/upload/$key") }
            }))
        }
        val matching = rows.values.filter { row ->
            val date = row.getValue("preparedOn").jsonPrimitive.content
            (request.url.parameters["from"]?.let { date >= it } != false) &&
                (request.url.parameters["to"]?.let { date <= it } != false)
        }.sortedByDescending { it.getValue("preparedAtUtc").jsonPrimitive.content }
        if (request.method == HttpMethod.Get && path == "$home/calendar") return ok(JsonArray(
            matching.groupBy { it.getValue("preparedOn").jsonPrimitive.content }.map { (date, entries) ->
                buildJsonObject { put("preparedOn", date); put("brewCount", entries.size) }
            }))
        if (request.method == HttpMethod.Get && path == home) {
            val page = request.url.parameters["pageNumber"]?.toIntOrNull() ?: 1
            val size = request.url.parameters["pageSize"]?.toIntOrNull() ?: 20
            if (page !in 1..10000 || size !in 1..100) return fail(HttpStatusCode.BadRequest)
            return ok(buildJsonObject { put("totalCount", matching.size); put("items", JsonArray(matching.drop((page - 1) * size).take(size))) })
        }
        if (request.method == HttpMethod.Post && path == home) {
            val input = body(request) as? JsonObject ?: return fail(HttpStatusCode.BadRequest)
            val id = UUID.randomUUID().toString()
            val row = update(id, input, null) ?: return fail(HttpStatusCode.BadRequest)
            rows[id] = row
            return ok(row)
        }
        if (path.startsWith("$home/")) {
            val id = path.removePrefix("$home/")
            val existing = rows[id] ?: return fail(HttpStatusCode.NotFound)
            return when (request.method) {
                HttpMethod.Get -> ok(existing)
                HttpMethod.Delete -> { rows.remove(id); ok(JsonNull) }
                HttpMethod.Put -> {
                    val input = body(request) as? JsonObject ?: return fail(HttpStatusCode.BadRequest)
                    val row = update(id, input, existing) ?: return fail(HttpStatusCode.BadRequest)
                    rows[id] = row; ok(row)
                }
                else -> fail(HttpStatusCode.MethodNotAllowed)
            }
        }
        return fail(HttpStatusCode.NotFound) // Never fall back to a live transport.
    }

    private fun update(id: String, input: JsonObject, existing: JsonObject?): JsonObject? {
        val instant = input["preparedAt"]?.jsonPrimitive?.content ?: return null
        val date = input["preparedOn"]?.jsonPrimitive?.contentOrNull ?: instant.take(10)
        fun photo(value: JsonElement?): JsonObject? {
            val key = (value as? JsonObject)?.get("storageKey")?.jsonPrimitive?.content ?: return null
            if (key !in uploaded) return null
            return buildJsonObject { put("id", key); put("url", "/media/$key") }
        }
        val originals = (input["photos"] as? JsonArray).orEmpty()
        val photos = originals.map { photo(it) ?: return null }
        return buildJsonObject {
            existing?.forEach { (key, value) -> put(key, value) }
            input.forEach { (key, value) -> if (key !in setOf("photos", "drinkPhoto", "preparedAt")) put(key, value) }
            put("id", id); put("preparedOn", date); put("preparedAtUtc", instant)
            put("createdAtUtc", existing?.get("createdAtUtc") ?: JsonPrimitive(instant))
            input["customBeanName"]?.let { put("beanName", it) }
            if (input["coffeeProductSlug"]?.jsonPrimitive?.contentOrNull == "sandbox-ethiopia") {
                put("coffeeProductId", "22222222-2222-4222-8222-222222222222"); put("beanName", "Sandbox Ethiopia")
            }
            val drinkName = input["customDrinkName"]?.jsonPrimitive?.contentOrNull
            if (drinkName != null) putJsonObject("drink") { put("nameRu", drinkName); put("slug", "other") }
            if (existing == null) put("photos", JsonArray(photos))
            val visibility = input["visibility"]?.jsonPrimitive?.content ?: "Private"
            put("visibility", visibility); put("moderationState", if (visibility == "Public") "Pending" else "NotSubmitted")
        }
    }

    private fun body(request: HttpRequestData) = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()
        ?.decodeToString()?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() }
    private fun ok(data: JsonElement) = HttpStatusCode.OK to buildJsonObject { put("isSuccess", true); put("data", data) }
    private fun fail(status: HttpStatusCode) = status to buildJsonObject { put("isSuccess", false); put("data", JsonNull) }
    fun close() { gateway.close(); uploader.close() }
}
