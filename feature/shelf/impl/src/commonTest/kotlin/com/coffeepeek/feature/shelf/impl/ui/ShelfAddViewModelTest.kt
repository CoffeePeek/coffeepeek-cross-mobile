package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shelf.api.ShelfCoffeeOption
import com.coffeepeek.feature.shelf.api.ShelfCoffeeSearch
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.BagPhotoCropper
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfAddViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    private val catalog = listOf(ShelfCoffeeOption("kenya", "Кения Ньери", "River Roasters"))
    private val searches = mutableListOf<String>()
    private val coffees = ShelfCoffeeSearch { query -> searches += query; Result.success(catalog) }
    private val cropped = ManualBagCrop(ShelfBagCard(byteArrayOf(42), "image/jpeg", "bag.jpg"), 400, 600)
    private val cropper = BagPhotoCropper { _, _ -> Result.success(cropped) }
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(
        repository: FakeShelfRepository = FakeShelfRepository(emptyList()), scanner: FakeBagScanner = FakeBagScanner(),
        arrivals: ShelfArrivals = ShelfArrivals(),
    ) = ShelfAddViewModel(repository, scanner, cropper, coffees, arrivals, testCalendar()).also { store.put("add-${nextKey++}", it) }

    /** Picks a photo and lets an accepted card finish peeling off. */
    private fun TestScope.photo(model: ShelfAddViewModel) {
        model.onAction(ShelfAddAction.PhotoPicked(ShelfPhoto(byteArrayOf(1), fromCamera = true)))
        runCurrent()
        model.onAction(ShelfAddAction.RevealFinished)
        runCurrent()
    }

    @Test fun selectedPhotoWaitsForScannerThenTheSamePhotoIsScanned() = runTest(dispatcher) {
        val scanner = FakeBagScanner(BagScannerReadiness.NeedsDownload(28_000_000))
        val model = model(scanner = scanner)
        runCurrent()
        assertEquals(ShelfScannerStatus.NeedsDownload(28_000_000), model.state.value.scanner)
        val picked = ShelfPhoto(byteArrayOf(1, 2, 3), fromCamera = false)
        model.onAction(ShelfAddAction.PhotoPicked(picked))
        runCurrent()
        assertEquals(0, scanner.scans)
        assertEquals(ShelfAddStep.Preparing, model.state.value.step)
        assertTrue(model.state.value.photo === picked)

        model.onAction(ShelfAddAction.DownloadScanner)
        runCurrent()
        assertEquals(ShelfScannerStatus.Ready, model.state.value.scanner)
        assertEquals(1, scanner.scans)
        assertTrue(scanner.scannedPhotos.single() === picked.bytes)
        assertEquals(ShelfAddStep.Revealing, model.state.value.step)
    }

    @Test fun theCameraOpensAtOnceAndBackReturnsWhereItCameFrom() = runTest(dispatcher) {
        val scanner = FakeBagScanner()
        val model = model(scanner = scanner)
        runCurrent()
        assertEquals(ShelfAddStep.Intro, model.state.value.step)
        model.onAction(ShelfAddAction.OpenCamera)
        runCurrent()
        assertEquals(ShelfAddStep.Camera, model.state.value.step)

        scanner.result = Result.success(scanResult(PackModerationStatus.Retake, listOf(PackModerationReason.TooFar), card = false))
        photo(model)
        model.onAction(ShelfAddAction.OpenCamera)
        runCurrent()
        assertEquals(ShelfAddStep.Camera, model.state.value.step)
        model.onAction(ShelfAddAction.CloseCamera)
        runCurrent()
        assertEquals(ShelfAddStep.Retake, model.state.value.step)
    }

    @Test fun closingTheFirstCameraReturnsToPhotoActions() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(ShelfAddAction.OpenCamera)
        runCurrent()
        model.onAction(ShelfAddAction.CloseCamera)
        runCurrent()
        assertEquals(ShelfAddStep.Intro, model.state.value.step)
    }

    @Test fun failedDownloadCanBeRetried() = runTest(dispatcher) {
        val scanner = FakeBagScanner(BagScannerReadiness.NeedsDownload(100)).apply { prepareResult = Result.failure(BagScanFailure(BagScanIssue.ModelDownload)) }
        val model = model(scanner = scanner)
        runCurrent()
        model.onAction(ShelfAddAction.DownloadScanner)
        runCurrent()
        assertEquals(ShelfScannerStatus.DownloadFailed, model.state.value.scanner)
        scanner.prepareResult = Result.success(Unit)
        model.onAction(ShelfAddAction.DownloadScanner)
        runCurrent()
        assertEquals(ShelfScannerStatus.Ready, model.state.value.scanner)
    }

    @Test fun failedPreparationKeepsThePhotoAndManualCropCanSaveIt() = runTest(dispatcher) {
        val scanner = FakeBagScanner(BagScannerReadiness.NeedsDownload(33_000_000)).apply {
            prepareResult = Result.failure(BagScanFailure(BagScanIssue.ModelDownload))
        }
        val repository = FakeShelfRepository(emptyList())
        val model = model(repository, scanner)
        runCurrent()
        val picked = ShelfPhoto(byteArrayOf(7, 8), fromCamera = false)
        model.onAction(ShelfAddAction.PhotoPicked(picked))
        model.onAction(ShelfAddAction.DownloadScanner)
        runCurrent()
        assertEquals(ShelfScannerStatus.DownloadFailed, model.state.value.scanner)
        assertTrue(model.state.value.photo === picked)
        assertEquals(0, scanner.scans)
        model.onAction(ShelfAddAction.StartManualCrop)
        runCurrent()
        assertEquals(ShelfAddStep.ManualCrop, model.state.value.step)
        model.onAction(ShelfAddAction.ApplyManualCrop(BagCropBounds.Default))
        runCurrent()
        assertEquals(ShelfAddStep.Form, model.state.value.step)
        assertNull(model.state.value.result)
        assertEquals(cropped, model.state.value.manualCrop)
        model.onAction(ShelfAddAction.Save(later = true))
        runCurrent()
        assertTrue(repository.addedCards.single() === cropped.card)
    }

    @Test fun closingCameraAfterPreparationResumesTheSelectedPhoto() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val scanner = FakeBagScanner(BagScannerReadiness.NeedsDownload(33)).apply { prepareGate = { gate.await() } }
        val model = model(scanner = scanner)
        runCurrent()
        val picked = ShelfPhoto(byteArrayOf(5), fromCamera = false)
        model.onAction(ShelfAddAction.PhotoPicked(picked))
        model.onAction(ShelfAddAction.DownloadScanner)
        runCurrent()
        model.onAction(ShelfAddAction.OpenCamera)
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(ShelfAddStep.Camera, model.state.value.step)
        model.onAction(ShelfAddAction.CloseCamera)
        runCurrent()
        assertEquals(1, scanner.scans)
        assertTrue(scanner.scannedPhotos.single() === picked.bytes)
    }

    @Test fun choosingManualCropStopsTheOngoingDownload() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val scanner = FakeBagScanner(BagScannerReadiness.NeedsDownload(33)).apply { prepareGate = { gate.await() } }
        val model = model(scanner = scanner)
        runCurrent()
        model.onAction(ShelfAddAction.PhotoPicked(ShelfPhoto(byteArrayOf(4), fromCamera = false)))
        model.onAction(ShelfAddAction.DownloadScanner)
        runCurrent()
        assertTrue(model.state.value.scanner is ShelfScannerStatus.Downloading)
        model.onAction(ShelfAddAction.StartManualCrop)
        runCurrent()
        assertEquals(1, scanner.cancelledPreparations)
        assertEquals(ShelfScannerStatus.NeedsDownload(33), model.state.value.scanner)
        assertEquals(ShelfAddStep.ManualCrop, model.state.value.step)
        assertEquals(0, scanner.scans)
    }

    @Test fun manualCropUsesSelectedBoundsAndCanBeCancelled() = runTest(dispatcher) {
        val used = mutableListOf<Pair<ByteArray, BagCropBounds>>()
        val cropper = BagPhotoCropper { bytes, bounds -> used += bytes to bounds; Result.success(cropped) }
        val model = ShelfAddViewModel(FakeShelfRepository(emptyList()), FakeBagScanner(BagScannerReadiness.NeedsDownload(33)),
            cropper, coffees, ShelfArrivals(), testCalendar()).also { store.put("crop", it) }
        runCurrent()
        val picked = ShelfPhoto(byteArrayOf(9), fromCamera = true)
        model.onAction(ShelfAddAction.PhotoPicked(picked))
        model.onAction(ShelfAddAction.StartManualCrop)
        runCurrent()
        model.onAction(ShelfAddAction.CancelManualCrop)
        runCurrent()
        assertEquals(ShelfAddStep.Preparing, model.state.value.step)
        model.onAction(ShelfAddAction.StartManualCrop)
        val bounds = BagCropBounds(0.2f, 0.1f, 0.7f, 0.8f)
        model.onAction(ShelfAddAction.CropChanged(bounds))
        model.onAction(ShelfAddAction.ApplyManualCrop(bounds))
        runCurrent()
        assertEquals(1, used.size)
        assertTrue(used.single().first === picked.bytes)
        assertEquals(bounds, used.single().second)
    }

    @Test fun stagesTickOffAndAnAcceptedScanOpensTheForm() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = model(scanner = FakeBagScanner().apply { this.gate = { gate.await() } })
        runCurrent()
        photo(model)
        assertEquals(ShelfAddStep.Scanning, model.state.value.step)
        assertEquals(setOf(BagScanStage.Segmentation), model.state.value.stagesDone)
        gate.complete(Unit)
        runCurrent()
        assertEquals(ShelfAddStep.Revealing, model.state.value.step)
        assertEquals(BagScanStage.entries.toSet(), model.state.value.stagesDone)
        model.onAction(ShelfAddAction.RevealFinished)
        runCurrent()
        assertEquals(ShelfAddStep.Form, model.state.value.step)
        assertTrue(model.state.value.cardLanding)
        model.onAction(ShelfAddAction.CardLanded)
        runCurrent()
        assertFalse(model.state.value.cardLanding)
    }

    @Test fun theFoundFrontShowsBeforeTheCardAndARevealThatNeverEndsStillOpensTheForm() = runTest(dispatcher) {
        val frame = BagScanFrame(listOf(0.1f to 0.1f, 0.9f to 0.1f, 0.9f to 0.9f, 0.1f to 0.9f))
        val gate = CompletableDeferred<Unit>()
        val scanner = object : BagScanner by FakeBagScanner() {
            override suspend fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit, onFront: (BagScanFrame) -> Unit): Result<BagScanResult> {
                onFront(frame)
                gate.await()
                return Result.success(scanResult(PackModerationStatus.Accepted))
            }
        }
        val model = ShelfAddViewModel(FakeShelfRepository(emptyList()), scanner, cropper, coffees, ShelfArrivals(), testCalendar()).also { store.put("frame", it) }
        runCurrent()
        model.onAction(ShelfAddAction.PhotoPicked(ShelfPhoto(byteArrayOf(1), fromCamera = false)))
        runCurrent()
        assertEquals(frame, model.state.value.liveFrame)
        gate.complete(Unit)
        runCurrent()
        assertEquals(ShelfAddStep.Revealing, model.state.value.step)
        advanceTimeBy(2_600); runCurrent()
        assertEquals(ShelfAddStep.Form, model.state.value.step)
    }

    @Test fun verdictsDecideBetweenFormWithHintsAndRetake() = runTest(dispatcher) {
        val scanner = FakeBagScanner()
        val model = model(scanner = scanner)
        runCurrent()
        scanner.result = Result.success(scanResult(PackModerationStatus.AcceptedWithHint, listOf(PackModerationReason.Glare)))
        photo(model)
        assertEquals(ShelfAddStep.Form, model.state.value.step)
        assertEquals(listOf(PackModerationReason.Glare), model.state.value.hints)

        scanner.result = Result.success(scanResult(PackModerationStatus.Retake, listOf(PackModerationReason.ObjectsOnBag), card = false))
        photo(model)
        assertEquals(ShelfAddStep.Retake, model.state.value.step)

        scanner.result = Result.failure(BagScanFailure(BagScanIssue.OutOfMemory))
        photo(model)
        assertEquals(ShelfAddStep.Retake, model.state.value.step)
        assertEquals(BagScanIssue.OutOfMemory, model.state.value.failure)
    }

    @Test fun catalogueSearchWaitsForTypingAndAChoiceFillsTheRoaster() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(ShelfAddAction.QueryChanged("К"))
        model.onAction(ShelfAddAction.QueryChanged("Ке"))
        model.onAction(ShelfAddAction.QueryChanged("Кен"))
        advanceTimeBy(350); runCurrent()
        assertEquals(listOf("Кен"), searches)
        assertEquals(catalog, model.state.value.options)

        model.onAction(ShelfAddAction.CoffeeChosen(catalog[0]))
        runCurrent()
        assertEquals(ShelfBagForm(query = "Кения Ньери", coffee = catalog[0], roaster = "River Roasters"), model.state.value.form)
        assertTrue(model.state.value.options.isEmpty())

        model.onAction(ShelfAddAction.QueryChanged("Кения Ньери АА"))
        runCurrent()
        assertNull(model.state.value.form.coffee)
    }

    @Test fun aSavedBagIsHandedToTheShelf() = runTest(dispatcher) {
        val arrivals = ShelfArrivals()
        val model = model(arrivals = arrivals)
        runCurrent()
        photo(model)
        model.onAction(ShelfAddAction.Save(later = true))
        runCurrent()
        assertEquals("new", arrivals.arriving.value)
    }

    @Test fun savingSendsTheFormAndLaterSendsOnlyTheCard() = runTest(dispatcher) {
        val repository = FakeShelfRepository(emptyList())
        val model = model(repository)
        runCurrent()
        photo(model)
        model.onAction(ShelfAddAction.QueryChanged("Свой кофе"))
        model.onAction(ShelfAddAction.WeightChosen(250))
        model.onAction(ShelfAddAction.GrindChosen(ShelfGrind.Beans))
        model.onAction(ShelfAddAction.RoastPurposeChosen(ShelfRoastPurpose.Filter))
        model.onAction(ShelfAddAction.RoastDateChosen(ShelfDay(2026, 10, 20)))
        model.onAction(ShelfAddAction.RoastDateChosen(ShelfDay(2026, 10, 30)))
        runCurrent()
        assertNull(model.state.value.form.roastedOn)
        model.onAction(ShelfAddAction.RoastDateChosen(ShelfDay(2026, 10, 20)))
        model.onAction(ShelfAddAction.Save(later = false))
        runCurrent()
        assertEquals(ShelfAddEvent.Saved, model.events.first())
        assertEquals(ShelfBagDetails(ShelfBean.Custom("Свой кофе"), null, 250, ShelfGrind.Beans, ShelfRoastPurpose.Filter, "2026-10-20"),
            repository.added.single())

        model.onAction(ShelfAddAction.Save(later = true))
        runCurrent()
        assertEquals(ShelfBagDetails(), repository.added.last())
    }

    @Test fun aSecondBagOfACatalogueCoffeeAsksFirst() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("old").copy(coffeeProductSlug = "kenya")))
        val model = model(repository)
        runCurrent()
        photo(model)
        model.onAction(ShelfAddAction.CoffeeChosen(catalog[0]))
        model.onAction(ShelfAddAction.Save(later = false))
        runCurrent()
        assertTrue(model.state.value.confirmDuplicate)
        assertTrue(repository.added.isEmpty())

        model.onAction(ShelfAddAction.ConfirmDuplicate)
        runCurrent()
        assertFalse(model.state.value.confirmDuplicate)
        assertEquals(ShelfBean.Catalog("kenya"), repository.added.single().bean)
    }

    @Test fun failedSaveKeepsTheFormAndReportsWhy() = runTest(dispatcher) {
        val repository = FakeShelfRepository(emptyList()).apply { writeFailure = ShelfIssue.PhotoUpload }
        val model = model(repository)
        runCurrent()
        photo(model)
        model.onAction(ShelfAddAction.QueryChanged("Свой кофе"))
        model.onAction(ShelfAddAction.Save(later = false))
        runCurrent()
        assertEquals(ShelfAddMessage.Failed(ShelfIssue.PhotoUpload), model.state.value.message)
        assertEquals(ShelfAddStep.Form, model.state.value.step)
        assertEquals("Свой кофе", model.state.value.form.query)
        assertFalse(model.state.value.saving)
    }
}
