package com.coffeepeek.core.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Persisted shape of a session. The field names are the stored JSON and must not change. */
@Serializable
data class SessionCredentials(val accessToken: String, val refreshToken: String = "")

internal val SessionJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
