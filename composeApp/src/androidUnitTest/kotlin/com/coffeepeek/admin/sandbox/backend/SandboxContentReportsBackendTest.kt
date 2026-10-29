package com.coffeepeek.admin.sandbox.backend

import io.ktor.http.*
import kotlin.test.*

class SandboxContentReportsBackendTest {
    @Test fun reportsAreTrimmedLocalRecordsAndResetClearsThem() {
        val backend = SandboxContentReportsBackend()
        val result = backend.submit(HttpMethod.Post, "fixture", false, "  Причина  ")
        assertEquals(HttpStatusCode.Created, result.first)
        assertEquals(listOf(SandboxContentReport("fixture", "Причина")), backend.records)
        backend.clear()
        assertTrue(backend.records.isEmpty())
    }
    @Test fun ownPostsInvalidReasonsAndWrongMethodsDoNotCreateRecords() {
        val backend = SandboxContentReportsBackend()
        assertEquals(HttpStatusCode.Forbidden, backend.submit(HttpMethod.Post, "self", true, "reason").first)
        assertEquals(HttpStatusCode.BadRequest, backend.submit(HttpMethod.Post, "fixture", false, " ").first)
        assertEquals(HttpStatusCode.BadRequest, backend.submit(HttpMethod.Post, "fixture", false, "x".repeat(2001)).first)
        assertEquals(HttpStatusCode.MethodNotAllowed, backend.submit(HttpMethod.Put, "fixture", false, "reason").first)
        assertTrue(backend.records.isEmpty())
    }
}
