package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class SandboxOwnershipBackendTest {
    @Test fun ownerFixturesAndNewClaimsStayInsideTheInMemoryBackend() = runTest {
        withSandbox { backend ->
            val initial = backend.gateway.get("/api/owner/ownership-claims").body<JsonObject>()
            assertEquals(2, initial["data"]!!.jsonObject["totalCount"]!!.jsonPrimitive.int)
            val submitted = backend.gateway.post("/api/owner/ownership-claims") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("businessType", "CoffeeShop")
                    put("businessSlug", "sandbox-lab")
                    put("contact", "fixture@example.test")
                    put("evidence", "Local fixture")
                })
            }
            assertEquals(HttpStatusCode.Created, submitted.status)
            assertEquals("Pending", submitted.body<JsonObject>()["data"]!!.jsonObject["status"]!!.jsonPrimitive.content)
        }
    }
    @Test fun aStaleProfileRevisionIsRejectedWithoutChangingTheFakeBusiness() = runTest {
        withSandbox { backend ->
            val path = "/api/owner/roasters/${SandboxOwnershipBackend.ROASTER}"
            val failure = assertFailsWith<ResponseException> {
                backend.gateway.put(path) {
                    header(HttpHeaders.IfMatch, "\"0\"")
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { put("name", "Must not apply") })
                }
            }
            assertEquals(HttpStatusCode.Conflict, failure.response.status)
            assertEquals("Sandbox Roaster", backend.gateway.get(path).body<JsonObject>()["data"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        }
    }
}
private suspend fun withSandbox(block: suspend (SandboxBackend) -> Unit) {
    val backend = SandboxBackend(byteArrayOf(1))
    try { block(backend) } finally {
        backend.gateway.close()
        backend.uploader.close()
    }
}
