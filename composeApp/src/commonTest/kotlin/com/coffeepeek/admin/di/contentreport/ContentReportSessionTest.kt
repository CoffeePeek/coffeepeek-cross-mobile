package com.coffeepeek.admin.di.contentreport

import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.contentreport.domain.model.*
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ContentReportSessionTest {
    private val target = ContentReportTarget(ContentReportKind.CheckIn, "publication")

    @Test fun guestAndExpiredSessionDoNotCallTheWriter() = runTest {
        for (value in listOf(null, Session("expired", userId = "owner"))) {
            val lease = ContentReportSession(ReportTestSessions(value))
            val repository = lease.bind(ContentReportRepository { _, _ -> error("No guest writes") })
            assertEquals(ContentReportProblem.SignInRequired, (repository.submit(target, "reason").exceptionOrNull() as ContentReportFailure).problem)
        }
    }

    @Test fun sameUserTokenRenewalPreservesTheLeaseButReplacementIsPermanent() = runTest {
        val sessions = ReportTestSessions()
        val lease = ContentReportSession(sessions)
        sessions.current.value = Session("token-new", userId = "owner")
        assertTrue(lease.isCurrent())
        sessions.current.value = Session("token-other", userId = "other")
        val repository = lease.bind(ContentReportRepository { _, _ -> error("No stale writes") })
        assertFailsWith<CancellationException> { repository.submit(target, "reason") }
        sessions.current.value = Session("token-a", userId = "owner")
        assertFalse(lease.isCurrent())
    }

    @Test fun logoutDuringANonCooperativeWriteCannotAcceptItsLateSuccess() = runTest {
        val sessions = ReportTestSessions()
        val lease = ContentReportSession(sessions)
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val writer = lease.bind(ContentReportRepository { _, _ ->
            calls++
            withContext(NonCancellable) { gate.await() }
            Result.success(Unit)
        })
        val attempt = async { writer.submit(target, "reason") }
        runCurrent()
        sessions.current.value = null
        runCurrent()
        gate.complete(Unit)
        assertFailsWith<CancellationException> { attempt.await() }
        assertEquals(1, calls)
        assertFalse(lease.isCurrent())
    }

    @Test fun observationFailureStopsTheWriteBeforeItStarts() = runTest {
        val sessions = ReportTestSessions().apply { source = flow { error("private storage details") } }
        val lease = ContentReportSession(sessions)
        val writer = lease.bind(ContentReportRepository { _, _ -> error("Must not start") })
        assertFailsWith<CancellationException> { writer.submit(target, "reason") }
        assertFalse(lease.isCurrent())
    }

    @Test fun cancellationReturnedAsResultIsRethrown() = runTest {
        val lease = ContentReportSession(ReportTestSessions())
        val writer = lease.bind(ContentReportRepository { _, _ -> Result.failure(CancellationException("cancelled")) })
        assertFailsWith<CancellationException> { writer.submit(target, "reason") }
    }

    @Test fun successfulOperationIsDeliveredOnceWithoutRetry() = runTest {
        val lease = ContentReportSession(ReportTestSessions())
        var calls = 0
        val writer = lease.bind(ContentReportRepository { actual, reason ->
            calls++
            assertEquals(target, actual)
            assertEquals("reason", reason)
            Result.success(Unit)
        })
        writer.submit(target, "reason").getOrThrow()
        assertEquals(1, calls)
    }
}
