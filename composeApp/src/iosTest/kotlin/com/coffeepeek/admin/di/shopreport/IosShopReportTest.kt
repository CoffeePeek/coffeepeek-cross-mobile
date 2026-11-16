package com.coffeepeek.admin.di.shopreport

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class IosShopReportTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = Sessions()
    private val repository = Repository()
    private lateinit var report: IosShopReport
    private val observations = mutableListOf<IosShopReportObservation>()

    @BeforeTest fun setUp() {
        Dispatchers.setMain(dispatcher)
        report = IosShopReport("shop-1", repository, sessions)
    }

    @AfterTest fun tearDown() {
        observations.forEach { it.cancel() }
        report.close()
        Dispatchers.resetMain()
    }

    @Test fun observationReplaysAndCancelsWithoutDiscardingDraft() = runTest(dispatcher) {
        val received = mutableListOf<IosShopReportState>()
        val observation = report.observe { received += it }.also { observations += it }
        runCurrent()
        assertEquals(1, received.size)
        observation.cancel()
        report.changeDescription("draft")
        runCurrent()
        assertEquals(1, received.size)
        val resumed = mutableListOf<IosShopReportState>()
        observations += report.observe { resumed += it }
        runCurrent()
        assertEquals("draft", resumed.single().descriptionText)
    }

    @Test fun accountReplacementClearsDraftAndCancelsInFlightRequest() = runTest(dispatcher) {
        report.selectCategory("Other")
        report.changeDescription("private draft")
        report.submit()
        runCurrent()
        assertTrue(report.currentState().isSubmitting)
        sessions.generation++
        sessions.value.value = Session("second-token", userId = "second")
        runCurrent()
        assertTrue(repository.cancelled)
        assertTrue(report.currentState().sessionEnded)
        assertEquals("", report.currentState().descriptionText)
        report.submit()
        runCurrent()
        assertEquals(1, repository.calls)
    }

    @Test fun actionsRejectChangedAccountBeforeTheObserverRuns() = runTest(dispatcher) {
        report.selectCategory("ShopClosed")
        runCurrent()
        sessions.generation++
        report.submit()
        runCurrent()
        assertEquals(0, repository.calls)
        assertTrue(report.currentState().sessionEnded)
    }

    @Test fun tokenRefreshPreservesTheSameAccountDraft() = runTest(dispatcher) {
        report.selectCategory("Other")
        report.changeDescription("draft")
        runCurrent()
        sessions.value.value = Session("refreshed-token", userId = "first")
        runCurrent()
        assertFalse(report.currentState().sessionEnded)
        assertEquals("Other", report.currentState().selectedCategory)
        assertEquals("draft", report.currentState().descriptionText)
    }

    @Test fun queuedSubmissionCannotUseReplacementAccountCredentials() = runTest(dispatcher) {
        report.selectCategory("ShopClosed")
        runCurrent()
        report.submit()
        sessions.generation++
        sessions.value.value = Session("second-token", userId = "second")
        runCurrent()
        assertEquals(0, repository.calls)
        assertTrue(report.currentState().sessionEnded)
    }

    private class Sessions : SessionReader {
        var generation = 1L
        val value = MutableStateFlow<Session?>(Session("token", userId = "first"))
        override fun accountGeneration() = generation
        override fun peekSession() = value.value
        override fun isActiveSession(session: Session?) = session == value.value
        override fun observeSession() = value
        override suspend fun getSession() = value.value
    }

    private class Repository : ShopIssueReportRepository {
        val gate = CompletableDeferred<Unit>()
        var cancelled = false
        var calls = 0

        override suspend fun submitReport(shopId: String, category: ShopIssueCategory, description: String?): Result<Unit> {
            calls++
            try {
                gate.await()
                return Result.success(Unit)
            } catch (failure: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw failure
            }
        }
    }
}
