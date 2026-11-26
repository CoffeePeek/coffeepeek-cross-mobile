package com.coffeepeek.feature.shopreport.impl

import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.model.ShopReportDeliveryUnconfirmed
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NativeShopReportTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = Repository()
    private lateinit var report: NativeShopReport

    @BeforeTest fun setUp() {
        Dispatchers.setMain(dispatcher)
        report = createNativeShopReport("shop-1", repository)
    }

    @AfterTest fun tearDown() {
        report.close()
        Dispatchers.resetMain()
    }

    @Test fun nativeInputsReuseValidationAndUnicodeLengthLimit() = runTest(dispatcher) {
        report.selectCategory("Other")
        report.submit()
        runCurrent()
        assertEquals("DescriptionRequired", report.currentState().error)
        report.changeDescription("a".repeat(499) + "\uD83D\uDE00")
        runCurrent()
        assertEquals("a".repeat(499), report.currentState().description)
        report.submit()
        runCurrent()
        assertEquals("a".repeat(499), repository.description)
        assertTrue(report.currentState().isSubmitted)
    }

    @Test fun nativeDuplicateAndUnconfirmedSubmissionsStayBlocked() = runTest(dispatcher) {
        repository.result = Result.failure(ShopReportDeliveryUnconfirmed())
        report.selectCategory("ShopClosed")
        report.submit()
        report.submit()
        runCurrent()
        assertTrue(report.currentState().deliveryUnconfirmed)
        report.changeDescription("replacement")
        report.submit()
        runCurrent()
        assertEquals(1, repository.calls)
        assertEquals("", report.currentState().description)
    }

    @Test fun nativeRetryReusesTheFormAfterKnownFailure() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("private response"))
        report.selectCategory("WrongOpeningHours")
        report.submit()
        runCurrent()
        assertEquals("SubmissionFailed", report.currentState().error)
        repository.result = Result.success(Unit)
        report.submit()
        runCurrent()
        assertTrue(report.currentState().isSubmitted)
        assertEquals(2, repository.calls)
    }

    @Test fun closingCancelsPendingSubmissionAndRejectsFurtherActions() = runTest(dispatcher) {
        repository.gate = CompletableDeferred()
        report.selectCategory("IncorrectAddress")
        report.submit()
        runCurrent()
        assertTrue(report.currentState().isSubmitting)
        report.close()
        runCurrent()
        assertTrue(repository.cancelled)
        report.submit()
        report.changeDescription("late")
        runCurrent()
        assertEquals(1, repository.calls)
        assertFalse(report.currentState().isSubmitting)
        assertEquals("", report.currentState().description)
    }

    @Test fun unknownCategoryCannotBecomeASubmission() = runTest(dispatcher) {
        report.selectCategory("unknown")
        report.submit()
        runCurrent()
        assertEquals("CategoryRequired", report.currentState().error)
        assertEquals(0, repository.calls)
    }

    private class Repository : ShopIssueReportRepository {
        var result = Result.success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        var calls = 0
        var description: String? = null
        var cancelled = false

        override suspend fun submitReport(shopId: String, category: ShopIssueCategory, description: String?): Result<Unit> {
            assertEquals("shop-1", shopId)
            calls++
            this.description = description
            try {
                gate?.await()
                return result
            } catch (failure: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw failure
            }
        }
    }
}
