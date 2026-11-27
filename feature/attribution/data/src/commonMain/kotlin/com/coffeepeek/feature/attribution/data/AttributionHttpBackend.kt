package com.coffeepeek.feature.attribution.data

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.network.authorizeWhile
import com.coffeepeek.feature.attribution.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.time.Instant

internal class AttributionHttpBackend(private val client: HttpClient, private val sessions: SessionReader, private val authenticated: HttpClient = client) : AttributionBackend {
    override suspend fun enabled(): Boolean {
        val data = request("capabilities", HttpMethod.Get)
        return data["analyticsEnabled"]?.jsonPrimitive?.booleanOrNull == true &&
            data["supportedMajorVersions"]?.jsonArray?.any { it.jsonPrimitive.intOrNull == 1 } == true
    }

    override suspend fun resolve(code: String): AttributionResolution {
        val data = request("resolve", body = buildJsonObject { put("contractVersion", 1); put("code", code) }, key = randomKey())
        return AttributionResolution(data.text("canonicalPath"), data["trackingEnabled"]?.jsonPrimitive?.booleanOrNull == true)
    }

    override suspend fun touch(observation: AttributionObservation, observer: String?): AttributionEvidence {
        val body = buildJsonObject {
            put("contractVersion", 1)
            put("observationId", observation.id)
            put("entry", attributionEntry(observation.entry))
            put("context", attributionContext(observation.identity))
        }
        val data = request("touches", body = body, key = observation.id, identity = observation.identity, observer = observer)
        return AttributionEvidence(data.text("observerToken"), epoch(data.text("observerTokenExpiresAtUtc")),
            data.text("receipt"), epoch(data.text("receiptExpiresAtUtc")), observation.identity.accountId)
    }

    override suspend fun claim(evidence: AttributionEvidence, identity: AttributionIdentity, key: String) {
        request("claim", body = buildJsonObject {
            put("contractVersion", 1)
            put("receipt", evidence.receipt)
            put("context", attributionContext(identity))
        }, key = key, identity = identity, observer = evidence.observerToken)
    }

    override suspend fun installation(id: String, channel: String, evidence: AttributionEvidence, identity: AttributionIdentity, key: String) {
        request("installations", body = buildJsonObject {
            put("contractVersion", 1)
            put("installationId", id)
            put("platform", identity.platform)
            put("installChannel", channel)
            put("firstRunObserved", false)
            put("receipt", evidence.receipt)
            put("context", attributionContext(identity))
        }, key = key, identity = identity, observer = evidence.observerToken)
    }

    override suspend fun revoke(token: String) {
        request("observer", HttpMethod.Delete, observer = token)
    }

    override suspend fun share(canonicalPath: String, placement: String, key: String, observer: String?): String {
        val session = sessions.getSession() ?: throw AttributionFailure(401, null)
        val data = request("share-links", body = buildJsonObject {
            put("destination", attributionDestination(canonicalPath))
            put("placement", placement)
        }, key = key, observer = observer, account = session.userId ?: throw AttributionFailure(401, null))
        return validatedShareUrl(data.text("publicUrl"))
    }

    private suspend fun request(
        route: String, method: HttpMethod = HttpMethod.Post, body: JsonObject? = null, key: String? = null,
        identity: AttributionIdentity? = null, observer: String? = null, account: String? = identity?.accountId,
    ): JsonObject {
        val session = if (account == null) null else sessions.getSession()
        if (account != null && (session?.userId != account || !sessions.isActiveSession(session))) throw AttributionFailure(401, null)
        if (identity != null && !identity.isCurrent()) throw AttributionFailure(403, "ANALYTICS_NOT_PERMITTED")
        val generation = sessions.accountGeneration()
        val http = if (session == null) client else authenticated
        val response = http.request(if (route == "share-links") "/api/v1/share-links" else "/api/v1/attribution/$route") {
            this.method = method
            if (account != null) authorizeWhile {
                sessions.accountGeneration() == generation && sessions.peekSession()?.userId == account && (identity?.isCurrent() != false)
            }
            attributionHeaders(key, observer, session?.accessToken, body)
        }
        val raw = response.bodyAsText()
        val parsed = raw.takeIf { it.isNotBlank() }?.let { Json.parseToJsonElement(it).jsonObject }
        if (!response.status.isSuccess()) throw AttributionFailure(response.status.value, errorCode(parsed))
        if (response.status.value == 204) return buildJsonObject { }
        return requireNotNull(parsed?.get("data")?.jsonObject)
    }

    private fun errorCode(body: JsonObject?): String? = body?.get("code")?.jsonPrimitive?.contentOrNull
        ?: body?.get("errorCode")?.jsonPrimitive?.contentOrNull
    private fun epoch(value: String) = Instant.parse(value).toEpochMilliseconds()
}

private fun HttpRequestBuilder.attributionHeaders(key: String?, observer: String?, token: String?, body: JsonObject?) {
    header(HttpHeaders.CacheControl, "no-store")
    key?.let { header("Idempotency-Key", it) }
    observer?.let { header("X-Attribution-Observer", it) }
    token?.let { bearerAuth(it) }
    if (body != null) {
        contentType(ContentType.Application.Json)
        setBody(body.toString())
    }
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
internal fun randomKey() = kotlin.uuid.Uuid.random().toString()
