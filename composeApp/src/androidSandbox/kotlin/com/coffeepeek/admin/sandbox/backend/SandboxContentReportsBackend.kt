package com.coffeepeek.admin.sandbox.backend

import io.ktor.http.*
import kotlinx.serialization.json.*
import java.util.UUID

internal data class SandboxContentReport(val checkInId: String, val reason: String)

/** In-memory moderation fixture; no real report, account or external request. */
internal class SandboxContentReportsBackend {
    private val submitted = mutableListOf<SandboxContentReport>()
    val records: List<SandboxContentReport> get() = submitted.toList()
    fun clear() { submitted.clear() }

    fun submit(method: HttpMethod, id: String, self: Boolean, reason: String): Pair<HttpStatusCode, JsonObject> {
        val status = when {
            method != HttpMethod.Post -> HttpStatusCode.MethodNotAllowed
            self -> HttpStatusCode.Forbidden
            reason.trim().length !in 1..2000 -> HttpStatusCode.BadRequest
            else -> HttpStatusCode.Created
        }
        if (status == HttpStatusCode.Created) submitted += SandboxContentReport(id, reason.trim())
        return status to buildJsonObject {
            put("isSuccess", status == HttpStatusCode.Created)
            put("data", if (status == HttpStatusCode.Created) buildJsonObject { put("id", UUID.randomUUID().toString()) } else JsonNull)
        }
    }
}
