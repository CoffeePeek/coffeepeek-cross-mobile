@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.media

import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.installImageAccess
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IosCatalogueMediaTest {
    @Test
    fun protectedCheckInAndBagPhotosUseAuthorizedTransportAndDecode() = runTest {
        val fixture = MediaFixture()
        try {
            for (path in listOf(CHECK_IN_PHOTO, BAG_PHOTO, CHECK_IN_VARIANT)) {
                assertNotNull(fixture.media.observeImage(path) { true }.first())
            }
            assertEquals(3, fixture.protectedRequests)
            assertEquals(0, fixture.publicRequests)
        } finally { fixture.close() }
    }

    @Test
    fun permissionInvalidationClearsAnAlreadyDecodedImage() = runTest {
        val fixture = MediaFixture()
        try {
            val frames = fixture.media.observeImage(CHECK_IN_PHOTO) { true }.onEach {
                if (it != null) fixture.invalidate()
            }.take(2).toList()
            assertNotNull(frames[0])
            assertNull(frames[1])
        } finally { fixture.close() }
    }

    @Test
    fun deniedPhotoProducesOnlyAClearedFrame() = runTest {
        val fixture = MediaFixture(HttpStatusCode.Forbidden)
        try {
            assertNull(fixture.media.observeImage(CHECK_IN_PHOTO) { true }.first())
            assertEquals(1, fixture.protectedRequests)
        } finally { fixture.close() }
    }

    @Test
    fun revokedOwnerClearsTheImageWithoutReloading() = runTest {
        val fixture = MediaFixture()
        var current = true
        try {
            val frames = fixture.media.observeImage(CHECK_IN_PHOTO) { current }.onEach {
                if (it != null) {
                    current = false
                    fixture.invalidate()
                }
            }.take(2).toList()
            assertNull(frames[1])
            assertEquals(1, fixture.protectedRequests)
        } finally { fixture.close() }
    }

    @Test
    fun publicAvatarCompletesWithItsDecodedFrame() = runTest {
        val fixture = MediaFixture()
        try {
            val frames = fixture.media.observeImage("/avatar.png") { true }.toList()
            assertEquals(1, frames.size)
            assertNotNull(frames.single())
            assertEquals(1, fixture.publicRequests)
        } finally { fixture.close() }
    }
}

private class MediaFixture(status: HttpStatusCode = HttpStatusCode.OK) {
    var publicRequests = 0
        private set
    var protectedRequests = 0
        private set
    private val publicClient = HttpClient(MockEngine {
        publicRequests++
        respond(PNG, headers = IMAGE_HEADERS)
    })
    private val protectedClient = HttpClient(MockEngine {
        protectedRequests++
        respond(PNG, status, IMAGE_HEADERS)
    })
    val media = IosCatalogueMedia(ImageUrlFetcher(publicClient, protectedClient), "https://example.test")

    init { invalidate() }

    fun invalidate() { installImageAccess(GuestImages, protectedClient) }

    fun close() {
        publicClient.close()
        protectedClient.close()
    }
}

private object GuestImages : SessionReader {
    override fun peekSession(): Session? = null
    override fun isActiveSession(session: Session?) = false
    override fun observeSession() = flowOf<Session?>(null)
    override suspend fun getSession(): Session? = null
}

private const val CHECK_IN_PHOTO = "/api/v1/check-ins/fixture/photos/photo"
private const val CHECK_IN_VARIANT = "$CHECK_IN_PHOTO/variants/revision/1/thumb.jpg"
private const val BAG_PHOTO = "/api/v1/shelf/bags/fixture/photos/photo"
private val IMAGE_HEADERS = headersOf(HttpHeaders.ContentType to listOf("image/png"),
    HttpHeaders.CacheControl to listOf("private, no-store"))
private val PNG = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=")
