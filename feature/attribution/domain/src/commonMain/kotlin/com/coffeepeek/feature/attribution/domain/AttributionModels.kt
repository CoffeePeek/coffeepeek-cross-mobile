package com.coffeepeek.feature.attribution.domain

data class AttributionIdentity(
    val accountId: String,
    val sdkDistinctId: String,
    val sessionId: String?,
    val platform: String,
    val appVersion: String,
    val environment: String,
    val isCurrent: () -> Boolean,
)

sealed interface AttributionEntry {
    data class Link(val code: String) : AttributionEntry
    data class Handoff(val token: String, val transport: String) : AttributionEntry
    data class ExplicitCode(val code: String) : AttributionEntry
    data object Unknown : AttributionEntry
}

data class AttributionEvidence(
    val observerToken: String,
    val observerExpiresAt: Long,
    val receipt: String,
    val receiptExpiresAt: Long,
    val accountId: String,
)

data class ObserverRevocation(val token: String, val expiresAt: Long)

data class AttributionLocalState(
    val evidence: AttributionEvidence? = null,
    val installationId: String? = null,
    val installationConfirmed: Boolean = false,
    val referrerRead: Boolean = false,
    val revocations: List<ObserverRevocation> = emptyList(),
)

data class AttributionObservation(
    val id: String,
    val entry: AttributionEntry,
    val identity: AttributionIdentity,
)

data class AttributionResolution(val canonicalPath: String, val trackingEnabled: Boolean)

class AttributionFailure(val status: Int, val code: String?) : Exception("Attribution request failed ($status)")
