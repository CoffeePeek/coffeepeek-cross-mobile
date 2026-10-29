@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.feature.profile.data

import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.profile.domain.ProfileAddress
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileAccountRepositoryTest {
    @Test fun ownProfileAcceptsTheCapitalizedLegacyEnvelope() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { respond("""{"IsSuccess":true,"Data":${profileJson()}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")) }
            assertEquals("Anna", f.data.accounts.loadOwnProfile(false).getOrThrow().userName)
        } finally { f.close() }
    }

    @Test fun currentKeyTracksAccountReplacementAndKeepsTokenRotationStable() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            val seen = mutableListOf<String?>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.data.accounts.observeActiveAccount().toList(seen) }
            assertEquals("a", f.data.accounts.activeAccount())
            f.sessions.state.value = Session("rotated", userId = "a")
            f.sessions.state.value = Session("token-only")
            assertEquals("token-only", f.data.accounts.activeAccount())
            f.sessions.state.value = Session(" ", userId = "inactive")
            assertNull(f.data.accounts.activeAccount())
            f.sessions.state.value = null
            assertEquals(listOf("a", "token-only", null), seen)
        } finally { f.close() }
    }

    @Test fun oneSharedCacheRetainsAllProfileFieldsAndRefreshIsExplicit() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            assertNull(f.data.accounts.cachedOwnProfile())
            assertNull(f.data.accounts.observeOwnProfile().first())
            val row = f.data.accounts.loadOwnProfile(false).getOrThrow()
            assertEquals(listOf(3, 2, 1, 4), listOf(row.checkInCount, row.addedShopsCount, row.addedRoastersCount, row.followersCount))
            assertEquals(ProfileAddress("anna", "/users/anna", 2, true), row.address)
            assertEquals(row, f.data.accounts.observeOwnProfile().first())
            assertEquals(row, f.data.accounts.loadOwnProfile(false).getOrThrow())
            assertEquals(1, f.requests.size)
            f.data.accounts.loadOwnProfile(true).getOrThrow()
            assertEquals(2, f.requests.size)
            assertEquals("no-store", f.requests.last().headers["Cache-Control"])
        } finally { f.close() }
    }

    @Test fun logoutRejectsOldReadAndNeverMakesAnotherRequestAsAGuest() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.data.accounts.loadOwnProfile(false).getOrThrow()
            val gate = CompletableDeferred<Unit>()
            f.handler = { gate.await(); accountResponse(profileJson("Late")) }
            val pending = async(start = CoroutineStart.UNDISPATCHED) { f.data.accounts.loadOwnProfile(true) }
            f.sessions.state.value = null
            runCurrent()
            assertNull(f.data.accounts.cachedOwnProfile())
            gate.complete(Unit)
            assertTrue(pending.await().isFailure)
            assertTrue(f.data.accounts.loadOwnProfile(false).isFailure)
            assertEquals(2, f.requests.size)
        } finally { f.close() }
    }

    @Test fun anotherAccountLoadsWhileThePreviousReadWaitsAndLateResponseCannotOverwriteIt() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            val gate = CompletableDeferred<Unit>()
            f.handler = { gate.await(); accountResponse(profileJson("A")) }
            val pending = async(start = CoroutineStart.UNDISPATCHED) { f.data.accounts.loadOwnProfile(true) }
            f.sessions.state.value = Session("b-token", userId = "b")
            f.handler = { accountResponse(profileJson("B")) }
            assertEquals("B", f.data.accounts.loadOwnProfile(false).getOrThrow().userName)
            gate.complete(Unit)
            assertTrue(pending.await().isFailure)
            assertEquals("B", f.data.accounts.cachedOwnProfile()?.userName)
        } finally { f.close() }
    }

    @Test fun leavingAndReenteringTheSameAccountRevokesTheOldRead() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            val gate = CompletableDeferred<Unit>()
            f.handler = { gate.await(); accountResponse(profileJson()) }
            val pending = async(start = CoroutineStart.UNDISPATCHED) { f.data.accounts.loadOwnProfile(true) }
            f.sessions.state.value = null
            runCurrent()
            f.sessions.state.value = Session("new-token", userId = "a")
            runCurrent()
            gate.complete(Unit)
            assertTrue(pending.await().isFailure)
            assertNull(f.data.accounts.cachedOwnProfile())
        } finally { f.close() }
    }

    @Test fun cancellationEscapesLoadAndNeverPublishesProfile() = runTest {
        val f = ProfileAccountFixture(backgroundScope)
        try {
            f.handler = { throw CancellationException("cancelled") }
            assertFailsWith<CancellationException> { f.data.accounts.loadOwnProfile(true) }
            assertNull(f.data.accounts.cachedOwnProfile())
        } finally { f.close() }
    }
}
