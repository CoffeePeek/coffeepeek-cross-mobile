package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.domain.model.ShopReviewUpdateInput
import com.coffeepeek.feature.shop.domain.repository.ShopReviewWriteRepository
import com.coffeepeek.feature.shop.domain.usecase.ShopReviewFieldError
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormEvent
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftStore
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
class ShopReviewCreateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Drafts : ShopReviewDraftStore {
        var loaded: Result<ShopReviewDraftSnapshot?> = Result.success(null)
        var saved: ShopReviewDraftSnapshot? = null
        var saveCalls = 0
        var clearCalls = 0
        var clearResult: Result<Unit> = Result.success(Unit)
        override suspend fun load(): Result<ShopReviewDraftSnapshot?> = loaded
        override fun save(draft: ShopReviewDraftSnapshot): Result<Unit> {
            saved = draft
            saveCalls++
            return Result.success(Unit)
        }
        override suspend fun clear(): Result<Unit> {
            clearCalls++
            return clearResult
        }
    }

    private class Writes : ShopReviewWriteRepository {
        var calls = 0
        var input: ShopReviewCreateInput? = null
        var result: Result<Unit> = Result.success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun create(input: ShopReviewCreateInput): Result<Unit> {
            calls++
            this.input = input
            gate?.await()
            return result
        }
        override suspend fun update(input: ShopReviewUpdateInput): Result<Unit> = error("Unexpected update")
    }

    private fun viewModel(drafts: Drafts = Drafts(), writes: Writes = Writes()): ShopReviewCreateViewModel =
        ShopReviewCreateViewModel("shop-1", "Coffee", writes, drafts).also { store.put("form", it) }

    @Test fun restoresDraftAndKeepsFivePhotoLimitAcrossEdits() = runTest(dispatcher) {
        val photo = ShopReviewPhoto(byteArrayOf(1), "photo.jpg")
        val drafts = Drafts().apply { loaded = Result.success(ShopReviewDraftSnapshot(
            "Old title", "Old comment", ShopRating(3, 4, 5), listOf(photo))) }
        val viewModel = viewModel(drafts)
        runCurrent()
        assertEquals("Old title", viewModel.state.value.header)
        assertTrue(viewModel.state.value.draftRestored)
        viewModel.onAction(ShopReviewFormAction.HeaderChanged("H".repeat(130)))
        viewModel.onAction(ShopReviewFormAction.PhotosAdded((1..7).map {
            ShopReviewPhoto(byteArrayOf(it.toByte()), "$it.jpg")
        }))
        runCurrent()
        assertEquals(120, viewModel.state.value.header.length)
        assertEquals(5, viewModel.state.value.newPhotos.size)
        assertEquals(5, drafts.saved?.photos?.size)
        viewModel.onAction(ShopReviewFormAction.DiscardDraft)
        runCurrent()
        assertEquals(1, drafts.clearCalls)
        assertTrue(viewModel.state.value.newPhotos.isEmpty())
        assertFalse(viewModel.state.value.draftRestored)
    }

    @Test fun validationAndDuplicateSubmitGuardProtectWrite() = runTest(dispatcher) {
        val writes = Writes().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(writes = writes)
        runCurrent()
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals(ShopReviewFieldError.Required, viewModel.state.value.headerError)
        assertEquals(0, writes.calls)

        viewModel.onAction(ShopReviewFormAction.HeaderChanged(" Coffee "))
        viewModel.onAction(ShopReviewFormAction.CommentChanged(" Delicious coffee "))
        runCurrent()
        viewModel.onAction(ShopReviewFormAction.Submit)
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals(1, writes.calls)
        assertTrue(viewModel.state.value.isSubmitting)
        assertEquals("Coffee", writes.input?.header)
        assertEquals("Delicious coffee", writes.input?.comment)
        writes.gate!!.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.isSubmitting)
        assertEquals(ShopReviewFormEvent.Submitted, viewModel.events.first())
    }

    @Test fun failurePreservesDraftAndAllowsRetry() = runTest(dispatcher) {
        val drafts = Drafts()
        val writes = Writes().apply { result = Result.failure(IllegalStateException("server rejected")) }
        val viewModel = viewModel(drafts, writes)
        runCurrent()
        viewModel.onAction(ShopReviewFormAction.HeaderChanged("Coffee"))
        viewModel.onAction(ShopReviewFormAction.CommentChanged("Delicious coffee"))
        runCurrent()
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals("server rejected", viewModel.state.value.submitError)
        assertEquals(0, drafts.clearCalls)
        writes.result = Result.success(Unit)
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals(2, writes.calls)
        assertEquals(1, drafts.clearCalls)
        assertEquals(ShopReviewFormEvent.Submitted, viewModel.events.first())
    }

    @Test fun successfulWriteIsNotRetriedWhenDraftClearFails() = runTest(dispatcher) {
        val drafts = Drafts().apply { clearResult = Result.failure(IllegalStateException("storage")) }
        val writes = Writes()
        val viewModel = viewModel(drafts, writes)
        runCurrent()
        viewModel.onAction(ShopReviewFormAction.HeaderChanged("Coffee"))
        viewModel.onAction(ShopReviewFormAction.CommentChanged("Delicious coffee"))
        runCurrent()

        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()

        assertEquals(1, writes.calls)
        assertTrue(viewModel.state.value.draftError)
        assertEquals(ShopReviewFormEvent.DraftClearFailed, viewModel.events.first())
        assertEquals(ShopReviewFormEvent.Submitted, viewModel.events.first())
        viewModel.onAction(ShopReviewFormAction.HeaderChanged("Coffee again"))
        viewModel.onAction(ShopReviewFormAction.CommentChanged("Another review"))
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals(1, writes.calls)
    }
}
