package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.homecheckin.HomePreparationTestReader
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
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
class HomeHistorySessionTest {
    private val range = HomeHistoryDateRange("2026-10-01", "2026-10-31")

    @Test fun guestAndInvalidAccountsNeverInvokeHomeReads() = runTest {
        for (session in listOf(null, Session("", userId = "owner"), Session("token", userId = ""))) {
            val source = HomePreparationTestReader()
            val lease = AndroidCheckInHistorySession(CheckInTestSessions(session))
            val reader = lease.bind(HomeHistoryReaderAdapter(source))
            assertTrue(reader.page(1, 20, null).isFailure)
            assertTrue(reader.calendar(range).isFailure)
            assertEquals(0, source.queries.size + source.ranges.size)
        }
    }

    @Test fun logoutAndAccountReplacementDiscardEvenNonCooperativePageAndCalendarResponses() = runTest {
        for (replacement in listOf(null, Session("other-token", userId = "other"))) {
            val sessions = CheckInTestSessions()
            val gate = CompletableDeferred<Unit>()
            val source = HomePreparationTestReader()
            val originalPage = source.history
            val originalCalendar = source.days
            source.history = { query -> withContext(NonCancellable) { gate.await(); originalPage(query) } }
            source.days = { from, to -> withContext(NonCancellable) { gate.await(); originalCalendar(from, to) } }
            val lease = AndroidCheckInHistorySession(sessions)
            val reader = lease.bind(HomeHistoryReaderAdapter(source))
            val page = async { reader.page(1, 20, null) }
            val calendar = async { reader.calendar(range) }
            runCurrent()
            sessions.applySession(replacement)
            runCurrent()
            gate.complete(Unit)
            assertFailsWith<CancellationException> { page.await() }
            assertFailsWith<CancellationException> { calendar.await() }
            sessions.applySession(Session("back", userId = "owner"))
            assertFalse(lease.isCurrent())
            assertFailsWith<CancellationException> { reader.page(1, 20, null) }
            assertEquals(2, source.queries.size + source.ranges.size)
        }
    }

    @Test fun sameAccountTokenRenewalRetainsTheHomeReadAndLease() = runTest {
        val sessions = CheckInTestSessions()
        val source = HomePreparationTestReader()
        val originalPage = source.history
        val gate = CompletableDeferred<Unit>()
        source.history = { query -> gate.await(); originalPage(query) }
        val lease = AndroidCheckInHistorySession(sessions)
        val reader = lease.bind(HomeHistoryReaderAdapter(source))
        val result = async { reader.page(1, 20, null) }
        runCurrent()
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertFalse(result.isCompleted)
        gate.complete(Unit)
        assertTrue(result.await().isSuccess)
        assertTrue(lease.isCurrent())
        assertEquals(1, source.queries.size)
    }

    @Test fun explicitRevocationAndSynchronousReplacementBlockFollowingReads() = runTest {
        val sessions = CheckInTestSessions()
        val source = HomePreparationTestReader()
        val originalPage = source.history
        source.history = { query -> sessions.applySession(null); originalPage(query) }
        val lease = AndroidCheckInHistorySession(sessions)
        val reader = lease.bind(HomeHistoryReaderAdapter(source))
        assertFailsWith<CancellationException> { reader.page(1, 20, null) }
        assertFailsWith<CancellationException> { reader.calendar(range) }
        assertEquals(1, source.queries.size)
        assertEquals(0, source.ranges.size)
        val revoked = AndroidCheckInHistorySession(CheckInTestSessions())
        revoked.revoke()
        assertFailsWith<CancellationException> {
            revoked.bind(HomeHistoryReaderAdapter(source)).page(1, 20, null)
        }
    }

    @Test fun observerFailureRevokesBeforeTransportAndResultFailuresRemainExplicit() = runTest {
        val broken = object : SessionRepository by CheckInTestSessions() {
            override fun observeSession() = flow<Session?> { error("Session store unavailable") }
        }
        val source = HomePreparationTestReader()
        val adapter = HomeHistoryReaderAdapter(source)
        assertFailsWith<CancellationException> { AndroidCheckInHistorySession(broken).bind(adapter).page(1, 20, null) }
        assertEquals(0, source.queries.size)
        val failure = IllegalStateException("Unavailable")
        source.history = { Result.failure(failure) }
        val reader = AndroidCheckInHistorySession(CheckInTestSessions()).bind(adapter)
        assertSame(failure, reader.page(1, 20, null).exceptionOrNull())
        source.history = { Result.failure(CancellationException("Canceled")) }
        assertFailsWith<CancellationException> { reader.page(1, 20, null) }
    }
}
