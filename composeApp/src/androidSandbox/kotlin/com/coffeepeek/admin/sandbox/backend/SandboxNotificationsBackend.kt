package com.coffeepeek.admin.sandbox.backend

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.*
import java.time.LocalDate

/** Existing notification HTTP contracts over synthetic account data, including current destinations. */
internal class SandboxNotificationsBackend(private val today: LocalDate) {
    private val rows = linkedMapOf<String, JsonObject>()
    private var push = true

    init {
        reset(true)
    }

    fun reset(populated: Boolean) {
        rows.clear()
        push = true
        if (!populated) return
        add(NEW_SHOP, "NewShop", "Новая кофейня", "Sandbox Lab появилась на карте.", "Shop", "published-lab")
        add(CORRECTION, "CorrectionModeration", "Правка одобрена", "Описание Sandbox Cafe обновлено.", "ShopChange", "change-approved")
        add(READ_SHOP, "NewShop", "Кофейня на карте", "Посмотрите Sandbox Cafe.", "Shop", "published-cafe", read = true)
        add(APPROVED, "ShopApproved", "Кофейня одобрена", "Ваша кофейня Sandbox Cafe опубликована.", "Shop", "shop-approved-published")
        add(REJECTED, "ShopRejected", "Заявка отклонена", "Откройте отправленные кофейни, чтобы увидеть решение.", "Shop", "shop-rejected")
        add(MISSING, "NewShop", "Недоступная кофейня", "Проверка недоступной карточки.", "Shop", "missing")
        add(GENERAL, "General", "Новости CoffeePeek", "Тестовое сообщение без связанной страницы.", null, null)
    }

    fun handle(path: String, method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        if (!path.startsWith(ROOT)) return null
        if (path == ROOT && method == HttpMethod.Get) return inbox()
        if (path == "$ROOT/push") return pushPreference(method, input)
        if (path.startsWith("$ROOT/devices/") || path == "$ROOT/activity") return empty()
        if (path == "$ROOT/read" && method == HttpMethod.Put) return readAll(input)
        val id = path.removePrefix("$ROOT/").substringBefore('/')
        val item = rows[id] ?: return missing()
        return when {
            path.endsWith("/destination") && method == HttpMethod.Get -> destination(id)
            path.endsWith("/read") && method == HttpMethod.Put -> {
                markRead(id, item)
                empty()
            }
            else -> missing()
        }
    }

    private fun inbox() = ok(buildJsonObject {
        put("items", JsonArray(rows.values.toList()))
        put("nextCursor", JsonNull)
        put("unreadCount", rows.values.count { it["readAtUtc"] == JsonNull })
    })

    private fun pushPreference(method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        if (method == HttpMethod.Put) push = input["enabled"]?.jsonPrimitive?.booleanOrNull ?: push
        return ok(buildJsonObject { put("enabled", push) })
    }

    private fun destination(id: String): Pair<HttpStatusCode, JsonObject> = when (id) {
        NEW_SHOP -> target("Shop", "sandbox-lab")
        READ_SHOP -> target("Shop", "sandbox-cafe")
        CORRECTION -> target("ShopChange", "change-approved")
        APPROVED -> target("ShopSubmission", "shop-approved-published")
        REJECTED -> target("ShopSubmission", "shop-rejected")
        MISSING -> missing()
        else -> empty()
    }

    private fun readAll(input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val anchor = input["throughId"]?.jsonPrimitive?.contentOrNull
        val ids = rows.keys.toList()
        val index = ids.indexOf(anchor)
        if (index < 0) return missing()
        ids.drop(index).forEach { markRead(it, rows.getValue(it)) }
        return empty()
    }

    private fun markRead(id: String, item: JsonObject) {
        rows[id] = JsonObject(item + ("readAtUtc" to JsonPrimitive("${today}T13:00:00Z")))
    }

    private fun add(id: String, type: String, title: String, body: String, entity: String?, entityId: String?, read: Boolean = false) {
        rows[id] = buildJsonObject {
            put("id", id)
            put("type", type)
            put("title", title)
            put("body", body)
            put("createdAtUtc", "${today}T12:${59 - rows.size}:00Z")
            put("readAtUtc", if (read) JsonPrimitive("${today}T13:00:00Z") else JsonNull)
            put("entityType", entity?.let(::JsonPrimitive) ?: JsonNull)
            put("entityId", entityId?.let(::JsonPrimitive) ?: JsonNull)
            put("status", if (type == "ShopRejected") "Rejected" else "Approved")
        }
    }

    private fun target(kind: String, value: String) = ok(buildJsonObject {
        put("kind", kind)
        put("value", value)
    })
    private fun ok(value: JsonObject) = HttpStatusCode.OK to value
    private fun empty() = HttpStatusCode.NoContent to buildJsonObject {}
    private fun missing() = HttpStatusCode.NotFound to buildJsonObject { put("message", "Notification target not found") }

    companion object {
        const val ROOT = "/api/v1/me/notifications"
        const val NEW_SHOP = "a0000000-0000-4000-8000-000000000001"
        const val CORRECTION = "a0000000-0000-4000-8000-000000000002"
        const val READ_SHOP = "a0000000-0000-4000-8000-000000000003"
        const val APPROVED = "a0000000-0000-4000-8000-000000000004"
        const val REJECTED = "a0000000-0000-4000-8000-000000000005"
        const val MISSING = "a0000000-0000-4000-8000-000000000006"
        const val GENERAL = "a0000000-0000-4000-8000-000000000007"
    }
}
