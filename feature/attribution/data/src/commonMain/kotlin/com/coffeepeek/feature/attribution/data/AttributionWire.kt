package com.coffeepeek.feature.attribution.data

import com.coffeepeek.feature.attribution.domain.*
import io.ktor.http.parseQueryString
import kotlinx.serialization.json.*

internal fun attributionContext(identity: AttributionIdentity) = buildJsonObject {
    put("platform", identity.platform)
    put("appVersion", identity.appVersion.take(64))
    put("environment", identity.environment)
    put("permissionBasis", "account_preference")
    put("permissionPolicyVersion", "acquisition-v1")
    put("sdkDistinctId", identity.sdkDistinctId)
    identity.sessionId?.let { put("sessionId", it) }
}

internal fun attributionEntry(entry: AttributionEntry) = buildJsonObject {
    when (entry) {
        is AttributionEntry.Link -> { put("kind", "tracked_link"); put("code", entry.code) }
        is AttributionEntry.ExplicitCode -> { put("kind", "explicit_code"); put("code", entry.code) }
        is AttributionEntry.Handoff -> {
            put("kind", "handoff")
            put("handoffToken", entry.token)
            put("transport", entry.transport)
        }
        AttributionEntry.Unknown -> { put("kind", "external"); put("landingPath", "/") }
    }
}

internal fun attributionDestination(path: String): JsonObject {
    require(validCanonicalAttributionPath(path))
    val parts = path.trim('/').split('/')
    return buildJsonObject {
        when {
            path == "/" -> put("kind", "home")
            parts.size == 1 && parts[0] in setOf("coffee-shops", "roasters", "coffees") -> {
                put("kind", "catalog")
                put("catalog", if (parts[0] == "coffee-shops") "shops" else parts[0])
            }
            parts.size == 2 -> putEntityDestination(parts[0], parts[1])
            else -> error("Unsupported share destination")
        }
    }
}

private fun JsonObjectBuilder.putEntityDestination(kind: String, slug: String) {
    require(slug.isNotBlank() && slug !in setOf(".", ".."))
    if (kind == "check-ins") {
        require(slug.matches(Regex("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")))
        put("kind", "check_in")
        put("id", slug)
        return
    }
    val entity = mapOf("coffee-shops" to "coffee_shop", "shops" to "coffee_shop", "roasters" to "roaster", "coffees" to "coffee", "users" to "user")[kind]
    require(entity != null)
    put("kind", "entity")
    put("entityType", entity)
    put("slug", slug)
}

internal fun JsonObject.text(key: String): String = requireNotNull(get(key)?.jsonPrimitive?.contentOrNull)

/** Never allow a server response to turn the system share action into an external redirect. */
internal fun validatedShareUrl(value: String): String {
    require(value.startsWith("https://coffeepeek.by/l/"))
    require(validAttributionCode(value.removePrefix("https://coffeepeek.by/l/")))
    return value
}

internal fun handoffFromReferrer(value: String): String? {
    if (value.length > 8192) return null
    return parseQueryString(value).getAll("cp_handoff")?.singleOrNull()
        ?.takeIf { it.length in 1..4096 && it.none(Char::isISOControl) }
}
