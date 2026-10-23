package com.coffeepeek.admin.utils.image

import com.coffeepeek.admin.utils.CustomUrl
import com.coffeepeek.domain.model.Session
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.http.*
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.*

class ProtectedPhotoTransportTest {
    private fun session(expiry: Long = SystemPhotoCacheClock.wallMillis() / 1000 + 3600) =
        Session("header.${Base64.UrlSafe.encode("{\"exp\":$expiry}".encodeToByteArray())}.signature", "refresh", "viewer")
    private fun data(name: String): CustomUrl = CustomUrl(
        "https://gateway.example/api/v1/check-ins/visit/photos/$name/variants/source/1/feed.png",
        ProtectedPhotoSession.scope(session()), PhotoDisplayPermission(),
    )
    private fun headers(policy: String) = headersOf(
        HttpHeaders.CacheControl to listOf(policy), HttpHeaders.ETag to listOf("\"variant-a\""),
        HttpHeaders.ContentType to listOf("image/png"),
    )

    @Test fun dedicatedUncachedTransportReauthorizes304AndRestoresExactBytes() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            assertEquals("no-cache, no-store", request.headers[HttpHeaders.CacheControl])
            calls++
            if (calls == 1) {
                assertNull(request.headers[HttpHeaders.IfNoneMatch])
                respond(ByteReadChannel(byteArrayOf(2, 4)), HttpStatusCode.OK, headers("private,max-age=0,must-revalidate"))
            } else {
                assertEquals("\"variant-a\"", request.headers[HttpHeaders.IfNoneMatch])
                respond(ByteReadChannel(byteArrayOf()), HttpStatusCode.NotModified, headers("private,max-age=0,must-revalidate"))
            }
        })
        try {
            val request = data("native-bypass")
            assertContentEquals(byteArrayOf(2, 4), protectedVariantBytes(client, request))
            assertContentEquals(byteArrayOf(2, 4), protectedVariantBytes(client, request))
            assertEquals(2, calls)
            assertNotNull(request.displayPermission?.permission?.value)
        } finally { client.close() }
    }

    @Test fun aTimedOutAttemptIsRepeatedOnceAtOnceAndASecondTimeoutFails() = runTest {
        var calls = 0
        val flaky = HttpClient(MockEngine {
            calls++
            if (calls == 1) throw ConnectTimeoutException("stalled connection")
            respond(ByteReadChannel(byteArrayOf(7)), HttpStatusCode.OK, headers("private,max-age=60"))
        })
        val down = HttpClient(MockEngine { calls++; throw ConnectTimeoutException("offline") })
        try {
            assertContentEquals(byteArrayOf(7), protectedVariantBytes(flaky, data("retry-after-timeout")))
            assertEquals(2, calls)
            calls = 0
            assertFailsWith<ConnectTimeoutException> { protectedVariantBytes(down, data("always-times-out")) }
            assertEquals(2, calls)
        } finally { flaky.close(); down.close() }
    }

    @Test fun anAccessDeniedOrOtherFailureIsNotRetried() = runTest {
        var calls = 0
        val denied = HttpClient(MockEngine { calls++; respond(ByteReadChannel(byteArrayOf()), HttpStatusCode.Forbidden, headersOf()) })
        val broken = HttpClient(MockEngine { calls++; error("boom") })
        try {
            assertFailsWith<PhotoAccessDenied> { protectedVariantBytes(denied, data("denied-once")) }
            assertEquals(1, calls)
            calls = 0
            assertFailsWith<IllegalStateException> { protectedVariantBytes(broken, data("broken-once")) }
            assertEquals(1, calls)
        } finally { denied.close(); broken.close() }
    }

    @Test fun accidentallyCachedTransportFailsBeforeAnyProtectedRequest() = runTest {
        val client = HttpClient(MockEngine { error("must not use native HTTP cache") }) { install(HttpCache) }
        try {
            assertFailsWith<IllegalStateException> { protectedVariantBytes(client, data("misconfigured-cache")) }
        } finally { client.close() }
    }

    @Test fun noStoreNeverSendsRetainedValidatorAndAcceptsBackendEightMiBLimit() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            assertNull(request.headers[HttpHeaders.IfNoneMatch]); calls++
            respond(ByteReadChannel(ByteArray(5 * 1024 * 1024) { 1 }), HttpStatusCode.OK, headers("private,no-store"))
        })
        try {
            val request = data("large-no-store")
            repeat(2) { assertEquals(5 * 1024 * 1024, protectedVariantBytes(client, request).size) }
            assertEquals(2, calls)
        } finally { client.close() }
    }

    @Test fun anonymousVariantsReuseOnlyExplicitFreshPermissionWithinTheSameScope() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(ByteReadChannel(byteArrayOf(3, 7)), HttpStatusCode.OK, Headers.build {
                appendAll(headers("private,max-age=20,must-revalidate"))
                append(HttpHeaders.Date, GMTDate().toHttpDate())
            })
        })
        try {
            val scope = ProtectedPhotoSession.scope(null)
            val url = "https://gateway.example/api/v1/check-ins/visit/photos/anonymous-fresh/variants/source/1/feed.png"
            val first = CustomUrl(url, scope, PhotoDisplayPermission())
            assertContentEquals(byteArrayOf(3, 7), protectedVariantBytes(client, first))
            val reopened = CustomUrl(url, scope, PhotoDisplayPermission())
            assertContentEquals(byteArrayOf(3, 7), protectedVariantBytes(client, reopened))
            assertEquals(1, calls)
            assertNotNull(first.displayPermission?.reusablePermission)
            assertEquals(first.displayPermission?.reusablePermission, reopened.displayPermission?.reusablePermission)
            val renewal = first.copy(displayPermission = PhotoDisplayPermission(revalidate = true))
            assertContentEquals(byteArrayOf(3, 7), protectedVariantBytes(client, renewal))
            assertEquals(2, calls)
            ProtectedPhotoSession.invalidate()
            assertFailsWith<IllegalStateException> { protectedVariantBytes(client, reopened) }
        } finally { client.close() }
    }

    @Test fun malformedOrExpiredAuthenticatedJwtNeverFallsBackToAnonymous() = runTest {
        val client = HttpClient(MockEngine { error("must not request anonymously") })
        try {
            for (session in listOf(Session("invalid", "refresh", "viewer"), session(1))) {
                val request = data("expired").copy(permissionScope = ProtectedPhotoSession.scope(session))
                assertFailsWith<IllegalStateException> { protectedVariantBytes(client, request) }
            }
        } finally { client.close() }
    }

    @Test fun oversizedOrWrongMimeResponseNeverGrantsDisplayPermission() = runTest {
        for ((name, size, type) in listOf(Triple("oversized", 8 * 1024 * 1024 + 1, "image/png"), Triple("wrong-mime", 2, "text/html"))) {
            val client = HttpClient(MockEngine {
                respond(ByteReadChannel(ByteArray(size) { 1 }), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, type))
            })
            try {
                val request = data(name)
                assertFailsWith<IllegalStateException> { protectedVariantBytes(client, request) }
                assertNull(request.displayPermission?.permission?.value)
            } finally { client.close() }
        }
    }

    @Test fun sessionAndLocalInvalidationsRotateOpaqueScope() {
        val signedIn = session()
        val old = ProtectedPhotoSession.scope(signedIn)
        assertTrue(ProtectedPhotoSession.current(old))
        ProtectedPhotoSession.invalidate()
        assertFalse(ProtectedPhotoSession.current(old))
        val next = ProtectedPhotoSession.scope(signedIn)
        assertNotSame(old, next)
        ProtectedPhotoSession.scope(null)
        assertFalse(ProtectedPhotoSession.current(next))
    }
}
