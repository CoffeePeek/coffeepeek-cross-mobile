package com.coffeepeek.admin.sandbox.backend

import kotlinx.serialization.json.*

/** Home fixtures use the same canonical post shape as the server feed and detail endpoints. */
internal fun JsonObject.asSandboxHomePost() = buildJsonObject {
    forEach { (key, value) -> put(key, value) }
    put("kind", "Home")
    put("authorId", SandboxApplicationBackend.SELF_ID)
    put("username", "Sandbox")
    putJsonObject("author") { put("slug", "sandbox"); put("canonicalPath", "/users/sandbox") }
    put("home", this@asSandboxHomePost)
    put("visitedAt", this@asSandboxHomePost["preparedAtUtc"] ?: JsonNull)
    put("shelfBagPhotoUrl", (this@asSandboxHomePost["shelfBagPhoto"] as? JsonObject)?.get("url") ?: JsonNull)
}
