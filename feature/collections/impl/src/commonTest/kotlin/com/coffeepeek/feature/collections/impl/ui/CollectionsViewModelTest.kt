package com.coffeepeek.feature.collections.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val shop = CollectionPlace(CollectionPlaceKind.CoffeeShop, "same", "Shop")
    private val roaster = CollectionPlace(CollectionPlaceKind.Roaster, "same", "Roaster")
    private val account = Account()
    private val repository = Repository()
    @BeforeTest fun setup() = Dispatchers.setMain(dispatcher)
    @AfterTest fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model(id: String? = null) = CollectionsViewModel(repository, account, id).also { store.put("collections", it) }
    private fun publish(model: CollectionsViewModel) { model.onAction(CollectionsAction.SelectAll); model.onAction(CollectionsAction.Create("Title")) }

    @Test fun selectingBothKindsWithSameSlugCreatesExactlyRequestedOrder() = runTest(dispatcher) {
        repository.places = listOf(shop, roaster)
        val model = model(); runCurrent()
        model.onAction(CollectionsAction.Toggle(roaster.key)); model.onAction(CollectionsAction.Toggle(shop.key)); runCurrent()
        model.onAction(CollectionsAction.Create("Default")); runCurrent()
        assertEquals(listOf(shop, roaster), repository.sent)
        assertEquals(CollectionsPage.Preview, model.state.value.page)
        assertFalse(model.state.value.busy)
    }

    @Test fun noImplicitPublicationAndDuplicateTapsDoNotCreateTwoLinks() = runTest(dispatcher) {
        repository.places = listOf(shop)
        repository.createGate = CompletableDeferred()
        val model = model(); runCurrent()
        assertEquals(0, repository.creates)
        assertTrue(model.state.value.selected.isEmpty())
        publish(model); runCurrent()
        model.onAction(CollectionsAction.Create("Duplicate")); runCurrent()
        assertEquals(1, repository.creates)
        assertTrue(model.state.value.busy)
        repository.createGate!!.complete(Unit); runCurrent()
        assertFalse(model.state.value.busy)
    }

    @Test fun failedCreationKeepsSelectionAndCanBeRetried() = runTest(dispatcher) {
        repository.places = listOf(shop); repository.createFailure = IllegalStateException("Offline")
        val model = model(); runCurrent(); publish(model); runCurrent()
        assertEquals(setOf(shop.key), model.state.value.selected)
        assertEquals(CollectionsFailure.Create, model.state.value.failure)
        assertFalse(model.state.value.busy)
        repository.createFailure = null
        model.onAction(CollectionsAction.Create("Retry")); runCurrent()
        assertEquals(CollectionsPage.Preview, model.state.value.page)
    }

    @Test fun switchingAccountClearsSelectionAndRejectsLateNonCooperativeReads() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.favoritesBlock = { withContext(NonCancellable) { gate.await() }; listOf(shop) }
        val model = model(); runCurrent()
        repository.favoritesBlock = { listOf(roaster) }
        account.ids.value = "second"; runCurrent()
        assertEquals(listOf(roaster), model.state.value.places)
        assertTrue(model.state.value.selected.isEmpty())
        gate.complete(Unit); runCurrent()
        assertEquals(listOf(roaster), model.state.value.places)
    }

    @Test fun guestCanReadPublicCollectionWithoutReadingPrivateFavorites() = runTest(dispatcher) {
        account.ids.value = null
        val model = model("public"); runCurrent()
        assertTrue(model.state.value.publicView)
        assertNotNull(model.state.value.collection)
        assertEquals(0, repository.favoriteReads)
        assertEquals(0, repository.creates)
    }

    @Test fun signOutClearsCreatedCollectionAndPrivateLinks() = runTest(dispatcher) {
        repository.places = listOf(shop)
        val model = model(); runCurrent(); publish(model); runCurrent()
        assertNotNull(model.state.value.collection)
        account.ids.value = null; runCurrent()
        assertNull(model.state.value.collection)
        assertTrue(model.state.value.places.isEmpty())
        assertTrue(model.state.value.links.isEmpty())
        assertFalse(model.state.value.signedIn)
    }

    @Test fun revokeUsesOwnedListAndDoesNotOptimisticallyEraseFailedRequests() = runTest(dispatcher) {
        val model = model(); runCurrent(); model.onAction(CollectionsAction.OpenLinks); runCurrent()
        model.onAction(CollectionsAction.Revoke("foreign")); runCurrent()
        assertEquals(0, repository.revocations)
        repository.revokeFailure = true
        model.onAction(CollectionsAction.Revoke("owned")); runCurrent()
        assertEquals(1, model.state.value.links.size)
        assertEquals(CollectionsFailure.Revoke, model.state.value.failure)
        repository.revokeFailure = false
        model.onAction(CollectionsAction.Revoke("owned")); runCurrent()
        assertTrue(model.state.value.links.isEmpty())
    }

    @Test fun cancellationReleasesBusyStateWithoutTurningIntoSuccess() = runTest(dispatcher) {
        repository.places = listOf(shop); repository.createFailure = CancellationException("Cancelled")
        val model = model(); runCurrent(); publish(model); runCurrent()
        assertFalse(model.state.value.busy)
        assertNull(model.state.value.collection)
        assertEquals(CollectionsPage.Selection, model.state.value.page)
    }

    @Test fun cancelledReadForUnchangedAccountShowsLoadFailureInsteadOfAnEmptyFavoritesMessage() = runTest(dispatcher) {
        repository.favoritesBlock = { throw CancellationException("Read cancelled") }
        val model = model()
        runCurrent()
        assertFalse(model.state.value.loading)
        assertEquals(CollectionsFailure.Load, model.state.value.failure)
    }

    @Test fun previousAccountExportCannotReleaseOrShareANewAccountExport() = runTest(dispatcher) {
        repository.places = listOf(shop)
        val model = model()
        runCurrent()
        val previous = startExport(model)
        account.ids.value = "second"
        runCurrent()
        val current = startExport(model)
        assertFalse(model.isCurrentExport(previous.generation, previous.collection.id))
        model.onAction(CollectionsAction.ExportFinished(previous.generation, previous.collection.id, true))
        runCurrent()
        assertTrue(model.state.value.busy)
        assertNull(model.state.value.failure)
        assertTrue(model.isCurrentExport(current.generation, current.collection.id))
    }

    @Test fun backCancelsTheExportAndReturnsToSelection() = runTest(dispatcher) {
        repository.places = listOf(shop)
        val model = model()
        runCurrent()
        val export = startExport(model)
        model.onAction(CollectionsAction.Back)
        runCurrent()
        assertFalse(model.isCurrentExport(export.generation, export.collection.id))
        assertFalse(model.state.value.busy)
        assertEquals(CollectionsPage.Selection, model.state.value.page)
    }

    private suspend fun TestScope.startExport(model: CollectionsViewModel): CollectionsEvent.ExportImages {
        publish(model)
        runCurrent()
        model.onAction(CollectionsAction.ShareImages)
        runCurrent()
        return model.events.first() as CollectionsEvent.ExportImages
    }

    private class Account : CollectionsAccount {
        val ids = MutableStateFlow<String?>("first")
        override val identities = ids
        override fun currentId() = ids.value
        override suspend fun loadId() = currentId()
    }

    private class Repository : CollectionsRepository {
        var places = emptyList<CollectionPlace>()
        var sent = emptyList<CollectionPlace>()
        var creates = 0
        var favoriteReads = 0
        var revocations = 0
        var createGate: CompletableDeferred<Unit>? = null
        var createFailure: Throwable? = null
        var revokeFailure = false
        var favoritesBlock: (suspend () -> List<CollectionPlace>)? = null
        override suspend fun favorites(): Result<List<CollectionPlace>> { favoriteReads++; return Result.success(favoritesBlock?.invoke() ?: places) }
        override suspend fun create(title: String, places: List<CollectionPlace>): Result<SharedCollection> {
            creates++; sent = places.toList(); createGate?.await()
            return createFailure?.let { Result.failure(it) } ?: Result.success(SharedCollection("created", title, "https://coffeepeek.by", places))
        }
        override suspend fun read(id: String) = Result.success(SharedCollection(id, "Public", "https://coffeepeek.by", places))
        override suspend fun mine() = Result.success(listOf(CollectionSummary("owned", "Owned", "https://coffeepeek.by", 1)))
        override suspend fun revoke(id: String): Result<Unit> { revocations++; return if (revokeFailure) Result.failure(IllegalStateException()) else Result.success(Unit) }
        override suspend fun photo(url: String): ByteArray? = null
    }
}
