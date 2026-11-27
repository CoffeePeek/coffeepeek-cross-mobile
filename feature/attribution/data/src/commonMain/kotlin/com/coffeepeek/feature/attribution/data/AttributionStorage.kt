package com.coffeepeek.feature.attribution.data

import com.coffeepeek.feature.attribution.domain.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class StoredEvidence(val observer: String, val observerExpiry: Long, val receipt: String, val receiptExpiry: Long, val account: String)
@Serializable
private data class StoredRevocation(val token: String, val expiry: Long)
@Serializable
private data class StoredAttribution(
    val version: Int = 1,
    val evidence: StoredEvidence? = null,
    val installationId: String? = null,
    val installationConfirmed: Boolean = false,
    val referrerRead: Boolean = false,
    val revocations: List<StoredRevocation> = emptyList(),
)

internal fun encodeAttribution(state: AttributionLocalState): String = Json.encodeToString(StoredAttribution(
    evidence = state.evidence?.let { StoredEvidence(it.observerToken, it.observerExpiresAt, it.receipt, it.receiptExpiresAt, it.accountId) },
    installationId = state.installationId,
    installationConfirmed = state.installationConfirmed,
    referrerRead = state.referrerRead,
    revocations = state.revocations.map { StoredRevocation(it.token, it.expiresAt) },
))

internal fun decodeAttribution(value: String?): AttributionLocalState {
    if (value.isNullOrBlank() || value.length > 131072) return AttributionLocalState()
    val stored = try { Json.decodeFromString<StoredAttribution>(value) } catch (_: Exception) { return AttributionLocalState() }
    if (stored.version != 1) return AttributionLocalState()
    return AttributionLocalState(
        evidence = stored.evidence?.let { AttributionEvidence(it.observer, it.observerExpiry, it.receipt, it.receiptExpiry, it.account) },
        installationId = stored.installationId,
        installationConfirmed = stored.installationConfirmed,
        referrerRead = stored.referrerRead,
        revocations = stored.revocations.takeLast(20).map { ObserverRevocation(it.token, it.expiry) },
    )
}
