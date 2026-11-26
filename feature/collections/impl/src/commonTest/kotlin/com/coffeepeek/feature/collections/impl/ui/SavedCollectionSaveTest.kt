package com.coffeepeek.feature.collections.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCollectionSaveTest {
    private val dispatcher = StandardTestDispatcher()
    private val account = SavedTestAccount()
    private var accountLifetime = 0
    private val saveAccount = object : CollectionsAccount by account {
        override fun mediaScope() = account.currentId()?.let { "$it:$accountLifetime" }
    }
    private val saved = SavedTestRepository()
    private val store = ViewModelStore()
    private var nextModel = 0

    @BeforeTest fun before() {
        Dispatchers.setMain(dispatcher)
        saved.currentOwner = account::currentId
    }

    @AfterTest fun after() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun creationFinishesOnceOnlyAfterTheWriteSucceeds() = runTest(dispatcher) {
        val model = editor()
        val events = observe(model)
        saved.createGate = CompletableDeferred()
        model.onAction(SavedCollectionsAction.Title("My list"))
        model.onAction(SavedCollectionsAction.Save)
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertTrue(events.saved().isEmpty())
        assertEquals(1, saved.creates)
        saved.createGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, events.saved().size)
        assertTrue(model.isCurrentSave(events.saved().single()))
    }

    @Test fun metadataEditFinishesWithoutRewritingUnchangedItems() = runTest(dispatcher) {
        val model = editor("private")
        val events = observe(model)
        model.onAction(SavedCollectionsAction.Title("Edited"))
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals("Edited", model.state.value.title)
        assertEquals(1, saved.updates)
        assertEquals(0, saved.itemWrites)
        assertEquals(1, events.saved().size)
    }

    @Test fun editingItemsWaitsForBothMetadataAndItemWrites() = runTest(dispatcher) {
        val model = editor("private")
        val events = observe(model)
        saved.itemGate = CompletableDeferred()
        model.onAction(SavedCollectionsAction.TogglePlace(savedShop.key))
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(1, saved.updates)
        assertEquals(1, saved.itemWrites)
        assertTrue(events.saved().isEmpty())
        saved.itemGate!!.complete(Unit)
        runCurrent()
        assertTrue(model.state.value.places.isEmpty())
        assertEquals(1, events.saved().size)
    }

    @Test fun failedCreationKeepsTheEditor() = runTest(dispatcher) {
        saved.createFailure = IllegalStateException("Offline")
        assertFailedSave()
    }

    @Test fun metadataConflictKeepsTheEditor() = runTest(dispatcher) {
        saved.updateFailure = SavedCollectionConflict()
        assertFailedSave("private", SavedCollectionsFailure.Conflict)
    }

    @Test fun cancelledWriteDoesNotFinishTheEditor() = runTest(dispatcher) {
        saved.createFailure = CancellationException("Cancelled")
        assertFailedSave()
    }

    @Test fun failedCoverUploadKeepsTheDraftAndDoesNotFinish() = runTest(dispatcher) {
        val model = editor()
        val events = observe(model)
        saved.uploadFailure = true
        model.onAction(SavedCollectionsAction.Title("With cover"))
        model.onAction(SavedCollectionsAction.CoverPicked(
            CollectionCoverInput(byteArrayOf(1), "image/jpeg", "cover.jpg"), model.state.value.accountVersion))
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(0, saved.creates)
        assertNotNull(model.state.value.coverInput)
        assertEquals(SavedCollectionsFailure.Save, model.state.value.failure)
        assertTrue(events.saved().isEmpty())
    }

    @Test fun failedItemWriteKeepsTheSelectionAndFinishesOnlyAfterRetry() = runTest(dispatcher) {
        val model = editor("private")
        val events = observe(model)
        saved.itemFailure = IllegalStateException("Offline")
        model.onAction(SavedCollectionsAction.TogglePlace(savedShop.key))
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(SavedCollectionsFailure.Save, model.state.value.failure)
        assertTrue(model.state.value.selectedPlaces.isEmpty())
        assertTrue(events.saved().isEmpty())
        saved.itemFailure = null
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(2, saved.itemWrites)
        assertEquals(1, events.saved().size)
    }

    @Test fun lateSuccessAfterBackDoesNotFinishAgain() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = editor(repository = delayedCreation(gate))
        val events = observe(model)
        startCreation(model)
        model.onAction(SavedCollectionsAction.Back)
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertTrue(SavedCollectionsEvent.Back in events)
        assertTrue(events.saved().isEmpty())
    }

    @Test fun lateSuccessCannotNavigateTheNextAccount() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = editor(repository = delayedCreation(gate))
        val events = observe(model)
        startCreation(model)
        account.owner.value = "next"
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertTrue(events.saved().isEmpty())
    }

    @Test fun bufferedCompletionIsInvalidAfterBackOrAnAccountChange() = runTest(dispatcher) {
        val model = editor()
        val events = observe(model)
        startCreation(model)
        val completion = events.saved().single()
        account.owner.value = "next"
        assertFalse(model.isCurrentSave(completion))
        runCurrent()
        model.onAction(SavedCollectionsAction.Back)
        runCurrent()
        assertFalse(model.isCurrentSave(completion))
    }

    @Test fun bufferedCompletionRejectsAnAccountRoundTripBeforeCollectorsResume() = runTest(dispatcher) {
        val model = editor()
        val events = observe(model)
        startCreation(model)
        val completion = events.saved().single()
        val epoch = model.state.value.accountVersion
        changeLifetime("second")
        changeLifetime("first")
        assertEquals(epoch, model.state.value.accountVersion)
        assertFalse(model.isCurrentSave(completion))
        runCurrent()
        assertFalse(model.isCurrentSave(completion))
    }

    @Test fun bufferedCompletionRejectsANewLifetimeForTheSameIdentity() = runTest(dispatcher) {
        val model = editor()
        val events = observe(model)
        startCreation(model)
        val completion = events.saved().single()
        changeLifetime("first")
        assertFalse(model.isCurrentSave(completion))
    }

    @Test fun lateCreationAfterAConflatedAccountRoundTripDoesNotEmitCompletion() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = editor(repository = delayedCreation(gate))
        val events = observe(model)
        startCreation(model)
        changeLifetime("second")
        changeLifetime("first")
        gate.complete(Unit)
        runCurrent()
        assertTrue(events.saved().isEmpty())
    }

    @Test fun aNewSameIdentityLifetimeDuringItemWriteDoesNotEmitCompletion() = runTest(dispatcher) {
        val model = editor("private")
        val events = observe(model)
        saved.itemGate = CompletableDeferred()
        model.onAction(SavedCollectionsAction.TogglePlace(savedShop.key))
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
        changeLifetime("first")
        saved.itemGate!!.complete(Unit)
        runCurrent()
        assertTrue(events.saved().isEmpty())
    }

    private fun changeLifetime(owner: String) {
        accountLifetime++
        account.owner.value = owner
    }

    private fun TestScope.editor(id: String? = null, repository: SavedCollectionsRepository = saved): SavedCollectionsViewModel {
        val model = SavedCollectionsViewModel(repository, SavedTestSnapshots(), saveAccount, SavedCollectionsMode.Editor, id)
        store.put("editor:${nextModel++}", model)
        runCurrent()
        return model
    }

    private fun TestScope.observe(model: SavedCollectionsViewModel): MutableList<SavedCollectionsEvent> {
        val events = mutableListOf<SavedCollectionsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.collect { events += it } }
        return events
    }

    private fun List<SavedCollectionsEvent>.saved() = filterIsInstance<SavedCollectionsEvent.Saved>()

    private fun TestScope.startCreation(model: SavedCollectionsViewModel) {
        model.onAction(SavedCollectionsAction.Title("My list"))
        model.onAction(SavedCollectionsAction.Save)
        runCurrent()
    }

    private fun TestScope.assertFailedSave(id: String? = null, failure: SavedCollectionsFailure = SavedCollectionsFailure.Save) {
        val model = editor(id)
        val events = observe(model)
        startCreation(model)
        assertEquals(failure, model.state.value.failure)
        assertEquals("My list", model.state.value.title)
        assertFalse(model.state.value.busy)
        assertTrue(events.saved().isEmpty())
    }

    private fun delayedCreation(gate: CompletableDeferred<Unit>): SavedCollectionsRepository = object : SavedCollectionsRepository by saved {
        override suspend fun create(metadata: SavedCollectionMetadata): Result<SavedCollection> = withContext(NonCancellable) {
            gate.await()
            saved.create(metadata)
        }
    }
}
