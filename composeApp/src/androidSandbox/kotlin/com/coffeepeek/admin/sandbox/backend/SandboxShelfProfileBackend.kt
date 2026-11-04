package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.*

/** Local privacy fixture. Synthetic owner headers are not production authentication. */
internal class SandboxShelfProfileBackend(private val shelf: SandboxShelfBackend, private val authorAvailable: (String) -> Boolean) {
    private val visibility = mutableMapOf<String, Boolean>()
    private val publicShelf = Regex("/api/v1/profiles/([^/]+)/shelf")
    private val publicPhoto = Regex("/api/v1/profiles/([^/]+)/shelf/bags/([^/]+)/photos/([^/]+)")

    fun reset() = visibility.clear()

    fun handle(request: HttpRequestData, input: JsonObject): Pair<HttpStatusCode, JsonObject>? {
        val path = request.url.encodedPath
        if (path == VISIBILITY) return preference(request.headers[SandboxSavedCollectionsBackend.OWNER], request.method, input)
        val slug = publicShelf.matchEntire(path)?.groupValues?.get(1) ?: return null
        if (request.method != HttpMethod.Get) return fail(HttpStatusCode.MethodNotAllowed)
        val owner = profileOwner(slug) ?: return fail(HttpStatusCode.NotFound)
        if (!authorAvailable(slug)) return fail(HttpStatusCode.NotFound)
        if (visibility[owner] != true) return ok(JsonNull)
        val items = if (owner == SELF) shelf.publicItems(slug) else JsonArray(emptyList())
        return ok(buildJsonObject {
            put("items", items)
            put("totalCount", items.size)
        })
    }

    fun cardKey(path: String): String? {
        val match = publicPhoto.matchEntire(path) ?: return null
        val (slug, bag, photo) = match.destructured
        if (profileOwner(slug) != SELF || visibility[SELF] != true || !authorAvailable(slug)) return null
        return shelf.publicCardKey(bag, photo)
    }

    private fun preference(rawOwner: String?, method: HttpMethod, input: JsonObject): Pair<HttpStatusCode, JsonObject> {
        val owner = owner(rawOwner) ?: return fail(HttpStatusCode.Unauthorized)
        if (method == HttpMethod.Put) {
            val value = (input["isVisible"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
                ?: return fail(HttpStatusCode.BadRequest)
            visibility[owner] = value
        } else if (method != HttpMethod.Get) return fail(HttpStatusCode.MethodNotAllowed)
        return ok(buildJsonObject { put("isVisible", visibility[owner] == true) })
    }

    private fun owner(value: String?): String? = when (value) {
        "sandbox", SandboxApplicationBackend.SELF_ID, SandboxSavedCollectionsBackend.LOCAL_ACCOUNT -> SELF
        SandboxApplicationBackend.FRIEND_ID, SandboxApplicationBackend.FRIEND_SLUG -> FRIEND
        else -> null
    }

    private fun profileOwner(slug: String): String? = when (slug) {
        "sandbox" -> SELF
        SandboxApplicationBackend.FRIEND_SLUG -> FRIEND
        else -> null
    }

    private fun ok(data: JsonElement) = HttpStatusCode.OK to buildJsonObject { put("isSuccess", true); put("data", data) }
    private fun fail(status: HttpStatusCode) = status to buildJsonObject { put("isSuccess", false); put("data", JsonNull) }

    companion object {
        const val VISIBILITY = "/api/v1/shelf/profile-visibility"
        private const val SELF = "self"
        private const val FRIEND = "friend"
    }
}
