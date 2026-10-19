package com.coffeepeek.feature.homecheckin.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeCheckInDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class Repository : HomePreparationRepository {
        var detailCalls = 0
        var deleteCalls = 0
        var unexpectedCalls = 0
        var detailResult = Result.success(entry())
        var deleteResult = Result.success(Unit)
        var detailGate: CompletableDeferred<Unit>? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false

        override suspend fun detail(id: String): Result<HomePreparation> {
            assertEquals("home", id)
            detailCalls++
            val result = detailResult
            val gate = detailGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }

        override suspend fun delete(id: String): Result<Unit> {
            assertEquals("home", id)
            deleteCalls++
            val result = deleteResult
            val gate = deleteGate
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            return result
        }

        override suspend fun create(input: HomePreparationInput): Result<HomePreparation> = unexpected()
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> = unexpected()
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = unexpected()
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = unexpected()
        private fun unexpected(): Nothing { unexpectedCalls++; error("Detail must not load history or write an editor draft") }
    }

    private fun model(repository: Repository = Repository(), id: String = "home", access: () -> Boolean = { true }) =
        HomeCheckInDetailViewModel(id, repository, 7, access).also { store.put("detail-${nextKey++}", it) }

    @Test fun initialLoadFetchesOnlyDetailAndPreservesAllReturnedFields() = runTest(dispatcher) {
        val repository = Repository()
        val model = model(repository)
        runCurrent()
        assertEquals(entry(), model.state.value.entry)
        assertFalse(model.state.value.isLoading)
        assertEquals(1, repository.detailCalls)
        assertEquals(0, repository.unexpectedCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun duplicateLoadsAreIgnoredWhileTheCurrentRequestRuns() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { detailGate = gate }
        val model = model(repository)
        runCurrent()
        repeat(3) { model.onAction(HomeCheckInDetailAction.Reload) }
        runCurrent()
        assertTrue(model.state.value.isLoading)
        assertEquals(1, repository.detailCalls)
        gate.complete(Unit); runCurrent()
        assertEquals(entry(), model.state.value.entry)
    }

    @Test fun blankIdDoesNotReachRepository() = runTest(dispatcher) {
        val repository = Repository()
        val model = model(repository, id = " ")
        runCurrent()
        assertEquals(HomePreparationIssue.InvalidInput, model.state.value.loadIssue)
        assertEquals(0, repository.detailCalls)
    }

    @Test fun typedFailureCanBeRetriedWithoutLosingItsReason() = runTest(dispatcher) {
        val repository = Repository().apply {
            detailResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Offline))
        }
        val model = model(repository)
        runCurrent()
        assertEquals(HomePreparationIssue.Offline, model.state.value.loadIssue)
        assertNull(model.state.value.entry)
        repository.detailResult = Result.success(entry())
        model.onAction(HomeCheckInDetailAction.Reload); runCurrent()
        assertNull(model.state.value.loadIssue)
        assertEquals(entry(), model.state.value.entry)
        assertEquals(2, repository.detailCalls)
    }

    @Test fun unknownFailureCannotExposeRawExceptionText() = runTest(dispatcher) {
        val repository = Repository().apply { detailResult = Result.failure(IllegalStateException("private SQL payload")) }
        val model = model(repository)
        runCurrent()
        assertEquals(HomePreparationIssue.RequestFailed, model.state.value.loadIssue)
        assertNull(model.state.value.entry)
    }

    @Test fun mismatchedDetailIdIsNeverRendered() = runTest(dispatcher) {
        val repository = Repository().apply { detailResult = Result.success(entry().copy(id = "other")) }
        val model = model(repository)
        runCurrent()
        assertNull(model.state.value.entry)
        assertEquals(HomePreparationIssue.InvalidResponse, model.state.value.loadIssue)
    }

    @Test fun offlineRefreshRetainsContentButAccessOrMissingFailuresEraseIt() = runTest(dispatcher) {
        for (issue in listOf(HomePreparationIssue.Forbidden, HomePreparationIssue.Unauthorized, HomePreparationIssue.NotFound)) {
            val repository = Repository()
            val model = model(repository)
            runCurrent()
            repository.detailResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Offline))
            model.onAction(HomeCheckInDetailAction.Reload); runCurrent()
            assertEquals(entry(), model.state.value.entry)
            repository.detailResult = Result.failure(HomePreparationFailure(issue))
            model.onAction(HomeCheckInDetailAction.Reload); runCurrent()
            assertNull(model.state.value.entry)
            assertEquals(issue, model.state.value.loadIssue)
        }
    }

    @Test fun closeCancelsANonCooperativeLoadAndCannotBeUndoneByItsLateResult() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { detailGate = gate; ignoreCancellation = true }
        val model = model(repository)
        runCurrent()
        model.onAction(HomeCheckInDetailAction.Close); runCurrent()
        val close = model.events.first()
        assertEquals(HomeCheckInDetailEvent.Close, close.event)
        assertTrue(model.canDeliver(close))
        gate.complete(Unit); runCurrent()
        assertNull(model.state.value.entry)
        assertTrue(model.state.value.isClosed)
        model.onAction(HomeCheckInDetailAction.Reload); runCurrent()
        assertEquals(1, repository.detailCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun editorAndPhotoEventsUseOnlyTheLoadedRecord() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(HomeCheckInDetailAction.Edit); runCurrent()
        assertEquals(HomeCheckInDetailEvent.Edit("home"), model.events.first().event)
        model.onAction(HomeCheckInDetailAction.OpenPhoto(1)); runCurrent()
        assertEquals(HomeCheckInDetailEvent.OpenPhoto(listOf("original-a", "original-b"), 1), model.events.first().event)
        model.onAction(HomeCheckInDetailAction.OpenDrinkPhoto); runCurrent()
        assertEquals(HomeCheckInDetailEvent.OpenPhoto(listOf("cutout"), 0), model.events.first().event)
    }

    @Test fun detailAndOriginalAlbumsStayAlignedEvenWhenAPreviewIsUnavailable() = runTest(dispatcher) {
        val repository = Repository().apply { detailResult = Result.success(entry().copy(
            photos = listOf(HomePreparationPhoto("a", "original-a", detailUrl = "detail-a"),
                HomePreparationPhoto("b", "original-b", detailUrl = "")),
            drinkPhotoUrl = "cutout", drinkPhotoDetailUrl = "cutout-detail")) }
        val model = model(repository)
        runCurrent()
        model.onAction(HomeCheckInDetailAction.OpenPhoto(1)); runCurrent()
        assertEquals(HomeCheckInDetailEvent.OpenPhoto(listOf("detail-a", ""), 1,
            listOf("original-a", "original-b")), model.events.first().event)
        model.onAction(HomeCheckInDetailAction.OpenDrinkPhoto); runCurrent()
        assertEquals(HomeCheckInDetailEvent.OpenPhoto(listOf("cutout-detail"), 0, listOf("cutout")),
            model.events.first().event)
        assertEquals(1, repository.detailCalls)
    }

    @Test fun invalidPhotoIndicesAndBlankCutoutDoNotEmitEffects() = runTest(dispatcher) {
        val repository = Repository().apply {
            detailResult = Result.success(entry().copy(drinkPhotoUrl = " ", photos = listOf(HomePreparationPhoto("blank", ""))))
        }
        val model = model(repository)
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        model.onAction(HomeCheckInDetailAction.OpenPhoto(-1))
        model.onAction(HomeCheckInDetailAction.OpenPhoto(0))
        model.onAction(HomeCheckInDetailAction.OpenDrinkPhoto)
        runCurrent()
        assertTrue(events.isEmpty())
    }

    @Test fun aConfirmationIsRequiredAndCancelNeverWrites() = runTest(dispatcher) {
        val repository = Repository()
        val model = model(repository)
        runCurrent()
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertEquals(0, repository.deleteCalls)
        model.onAction(HomeCheckInDetailAction.RequestDelete); runCurrent()
        assertTrue(model.state.value.showDeleteConfirmation)
        model.onAction(HomeCheckInDetailAction.CancelDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertFalse(model.state.value.showDeleteConfirmation)
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun confirmationBlocksBackgroundEditPhotoAndReloadActions() = runTest(dispatcher) {
        val repository = Repository()
        val model = model(repository)
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        model.onAction(HomeCheckInDetailAction.RequestDelete); runCurrent()
        model.onAction(HomeCheckInDetailAction.Reload)
        model.onAction(HomeCheckInDetailAction.Edit)
        model.onAction(HomeCheckInDetailAction.OpenPhoto(0))
        runCurrent()
        assertEquals(1, repository.detailCalls)
        assertTrue(events.isEmpty())
    }

    @Test fun duplicateDeleteAndConflictingActionsCannotStartMoreWorkOrDismissEarly() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { deleteGate = gate }
        val model = model(repository)
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        model.onAction(HomeCheckInDetailAction.RequestDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        model.onAction(HomeCheckInDetailAction.ConfirmDelete)
        model.onAction(HomeCheckInDetailAction.Close)
        model.onAction(HomeCheckInDetailAction.Edit)
        model.onAction(HomeCheckInDetailAction.Reload)
        model.onAction(HomeCheckInDetailAction.CancelDelete)
        runCurrent()
        assertEquals(1, repository.deleteCalls)
        assertEquals(1, repository.detailCalls)
        assertTrue(model.state.value.isDeleting)
        assertFalse(model.state.value.isClosed)
        assertTrue(events.isEmpty())
        gate.complete(Unit); runCurrent()
        assertEquals(listOf(HomeCheckInDetailEvent.Deleted("home")), events.map { it.event })
        assertNull(model.state.value.entry)
        assertTrue(model.state.value.isClosed)
        assertFalse(model.state.value.isDeleting)
        assertTrue(model.canDeliver(events.single()))
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertEquals(1, repository.deleteCalls)
    }

    @Test fun failedDeleteRetainsDetailAndNeedsNewExplicitConfirmationToRetry() = runTest(dispatcher) {
        val repository = Repository().apply { deleteResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Timeout)) }
        val model = model(repository)
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        model.onAction(HomeCheckInDetailAction.RequestDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertEquals(entry(), model.state.value.entry)
        assertEquals(HomePreparationIssue.Timeout, model.state.value.deleteIssue)
        assertFalse(model.state.value.isDeleting)
        assertTrue(events.isEmpty())
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertEquals(1, repository.deleteCalls)
        repository.deleteResult = Result.success(Unit)
        model.onAction(HomeCheckInDetailAction.RequestDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertEquals(2, repository.deleteCalls)
        assertEquals(1, events.size)
    }

    @Test fun forbiddenDeleteErasesContentWithoutClaimingDeletionSucceeded() = runTest(dispatcher) {
        val repository = Repository().apply { deleteResult = Result.failure(HomePreparationFailure(HomePreparationIssue.Forbidden)) }
        val model = model(repository)
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        model.onAction(HomeCheckInDetailAction.RequestDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertNull(model.state.value.entry)
        assertEquals(HomePreparationIssue.Forbidden, model.state.value.deleteIssue)
        assertFalse(model.state.value.isClosed)
        assertTrue(events.isEmpty())
    }

    @Test fun cancellationInResultDoesNotBecomeALoadOrDeleteError() = runTest(dispatcher) {
        val repository = Repository().apply { detailResult = Result.failure(CancellationException("cancelled")) }
        val model = model(repository)
        runCurrent()
        assertNull(model.state.value.loadIssue)
        assertFalse(model.state.value.isLoading)
        repository.detailResult = Result.success(entry())
        model.onAction(HomeCheckInDetailAction.Reload); runCurrent()
        repository.deleteResult = Result.failure(CancellationException("cancelled delete"))
        model.onAction(HomeCheckInDetailAction.RequestDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertNull(model.state.value.deleteIssue)
        assertFalse(model.state.value.isDeleting)
        assertEquals(entry(), model.state.value.entry)
        assertEquals(1, repository.deleteCalls)
    }

    @Test fun revokedInitialAccessNeverReadsOrWrites() = runTest(dispatcher) {
        val repository = Repository()
        val model = model(repository, access = { false })
        runCurrent()
        model.onAction(HomeCheckInDetailAction.Edit)
        model.onAction(HomeCheckInDetailAction.RequestDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        assertTrue(model.state.value.isAccessRevoked)
        assertNull(model.state.value.entry)
        assertEquals(0, repository.detailCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun accessLossBeforeAnActionErasesContentInsteadOfEmittingIt() = runTest(dispatcher) {
        var current = true
        val model = model(access = { current })
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        current = false
        model.onAction(HomeCheckInDetailAction.Edit); runCurrent()
        assertNull(model.state.value.entry)
        assertTrue(model.state.value.isAccessRevoked)
        assertTrue(events.isEmpty())
    }

    @Test fun explicitRevocationRejectsNonCooperativeLateDetailAndCannotReuseTheOldOwner() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { detailGate = gate; ignoreCancellation = true }
        val model = model(repository)
        runCurrent()
        model.onAction(HomeCheckInDetailAction.RevokeAccess); runCurrent()
        gate.complete(Unit); runCurrent()
        assertNull(model.state.value.entry)
        assertTrue(model.state.value.isAccessRevoked)
        model.onAction(HomeCheckInDetailAction.Reload); runCurrent()
        assertEquals(1, repository.detailCalls)
    }

    @Test fun accessLostDuringDeleteCannotEmitASuccessForTheReplacementAccount() = runTest(dispatcher) {
        var current = true
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { deleteGate = gate; ignoreCancellation = true }
        val model = model(repository, access = { current })
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        model.onAction(HomeCheckInDetailAction.RequestDelete)
        model.onAction(HomeCheckInDetailAction.ConfirmDelete); runCurrent()
        current = false
        gate.complete(Unit); runCurrent()
        assertTrue(model.state.value.isAccessRevoked)
        assertNull(model.state.value.entry)
        assertTrue(events.isEmpty())
    }

    @Test fun closeRefreshAndRevocationInvalidateAlreadyBufferedEffects() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(HomeCheckInDetailAction.OpenPhoto(0)); runCurrent()
        val photo = model.events.first()
        assertTrue(model.canDeliver(photo))
        assertFalse(model.canDeliver(photo.copy(sessionGeneration = 8)))
        model.onAction(HomeCheckInDetailAction.Reload); runCurrent()
        assertFalse(model.canDeliver(photo))
        model.onAction(HomeCheckInDetailAction.Edit); runCurrent()
        val edit = model.events.first()
        model.onAction(HomeCheckInDetailAction.Close); runCurrent()
        assertFalse(model.canDeliver(edit))
        val close = model.events.first()
        assertTrue(model.canDeliver(close))
        model.onAction(HomeCheckInDetailAction.RevokeAccess); runCurrent()
        assertFalse(model.canDeliver(close))
    }

    @Test fun clearingLifecycleOwnerCancelsWorkAndErasesThePrivateSnapshot() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        assertNotNull(model.state.value.entry)
        store.clear(); runCurrent()
        assertTrue(model.state.value.isAccessRevoked)
        assertNull(model.state.value.entry)
    }

    @Test fun confirmationInvalidatesBufferedNavigationBeforeTheDialogIsCancelledOrConfirmed() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(HomeCheckInDetailAction.OpenPhoto(0)); runCurrent()
        val photo = model.events.first()
        assertTrue(model.canDeliver(photo))
        model.onAction(HomeCheckInDetailAction.RequestDelete); runCurrent()
        assertFalse(model.canDeliver(photo))
        model.onAction(HomeCheckInDetailAction.CancelDelete); runCurrent()
        assertFalse(model.canDeliver(photo))
    }

    @Test fun leaseLossRejectsBufferedEventsEvenBeforeTheRevocationActionRuns() = runTest(dispatcher) {
        var current = true
        val model = model(access = { current })
        runCurrent()
        model.onAction(HomeCheckInDetailAction.Edit); runCurrent()
        val edit = model.events.first()
        current = false
        assertFalse(model.canDeliver(edit))
        model.onAction(HomeCheckInDetailAction.RevokeAccess); runCurrent()
        assertNull(model.state.value.entry)
    }

    private companion object {
        fun entry() = HomePreparation(
            id = "home", preparedOn = "2026-10-17", preparedAtUtc = "2026-10-17T08:00:00Z",
            createdAtUtc = "2026-10-17T08:02:00Z", text = "Fake note", brewMethodCode = "v60",
            coffeeProductId = "historical-bean", beanName = "Fake bean", doseGrams = "18.5",
            brewDurationSeconds = 195, tasteScore = 4, drinkName = "Fake drink", drinkSlug = "other",
            customDrinkName = "Fake drink", photos = listOf(HomePreparationPhoto("a", "original-a"),
                HomePreparationPhoto("b", "original-b")), drinkPhotoUrl = "cutout",
            visibility = null, moderation = HomePreparationModeration.Unknown,
        )
    }
}
