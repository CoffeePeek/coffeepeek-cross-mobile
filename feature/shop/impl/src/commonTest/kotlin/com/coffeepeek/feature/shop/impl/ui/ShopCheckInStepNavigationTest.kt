package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInEditorStep
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.editorStep
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCheckInStepNavigationTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val initial = ShopCheckInCreateInput("shop", "", ShopRating(4, 4, 4), "2026-11-17T09:00:00Z")
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { store.clear(); Dispatchers.resetMain() }

    @Test fun pickerWaitsForEarlierWritesAndLatestDraftAndEmitsOnce() = runTest(dispatcher) {
        val drafts = Drafts().apply { gate = CompletableDeferred() }
        val model = model(drafts)
        val events = observe(model)
        runCurrent()
        model.onAction(ShopCheckInFormAction.TextChanged("first"))
        runCurrent()
        model.onAction(ShopCheckInFormAction.TextChanged("latest"))
        model.onAction(ShopCheckInFormAction.ChangeShop)
        model.onAction(ShopCheckInFormAction.ChangeShop)
        runCurrent()
        assertTrue(model.state.value.isChangingShop)
        assertTrue(events.isEmpty())
        drafts.gate!!.complete(Unit)
        runCurrent()
        assertEquals("latest", drafts.snapshot?.input?.text)
        assertEquals(listOf<ShopCheckInFormEvent>(ShopCheckInFormEvent.ChangeShop), events)
    }

    @Test fun savingForPickerBlocksEditsSubmitAndDismiss() = runTest(dispatcher) {
        val drafts = Drafts().apply {
            gate = CompletableDeferred()
            snapshot = ShopCheckInDraftSnapshot(initial, exists = true)
        }
        val repository = Repository()
        val model = model(drafts, repository)
        val events = observe(model)
        runCurrent()
        model.onAction(ShopCheckInFormAction.ChangeShop)
        runCurrent()
        model.onAction(ShopCheckInFormAction.TextChanged("stale edit"))
        model.onAction(ShopCheckInFormAction.Submit)
        model.onAction(ShopCheckInFormAction.Dismiss)
        runCurrent()
        assertEquals(initial, model.state.value.input)
        assertEquals(0, repository.writes)
        assertTrue(events.isEmpty())
    }

    @Test fun failedFlushKeepsEditorAndCanRetryWithoutLosingDraft() = runTest(dispatcher) {
        val drafts = Drafts().apply { fail = true }
        val model = model(drafts)
        val events = observe(model)
        runCurrent()
        model.onAction(ShopCheckInFormAction.TextChanged("keep me"))
        model.onAction(ShopCheckInFormAction.ChangeShop)
        runCurrent()
        assertTrue(model.state.value.draftFailed)
        assertFalse(model.state.value.isChangingShop)
        assertTrue(events.isEmpty())
        drafts.fail = false
        model.onAction(ShopCheckInFormAction.ChangeShop)
        runCurrent()
        assertEquals("keep me", drafts.snapshot?.input?.text)
        assertEquals(listOf<ShopCheckInFormEvent>(ShopCheckInFormEvent.ChangeShop), events)
    }

    @Test fun disposedEditorNeverOpensPickerAfterPendingSave() = runTest(dispatcher) {
        val drafts = Drafts().apply {
            gate = CompletableDeferred()
            snapshot = ShopCheckInDraftSnapshot(initial, exists = true)
        }
        val model = model(drafts)
        val events = observe(model)
        runCurrent()
        model.onAction(ShopCheckInFormAction.ChangeShop)
        runCurrent()
        store.clear()
        drafts.gate!!.complete(Unit)
        runCurrent()
        assertTrue(events.isEmpty())
        assertEquals(initial, drafts.snapshot?.input)
    }

    @Test fun changingShopPreservesUncertainDeliveryGuard() = runTest(dispatcher) {
        val drafts = Drafts().apply { snapshot = ShopCheckInDraftSnapshot(initial, deliveryUnconfirmed = true, exists = true) }
        val repository = Repository()
        val model = model(drafts, repository)
        val events = observe(model)
        runCurrent()
        model.onAction(ShopCheckInFormAction.ChangeShop)
        runCurrent()
        assertTrue(drafts.snapshot!!.deliveryUnconfirmed)
        assertEquals(0, repository.writes)
        assertEquals(listOf<ShopCheckInFormEvent>(ShopCheckInFormEvent.ChangeShop), events)
    }

    @Test fun backFromUntouchedFormDoesNotCreateAResumeDraft() = runTest(dispatcher) {
        val drafts = Drafts()
        val model = model(drafts)
        val events = observe(model)
        runCurrent()
        model.onAction(ShopCheckInFormAction.ChangeShop)
        runCurrent()
        assertNull(drafts.snapshot)
        assertFalse(model.state.value.hasDraft)
        assertEquals(listOf<ShopCheckInFormEvent>(ShopCheckInFormEvent.ChangeShop), events)
    }

    @Test fun validationRoutesToItsVisibleFields() {
        val first = setOf(ShopCheckInValidationError.InvalidRating, ShopCheckInValidationError.InvalidNote,
            ShopCheckInValidationError.TooManyPhotos, ShopCheckInValidationError.InvalidPhoto)
        for (error in ShopCheckInValidationError.entries) {
            assertEquals(if (error in first) ShopCheckInEditorStep.Ratings else ShopCheckInEditorStep.Details, error.editorStep())
        }
    }

    private fun model(drafts: Drafts, repository: Repository = Repository()) =
        ShopCheckInCreateViewModel(initial, repository, drafts, now = { Instant.parse("2026-11-17T12:00:00Z") })
            .also { store.put("editor", it) }

    private fun TestScope.observe(model: ShopCheckInCreateViewModel): MutableList<ShopCheckInFormEvent> {
        val events = mutableListOf<ShopCheckInFormEvent>()
        backgroundScope.launch { model.events.collect { events += it } }
        return events
    }

    private class Repository : ShopCheckInRepository {
        var writes = 0
        override suspend fun getDrinkOptions() = Result.success(emptyList<ShopConsumedDrinkOption>())
        override suspend fun create(input: ShopCheckInCreateInput): Result<String> {
            writes++
            return Result.success("created")
        }
    }

    private class Drafts : ShopCheckInDraftStore {
        var snapshot: ShopCheckInDraftSnapshot? = null
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        override suspend fun open(initial: ShopCheckInCreateInput) = Result.success(snapshot ?: ShopCheckInDraftSnapshot(initial))
        override suspend fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit> {
            gate?.await()
            if (fail) return Result.failure(IllegalStateException("storage unavailable"))
            this.snapshot = snapshot.copy(exists = true)
            return Result.success(Unit)
        }
        override suspend fun clear(shopSlug: String): Result<Unit> {
            snapshot = null
            return Result.success(Unit)
        }
    }
}
