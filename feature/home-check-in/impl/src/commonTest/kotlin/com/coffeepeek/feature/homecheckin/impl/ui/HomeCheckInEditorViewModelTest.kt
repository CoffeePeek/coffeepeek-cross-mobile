package com.coffeepeek.feature.homecheckin.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.usecase.homePreparationFieldErrors
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeCheckInEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var key = 0
    private val now = Instant.parse("2026-10-17T12:00:00Z")

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Repository : HomePreparationRepository {
        val detailIds = mutableListOf<String>()
        val creates = mutableListOf<HomePreparationInput>()
        val updates = mutableListOf<Pair<String, HomePreparationInput>>()
        val writeCompletions = mutableListOf<Throwable?>()
        var detailResult = Result.success(entry())
        var saveResult = Result.success(entry())
        var detailGate: CompletableDeferred<Unit>? = null
        var saveGate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false

        override suspend fun detail(id: String): Result<HomePreparation> {
            detailIds += id
            val result = detailResult
            val gate = detailGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }
        override suspend fun create(input: HomePreparationInput): Result<HomePreparation> { creates += input; return write() }
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> { updates += id to input; return write() }
        private suspend fun write(): Result<HomePreparation> {
            currentCoroutineContext().job.invokeOnCompletion { writeCompletions += it }
            val result = saveResult
            val gate = saveGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }
        override suspend fun delete(id: String): Result<Unit> = error("Editor must not delete")
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = error("No history request")
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = error("No calendar request")
    }

    private class Catalog : HomeCheckInEditorCatalog {
        var methodsCalls = 0
        var methodsGate: CompletableDeferred<Unit>? = null
        var methodsResult = Result.success(listOf(HomeCheckInBrewMethod("hario-v60", "Hario V60")))
        val queries = mutableListOf<String>()
        var search: suspend (String) -> Result<List<HomeCheckInBeanOption>> = {
            Result.success(listOf(HomeCheckInBeanOption("bean", "Эфиопия", "Обжарщик")))
        }
        override suspend fun brewMethods(): Result<List<HomeCheckInBrewMethod>> {
            methodsCalls++
            val result = methodsResult
            methodsGate?.await()
            return result
        }
        override suspend fun searchBeans(query: String): Result<List<HomeCheckInBeanOption>> {
            queries += query
            return search(query)
        }
    }


    private fun model(repo: Repository = Repository(), catalog: Catalog = Catalog(),
        id: String? = null, access: () -> Boolean = { true },
        analytics: HomeCheckInEditorAnalytics = HomeCheckInEditorAnalytics.None): HomeCheckInEditorViewModel = HomeCheckInEditorViewModel(
        repo, catalog, "2026-10-17T09:30:00Z", "2026-10-17", id, 7, access, { now }, analytics,
    ).also { store.put("editor-${key++}", it) }

    private fun HomeCheckInEditorViewModel.act(vararg actions: HomeCheckInEditorAction) { actions.forEach(::onAction) }

    @Test fun createStartsPrivateAtFiveWithoutLoadingHistoryOrDetail() = runTest(dispatcher) {
        val repo = Repository(); val catalog = Catalog(); val vm = model(repo, catalog)
        runCurrent()
        assertTrue(vm.state.value.isReady)
        assertEquals(5, vm.state.value.draft.tasteScore)
        assertEquals(HomePreparationVisibility.Private, vm.state.value.draft.visibility)
        assertTrue(homePreparationFieldErrors(vm.state.value.draft, now = now).isEmpty())
        assertEquals(1, catalog.methodsCalls)
        assertTrue(repo.detailIds.isEmpty())
        assertTrue(repo.creates.isEmpty())
    }

    @Test fun allRecipeFieldsRemainOptionalAndTappingSelectedRatingClearsIt() = runTest(dispatcher) {
        val repo = Repository(); val vm = model(repo); runCurrent()
        vm.act(HomeCheckInEditorAction.TasteRatingTapped(5), HomeCheckInEditorAction.Save); runCurrent()
        val sent = repo.creates.single()
        assertNull(sent.tasteScore)
        assertNull(sent.brewMethodCode)
        assertNull(sent.coffeeProductSlug)
        assertTrue(sent.photos.isEmpty())
        assertTrue(vm.state.value.isClosed)
        assertEquals(HomeCheckInEditorEvent.Saved("home", false), vm.events.first().event)
    }

    @Test fun editPreservesHistoricalIdentityUnknownVisibilityCustomDrinkAndServerPhotos() = runTest(dispatcher) {
        val original = entry().copy(visibility = null, tasteScore = null, coffeeProductId = "historical-product",
            drinkSlug = null, drinkName = "Мой кофе", customDrinkName = null,
            photos = listOf(HomePreparationPhoto("photo", "issued-original")))
        val repo = Repository().apply { detailResult = Result.success(original) }
        val vm = model(repo, id = "home"); runCurrent()
        assertEquals(original, vm.state.value.entry)
        assertNull(vm.state.value.draft.visibility)
        assertNull(vm.state.value.draft.tasteScore)
        assertEquals("historical-product", vm.state.value.draft.coffeeProductId)
        assertNull(vm.state.value.draft.customBeanName)
        assertEquals("other", vm.state.value.draft.drinkSlug)
        assertEquals("Мой кофе", vm.state.value.draft.customDrinkName)
        assertTrue(vm.state.value.draft.photos.isEmpty())
    }

    @Test fun detailLoadBlocksEditsAndWritesAndDuplicateRetryDoesNotStartAnotherLoad() = runTest(dispatcher) {
        val repo = Repository().apply { detailGate = CompletableDeferred() }
        val vm = model(repo, id = "home"); runCurrent()
        val draft = vm.state.value.draft
        vm.act(HomeCheckInEditorAction.DoseChanged("22"), HomeCheckInEditorAction.Save, HomeCheckInEditorAction.RetryDetail); runCurrent()
        assertEquals(draft, vm.state.value.draft)
        assertEquals(listOf("home"), repo.detailIds)
        assertTrue(repo.updates.isEmpty())
        assertTrue(repo.creates.isEmpty())
        repo.detailGate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.isReady)
    }

    @Test fun failedEditLoadNeverFallsBackToCreatingAndRetryCannotOverwriteLoadedEdits() = runTest(dispatcher) {
        val repo = Repository().apply { detailResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Offline)) }
        val vm = model(repo, id = "home"); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertTrue(repo.creates.isEmpty()); assertTrue(repo.updates.isEmpty())
        assertEquals(HomePreparationIssue.Offline, vm.state.value.loadIssue)
        repo.detailResult = Result.success(entry())
        vm.onAction(HomeCheckInEditorAction.RetryDetail); runCurrent()
        vm.act(HomeCheckInEditorAction.DoseChanged("23"), HomeCheckInEditorAction.RetryDetail); runCurrent()
        assertEquals("23", vm.state.value.draft.doseGrams)
        assertEquals(2, repo.detailIds.size)
    }

    @Test fun blankOrMismatchedEditIdNeverProducesAnEditableDifferentRecord() = runTest(dispatcher) {
        for (id in listOf("", "different")) {
            val repo = Repository(); val vm = model(repo, id = id); runCurrent()
            assertFalse(vm.state.value.isReady)
            assertEquals(if (id.isEmpty()) HomePreparationIssue.InvalidInput else HomePreparationIssue.InvalidResponse, vm.state.value.loadIssue)
            vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
            assertTrue(repo.creates.isEmpty()); assertTrue(repo.updates.isEmpty())
        }
    }

    @Test fun choosingSameCivilDayKeepsTimeButAnotherDayUpdatesBothFields() = runTest(dispatcher) {
        val vm = model(id = "home"); runCurrent()
        val original = vm.state.value.draft
        vm.onAction(HomeCheckInEditorAction.DateChosen(original.preparedOn!!, "2026-10-16T00:00:00Z")); runCurrent()
        assertEquals(original, vm.state.value.draft)
        vm.onAction(HomeCheckInEditorAction.DateChosen("2026-10-15", "2026-10-14T21:00:00Z")); runCurrent()
        assertEquals("2026-10-15", vm.state.value.draft.preparedOn)
        assertEquals("2026-10-14T21:00:00Z", vm.state.value.draft.preparedAt)
    }

    @Test fun futureDateIsValidatedBeforeAnyRequest() = runTest(dispatcher) {
        val repo = Repository(); val vm = model(repo); runCurrent()
        vm.act(HomeCheckInEditorAction.DateChosen("2026-10-18", "2026-10-18T09:00:00Z"), HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(HomePreparationIssue.FutureDate, vm.state.value.fieldErrors[HomePreparationField.Date])
        assertTrue(repo.creates.isEmpty())
    }

    @Test fun textLimitsCommaDoseAndEmptyCustomDrinkMatchTheCurrentForm() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.act(HomeCheckInEditorAction.DoseChanged("18,25"), HomeCheckInEditorAction.NoteChanged("n".repeat(1001)),
            HomeCheckInEditorAction.DrinkNameChanged("d".repeat(101))); runCurrent()
        assertEquals("18.25", vm.state.value.draft.doseGrams)
        assertEquals(1000, vm.state.value.draft.text!!.length)
        assertEquals(100, vm.state.value.draft.customDrinkName!!.length)
        assertEquals("other", vm.state.value.draft.drinkSlug)
        vm.act(HomeCheckInEditorAction.DrinkNameChanged(" "), HomeCheckInEditorAction.NoteChanged(""), HomeCheckInEditorAction.DoseChanged("")); runCurrent()
        assertNull(vm.state.value.draft.customDrinkName); assertNull(vm.state.value.draft.drinkSlug)
        assertNull(vm.state.value.draft.text); assertNull(vm.state.value.draft.doseGrams)
    }

    @Test fun localValidationCollectsAllErrorsAndOnlyChangedFieldsAreRevalidated() = runTest(dispatcher) {
        val repo = Repository(); val vm = model(repo); runCurrent()
        vm.act(HomeCheckInEditorAction.DoseChanged("0"), HomeCheckInEditorAction.DurationChanged(0),
            HomeCheckInEditorAction.MethodChanged("INVALID"), HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(setOf(HomePreparationField.Dose, HomePreparationField.Duration, HomePreparationField.Method), vm.state.value.fieldErrors.keys)
        assertTrue(repo.creates.isEmpty())
        vm.onAction(HomeCheckInEditorAction.DoseChanged("18.001")); runCurrent()
        assertEquals(HomePreparationIssue.DosePrecision, vm.state.value.fieldErrors[HomePreparationField.Dose])
        vm.act(HomeCheckInEditorAction.DoseChanged("18"), HomeCheckInEditorAction.NoteChanged("Note")); runCurrent()
        assertEquals(setOf(HomePreparationField.Duration, HomePreparationField.Method), vm.state.value.fieldErrors.keys)
    }

    @Test fun serverFieldErrorsSurviveUnrelatedEditsAndClearOnlyWhenCorrected() = runTest(dispatcher) {
        val errors = mapOf(HomePreparationField.Bean to HomePreparationIssue.BeanUnavailable, HomePreparationField.Method to HomePreparationIssue.InvalidMethod)
        val repo = Repository().apply { saveResult = Result.failure(HomePreparationFailure(HomePreparationIssue.InvalidInput, errors)) }
        val vm = model(repo); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        vm.onAction(HomeCheckInEditorAction.NoteChanged("Do not clear other fields")); runCurrent()
        assertEquals(errors, vm.state.value.fieldErrors)
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        vm.onAction(HomeCheckInEditorAction.BeanSelected("bean")); runCurrent()
        assertEquals(setOf(HomePreparationField.Method), vm.state.value.fieldErrors.keys)
        vm.onAction(HomeCheckInEditorAction.MethodChanged("hario-v60")); runCurrent()
        assertTrue(vm.state.value.fieldErrors.isEmpty())
        assertNull(vm.state.value.saveIssue)
    }

    @Test fun selectingCustomOrPublishedBeanReplacesRatherThanCombinesHistoricalSelectors() = runTest(dispatcher) {
        val repo = Repository().apply { detailResult = Result.success(entry().copy(coffeeProductId = "historical")) }
        val vm = model(repo, id = "home"); runCurrent()
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        vm.onAction(HomeCheckInEditorAction.CustomBeanChosen("  Свой кофе  ")); runCurrent()
        assertEquals("Свой кофе", vm.state.value.draft.customBeanName)
        assertNull(vm.state.value.draft.coffeeProductId)
        assertNull(vm.state.value.beanSelectionName)
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        vm.onAction(HomeCheckInEditorAction.BeanSelected("bean")); runCurrent()
        assertEquals("bean", vm.state.value.draft.coffeeProductSlug)
        assertNull(vm.state.value.draft.customBeanName)
        assertEquals("Эфиопия", vm.state.value.beanSelectionName)
        assertFalse(vm.state.value.isBeanPickerOpen)
    }

    @Test fun beanPickerLoadsInitiallyAndTypingDebouncesOnlyTheLatestTrimmedQuery() = runTest(dispatcher) {
        val catalog = Catalog(); val vm = model(catalog = catalog); runCurrent()
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        assertEquals(listOf(""), catalog.queries)
        vm.act(HomeCheckInEditorAction.BeanQueryChanged("э"), HomeCheckInEditorAction.BeanQueryChanged("эф"), HomeCheckInEditorAction.BeanQueryChanged(" эфиопия ")); runCurrent()
        advanceTimeBy(299); runCurrent(); assertEquals(1, catalog.queries.size)
        advanceTimeBy(1); runCurrent(); assertEquals(listOf("", "эфиопия"), catalog.queries)
        vm.onAction(HomeCheckInEditorAction.BeanQueryChanged("b".repeat(101))); runCurrent()
        assertEquals(100, vm.state.value.beanQuery.length)
    }

    @Test fun nonCooperativeOldBeanSearchCannotReplaceCurrentResults() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val catalog = Catalog().apply { search = { query ->
            if (query == "old") withContext(NonCancellable) { gate.await() }
            Result.success(listOf(HomeCheckInBeanOption(query, query, "Roaster")))
        } }
        val vm = model(catalog = catalog); runCurrent()
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        vm.onAction(HomeCheckInEditorAction.BeanQueryChanged("old")); advanceTimeBy(300); runCurrent()
        vm.onAction(HomeCheckInEditorAction.BeanQueryChanged("new")); advanceTimeBy(300); runCurrent()
        assertEquals("new", vm.state.value.beans.single().slug)
        gate.complete(Unit); runCurrent()
        assertEquals("new", vm.state.value.beans.single().slug)
    }

    @Test fun closingPickerCancelsItsLateSearchAndUnknownSelectionIsIgnored() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val catalog = Catalog().apply { search = { withContext(NonCancellable) { gate.await() }; Result.success(listOf(HomeCheckInBeanOption("late", "Late", ""))) } }
        val vm = model(catalog = catalog); runCurrent()
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        vm.act(HomeCheckInEditorAction.BeanSelected("not-a-result"), HomeCheckInEditorAction.CloseBeanPicker); runCurrent()
        gate.complete(Unit); runCurrent()
        assertFalse(vm.state.value.isBeanPickerOpen); assertTrue(vm.state.value.beans.isEmpty())
        assertNull(vm.state.value.draft.coffeeProductSlug)
    }

    @Test fun catalogueFailuresAreTypedAndExplicitRetryDoesNotResetTheDraft() = runTest(dispatcher) {
        val catalog = Catalog().apply { methodsResult = Result.failure(IllegalStateException("private backend payload")) }
        val vm = model(catalog = catalog); runCurrent()
        assertEquals(HomePreparationIssue.RequestFailed, vm.state.value.methodsIssue)
        vm.onAction(HomeCheckInEditorAction.DoseChanged("22")); runCurrent()
        catalog.methodsResult = Result.success(listOf(HomeCheckInBrewMethod("espresso", "Эспрессо")))
        vm.onAction(HomeCheckInEditorAction.RetryMethods); runCurrent()
        assertEquals("22", vm.state.value.draft.doseGrams)
        assertEquals("espresso", vm.state.value.methods.single().code)
        assertNull(vm.state.value.methodsIssue)
        catalog.search = { Result.failure(HomePreparationFailure(HomePreparationIssue.Offline)) }
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        assertEquals(HomePreparationIssue.Offline, vm.state.value.beansIssue)
        assertFalse(vm.state.value.beansLoading)
    }

    @Test fun duplicateMethodsRequestsCoalesceAndLateResultsCannotReopenSavedForm() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); val catalog = Catalog().apply { methodsGate = gate }
        val vm = model(catalog = catalog); runCurrent()
        vm.onAction(HomeCheckInEditorAction.RetryMethods); runCurrent()
        assertEquals(1, catalog.methodsCalls)
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        gate.complete(Unit); runCurrent()
        assertTrue(vm.state.value.isClosed)
        assertTrue(vm.state.value.methods.isEmpty())
    }

    @Test fun savingBlocksDuplicateWritesEditingClosingAndPickerActions() = runTest(dispatcher) {
        val repo = Repository().apply { saveGate = CompletableDeferred() }; val vm = model(repo); runCurrent()
        vm.onAction(HomeCheckInEditorAction.NoteChanged("Snapshot")); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        vm.act(HomeCheckInEditorAction.Save, HomeCheckInEditorAction.NoteChanged("Lost edit"), HomeCheckInEditorAction.Close,
            HomeCheckInEditorAction.OpenBeanPicker, HomeCheckInEditorAction.RetryMethods); runCurrent()
        assertTrue(vm.state.value.isSaving)
        assertEquals("Snapshot", vm.state.value.draft.text)
        assertEquals(1, repo.creates.size)
        assertFalse(vm.state.value.isBeanPickerOpen)
        repo.saveGate!!.complete(Unit); runCurrent()
        assertEquals(HomeCheckInEditorEvent.Saved("home", false), vm.events.first().event)
    }

    @Test fun unknownSaveFailureRetainsDraftAndNeverAutomaticallyRepeatsTheWrite() = runTest(dispatcher) {
        val repo = Repository().apply { saveResult = Result.failure(IllegalStateException("SQL private data")) }
        val vm = model(repo); runCurrent()
        vm.onAction(HomeCheckInEditorAction.NoteChanged("Keep draft")); runCurrent()
        val draft = vm.state.value.draft
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent(); advanceTimeBy(10_000); runCurrent()
        assertEquals(HomePreparationIssue.RequestFailed, vm.state.value.saveIssue)
        assertEquals(draft, vm.state.value.draft)
        assertFalse(vm.state.value.isSaving); assertEquals(1, repo.creates.size)
        assertNull(withTimeoutOrNull(1) { vm.events.first() })
        repo.saveResult = Result.success(entry())
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(2, repo.creates.size)
    }

    @Test fun cancellationWrappedInResultIsRethrownWithoutGenericErrorOrLosingDraft() = runTest(dispatcher) {
        val repo = Repository().apply { saveResult = Result.failure(CancellationException("cancelled")) }
        val vm = model(repo); runCurrent()
        val draft = vm.state.value.draft
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertFalse(vm.state.value.isSaving); assertNull(vm.state.value.saveIssue)
        assertEquals(draft, vm.state.value.draft); assertFalse(vm.state.value.isClosed)
        assertEquals(1, repo.creates.size)
        assertIs<CancellationException>(repo.writeCompletions.single())
        assertNull(withTimeoutOrNull(1) { vm.events.first() })
    }

    @Test fun visibilityMismatchReturnsSavedIdOnceWithoutAnotherCreate() = runTest(dispatcher) {
        for (visibility in listOf(null, HomePreparationVisibility.Private, HomePreparationVisibility.Public)) {
            val repo = Repository().apply { saveResult = Result.success(entry().copy(visibility = visibility, moderation = HomePreparationModeration.Pending)) }
            val vm = model(repo); runCurrent()
            vm.act(HomeCheckInEditorAction.VisibilityChanged(HomePreparationVisibility.Public), HomeCheckInEditorAction.Save); runCurrent()
            val event = vm.events.first()
            assertEquals(HomeCheckInEditorEvent.Saved("home", visibility != HomePreparationVisibility.Public), event.event)
            assertTrue(vm.canDeliver(event))
            vm.act(HomeCheckInEditorAction.Save, HomeCheckInEditorAction.Initialize); runCurrent()
            assertEquals(1, repo.creates.size)
            assertTrue(vm.state.value.draft.photos.isEmpty())
            assertNull(vm.state.value.draft.text)
        }
    }

    @Test fun editSendsPutWithUnknownVisibilityAndNeverUploadsOrCreates() = runTest(dispatcher) {
        val repo = Repository().apply { detailResult = Result.success(entry().copy(visibility = null)) }
        val vm = model(repo, id = "home"); runCurrent()
        vm.act(HomeCheckInEditorAction.PhotosAdded(listOf(photo("ignored"))), HomeCheckInEditorAction.RemovePhoto(0),
            HomeCheckInEditorAction.Save); runCurrent()
        assertEquals("home", repo.updates.single().first)
        assertNull(repo.updates.single().second.visibility)
        assertTrue(repo.updates.single().second.photos.isEmpty())
        assertTrue(repo.creates.isEmpty())
    }

    @Test fun invalidSavedIdentityIsNotReportedAsSuccessOrRetriedAsCreate() = runTest(dispatcher) {
        val repo = Repository().apply { saveResult = Result.success(entry().copy(id = "other")) }
        val vm = model(repo, id = "home"); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(HomePreparationIssue.InvalidResponse, vm.state.value.saveIssue)
        assertEquals(1, repo.updates.size); assertTrue(repo.creates.isEmpty())
        val createRepo = Repository().apply { saveResult = Result.success(entry().copy(id = "")) }
        val creator = model(createRepo); runCurrent(); creator.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(HomePreparationIssue.InvalidResponse, creator.state.value.saveIssue)
        assertFalse(creator.state.value.isClosed)
    }

    @Test fun photoUploadFailureKeepsItsStageWithoutBackgroundRetry() = runTest(dispatcher) {
        val repo = Repository().apply { saveResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Offline, operation = HomePreparationOperation.PhotoUpload)) }
        val vm = model(repo); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(HomePreparationOperation.PhotoUpload, vm.state.value.failedOperation)
        assertEquals(HomePreparationIssue.Offline, vm.state.value.saveIssue)
        advanceTimeBy(10_000); runCurrent(); assertEquals(1, repo.creates.size)
    }

    @Test fun photosAreLimitedToFiveAndTheTransferredBytesAreDetached() = runTest(dispatcher) {
        val incoming = photo("original"); val vm = model(); runCurrent()
        vm.onAction(HomeCheckInEditorAction.PhotosAdded(listOf(incoming) + (1..6).map { photo("$it") })); runCurrent()
        val originalByte = vm.state.value.draft.photos.first().bytes.first()
        incoming.bytes[0] = 0
        assertEquals(originalByte, vm.state.value.draft.photos.first().bytes.first())
        assertEquals(5, vm.state.value.draft.photos.size)
        vm.onAction(HomeCheckInEditorAction.RemovePhoto(99)); runCurrent()
        assertEquals(5, vm.state.value.draft.photos.size)
    }

    @Test fun originalPhotoCanBeReplacedAndSavedWithoutAnExtractionStep() = runTest(dispatcher) {
        val repo = Repository()
        val vm = model(repo); runCurrent()
        vm.act(HomeCheckInEditorAction.PhotosAdded(listOf(photo("first"), photo("second"))),
            HomeCheckInEditorAction.NoteChanged("Keep note")); runCurrent()
        vm.onAction(HomeCheckInEditorAction.RemovePhoto(0)); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(listOf("second"), repo.creates.single().photos.map { it.fileName })
        assertContentEquals("second".encodeToByteArray(), repo.creates.single().photos.single().bytes)
        assertEquals("Keep note", repo.creates.single().text)
        assertTrue(vm.state.value.isClosed)
    }

    @Test fun closingBeforeLateDetailErasesPrivateStateAndEmitsOnlyClose() = runTest(dispatcher) {
        val repo = Repository().apply { detailGate = CompletableDeferred(); ignoreCancellation = true }
        val vm = model(repo, id = "home"); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Close); runCurrent()
        val close = vm.events.first()
        assertEquals(HomeCheckInEditorEvent.Close, close.event); assertTrue(vm.canDeliver(close))
        repo.detailGate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.isClosed); assertNull(vm.state.value.entry)
        assertEquals("", vm.state.value.draft.preparedAt)
    }

    @Test fun leaseReplacementDuringNonCooperativeSaveRejectsOutcomeAndClearsDraft() = runTest(dispatcher) {
        var allowed = true
        val repo = Repository().apply { saveGate = CompletableDeferred(); ignoreCancellation = true }
        val vm = model(repo, access = { allowed }); runCurrent()
        vm.act(HomeCheckInEditorAction.NoteChanged("Private note"), HomeCheckInEditorAction.Save); runCurrent()
        allowed = false
        vm.onAction(HomeCheckInEditorAction.RevokeAccess); runCurrent()
        repo.saveGate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.isAccessRevoked)
        assertNull(vm.state.value.draft.text); assertFalse(vm.state.value.isSaving)
        assertEquals(1, repo.creates.size)
    }

    @Test fun revokedOrClearedOwnerErasesPrivatePhotoSnapshots() = runTest(dispatcher) {
        for (clearOwner in listOf(false, true)) {
            val vm = model(); runCurrent()
            vm.onAction(HomeCheckInEditorAction.PhotosAdded(listOf(photo("private")))); runCurrent()
            if (clearOwner) store.clear() else vm.onAction(HomeCheckInEditorAction.RevokeAccess)
            runCurrent()
            assertTrue(vm.state.value.isAccessRevoked)
            assertTrue(vm.state.value.draft.photos.isEmpty())
        }
    }

    @Test fun initiallyDeniedAccountDoesNotStartAnyPortAndLaterActionsCannotReviveIt() = runTest(dispatcher) {
        val repo = Repository(); val catalog = Catalog()
        val vm = model(repo, catalog, access = { false }); runCurrent()
        vm.act(HomeCheckInEditorAction.Initialize, HomeCheckInEditorAction.Save, HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        assertTrue(vm.state.value.isAccessRevoked)
        assertEquals(0, catalog.methodsCalls); assertTrue(catalog.queries.isEmpty())
        assertTrue(repo.creates.isEmpty())
    }

    @Test fun authorizationFailureDoesNotEraseCurrentOwnersDraftOrTreatTokenRenewalAsAccountChange() = runTest(dispatcher) {
        for (issue in listOf(HomePreparationIssue.Unauthorized, HomePreparationIssue.Forbidden)) {
            val repo = Repository().apply { saveResult = Result.failure(HomePreparationFailure(issue)) }
            val vm = model(repo); runCurrent()
            vm.act(HomeCheckInEditorAction.NoteChanged("Private"), HomeCheckInEditorAction.Save); runCurrent()
            assertFalse(vm.state.value.isAccessRevoked)
            assertEquals("Private", vm.state.value.draft.text)
            assertEquals(issue, vm.state.value.saveIssue)
            repo.saveResult = Result.success(entry())
            vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
            assertEquals(HomeCheckInEditorEvent.Saved("home", false), vm.events.first().event)
        }
    }

    @Test fun bufferedEffectsRequireTheSameAccountAndRevisionEvenBeforeRevokeAction() = runTest(dispatcher) {
        var allowed = true
        val vm = model(access = { allowed }); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        val saved = vm.events.first()
        assertTrue(vm.canDeliver(saved)); assertFalse(vm.canDeliver(saved.copy(sessionGeneration = 8)))
        assertFalse(vm.canDeliver(saved.copy(revision = saved.revision - 1)))
        allowed = false
        assertFalse(vm.canDeliver(saved))
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertTrue(vm.state.value.isAccessRevoked)
    }

    @Test fun leaseRevocationDetectedByLateLoadErasesStateWithoutNeedingAnotherAction() = runTest(dispatcher) {
        var allowed = true
        val repo = Repository().apply { detailGate = CompletableDeferred(); ignoreCancellation = true }
        val vm = model(repo, id = "home", access = { allowed }); runCurrent()
        allowed = false
        repo.detailGate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.isAccessRevoked)
        assertNull(vm.state.value.entry); assertEquals("", vm.state.value.draft.preparedAt)
        assertNull(withTimeoutOrNull(1) { vm.events.first() })
    }

    @Test fun revokedSaveNeverQueuesEvenAnUndeliverableSavedEvent() = runTest(dispatcher) {
        val repo = Repository().apply { saveGate = CompletableDeferred(); ignoreCancellation = true }
        val vm = model(repo); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        vm.onAction(HomeCheckInEditorAction.RevokeAccess); runCurrent()
        repo.saveGate!!.complete(Unit); runCurrent()
        assertNull(withTimeoutOrNull(1) { vm.events.first() })
        assertTrue(vm.state.value.isAccessRevoked)
    }

    @Test fun portCancellationNeverBecomesCatalogueFailure() = runTest(dispatcher) {
        val catalog = Catalog().apply { methodsResult = Result.failure(CancellationException("methods cancelled")) }
        val vm = model(catalog = catalog); runCurrent()
        assertFalse(vm.state.value.methodsLoading); assertNull(vm.state.value.methodsIssue)
    }

    @Test fun blankCustomBeanChoiceDoesNotEraseHistoricalSelectionOrClosePicker() = runTest(dispatcher) {
        val repo = Repository().apply { detailResult = Result.success(entry().copy(coffeeProductId = "historical")) }
        val vm = model(repo, id = "home"); runCurrent()
        vm.onAction(HomeCheckInEditorAction.OpenBeanPicker); runCurrent()
        vm.onAction(HomeCheckInEditorAction.CustomBeanChosen("  ")); runCurrent()
        assertEquals("historical", vm.state.value.draft.coffeeProductId)
        assertTrue(vm.state.value.isBeanPickerOpen)
    }

    @Test fun detailAuthFailureCanBeRetriedForTheSameOwnerAfterTokenRenewal() = runTest(dispatcher) {
        val repo = Repository().apply { detailResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Unauthorized)) }
        val vm = model(repo, id = "home"); runCurrent()
        assertEquals(HomePreparationIssue.Unauthorized, vm.state.value.loadIssue)
        assertFalse(vm.state.value.isAccessRevoked)
        assertFalse(vm.state.value.isReady)
        repo.detailResult = Result.success(entry())
        vm.onAction(HomeCheckInEditorAction.RetryDetail); runCurrent()
        assertTrue(vm.state.value.isReady)
        assertEquals("home", vm.state.value.entry!!.id)
    }

    @Test fun metricsContainOnlyCategoriesAndInitializationAndUnchangedFieldsDoNotDuplicate() = runTest(dispatcher) {
        val metrics = mutableListOf<HomeCheckInEditorMetric>()
        val vm = model(analytics = HomeCheckInEditorAnalytics(metrics::add)); runCurrent()
        vm.act(HomeCheckInEditorAction.Initialize, HomeCheckInEditorAction.NoteChanged("Private coffee note"),
            HomeCheckInEditorAction.NoteChanged("Private coffee note")); runCurrent()
        assertEquals(listOf(HomeCheckInEditorMetric.Opened, HomeCheckInEditorMetric.FieldChanged(HomePreparationField.Note)), metrics)
        assertFalse(metrics.toString().contains("Private coffee note"))
        vm.onAction(HomeCheckInEditorAction.Close); runCurrent()
        store.clear()
        assertEquals(1, metrics.count { it == HomeCheckInEditorMetric.Cancelled })
    }

    @Test fun metricsDistinguishClientServerAndPhotoFailuresAndSuccessIsTerminal() = runTest(dispatcher) {
        val metrics = mutableListOf<HomeCheckInEditorMetric>()
        val repo = Repository()
        val vm = model(repo, analytics = HomeCheckInEditorAnalytics(metrics::add)); runCurrent()
        vm.act(HomeCheckInEditorAction.DoseChanged("0"), HomeCheckInEditorAction.Save); runCurrent()
        assertTrue(repo.creates.isEmpty())
        assertTrue(metrics.contains(HomeCheckInEditorMetric.Validation(setOf(HomePreparationField.Dose), false)))
        repo.saveResult = Result.failure(HomePreparationFailure(HomePreparationIssue.InvalidInput,
            mapOf(HomePreparationField.Bean to HomePreparationIssue.BeanUnavailable)))
        vm.act(HomeCheckInEditorAction.DoseChanged("18"), HomeCheckInEditorAction.Save); runCurrent()
        assertTrue(metrics.contains(HomeCheckInEditorMetric.Validation(setOf(HomePreparationField.Bean), true)))
        repo.saveResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Offline, operation = HomePreparationOperation.PhotoUpload))
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertTrue(metrics.contains(HomeCheckInEditorMetric.Failed(HomeCheckInEditorStage.Photo, HomePreparationIssue.Offline)))
        repo.saveResult = Result.success(entry())
        vm.act(HomeCheckInEditorAction.Save, HomeCheckInEditorAction.Save); runCurrent()
        store.clear()
        assertEquals(1, metrics.count { it is HomeCheckInEditorMetric.Saved })
        assertEquals(0, metrics.count { it == HomeCheckInEditorMetric.Cancelled })
    }

    @Test fun deniedAndRevokedOwnersNeverRecordLateSuccessOrCancellation() = runTest(dispatcher) {
        val metrics = mutableListOf<HomeCheckInEditorMetric>()
        model(access = { false }, analytics = HomeCheckInEditorAnalytics(metrics::add)); runCurrent()
        assertTrue(metrics.isEmpty())
        var allowed = true
        val repo = Repository().apply { saveGate = CompletableDeferred(); ignoreCancellation = true }
        val vm = model(repo, access = { allowed }, analytics = HomeCheckInEditorAnalytics(metrics::add)); runCurrent()
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        val before = metrics.toList()
        allowed = false; vm.onAction(HomeCheckInEditorAction.RevokeAccess); runCurrent()
        repo.saveGate!!.complete(Unit); runCurrent(); store.clear()
        assertEquals(before, metrics)
    }

    @Test fun failedEditLoadDoesNotOpenFlowAndCancelledRequestDoesNotBecomeMetricFailure() = runTest(dispatcher) {
        val metrics = mutableListOf<HomeCheckInEditorMetric>()
        val repo = Repository().apply { detailResult = Result.failure(HomePreparationFailure(HomePreparationIssue.NotFound)) }
        val vm = model(repo, id = "home", analytics = HomeCheckInEditorAnalytics(metrics::add)); runCurrent()
        assertTrue(metrics.isEmpty())
        repo.detailResult = Result.success(entry())
        vm.onAction(HomeCheckInEditorAction.RetryDetail); runCurrent()
        assertEquals(listOf<HomeCheckInEditorMetric>(HomeCheckInEditorMetric.Opened), metrics)
        repo.saveResult = Result.failure(CancellationException("cancelled"))
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertFalse(metrics.any { it is HomeCheckInEditorMetric.Failed })
    }

    @Test fun brokenTelemetryCannotBlockEditingOrTurnAcceptedCreateIntoRetry() = runTest(dispatcher) {
        val repo = Repository()
        val vm = model(repo, analytics = HomeCheckInEditorAnalytics { error("Unavailable metrics SDK") }); runCurrent()
        assertTrue(vm.state.value.isReady)
        vm.act(HomeCheckInEditorAction.NoteChanged("Private"), HomeCheckInEditorAction.Save); runCurrent()
        assertTrue(vm.state.value.isClosed)
        assertNull(vm.state.value.saveIssue)
        assertEquals(HomeCheckInEditorEvent.Saved("home", false), vm.events.first().event)
        vm.onAction(HomeCheckInEditorAction.Save); runCurrent()
        assertEquals(1, repo.creates.size)
    }

    companion object {
        private fun entry() = HomePreparation("home", "2026-10-16", "2026-10-16T07:30:00Z", "2026-10-16T07:30:00Z",
            "Private note", "hario-v60", null, "Эфиопия", "18", 180, 2, "Фильтр", "filter", null,
            emptyList(), HomePreparationVisibility.Private, HomePreparationModeration.NotSubmitted)
        private fun photo(name: String) = HomePreparationUpload(name, "image/jpeg", name.encodeToByteArray())
    }
}
