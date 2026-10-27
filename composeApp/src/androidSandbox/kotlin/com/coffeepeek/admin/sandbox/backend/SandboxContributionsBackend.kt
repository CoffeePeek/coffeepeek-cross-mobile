package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

/**
 * What the moderation endpoints answer. `Seeded` (default) holds a few submissions per status; `Paged` adds 45 approved shops, roasters and changes behind
 * paging; `Empty` has nothing; `ListFailure` answers 500 for every list; `SubmitFailure` rejects every create, submit and update with a server message.
 */
internal enum class SandboxContributions { Seeded, Paged, Empty, ListFailure, SubmitFailure }

/** Local fixtures for `ModerationShops`, `ModerationRoasters` and `ShopChangeRequests`; submissions live in RAM and are cleared by [reset]. */
internal class SandboxContributionsBackend(private val today: LocalDate, private val shopSlugs: Set<String>) {
    companion object {
        const val PUBLISHED_SHOP = "sandbox-cafe"
        const val REJECTED_REASON = "Адрес не найден на карте"
        const val SUBMIT_FAILURE = "Заявка не принята. Проверьте данные и повторите."
        private val statuses = listOf("Approved", "Pending", "Rejected")
        private val sections = listOf("Photos", "Contacts", "Description", "Tags", "Roasters", "Equipment", "Menu", "BrewMethods")
    }
    @Volatile var mode = SandboxContributions.Seeded
    private val shops = mutableListOf<JsonObject>()
    private val roasters = mutableListOf<JsonObject>()
    private val changes = mutableListOf<JsonObject>()

    fun reset(populated: Boolean) {
        mode = SandboxContributions.Seeded
        shops.clear(); roasters.clear(); changes.clear()
        if (populated) seed(extra = 0)
    }

    /** Applies the chosen fixture; `Paged` regenerates the lists with the extra items, any other mode restores the base set. */
    fun select(value: SandboxContributions, populated: Boolean) {
        mode = value
        shops.clear(); roasters.clear(); changes.clear()
        if (populated && value != SandboxContributions.Empty) seed(extra = if (value == SandboxContributions.Paged) 45 else 0)
    }

    fun handle(path: String, method: HttpMethod, request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        val isList = method == HttpMethod.Get && path.endsWith("/mine")
        return when {
            path == "/api/moderationshops/mine" && isList -> list(request, shops) { row, status, _ -> row.status() == status }
            path == "/api/moderationroasters/mine" && isList -> list(request, roasters) { row, status, _ -> row.status() == status }
            path == "/api/shopchangerequests/mine" && isList -> list(request, changes) { row, status, params ->
                row.status() == status && params["shop"]?.let { shop -> row.shopSlug() == shop } != false &&
                    params["section"]?.let { section -> row["section"]?.jsonPrimitive?.content == section } != false
            }
            path == "/api/moderationshops" && method == HttpMethod.Post -> submit { createShop(input) }
            path == "/api/moderationroasters" && method == HttpMethod.Post -> submit { createRoaster(input) }
            path == "/api/shopchangerequests" && method == HttpMethod.Post -> submit { createChange(input) }
            path.startsWith("/api/shopchangerequests/") && !path.endsWith("/mine") && method in listOf(HttpMethod.Get, HttpMethod.Put) ->
                changeById(path.removePrefix("/api/shopchangerequests/"), method, input)
            else -> null
        }
    }

    private fun list(
        request: HttpRequestData,
        rows: List<JsonObject>,
        matches: (row: JsonObject, status: String, params: Parameters) -> Boolean,
    ): Pair<HttpStatusCode, JsonObject> {
        if (mode == SandboxContributions.ListFailure) return HttpStatusCode.InternalServerError to failure("Не удалось загрузить список")
        val params = request.url.parameters
        val status = params["status"] ?: return HttpStatusCode.BadRequest to failure("Укажите статус")
        val page = params["page"]?.toIntOrNull()?.takeIf { it >= 1 } ?: return HttpStatusCode.BadRequest to failure("Некорректная страница")
        val size = params["pageSize"]?.toIntOrNull()?.takeIf { it in 1..100 } ?: return HttpStatusCode.BadRequest to failure("Некорректный размер страницы")
        val matching = rows.filter { matches(it, status, params) }
        return ok(buildJsonObject {
            put("items", JsonArray(matching.drop((page - 1) * size).take(size)))
            put("totalItems", matching.size); put("totalPages", (matching.size + size - 1) / size); put("currentPage", page); put("pageSize", size)
        })
    }

    private fun submit(create: () -> Pair<HttpStatusCode, JsonObject>): Pair<HttpStatusCode, JsonObject> =
        if (mode == SandboxContributions.SubmitFailure) HttpStatusCode.BadRequest to failure(SUBMIT_FAILURE) else create()

    private fun createShop(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val name = input["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val address = input["address"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (name.isEmpty() || name.length > 55 || address.isEmpty() || input["city"]?.jsonPrimitive?.contentOrNull.isNullOrBlank())
            return HttpStatusCode.BadRequest to failure("Укажите название, адрес и город")
        shops += shop(UUID.randomUUID().toString(), name, address, "Pending", reason = null, published = false)
        return HttpStatusCode.OK to buildJsonObject { put("isSuccess", true); put("data", JsonNull) }
    }

    private fun createRoaster(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val name = input["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val city = input["city"]?.jsonPrimitive?.contentOrNull
        val address = input["address"]?.jsonPrimitive?.contentOrNull
        if (name.isEmpty() || name.length > 100 || city.isNullOrBlank() != address.isNullOrBlank())
            return HttpStatusCode.BadRequest to failure("Укажите название; город и адрес вместе")
        val id = UUID.randomUUID().toString()
        roasters += roaster(id, name, input["about"]?.jsonPrimitive?.contentOrNull, "Pending", reason = null)
        return HttpStatusCode.OK to buildJsonObject {
            put("isSuccess", true); put("message", "Обжарщик отправлен на модерацию")
            put("data", buildJsonObject { put("roasterId", id); put("status", "Pending"); put("isAddressValidated", !address.isNullOrBlank()) })
        }
    }

    private fun createChange(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val slug = input["shop"]?.jsonPrimitive?.contentOrNull
        val section = input["section"]?.jsonPrimitive?.contentOrNull
        val payload = input["payload"] as? JsonObject
        if (slug !in shopSlugs) return HttpStatusCode.NotFound to failure("Кофейня не найдена")
        if (section !in sections || payload == null) return HttpStatusCode.BadRequest to failure("Некорректная заявка")
        val row = change(UUID.randomUUID().toString(), slug!!, section!!, payload, "Pending", reason = null)
        changes += row
        return ok(row)
    }

    private fun changeById(id: String, method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val index = changes.indexOfFirst { it["id"]?.jsonPrimitive?.content == id }
        if (index < 0) return HttpStatusCode.NotFound to failure("Заявка не найдена")
        if (method == HttpMethod.Get) return ok(changes[index])
        if (mode == SandboxContributions.SubmitFailure) return HttpStatusCode.BadRequest to failure(SUBMIT_FAILURE)
        val old = changes[index]
        if (old.status() == "Approved") return HttpStatusCode.Conflict to failure("Одобренную заявку нельзя изменить")
        val section = input["section"]?.jsonPrimitive?.contentOrNull
        val payload = input["payload"] as? JsonObject
        if (section !in sections || payload == null) return HttpStatusCode.BadRequest to failure("Некорректная заявка")
        val updated = change(id, old.shopSlug(), section!!, payload, "Pending", reason = null, createdAt = old["createdAtUtc"]?.jsonPrimitive?.content)
        changes[index] = updated
        return ok(updated)
    }

    private fun seed(extra: Int) {
        shops += shop("shop-approved-published", "Sandbox Cafe", "Тестовая улица, 1", "Approved", reason = null, published = true)
        shops += shop("shop-approved-waiting", "Sandbox Roasting Point", "Тестовая улица, 7", "Approved", reason = null, published = false)
        shops += shop("shop-pending", "Sandbox New Place", "Тестовая улица, 12", "Pending", reason = null, published = false)
        shops += shop("shop-rejected", "Sandbox Unknown Address", "Нет такой улицы, 0", "Rejected", REJECTED_REASON, published = false)
        roasters += roaster("roaster-approved", "Sandbox Roaster", "Локальный обжарщик", "Approved", reason = null)
        roasters += roaster("roaster-pending", "Sandbox Pending Roaster", "Ждёт проверки", "Pending", reason = null)
        roasters += roaster("roaster-rejected", "Sandbox Rejected Roaster", null, "Rejected", "Недостаточно данных")
        changes += change("change-approved", PUBLISHED_SHOP, "Description", buildJsonObject { put("description", "Новое описание кофейни") }, "Approved", reason = null)
        changes += change("change-pending", PUBLISHED_SHOP, "Contacts", buildJsonObject {
            put("contacts", buildJsonObject { put("phoneNumber", "+375291112233"); put("siteLink", "https://sandbox.invalid") })
        }, "Pending", reason = null)
        changes += change("change-rejected", PUBLISHED_SHOP, "Photos", buildJsonObject {
            put("photos", buildJsonObject { putJsonArray("retainedPhotoIds") { add("sample") }; putJsonArray("newPhotos") {} })
        }, "Rejected", "Фото не подходят")
        repeat(extra) { index ->
            shops += shop("shop-extra-$index", "Sandbox Extra Shop ${index + 1}", "Тестовая улица, ${index + 20}", "Approved", reason = null, published = index % 2 == 0)
            roasters += roaster("roaster-extra-$index", "Sandbox Extra Roaster ${index + 1}", "Тестовый обжарщик ${index + 1}", "Approved", reason = null)
            changes += change("change-extra-$index", PUBLISHED_SHOP, sections[index % sections.size], buildJsonObject {
                if (index % sections.size == 2) put("description", "Описание ${index + 1}")
            }, "Approved", reason = null)
        }
    }

    private fun shop(id: String, name: String, address: String, status: String, reason: String?, published: Boolean) = buildJsonObject {
        put("id", id); put("name", name); put("address", address); put("moderationStatus", status)
        reason?.let { put("rejectedReason", it) }
        if (published) put("publishedShop", address(PUBLISHED_SHOP, "/coffee-shops/$PUBLISHED_SHOP"))
    }

    private fun roaster(id: String, name: String, about: String?, status: String, reason: String?) = buildJsonObject {
        put("id", id); put("name", name); about?.let { put("about", it) }; put("moderationStatus", status); reason?.let { put("rejectedReason", it) }
    }

    private fun change(id: String, shop: String, section: String, payload: JsonObject, status: String, reason: String?, createdAt: String? = null) = buildJsonObject {
        put("id", id); put("shop", address(shop, "/coffee-shops/$shop")); put("section", section); put("payload", payload); put("status", status)
        reason?.let { put("rejectionReason", it) }
        put("createdAtUtc", createdAt ?: "${today}T07:00:00Z")
    }

    private fun address(slug: String, path: String) = buildJsonObject {
        put("slug", slug); put("canonicalPath", path); put("revision", 0); put("isAlias", false)
    }
    private fun JsonObject.status() = (this["moderationStatus"] ?: this["status"])?.jsonPrimitive?.content
    private fun JsonObject.shopSlug() = this["shop"]?.jsonObject?.get("slug")?.jsonPrimitive?.content.orEmpty()
    private fun ok(data: JsonElement) = HttpStatusCode.OK to buildJsonObject { put("isSuccess", true); put("data", data) }
    private fun failure(message: String) = buildJsonObject { put("isSuccess", false); put("data", JsonNull); put("message", message) }
}
