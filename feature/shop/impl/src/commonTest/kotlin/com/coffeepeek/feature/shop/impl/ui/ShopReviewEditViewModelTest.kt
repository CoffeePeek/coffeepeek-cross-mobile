package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReview
import com.coffeepeek.feature.shop.domain.model.ShopReviewCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.domain.model.ShopReviewUpdateInput
import com.coffeepeek.feature.shop.domain.repository.ShopReviewWriteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopUserReviewRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormEvent
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftStore
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
class ShopReviewEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Reviews(var review: ShopReview? = sampleReview()) : ShopUserReviewRepository {
        var calls = 0
        override suspend fun findForEdit(userId: String, reviewId: String): Result<ShopReview?> {
            calls++
            assertEquals("user-1", userId)
            assertEquals("published-1", reviewId)
            return Result.success(review)
        }
    }

    private class Writes : ShopReviewWriteRepository {
        var updates = 0
        var last: ShopReviewUpdateInput? = null
        var result: Result<Unit> = Result.success(Unit)
        override suspend fun create(input: ShopReviewCreateInput): Result<Unit> = error("Unexpected create")
        override suspend fun update(input: ShopReviewUpdateInput): Result<Unit> {
            updates++
            last = input
            return result
        }
    }

    private class Drafts : ShopReviewDraftStore {
        var loaded: ShopReviewDraftSnapshot? = null
        var saved: ShopReviewDraftSnapshot? = null
        var clearCalls = 0
        override suspend fun load(): Result<ShopReviewDraftSnapshot?> = Result.success(loaded)
        override fun save(draft: ShopReviewDraftSnapshot): Result<Unit> {
            saved = draft
            return Result.success(Unit)
        }
        override suspend fun clear(): Result<Unit> {
            clearCalls++
            return Result.success(Unit)
        }
    }

    private fun viewModel(reviews: Reviews = Reviews(), writes: Writes = Writes(), drafts: Drafts = Drafts()) =
        ShopReviewEditViewModel("published-1", "user-1", reviews, writes, drafts)
            .also { store.put("edit", it) }

    @Test fun restoresDraftAndDiscardsBackToPublishedBaseline() = runTest(dispatcher) {
        val drafts = Drafts().apply {
            loaded = ShopReviewDraftSnapshot("Draft title", "Draft comment", ShopRating(2, 3, 4),
                listOf(ShopReviewPhoto(byteArrayOf(1), "unsent.jpg")))
        }
        val viewModel = viewModel(drafts = drafts)
        runCurrent()
        assertEquals("Draft title", viewModel.state.value.header)
        assertTrue(viewModel.state.value.draftRestored)
        assertTrue(viewModel.state.value.ignoredDraftPhotos)
        assertTrue(viewModel.state.value.newPhotos.isEmpty())
        viewModel.onAction(ShopReviewFormAction.PhotosAdded(
            listOf(ShopReviewPhoto(byteArrayOf(2), "new.jpg"))))
        runCurrent()
        assertTrue(viewModel.state.value.newPhotos.isEmpty())

        viewModel.onAction(ShopReviewFormAction.DiscardDraft)
        runCurrent()
        assertEquals("Published title", viewModel.state.value.header)
        assertEquals(ShopRating(4, 5, 3), viewModel.state.value.rating)
        assertFalse(viewModel.state.value.draftRestored)
        assertFalse(viewModel.state.value.ignoredDraftPhotos)
        assertEquals(1, drafts.clearCalls)
        assertEquals(1, viewModel.state.value.existingPhotoUrls.size)
    }

    @Test fun editWritesToModerationRecordOnlyOnce() = runTest(dispatcher) {
        val writes = Writes()
        val drafts = Drafts()
        val viewModel = viewModel(writes = writes, drafts = drafts)
        runCurrent()
        viewModel.onAction(ShopReviewFormAction.HeaderChanged(" Updated title "))
        runCurrent()
        assertEquals(" Updated title ", drafts.saved?.header)
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals("moderation-1", writes.last?.reviewId)
        assertEquals("Updated title", writes.last?.header)
        assertEquals(1, writes.updates)
        assertEquals(1, drafts.clearCalls)
        assertEquals(ShopReviewFormEvent.Submitted, viewModel.events.first())
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals(1, writes.updates)
    }

    @Test fun missingModerationRecordCannotBeSubmitted() = runTest(dispatcher) {
        val reviews = Reviews(sampleReview().copy(moderationReviewId = null))
        val writes = Writes()
        val viewModel = viewModel(reviews, writes)
        runCurrent()
        assertFalse(viewModel.state.value.canEdit)
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals(0, writes.updates)
    }

    @Test fun failedWriteKeepsDraftAndAllowsRetry() = runTest(dispatcher) {
        val writes = Writes().apply { result = Result.failure(IllegalStateException("rejected")) }
        val drafts = Drafts()
        val viewModel = viewModel(writes = writes, drafts = drafts)
        runCurrent()
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals("rejected", viewModel.state.value.submitError)
        assertEquals(0, drafts.clearCalls)
        writes.result = Result.success(Unit)
        viewModel.onAction(ShopReviewFormAction.Submit)
        runCurrent()
        assertEquals(2, writes.updates)
        assertEquals(1, drafts.clearCalls)
    }
}

private fun sampleReview() = ShopReview(
    id = "published-1",
    moderationReviewId = "moderation-1",
    userId = "user-1",
    shopId = "shop-1",
    username = "User",
    header = "Published title",
    comment = "Published comment",
    rating = ShopRating(4, 5, 3),
    createdAtUtc = "2026-10-01T12:00:00Z",
    photoUrls = listOf("https://files.example.com/photo.jpg"),
    helpfulCount = 0,
    isHelpfulByCurrentUser = false,
)
