package com.coffeepeek.feature.profile.data

import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileAvatarUploadTest {
    private val image = ProfileAvatarImage("avatar.png", "image/png", byteArrayOf(1, 2, 3))
    private val target = """{"uploadUrl":"https://images.example/avatar?signed=1","storageKey":"avatar-key"}"""

    @Test fun avatarUsesPresignedCredentialFreeUploadThenPatchesMetadataAndRefreshesTheSameCache() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { request -> accountResponse(when (request.method) {
                HttpMethod.Post -> target
                HttpMethod.Patch -> "null"
                else -> profileJson()
            }) }
            f.data.edit.updateAvatar(image).getOrThrow()
            assertEquals(listOf("/api/Photos/avatar", "/api/Users/me/avatar", "/api/Users/me"), f.requests.map { it.url.encodedPath })
            assertEquals("Anna", f.data.accounts.cachedOwnProfile()?.userName)
            assertUpload(f)
        } finally { f.close() }
    }

    private suspend fun assertUpload(f: ProfileAccountFixture) {
        val uploaded = f.uploadRequests.single()
        assertEquals(HttpMethod.Put, uploaded.method)
        assertEquals("https://images.example/avatar?signed=1", uploaded.url.toString())
        assertEquals(image.bytes.toList(), uploaded.body.toByteArray().toList())
        assertEquals("image/png", uploaded.body.contentType.toString())
        assertNull(uploaded.headers[HttpHeaders.Authorization])
        assertNull(uploaded.headers[HttpHeaders.Cookie])
        val metadata = Json.parseToJsonElement(f.requests[1].body.toByteArray().decodeToString()).jsonObject
        assertEquals(setOf("uploadedPhoto"), metadata.keys)
        assertEquals("\"avatar-key\"", metadata.getValue("uploadedPhoto").jsonObject["storageKey"].toString())
    }

    @Test fun accountReplacementAfterUploadNeverPatchesTheNextAccountsAvatar() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { accountResponse(target) }
            f.uploadHandler = {
                f.sessions.state.value = com.coffeepeek.core.session.Session("b", userId = "b")
                respond("", HttpStatusCode.OK)
            }
            assertTrue(f.data.edit.updateAvatar(image).isFailure)
            assertEquals(1, f.requests.size)
            assertNull(f.data.accounts.cachedOwnProfile())
        } finally { f.close() }
    }

    @Test fun privatePresignedUrlIsRejectedBeforeAnyBytesOrAvatarPatch() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { accountResponse(target.replace("images.example", "127.0.0.1")) }
            assertTrue(f.data.edit.updateAvatar(image).isFailure)
            assertTrue(f.uploadRequests.isEmpty())
            assertEquals(1, f.requests.size)
        } finally { f.close() }
    }

    @Test fun failedOrCancelledUploadNeverPatchesAvatar() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { accountResponse(target) }
            f.uploadHandler = { respond("", HttpStatusCode.Forbidden) }
            assertTrue(f.data.edit.updateAvatar(image).isFailure)
            assertEquals(1, f.requests.size)
            f.uploadHandler = { throw CancellationException("cancelled") }
            assertFailsWith<CancellationException> { f.data.edit.updateAvatar(image) }
            assertEquals(2, f.requests.size)
        } finally { f.close() }
    }
}
