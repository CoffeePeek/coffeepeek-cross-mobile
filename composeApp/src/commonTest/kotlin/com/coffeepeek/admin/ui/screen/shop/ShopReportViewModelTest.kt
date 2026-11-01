package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.ShopIssueCategory
import com.coffeepeek.domain.repository.ShopIssueReportRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopReportViewModelTest {
    private class GatedRepository : ShopIssueReportRepository {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        override suspend fun submitReport(shopId: String, category: ShopIssueCategory, description: String?): Result<Unit> {
            calls++
            gate.await()
            return Result.success(Unit)
        }
    }

    @Test fun aSecondTapWhileTheReportIsBeingSentDoesNotSendItAgain(): Unit = runBlocking {
        val repository = GatedRepository()
        val viewModel = ShopReportViewModel("shop", repository)
        viewModel.selectReason(ShopReportReason.SHOP_CLOSED)
        viewModel.submit()
        viewModel.submit()
        assertTrue(viewModel.state.value.isSubmitting)
        repository.gate.complete(Unit)
        withTimeout(5_000) { while (!viewModel.state.value.isSubmitted) delay(10) }
        viewModel.submit()
        withContext(Dispatchers.Default) { delay(100) }
        assertEquals(1, repository.calls)
        viewModel.close()
    }
    @Test fun missingDescriptionStaysSemanticAndUsesPreparedCopy() {
        val viewModel = ShopReportViewModel("shop", GatedRepository())
        try {
            viewModel.selectReason(ShopReportReason.OTHER)
            viewModel.submit()
            assertEquals(ShopReportValidation.DescriptionRequired, viewModel.state.value.validation)
            assertEquals("Апішыце праблему", viewModel.state.value.validation?.message(ShopReportValidationLabels("Выберыце прычыну", "Апішыце праблему")))
        } finally { viewModel.close() }
    }
}
