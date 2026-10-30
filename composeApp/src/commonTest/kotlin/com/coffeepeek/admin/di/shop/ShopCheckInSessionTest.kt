package com.coffeepeek.admin.di.shop

import com.coffeepeek.domain.model.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCheckInSessionTest {
    private val input = ShopCheckInCreateInput("public-slug", "Visit", ShopRating(4, 4, 4), "2026-10-09T09:00:00Z")

    @Test fun successfulCreationNotifiesOnceAndReadingOptionsDoesNotNotify() = runTest {
        var notifications = 0
        val lease = ShopCheckInSession(CheckInTestSessions(), Repository()) { notifications++ }
        lease.getDrinkOptions()
        assertEquals(0, notifications)
        assertTrue(lease.create(input).isSuccess)
        assertEquals(1, notifications)
    }

    @Test fun guestAndExpiredSessionCannotReadOrWrite() = runTest {
        for (session in listOf(null, Session("", userId = "owner"))) {
            val sessions = CheckInTestSessions(session)
            val repository = Repository()
            val lease = ShopCheckInSession(sessions, repository)
            assertFailsWith<CancellationException> { lease.getDrinkOptions() }
            assertFailsWith<CancellationException> { lease.create(input) }
            assertEquals(0, repository.reads)
            assertEquals(0, repository.writes)
        }
    }

    @Test fun sameAccountTokenRenewalDoesNotCancelOrRetryTheWrite() = runTest {
        val sessions = CheckInTestSessions()
        val complete = CompletableDeferred<Unit>()
        val repository = Repository().apply { write = { complete.await(); Result.success(Unit) } }
        val lease = ShopCheckInSession(sessions, repository)
        val result = async { lease.create(input) }
        runCurrent()
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertFalse(result.isCompleted)
        assertTrue(lease.isCurrent())
        complete.complete(Unit)
        assertTrue(result.await().isSuccess)
        assertEquals(1, repository.writes)
    }

    @Test fun logoutAndAccountReplacementCancelTheRequestAndPermanentlyRevokeTheLease() = runTest {
        for (replacement in listOf(null, Session("other-token", userId = "other"))) {
            val sessions = CheckInTestSessions()
            var cancelled = false
            val repository = Repository().apply {
                write = { try { awaitCancellation() } finally { cancelled = true } }
            }
            val lease = ShopCheckInSession(sessions, repository)
            val result = async { lease.create(input) }
            runCurrent()
            sessions.applySession(replacement)
            runCurrent()
            assertFailsWith<CancellationException> { result.await() }
            assertTrue(cancelled)
            sessions.applySession(Session("another-token", userId = "owner"))
            assertFalse(lease.isCurrent())
            assertFailsWith<CancellationException> { lease.create(input) }
            assertEquals(1, repository.writes)
        }
    }

    @Test fun currentAccountIsCheckedEvenBeforeTheSessionFlowCatchesUp() = runTest {
        var notifications = 0
        val sessions = CheckInTestSessions()
        val repository = Repository().apply {
            write = { sessions.applySession(Session("changed", userId = "other")); Result.success(Unit) }
        }
        val lease = ShopCheckInSession(sessions, repository) { notifications++ }
        assertFailsWith<CancellationException> { lease.create(input) }
        assertFalse(lease.isCurrent())
        assertEquals(0, notifications)
    }

    @Test fun resultFailureAndCallerCancellationArePreserved() = runTest {
        var notifications = 0
        val sessions = CheckInTestSessions()
        val failure = IllegalStateException("Known rejection")
        val repository = Repository().apply { write = { Result.failure(failure) } }
        val lease = ShopCheckInSession(sessions, repository) { notifications++ }
        assertSame(failure, lease.create(input).exceptionOrNull())
        assertTrue(lease.isCurrent())
        repository.write = { awaitCancellation() }
        val pending = async { lease.create(input) }
        runCurrent()
        pending.cancel()
        assertFailsWith<CancellationException> { pending.await() }
        assertEquals(0, notifications)
    }

    @Test fun failedSessionObservationCancelsBeforeAnyTransportCall() = runTest {
        val sessions = object : SessionStore by CheckInTestSessions() {
            override fun observeSession() = flow<Session?> { throw IllegalStateException("Secure store unavailable") }
        }
        val repository = Repository()
        val lease = ShopCheckInSession(sessions, repository)
        assertFailsWith<CancellationException> { lease.create(input) }
        assertFalse(lease.isCurrent())
        assertEquals(0, repository.writes)
    }

    private class Repository : ShopCheckInRepository {
        var reads = 0
        var writes = 0
        var write: suspend () -> Result<Unit> = { Result.success(Unit) }
        override suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>> {
            reads++
            return Result.success(emptyList())
        }
        override suspend fun create(input: ShopCheckInCreateInput): Result<Unit> {
            writes++
            return write()
        }
    }
}

internal class CheckInTestSessions(initial: Session? = Session("token", userId = "owner")) : SessionStore {
    private val state = MutableStateFlow(initial)
    override fun peekSession() = state.value
    override fun applySession(session: Session?) { state.value = session }
    override fun isActiveSession(session: Session?) = session != null && session.accessToken.isNotBlank() && !session.userId.isNullOrBlank()
    override suspend fun getSession() = state.value
    override suspend fun persistSession(session: Session?) = Unit
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override fun observeSession() = state
    override suspend fun isLoggedIn() = isActiveSession(state.value)
}
