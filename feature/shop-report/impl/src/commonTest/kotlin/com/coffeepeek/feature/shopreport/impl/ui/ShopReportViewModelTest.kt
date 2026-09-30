package com.coffeepeek.feature.shopreport.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportAction
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportError
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class ShopReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Repository : ShopIssueReportRepository {
        var submissions = 0
        var lastCategory: ShopIssueCategory? = null
        var lastDescription: String? = null
        var result: Result<Unit> = Result.success(Unit)
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun submitReport(
            shopId: String,
            category: ShopIssueCategory,
            description: String?,
        ): Result<Unit> {
            assertEquals("shop-1", shopId)
            submissions++
            lastCategory = category
            lastDescription = description
            gate?.await()
            return result
        }
    }

    private fun viewModel(repository: Repository) = ShopReportViewModel("shop-1", repository)
        .also { store.put("shop-report", it) }

    @Test fun otherRequiresDescriptionAndLimitsItTo500Characters() = runTest(dispatcher) {
        val repository = Repository()
        val viewModel = viewModel(repository)
        viewModel.onAction(ShopReportAction.SelectCategory(ShopIssueCategory.Other))
        viewModel.onAction(ShopReportAction.Submit)
        runCurrent()
        assertEquals(ShopReportError.DescriptionRequired, viewModel.state.value.error)
        assertEquals(0, repository.submissions)

        viewModel.onAction(ShopReportAction.ChangeDescription("x".repeat(501)))
        runCurrent()
        assertEquals(500, viewModel.state.value.description.length)
        viewModel.onAction(ShopReportAction.Submit)
        runCurrent()
        assertEquals(1, repository.submissions)
        assertEquals(500, repository.lastDescription?.length)
        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test fun duplicateSubmissionsAreBlockedUntilCompletion() = runTest(dispatcher) {
        val repository = Repository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(repository)
        viewModel.onAction(ShopReportAction.SelectCategory(ShopIssueCategory.ShopClosed))
        runCurrent()
        viewModel.onAction(ShopReportAction.Submit)
        viewModel.onAction(ShopReportAction.Submit)
        runCurrent()
        assertEquals(1, repository.submissions)
        assertTrue(viewModel.state.value.isSubmitting)
        assertEquals(null, repository.lastDescription)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.isSubmitting)
        assertTrue(viewModel.state.value.isSubmitted)
        viewModel.onAction(ShopReportAction.Submit)
        runCurrent()
        assertEquals(1, repository.submissions)
    }

    @Test fun requestFailureIsVisibleAndCanBeRetried() = runTest(dispatcher) {
        val repository = Repository().apply { result = Result.failure(IllegalStateException("secret")) }
        val viewModel = viewModel(repository)
        viewModel.onAction(ShopReportAction.SelectCategory(ShopIssueCategory.WrongOpeningHours))
        runCurrent()
        viewModel.onAction(ShopReportAction.Submit)
        runCurrent()
        assertEquals(ShopReportError.SubmissionFailed, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
        assertFalse(viewModel.state.value.isSubmitted)
        repository.result = Result.success(Unit)
        viewModel.onAction(ShopReportAction.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.isSubmitted)
        assertEquals(2, repository.submissions)
    }

    @Test fun backIsOneOffEventEvenWhileRequestIsPending() = runTest(dispatcher) {
        val repository = Repository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(repository)
        viewModel.onAction(ShopReportAction.SelectCategory(ShopIssueCategory.ShopClosed))
        runCurrent()
        viewModel.onAction(ShopReportAction.Submit)
        runCurrent()
        viewModel.onAction(ShopReportAction.Back)
        runCurrent()
        assertEquals(ShopReportEvent.Back, viewModel.events.first())
        store.clear()
        runCurrent()
        assertFalse(viewModel.state.value.isSubmitting)
    }
}
