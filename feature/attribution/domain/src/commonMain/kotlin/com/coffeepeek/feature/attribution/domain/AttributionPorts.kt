package com.coffeepeek.feature.attribution.domain

interface AttributionBackend {
    suspend fun enabled(): Boolean
    suspend fun resolve(code: String): AttributionResolution
    suspend fun touch(observation: AttributionObservation, observer: String?): AttributionEvidence
    suspend fun claim(evidence: AttributionEvidence, identity: AttributionIdentity, key: String)
    suspend fun installation(id: String, channel: String, evidence: AttributionEvidence, identity: AttributionIdentity, key: String)
    suspend fun revoke(token: String)
    suspend fun share(canonicalPath: String, placement: String, key: String, observer: String?): String
}

interface AttributionStore {
    suspend fun load(): AttributionLocalState
    suspend fun save(state: AttributionLocalState)
}

data class AttributionClock(val now: () -> Long, val uuid: () -> String)

/** Read once, only after account permission. Returns a validated handoff token, never a raw referrer. */
fun interface AttributionInstallSource {
    suspend fun read(): String?
}

fun validAttributionCode(code: String): Boolean = code.matches(Regex("[A-Za-z0-9_-]{16,128}"))

fun validCanonicalAttributionPath(path: String): Boolean = path.length <= 512 &&
    path.startsWith('/') && !path.startsWith("//") && !path.startsWith("/l/") &&
    path.none { it == '?' || it == '#' || it == '\\' || it.isISOControl() }
