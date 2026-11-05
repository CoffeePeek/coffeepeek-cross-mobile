@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.feature.profile.data

import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.UsernameChangeFailure
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileAccountWritesTest {
    @Test fun usernameAndAboutUseExactBodiesAndUpdateTheSharedCache() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.data.accounts.loadOwnProfile(false).getOrThrow()
            f.handler = { request ->
                if (request.url.encodedPath.endsWith("username")) accountResponse("""{"username":"Boris","address":{"slug":"boris","canonicalPath":"/users/boris"}}""")
                else accountResponse("null")
            }
            f.data.edit.updateUsername("Boris").getOrThrow()
            f.data.edit.updateAbout("Changed").getOrThrow()
            assertEquals("Boris", f.data.accounts.cachedOwnProfile()?.userName)
            assertEquals("boris", f.data.accounts.cachedOwnProfile()?.address?.slug)
            assertEquals("Changed", f.data.accounts.cachedOwnProfile()?.about)
            assertEquals(listOf(HttpMethod.Get, HttpMethod.Patch, HttpMethod.Patch), f.requests.map { it.method })
            assertEquals("""{"username":"Boris"}""", f.requests[1].body.toByteArray().decodeToString())
            assertEquals("""{"about":"Changed"}""", f.requests[2].body.toByteArray().decodeToString())
        } finally { f.close() }
    }

    @Test fun lateWritesNeverChangeAnotherAccountsCache() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.data.accounts.loadOwnProfile(false).getOrThrow()
            val gate = CompletableDeferred<Unit>()
            f.handler = { gate.await(); accountResponse("""{"username":"Late"}""") }
            val pending = async(start = CoroutineStart.UNDISPATCHED) { f.data.edit.updateUsername("Late") }
            f.sessions.state.value = Session("b-token", userId = "b")
            f.handler = { accountResponse(profileJson("B")) }
            f.data.accounts.loadOwnProfile(false).getOrThrow()
            gate.complete(Unit)
            assertTrue(pending.await().isFailure)
            assertEquals("B", f.data.accounts.cachedOwnProfile()?.userName)
        } finally { f.close() }
    }

    @Test fun sameAccountTokenRefreshKeepsWritesValid() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.data.accounts.loadOwnProfile(false).getOrThrow()
            val gate = CompletableDeferred<Unit>()
            f.handler = { gate.await(); accountResponse("null") }
            val pending = async(start = CoroutineStart.UNDISPATCHED) { f.data.edit.updateAbout("Fresh") }
            f.sessions.state.value = Session("renewed", userId = "a")
            gate.complete(Unit)
            pending.await().getOrThrow()
            assertEquals("Fresh", f.data.accounts.cachedOwnProfile()?.about)
        } finally { f.close() }
    }

    @Test fun readStartedBeforeEditDoesNotRollTheEditBack() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.data.accounts.loadOwnProfile(false).getOrThrow()
            val gate = CompletableDeferred<Unit>()
            f.handler = { gate.await(); accountResponse(profileJson()) }
            val read = async(start = CoroutineStart.UNDISPATCHED) { f.data.accounts.loadOwnProfile(true) }
            f.handler = { accountResponse("null") }
            f.data.edit.updateAbout("Edited").getOrThrow()
            gate.complete(Unit)
            assertEquals("Edited", read.await().getOrThrow().about)
            assertEquals("Edited", f.data.accounts.cachedOwnProfile()?.about)
        } finally { f.close() }
    }

    @Test fun deletionRequestKeepsSessionAndTreatsMissingRequestAsNull() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { respond("", HttpStatusCode.NotFound) }
            assertNull(f.data.deletion.existingRequest().getOrThrow())
            f.handler = { accountResponse("""{"ResendAvailableAtUtc":"later"}""") }
            assertEquals("later", f.data.deletion.request().getOrThrow().resendAvailableAtUtc)
            assertEquals("a", f.data.accounts.activeAccount())
            assertEquals("/api/Users/me", f.requests.last().url.encodedPath)
            assertEquals(HttpMethod.Delete, f.requests.last().method)
        } finally { f.close() }
    }

    @Test fun usernameServerErrorsKeepTypedReasonsAndRetryWithoutLeakingProse() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { respond("""{"isSuccess":false,"message":"Username can be changed once every 7 days. Try again after 2026-10-29T17:12:31Z."}""",
                HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.ContentType, "application/json")) }
            val error = assertIs<UsernameChangeFailure>(f.data.edit.updateUsername("Boris").exceptionOrNull())
            assertEquals(UsernameChangeFailure.Reason.LimitReached, error.reason)
            assertEquals("2026-10-29T17:12:31Z", error.retryAtUtc)
            assertNull(error.message)
        } finally { f.close() }
    }

    @Test fun cancellationOfWritesIsNeverAValidationFailure() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { throw CancellationException("cancelled") }
            assertFailsWith<CancellationException> { f.data.edit.updateUsername("Boris") }
            assertFailsWith<CancellationException> { f.data.edit.updateAbout("About") }
            assertFailsWith<CancellationException> { f.data.deletion.request() }
        } finally { f.close() }
    }
}
