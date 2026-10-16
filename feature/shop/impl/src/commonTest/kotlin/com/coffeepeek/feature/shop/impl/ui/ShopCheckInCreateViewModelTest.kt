package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInCreationUnconfirmed
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import com.coffeepeek.feature.shop.impl.ui.data.ShopDrinkPhotoProcessor
import kotlin.io.encoding.Base64
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCheckInCreateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val now = Instant.parse("2026-10-08T12:00:00Z")
    private val initial = ShopCheckInCreateInput("shop", "", ShopRating(4, 4, 4), "2026-10-08T10:00:00Z")

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Repository : ShopCheckInRepository {
        val writes = mutableListOf<ShopCheckInCreateInput>()
        var writeResult: Result<Unit> = Result.success(Unit)
        var drinkResult: Result<List<ShopConsumedDrinkOption>> = Result.success(listOf(ShopConsumedDrinkOption("filter", "Фильтр", "Filter")))
        var gate: CompletableDeferred<Unit>? = null
        var drinkCalls = 0
        override suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>> { drinkCalls++; return drinkResult }
        override suspend fun create(input: ShopCheckInCreateInput): Result<Unit> { writes += input; gate?.await(); return writeResult }
    }

    private class Drafts : ShopCheckInDraftStore {
        var snapshot: ShopCheckInDraftSnapshot? = null
        var saveFails = false
        var clearFails = false
        override fun open(initial: ShopCheckInCreateInput): Result<ShopCheckInDraftSnapshot> {
            if (snapshot?.input?.shopSlug != initial.shopSlug) snapshot = ShopCheckInDraftSnapshot(initial)
            return Result.success(snapshot!!)
        }
        override fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit> {
            if (saveFails) return Result.failure(IllegalStateException("storage"))
            this.snapshot = snapshot
            return Result.success(Unit)
        }
        override fun clear(shopSlug: String): Result<Unit> {
            if (clearFails) return Result.failure(IllegalStateException("cleanup"))
            if (snapshot?.input?.shopSlug == shopSlug) snapshot = null
            return Result.success(Unit)
        }
    }

    private fun viewModel(repository: Repository = Repository(), drafts: Drafts = Drafts(),
        processor: ShopDrinkPhotoProcessor? = null) =
        ShopCheckInCreateViewModel(initial, repository, drafts, now = { now }, drinkPhotos = processor)
            .also { store.put("create", it) }

    private fun png(name: String) = ShopCheckInPhoto(Base64.decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg=="), name, "image/png")

    @Test fun replacingFirstPhotoDropsLateProcessingAndKeepsTheNote() = runTest(dispatcher) {
        val oldGate = CompletableDeferred<Unit>()
        val processor = ShopDrinkPhotoProcessor { source ->
            if (source.fileName == "first.jpg") withContext(NonCancellable) { oldGate.await() }
            Result.success(listOf(png("${source.fileName}.png")))
        }
        val repository = Repository()
        val viewModel = viewModel(repository, processor = processor)
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.PhotosAdded(listOf(
            ShopCheckInPhoto(byteArrayOf(1), "first.jpg"), ShopCheckInPhoto(byteArrayOf(2), "second.jpg"))))
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Saved note"))
        runCurrent()
        assertTrue(viewModel.state.value.drinkPhotoProcessing)
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertTrue(repository.writes.isEmpty())
        viewModel.onAction(ShopCheckInFormAction.RemovePhoto(0))
        runCurrent()
        oldGate.complete(Unit)
        runCurrent()
        assertEquals("second.jpg.png", viewModel.state.value.input.drinkPhoto?.fileName)
        assertEquals("Saved note", viewModel.state.value.input.text)
        assertFalse(viewModel.state.value.drinkPhotoProcessing)
    }

    @Test fun multipleObjectsRequireAChoiceBeforeWriting() = runTest(dispatcher) {
        val choices = listOf(png("cup.png"), png("plate.png"))
        val repository = Repository()
        val viewModel = viewModel(repository, processor = ShopDrinkPhotoProcessor { Result.success(choices) })
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        viewModel.onAction(ShopCheckInFormAction.PhotosAdded(listOf(ShopCheckInPhoto(byteArrayOf(1), "full.jpg"))))
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertTrue(repository.writes.isEmpty())
        viewModel.onAction(ShopCheckInFormAction.ChooseDrinkPhoto(0))
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(choices.first(), repository.writes.single().drinkPhoto)
        assertEquals(1, repository.writes.single().photos.size)
    }

    @Test fun skippingDuringProcessingIgnoresLateResultAndAllowsSubmittingTheOriginal() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository()
        val drafts = Drafts()
        val viewModel = viewModel(repository, drafts, ShopDrinkPhotoProcessor {
            withContext(NonCancellable) { gate.await() }
            Result.success(listOf(png("late.png")))
        })
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        viewModel.onAction(ShopCheckInFormAction.PhotosAdded(listOf(ShopCheckInPhoto(byteArrayOf(1), "full.jpg"))))
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.SkipDrinkPhoto)
        runCurrent()
        assertTrue(viewModel.state.value.drinkPhotoSkipped)
        assertFalse(viewModel.state.value.drinkPhotoProcessing)
        gate.complete(Unit)
        runCurrent()
        assertEquals(null, viewModel.state.value.input.drinkPhoto)
        assertTrue(drafts.snapshot!!.drinkPhotoSkipped)
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(null, repository.writes.single().drinkPhoto)
        assertEquals(1, repository.writes.single().photos.size)
    }

    @Test fun explicitOptOutSurvivesReopeningWithoutRestartingProcessing() = runTest(dispatcher) {
        var calls = 0
        val processor = ShopDrinkPhotoProcessor { calls++; Result.failure(IllegalStateException("no model")) }
        val drafts = Drafts()
        val first = viewModel(drafts = drafts, processor = processor)
        runCurrent()
        first.onAction(ShopCheckInFormAction.PhotosAdded(listOf(ShopCheckInPhoto(byteArrayOf(1), "full.jpg"))))
        runCurrent()
        first.onAction(ShopCheckInFormAction.SkipDrinkPhoto)
        runCurrent()
        val reopened = viewModel(drafts = drafts, processor = processor)
        runCurrent()
        assertEquals(1, calls)
        assertTrue(reopened.state.value.drinkPhotoSkipped)
        assertEquals(null, reopened.state.value.input.drinkPhoto)
        assertEquals(1, reopened.state.value.input.photos.size)
    }

    @Test fun restoresProcessDraftAndKeepsEditsOnDismissWhileGuardingPhotoLimit() = runTest(dispatcher) {
        val drafts = Drafts().apply { snapshot = ShopCheckInDraftSnapshot(initial.copy(text = "Restored")) }
        val viewModel = viewModel(drafts = drafts)
        runCurrent()
        assertEquals("Restored", viewModel.state.value.input.text)
        assertEquals(ShopCheckInVisibility.Private, viewModel.state.value.input.visibility)
        viewModel.onAction(ShopCheckInFormAction.DrinkChanged("other"))
        viewModel.onAction(ShopCheckInFormAction.CustomDrinkChanged("Tonic"))
        val photo = ShopCheckInPhoto(byteArrayOf(1), "one.jpg")
        viewModel.onAction(ShopCheckInFormAction.PhotosAdded(listOf(photo, photo) + (2..7).map { ShopCheckInPhoto(byteArrayOf(it.toByte()), "$it.jpg") }))
        runCurrent()
        assertEquals(5, viewModel.state.value.input.photos.size)
        viewModel.onAction(ShopCheckInFormAction.Dismiss)
        runCurrent()
        assertEquals(ShopCheckInFormEvent.Dismiss, viewModel.events.first())
        assertEquals("Tonic", drafts.snapshot!!.input.customDrinkName)
        viewModel.onAction(ShopCheckInFormAction.DrinkChanged("filter"))
        runCurrent()
        assertEquals(null, viewModel.state.value.input.customDrinkName)
    }

    @Test fun invalidInputNeverWritesAndDrinkLoadFailureCanRetryIndependently() = runTest(dispatcher) {
        val repository = Repository().apply { drinkResult = Result.failure(IllegalStateException("catalog")) }
        val viewModel = viewModel(repository)
        runCurrent()
        assertTrue(viewModel.state.value.drinksFailed)
        viewModel.onAction(ShopCheckInFormAction.RatingChanged(ShopRating(0, 4, 4)))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(ShopCheckInValidationError.InvalidRating, viewModel.state.value.validationError)
        assertTrue(repository.writes.isEmpty())
        repository.drinkResult = Result.success(emptyList())
        viewModel.onAction(ShopCheckInFormAction.RetryDrinks)
        runCurrent()
        assertFalse(viewModel.state.value.drinksFailed)
        assertEquals(2, repository.drinkCalls)
    }

    @Test fun pendingWriteGuardsRepeatedSubmitEditingAndDismissal() = runTest(dispatcher) {
        val repository = Repository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        viewModel.onAction(ShopCheckInFormAction.VisibilityChanged(ShopCheckInVisibility.Public))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        viewModel.onAction(ShopCheckInFormAction.Submit)
        viewModel.onAction(ShopCheckInFormAction.TextChanged("must not change"))
        viewModel.onAction(ShopCheckInFormAction.Dismiss)
        runCurrent()
        assertEquals(1, repository.writes.size)
        assertEquals("Coffee", viewModel.state.value.input.text)
        assertTrue(viewModel.state.value.isSubmitting)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertEquals(ShopCheckInFormEvent.Submitted(ShopCheckInVisibility.Public), viewModel.events.first())
        assertTrue(viewModel.state.value.submitted)
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(1, repository.writes.size)
    }

    @Test fun knownRejectionKeepsDraftAndAllowsExplicitRetry() = runTest(dispatcher) {
        val repository = Repository().apply { writeResult = Result.failure(IllegalStateException("rejected")) }
        val drafts = Drafts()
        val viewModel = viewModel(repository, drafts)
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.submitFailed)
        assertFalse(viewModel.state.value.deliveryUnconfirmed)
        assertEquals("Coffee", drafts.snapshot!!.input.text)
        assertFalse(drafts.snapshot!!.deliveryUnconfirmed)
        repository.writeResult = Result.success(Unit)
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(2, repository.writes.size)
        assertEquals(null, drafts.snapshot)
    }

    @Test fun unconfirmedDeliveryRemainsGuardedAcrossRecreationUntilHistoryIsChecked() = runTest(dispatcher) {
        val repository = Repository().apply { writeResult = Result.failure(ShopCheckInCreationUnconfirmed()) }
        val drafts = Drafts()
        var viewModel = viewModel(repository, drafts)
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.deliveryUnconfirmed)
        assertTrue(drafts.snapshot!!.deliveryUnconfirmed)
        viewModel = viewModel(repository, drafts)
        runCurrent()
        // A delayed picker/edit callback must not replace the possibly accepted payload.
        viewModel.onAction(ShopCheckInFormAction.TextChanged("must not change uncertain payload"))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        viewModel.onAction(ShopCheckInFormAction.OpenHistory)
        runCurrent()
        assertEquals(1, repository.writes.size)
        assertEquals("Coffee", viewModel.state.value.input.text)
        assertEquals("Coffee", drafts.snapshot!!.input.text)
        assertEquals(ShopCheckInFormEvent.OpenHistory, viewModel.events.first())
        viewModel.onAction(ShopCheckInFormAction.HistoryChecked)
        repository.writeResult = Result.success(Unit)
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(2, repository.writes.size)
    }

    @Test fun draftCleanupFailureDoesNotConvertAcceptedWriteToRetry() = runTest(dispatcher) {
        val drafts = Drafts().apply { clearFails = true }
        val repository = Repository()
        val viewModel = viewModel(repository, drafts)
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.submitted)
        assertTrue(viewModel.state.value.draftFailed)
        assertEquals(ShopCheckInFormEvent.Submitted(ShopCheckInVisibility.Private), viewModel.events.first())
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(1, repository.writes.size)
    }

    @Test fun failedDeliveryGuardSavePreventsWriteAndCancellationKeepsGuardForReopen() = runTest(dispatcher) {
        val repository = Repository()
        val drafts = Drafts()
        val viewModel = viewModel(repository, drafts)
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        runCurrent()
        drafts.saveFails = true
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertTrue(repository.writes.isEmpty())
        assertTrue(viewModel.state.value.draftFailed)
        drafts.saveFails = false
        repository.writeResult = Result.failure(CancellationException("cancel"))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertTrue(drafts.snapshot!!.deliveryUnconfirmed)
        assertFalse(viewModel.state.value.isSubmitting)
        assertFalse(viewModel.state.value.submitFailed)
    }

    @Test fun successKeepsConfirmationUntilUserChoosesFeedOrHistory() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onAction(ShopCheckInFormAction.TextChanged("Coffee"))
        viewModel.onAction(ShopCheckInFormAction.Submit)
        runCurrent()
        assertEquals(ShopCheckInFormEvent.Submitted(ShopCheckInVisibility.Private), viewModel.events.first())
        assertTrue(viewModel.state.value.submitted)
        viewModel.onAction(ShopCheckInFormAction.GoToFeed)
        runCurrent()
        assertEquals(ShopCheckInFormEvent.GoToFeed, viewModel.events.first())
        viewModel.onAction(ShopCheckInFormAction.OpenHistory)
        runCurrent()
        assertEquals(ShopCheckInFormEvent.OpenHistory, viewModel.events.first())
    }
}
