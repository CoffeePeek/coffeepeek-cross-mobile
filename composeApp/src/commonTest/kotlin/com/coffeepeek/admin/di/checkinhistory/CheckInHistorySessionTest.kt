package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CheckInHistorySessionTest {
    private val period = HistoryPeriod("2026-10-01T00:00:00Z", "2026-11-01T00:00:00Z")

    @Test fun guestExpiredAndMissingAccountNeverReadPersonalHistory() = runTest {
        for (session in listOf(null, Session("", userId = "owner"), Session("token", userId = ""))) {
            val sessions = CheckInTestSessions(session)
            val repository = Repository()
            val lease = CheckInHistorySession(sessions)
            val bound = lease.bind(repository)
            assertFalse(lease.isLoggedIn)
            assertTrue(bound.page(1, 20).isFailure)
            assertTrue(bound.period(period).isFailure)
            assertEquals(0, repository.reads)
        }
    }

    @Test fun tokenRenewalPreservesOneInFlightRead() = runTest {
        val sessions = CheckInTestSessions()
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { load = { gate.await() } }
        val lease = CheckInHistorySession(sessions)
        val result = async { lease.bind(repository).page(2, 20) }
        runCurrent()
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertFalse(result.isCompleted)
        assertTrue(lease.isCurrent())
        gate.complete(Unit)
        assertEquals(2, result.await().getOrThrow().page)
        assertEquals(1, repository.reads)
    }

    @Test fun logoutOrReplacementCancelsPagingAndCalendarAndCannotResurrectTheLease() = runTest {
        for (replacement in listOf(null, Session("other-token", userId = "other"))) {
            val sessions = CheckInTestSessions()
            val gate = CompletableDeferred<Unit>()
            val repository = Repository().apply { load = { withContext(NonCancellable) { gate.await() } } }
            val lease = CheckInHistorySession(sessions)
            val bound = lease.bind(repository)
            val page = async { bound.page(1, 20) }
            val calendar = async { bound.period(period) }
            runCurrent()
            sessions.applySession(replacement)
            runCurrent()
            gate.complete(Unit)
            assertFailsWith<CancellationException> { page.await() }
            assertFailsWith<CancellationException> { calendar.await() }
            sessions.applySession(Session("returned", userId = "owner"))
            assertFalse(lease.isCurrent())
            assertFailsWith<CancellationException> { bound.page(1, 20) }
            assertEquals(2, repository.reads)
        }
    }

    @Test fun synchronousAccountChangeIsCheckedBeforeFlowCatchesUp() = runTest {
        val sessions = CheckInTestSessions()
        val repository = Repository().apply { load = { sessions.applySession(null) } }
        val lease = CheckInHistorySession(sessions)
        assertFailsWith<CancellationException> { lease.bind(repository).period(period) }
        assertFalse(lease.isCurrent())
    }

    @Test fun observationFailureRevokesBeforeAnyTransportCall() = runTest {
        val sessions = object : SessionRepository by CheckInTestSessions() {
            override fun observeSession() = flow<Session?> { error("Secure store unavailable") }
        }
        val repository = Repository()
        val lease = CheckInHistorySession(sessions)
        assertFailsWith<CancellationException> { lease.bind(repository).page(1, 20) }
        assertFalse(lease.isCurrent())
        assertEquals(0, repository.reads)
    }

    @Test fun resultFailureAndResultWrappedCancellationKeepTheirContracts() = runTest {
        val repository = Repository()
        val bound = CheckInHistorySession(CheckInTestSessions()).bind(repository)
        val failure = IllegalStateException("Server unavailable")
        repository.failure = failure
        assertSame(failure, bound.page(1, 20).exceptionOrNull())
        assertSame(failure, bound.period(period).exceptionOrNull())
        repository.failure = CancellationException("Read canceled")
        assertFailsWith<CancellationException> { bound.page(1, 20) }
        assertFailsWith<CancellationException> { bound.period(period) }
    }

    @Test fun callerCancellationDoesNotPermanentlyRevokeTheAccount() = runTest {
        val lease = CheckInHistorySession(CheckInTestSessions())
        val repository = Repository().apply { load = { awaitCancellation() } }
        val result = async { lease.bind(repository).page(1, 20) }
        runCurrent()
        result.cancel()
        assertFailsWith<CancellationException> { result.await() }
        assertTrue(lease.isCurrent())
        repository.load = {}
        assertTrue(lease.bind(repository).period(period).isSuccess)
    }

    private class Repository : CheckInHistoryRepository {
        var reads = 0
        var load: suspend () -> Unit = {}
        var failure: Throwable? = null
        override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> {
            reads++; load()
            return failure?.let { Result.failure(it) } ?: Result.success(CheckInHistoryPage(emptyList(), 0, page, pageSize))
        }
        override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
            reads++; load()
            return failure?.let { Result.failure(it) } ?: Result.success(emptyList())
        }
    }
}
