package com.coffeepeek.admin.di.contentreport

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contentreport.domain.model.ContentReportKind
import com.coffeepeek.feature.contentreport.domain.model.ContentReportTarget
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosContentReportTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = ReportTestSessions()
    private var report: IosContentReport? = null
    private var backs = 0
    private var signIns = 0

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() {
        report?.close()
        Dispatchers.resetMain()
    }

    @Test fun previewReviewBypassesAccountAndRepositoryAndCanGoBack() = runTest(dispatcher) {
        val form = open(preview = true, sessionReader = UnusedSessions)
        runCurrent()
        form.changeText("preview")
        form.submit()
        runCurrent()
        assertTrue(form.currentState().preview)
        assertTrue(form.currentState().submitted)
        assertEquals("", form.currentState().text)
        form.back()
        runCurrent()
        assertEquals(1, backs)
        assertEquals(0, signIns)
    }

    @Test fun checkInSubmitsTheTrimmedReasonOnlyOnceWhileRequestIsInFlight() = runTest(dispatcher) {
        val requests = mutableListOf<Pair<ContentReportTarget, String>>()
        val completion = CompletableDeferred<Result<Unit>>()
        val form = open(repository = ContentReportRepository { target, reason ->
            requests += target to reason
            completion.await()
        })
        runCurrent()
        form.changeText("  invalid photo  ")
        form.submit()
        form.submit()
        runCurrent()
        assertTrue(form.currentState().submitting)
        assertEquals(listOf(ContentReportTarget(ContentReportKind.CheckIn, "item-1") to "invalid photo"), requests)
        completion.complete(Result.success(Unit))
        runCurrent()
        assertTrue(form.currentState().submitted)
    }

    @Test fun renewalPreservesTheDraftAndReplacementRejectsAQueuedOldDraft() = runTest(dispatcher) {
        var requests = 0
        val form = open(repository = ContentReportRepository { _, _ ->
            requests++
            Result.success(Unit)
        })
        runCurrent()
        form.changeText("private draft")
        runCurrent()
        sessions.current.value = Session("renewed", userId = "owner")
        runCurrent()
        assertEquals("private draft", form.currentState().text)
        form.submit()
        sessions.current.value = Session("replacement", userId = "other")
        runCurrent()
        assertEquals("", form.currentState().text)
        assertFalse(form.currentState().submitted)
        assertFalse(form.currentState().canSubmit)
        assertEquals(0, requests)
    }

    @Test fun accessFailureCanRetryWithoutDismissal() = runTest(dispatcher) {
        sessions.source = flow { error("account stream unavailable") }
        val form = open()
        runCurrent()
        assertFalse(form.currentState().accessAvailable)
        assertFalse(form.currentState().canEdit)
        sessions.source = sessions.current
        form.retryAccess()
        runCurrent()
        assertTrue(form.currentState().accessAvailable)
        assertTrue(form.currentState().canEdit)
        assertEquals(0, backs)
    }

    @Test fun guestSignInKeepsTheDraftUntilTheAccountChanges() = runTest(dispatcher) {
        sessions.current.value = null
        val form = open()
        runCurrent()
        form.changeText("guest draft")
        form.submit()
        form.submit()
        runCurrent()
        assertEquals(1, signIns)
        assertEquals("guest draft", form.currentState().text)
        sessions.current.value = Session("new session", userId = "member")
        runCurrent()
        assertEquals("", form.currentState().text)
    }

    private fun open(
        preview: Boolean = false,
        sessionReader: SessionReader = sessions,
        repository: ContentReportRepository = ContentReportRepository { _, _ -> error("Unexpected report request") },
    ) = IosContentReport(
        "item-1", !preview, preview, sessionReader, repository, { backs++ }, { signIns++ },
    ).also { report = it }

    private object UnusedSessions : SessionReader {
        override fun peekSession(): Nothing = error("Preview read account")
        override fun isActiveSession(session: Session?): Nothing = error("Preview validated account")
        override fun observeSession(): Nothing = error("Preview observed account")
        override suspend fun getSession(): Nothing = error("Preview loaded account")
    }
}
